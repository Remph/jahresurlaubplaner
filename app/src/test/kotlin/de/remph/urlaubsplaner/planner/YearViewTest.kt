package de.remph.urlaubsplaner.planner

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationCalculator
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.balance.YearEntitlement
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.holidays.Holiday
import de.remph.urlaubsplaner.core.settings.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** What the screens read from a year: the day looks, the balance, the preview and the trips. */
@RunWith(AndroidJUnit4::class)
class YearViewTest {
    private val fixture = PlannerFixture()
    private val settings = UserSettings(land = Bundesland.NORDRHEIN_WESTFALEN)
    private val entitlement = YearEntitlement(2027, VacationDays.of(30), VacationDays.of(10))

    @After
    fun close() = fixture.close()

    private fun view(
        marks: Map<LocalDate, DayMark> =
            mapOf(
                fixture.date("2027-03-08") to DayMark.FULL,
                fixture.date("2027-03-15") to DayMark.HALF,
            ),
        entitlement: YearEntitlement? = this.entitlement,
        settings: UserSettings = this.settings,
    ) = YearView(
        year = 2027,
        today = fixture.date("2027-03-10"),
        settings = settings,
        entitlement = entitlement,
        marks = marks,
        calculator =
            settings.publicHolidayCheck()?.let {
                VacationCalculator(fixture.clock, settings.schedule, it)
            },
        schoolData = fixture.schoolHolidays,
    )

    @Test
    fun `the year knows its public holidays by date`() {
        val view = view()

        assertEquals(Holiday.KARFREITAG, view.holidays[fixture.date("2027-03-26")])
        assertEquals(Holiday.OSTERMONTAG, view.holidays[fixture.date("2027-03-29")])
        assertNull(view.holidays[fixture.date("2027-03-25")])
    }

    @Test
    fun `a day looks taken before today, planned after, and half when half`() {
        val view = view()

        val taken = view.look(fixture.date("2027-03-08"))
        assertEquals(DayMark.FULL, taken.mark)
        assertTrue(taken.taken)

        val planned = view.look(fixture.date("2027-03-15"))
        assertEquals(DayMark.HALF, planned.mark)
        assertFalse(planned.taken)
    }

    @Test
    fun `a mark that costs nothing shows nothing`() {
        val saturday = fixture.date("2027-03-13")

        val look = view(marks = mapOf(saturday to DayMark.FULL)).look(saturday)

        assertNull(look.mark)
        assertFalse(look.workDay)
    }

    @Test
    fun `today and school holidays are part of the look`() {
        val view = view()

        assertTrue(view.look(fixture.date("2027-03-10")).today)
        assertTrue(view.look(fixture.date("2027-03-30")).school)
        assertFalse(view.look(fixture.date("2027-03-25")).school)
        assertEquals(
            "Osterferien",
            view.schoolHolidaysOn(fixture.date("2027-03-30")).single().nameDe,
        )
    }

    @Test
    fun `school holidays are known only inside the file's coverage`() {
        val view = view()

        assertTrue(view.schoolHolidaysKnown(fixture.date("2027-03-01"), fixture.date("2027-03-31")))
        assertFalse(
            view.schoolHolidaysKnown(fixture.date("2027-12-01"), fixture.date("2028-01-31")),
        )
    }

    @Test
    fun `the balance counts taken and planned marks`() {
        val balance = view().balance!!

        assertEquals(VacationDays.ONE, balance.taken)
        assertEquals(VacationDays.HALF, balance.planned)
        assertEquals(VacationDays.of(38.5), balance.remaining)
    }

    @Test
    fun `without the year's numbers there is no balance but marking still works`() {
        val view = view(entitlement = null)

        assertNull(view.balance)
        assertNull(view.remainingAfter(listOf(fixture.date("2027-03-16")), DayMark.FULL))
        assertTrue(view.canMark(fixture.date("2027-03-16")))
    }

    @Test
    fun `without a Land nothing can be marked`() {
        val view = view(settings = UserSettings())

        assertFalse(view.hasLand)
        assertFalse(view.canMark(fixture.date("2027-03-16")))
        assertEquals(
            emptyList<LocalDate>(),
            view.costlyDays(fixture.date("2027-03-15"), fixture.date("2027-03-19")),
        )
    }

    @Test
    fun `a weekend and a public holiday cannot be marked`() {
        val view = view()

        assertFalse(view.canMark(fixture.date("2027-03-13")))
        assertFalse(view.canMark(fixture.date("2027-03-26")))
        assertTrue(view.canMark(fixture.date("2027-03-25")))
    }

    @Test
    fun `a range of days over Good Friday and a weekend marks only working days`() {
        val days = view().costlyDays(fixture.date("2027-03-24"), fixture.date("2027-03-30"))

        assertEquals(
            listOf("2027-03-24", "2027-03-25", "2027-03-30").map(fixture::date),
            days,
        )
    }

    @Test
    fun `the preview says how many days are left after marking or unmarking`() {
        val view = view()
        val week = (16..18).map { fixture.date("2027-03-$it") }

        assertEquals(VacationDays.of(35.5), view.remainingAfter(week, DayMark.FULL))
        assertEquals(VacationDays.of(37), view.remainingAfter(week, DayMark.HALF))
        assertEquals(
            VacationDays.of(39.5),
            view.remainingAfter(listOf(fixture.date("2027-03-08")), null),
        )
    }

    @Test
    fun `the next trip is the one that includes today or comes next`() {
        val marks =
            mapOf(
                fixture.date("2027-03-02") to DayMark.FULL,
                fixture.date("2027-03-09") to DayMark.FULL,
                fixture.date("2027-03-10") to DayMark.FULL,
                fixture.date("2027-03-11") to DayMark.FULL,
                fixture.date("2027-06-01") to DayMark.FULL,
            )

        val view = view(marks = marks)

        assertEquals(fixture.date("2027-03-09"), view.nextTrip?.start)
        assertEquals(fixture.date("2027-03-11"), view.nextTrip?.end)
        assertNotNull(view.trips.firstOrNull { it.start == fixture.date("2027-06-01") })
    }

    @Test
    fun `no marks in the future give no next trip`() {
        assertNull(view(marks = mapOf(fixture.date("2027-03-02") to DayMark.FULL)).nextTrip)
    }
}
