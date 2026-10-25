package de.remph.urlaubsplaner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.balance.WorkSchedule
import de.remph.urlaubsplaner.core.balance.YearEntitlement
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.holidays.RegionalOptOut
import de.remph.urlaubsplaner.core.settings.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Runs Room on the JVM through Robolectric, with a real SQLite database in memory. */
@RunWith(AndroidJUnit4::class)
class UserDataRepositoryTest {
    private lateinit var database: UrlaubsplanerDatabase
    private lateinit var repository: UserDataRepository
    private val year2026 = LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31)

    @Before
    fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrlaubsplanerDatabase::class.java).build()
        repository = UserDataRepository(database)
    }

    @After
    fun close() = database.close()

    private suspend fun marks() =
        repository.observeMarks(year2026.start, year2026.endInclusive).first()

    @Test
    fun `settings are the defaults until saved`() =
        runBlocking {
            assertEquals(UserSettings(), repository.observeSettings().first())
        }

    @Test
    fun `saved settings come back, and saving again replaces them`() =
        runBlocking {
            val first =
                UserSettings(
                    land = Bundesland.BAYERN,
                    optOuts = setOf(RegionalOptOut.MARIAE_HIMMELFAHRT_BAYERN),
                    schedule =
                        WorkSchedule(
                            specialDays = mapOf(WorkSchedule.SILVESTER to VacationDays.HALF),
                        ),
                )
            repository.saveSettings(first)
            assertEquals(first, repository.observeSettings().first())

            val second = UserSettings(land = Bundesland.HAMBURG)
            repository.saveSettings(second)
            assertEquals(second, repository.observeSettings().first())
        }

    @Test
    fun `an entitlement is per year and can be replaced and deleted`() =
        runBlocking {
            assertNull(repository.observeEntitlement(2026).first())

            val entitlement =
                YearEntitlement(2026, VacationDays.of(30), carryOver = VacationDays.of(3))
            repository.saveEntitlement(entitlement)
            repository.saveEntitlement(YearEntitlement(2027, VacationDays.of(28)))
            assertEquals(entitlement, repository.observeEntitlement(2026).first())

            val changed = entitlement.copy(days = VacationDays.of(29.5))
            repository.saveEntitlement(changed)
            assertEquals(changed, repository.observeEntitlement(2026).first())

            repository.deleteEntitlement(2026)
            assertNull(repository.observeEntitlement(2026).first())
            assertEquals(VacationDays.of(28), repository.observeEntitlement(2027).first()?.days)
        }

    @Test
    fun `marks come back for the asked range only, ends included`() =
        runBlocking {
            val dates =
                listOf(
                    LocalDate.of(2025, 12, 31),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    LocalDate.of(2027, 1, 1),
                )
            dates.forEach { repository.mark(it, DayMark.FULL) }

            assertEquals(setOf(dates[1], dates[2]), marks().keys)
        }

    @Test
    fun `marking again changes full to half, unmarking removes it`() =
        runBlocking {
            val day = LocalDate.of(2026, 8, 14)
            repository.mark(day, DayMark.FULL)
            repository.mark(day, DayMark.HALF)
            assertEquals(mapOf(day to DayMark.HALF), marks())

            repository.unmark(day)
            repository.unmark(day)
            assertEquals(emptyMap<LocalDate, DayMark>(), marks())
        }
}
