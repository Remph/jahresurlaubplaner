package de.remph.urlaubsplaner.planner

import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.Trip
import de.remph.urlaubsplaner.core.balance.VacationBalance
import de.remph.urlaubsplaner.core.balance.VacationCalculator
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.balance.YearEntitlement
import de.remph.urlaubsplaner.core.holidays.Holiday
import de.remph.urlaubsplaner.core.schoolholidays.SchoolHoliday
import de.remph.urlaubsplaner.core.schoolholidays.SchoolHolidayData
import de.remph.urlaubsplaner.core.settings.UserSettings
import java.time.LocalDate

/**
 * Everything the overview and the calendar show about one year, worked out once: what each day
 * is (holiday, school holiday, marked), the balance and the trips. The screens only draw it.
 *
 * [calculator] is null while no Land is chosen, because then there is no holiday rule and so no
 * cost; the screens send the user to the settings in that case.
 */
class YearView(
    val year: Int,
    val today: LocalDate,
    val settings: UserSettings,
    val entitlement: YearEntitlement?,
    /** Marks from the December before to the December after, so trips over New Year work. */
    val marks: Map<LocalDate, DayMark>,
    private val calculator: VacationCalculator?,
    private val schoolData: SchoolHolidayData,
) {
    val hasLand: Boolean get() = settings.land != null

    val holidays: Map<LocalDate, Holiday> =
        settings.publicHolidays(year).associate { it.date to it.holiday }

    private val schoolHolidays: List<SchoolHoliday> =
        settings.land?.let { schoolData.between(it, firstDay, lastDay) }.orEmpty()

    val balance: VacationBalance? =
        if (calculator != null &&
            entitlement != null
        ) {
            calculator.balance(entitlement, marks)
        } else {
            null
        }

    val trips: List<Trip> = calculator?.trips(marks).orEmpty()

    private val firstDay: LocalDate get() = LocalDate.of(year, 1, 1)
    private val lastDay: LocalDate get() = LocalDate.of(year, 12, 31)

    /** Whether the Land's school holidays are known for all of [from] to [to]. */
    fun schoolHolidaysKnown(
        from: LocalDate,
        to: LocalDate,
    ): Boolean = schoolData.covers(from, to)

    fun schoolHolidaysOn(date: LocalDate): List<SchoolHoliday> =
        schoolHolidays.filter { date in it }

    /** The school holidays that touch [from] to [to], for the list under a month. */
    fun schoolHolidaysBetween(
        from: LocalDate,
        to: LocalDate,
    ): List<SchoolHoliday> = schoolHolidays.filter { it.overlaps(from, to) }

    fun markOn(date: LocalDate): DayMark? = marks[date]

    /** Whether marking [date] would cost anything: a working day that is not a holiday. */
    fun canMark(date: LocalDate): Boolean =
        calculator != null && calculator.cost(date) > VacationDays.ZERO

    /** What [mark] on [date] would cost, zero where it costs nothing. */
    fun cost(
        date: LocalDate,
        mark: DayMark,
    ): VacationDays = calculator?.cost(date, mark) ?: VacationDays.ZERO

    /** The days from [from] to [to] that cost vacation, which is what a range marks. */
    fun costlyDays(
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> = calculator?.costlyDays(from, to).orEmpty()

    /**
     * The days left if [dates] were marked with [mark] (or unmarked when [mark] is null), for the
     * preview in the day sheet. Null without the year's numbers.
     */
    fun remainingAfter(
        dates: Collection<LocalDate>,
        mark: DayMark?,
    ): VacationDays? {
        val entitlement = entitlement ?: return null
        val calculator = calculator ?: return null
        val changed =
            if (mark == null) marks - dates.toSet() else marks + dates.associateWith { mark }
        return calculator.balance(entitlement, changed).remaining
    }

    /** The trip that includes [today] or the first one after it. */
    val nextTrip: Trip? get() = trips.firstOrNull { !it.end.isBefore(today) }
}
