package de.remph.urlaubsplaner.planner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.balance.YearEntitlement
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.schoolholidays.SchoolHolidayData
import de.remph.urlaubsplaner.core.settings.UserSettings
import de.remph.urlaubsplaner.data.UrlaubsplanerDatabase
import de.remph.urlaubsplaner.data.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset

/**
 * An in-memory database, a fixed clock and a small school holiday file, for the tests of the
 * overview and the calendar. [today] is Wednesday 10 March 2027; the file covers 2027 only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlannerFixture(
    today: String = "2027-03-10",
) {
    val clock: Clock = Clock.fixed(Instant.parse("${today}T10:00:00Z"), ZoneOffset.UTC)
    val database: UrlaubsplanerDatabase
    val repository: UserDataRepository
    val schoolHolidays =
        SchoolHolidayData.parse(
            """
            format: 1
            version: 2026-10-25
            covers: 2027-01-01..2027-12-31
            NW;2027-03-29;2027-04-10;Osterferien;Easter holidays
            NW;2027-07-19;2027-08-31;Sommerferien;Summer holidays
            """.trimIndent(),
        )

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrlaubsplanerDatabase::class.java).build()
        repository = UserDataRepository(database)
    }

    fun viewModel() = PlannerViewModel(repository, lazyOf(schoolHolidays), clock)

    /** Nordrhein-Westfalen, 30 days and 10 carried over until 31 March 2027, a few marks. */
    fun fill() =
        runBlocking {
            repository.saveSettings(UserSettings(land = Bundesland.NORDRHEIN_WESTFALEN))
            repository.saveEntitlement(
                YearEntitlement(2027, VacationDays.of(30), VacationDays.of(10), MonthDay.of(3, 31)),
            )
            // Taken: Monday and Tuesday before today. Planned: the week of 15 March.
            repository.markAll(listOf(date("2027-03-08"), date("2027-03-09")), DayMark.FULL)
            repository.markAll((15..19).map { date("2027-03-$it") }, DayMark.FULL)
        }

    fun date(text: String): LocalDate = LocalDate.parse(text)

    fun close() = database.close()
}
