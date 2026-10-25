package de.remph.urlaubsplaner.data

import androidx.room.withTransaction
import de.remph.urlaubsplaner.core.backup.BackupData
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.YearEntitlement
import de.remph.urlaubsplaner.core.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The user's data as `:core` types. Screens and the widget talk to this and never see the
 * entities. The flows send the current value first and again after every change.
 */
class UserDataRepository(
    private val database: UrlaubsplanerDatabase,
) {
    private val settingsDao = database.settingsDao()
    private val entitlementDao = database.entitlementDao()
    private val markedDayDao = database.markedDayDao()

    fun observeSettings(): Flow<UserSettings> =
        settingsDao.observeSettings().map {
            it.toUserSettings()
        }

    suspend fun saveSettings(settings: UserSettings) = settingsDao.upsert(settings.toEntity())

    /** Null when the user has not entered the numbers for [year] yet. */
    fun observeEntitlement(year: Int): Flow<YearEntitlement?> =
        entitlementDao.observe(year).map { it?.toYearEntitlement() }

    suspend fun saveEntitlement(entitlement: YearEntitlement) =
        entitlementDao.upsert(entitlement.toEntity())

    suspend fun deleteEntitlement(year: Int) = entitlementDao.delete(year)

    /** The marks from [from] to [to], both included. */
    fun observeMarks(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Map<LocalDate, DayMark>> =
        markedDayDao.observeBetween(from.toString(), to.toString()).map {
            it.toMarks()
        }

    /** Marking a day that is already marked replaces the mark (full to half and back). */
    suspend fun mark(
        date: LocalDate,
        mark: DayMark,
    ) = markedDayDao.upsert(markedDayEntity(date, mark))

    suspend fun unmark(date: LocalDate) = markedDayDao.delete(date.toString())

    /**
     * Everything the user has entered, for the backup file. A row that cannot be read (a damaged
     * value) is left out here as it is everywhere else in the app.
     */
    suspend fun readAll(): BackupData =
        database.withTransaction {
            BackupData(
                settings = settingsDao.get().toUserSettings(),
                entitlements = entitlementDao.getAll().mapNotNull { it.toYearEntitlement() },
                marks = markedDayDao.getAll().toMarks(),
            )
        }

    /**
     * Replaces everything with [data] in one transaction: afterwards the app holds exactly what
     * the backup holds, and if anything fails it still holds what it held before. Screens show
     * [readAll]'s summary next to the file's before they call this.
     */
    suspend fun replaceAll(data: BackupData) =
        database.withTransaction {
            settingsDao.upsert(data.settings.toEntity())
            entitlementDao.deleteAll()
            entitlementDao.upsertAll(data.entitlements.map { it.toEntity() })
            markedDayDao.deleteAll()
            markedDayDao.upsertAll(data.marks.map { (date, mark) -> markedDayEntity(date, mark) })
        }
}
