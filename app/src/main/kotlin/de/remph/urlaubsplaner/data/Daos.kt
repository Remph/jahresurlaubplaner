package de.remph.urlaubsplaner.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SETTINGS_ID}")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SETTINGS_ID}")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM year_entitlement WHERE year = :year")
    fun observe(year: Int): Flow<YearEntitlementEntity?>

    @Upsert
    suspend fun upsert(entitlement: YearEntitlementEntity)

    @Query("DELETE FROM year_entitlement WHERE year = :year")
    suspend fun delete(year: Int)

    @Query("SELECT * FROM year_entitlement ORDER BY year")
    suspend fun getAll(): List<YearEntitlementEntity>

    @Upsert
    suspend fun upsertAll(entitlements: List<YearEntitlementEntity>)

    @Query("DELETE FROM year_entitlement")
    suspend fun deleteAll()
}

@Dao
interface MarkedDayDao {
    /** [from] and [to] are ISO dates, which sort like dates; both ends are included. */
    @Query("SELECT * FROM marked_day WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(
        from: String,
        to: String,
    ): Flow<List<MarkedDayEntity>>

    @Upsert
    suspend fun upsert(day: MarkedDayEntity)

    @Query("DELETE FROM marked_day WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM marked_day WHERE date IN (:dates)")
    suspend fun deleteDates(dates: List<String>)

    @Query("SELECT * FROM marked_day ORDER BY date")
    suspend fun getAll(): List<MarkedDayEntity>

    @Upsert
    suspend fun upsertAll(days: List<MarkedDayEntity>)

    @Query("DELETE FROM marked_day")
    suspend fun deleteAll()
}
