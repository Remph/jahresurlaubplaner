package de.remph.urlaubsplaner.core.balance

import java.time.Clock
import java.time.LocalDate
import java.time.MonthDay

/**
 * The numbers the user enters for one calendar year. [carryOver] (Resturlaub) is typed in, not
 * computed from the year before, because the employer's figure is the one that counts. It can be
 * used up to and including [carryOverExpiry] (31 March by default, the usual German rule) and is
 * lost after that. Some employers let it run into a later year (a 2027 Resturlaub that expires
 * on 31 March 2028); [carryOverExpiryYears] is how many years after [year] that is, 0 or 1.
 */
data class YearEntitlement(
    val year: Int,
    val days: VacationDays,
    val carryOver: VacationDays = VacationDays.ZERO,
    val carryOverExpiry: MonthDay = MonthDay.of(3, 31),
    val carryOverExpiryYears: Int = 0,
) {
    init {
        require(days >= VacationDays.ZERO) { "The entitlement cannot be negative" }
        require(carryOver >= VacationDays.ZERO) { "The Resturlaub cannot be negative" }
        require(carryOverExpiryYears in 0..MAX_EXPIRY_YEARS) {
            "The Resturlaub can run at most $MAX_EXPIRY_YEARS year after the entitlement's year"
        }
    }

    /** The last day the Resturlaub can be used. */
    val carryOverExpiryDate: LocalDate get() = carryOverExpiry.atYear(year + carryOverExpiryYears)

    companion object {
        const val MAX_EXPIRY_YEARS = 1
    }
}

/** The status of one year. Every amount is in [VacationDays]; [remaining] can be negative. */
data class VacationBalance(
    val entitlement: VacationDays,
    val carryOver: VacationDays,
    val carryOverExpiry: LocalDate,
    /** Resturlaub used by marked days up to and including [carryOverExpiry]. */
    val carryOverUsed: VacationDays,
    /** Resturlaub that expired unused; zero until the day after [carryOverExpiry]. */
    val carryOverLost: VacationDays,
    /** Cost of marked days up to and including today. */
    val taken: VacationDays,
    /** Cost of marked days after today. */
    val planned: VacationDays,
    /**
     * What can still be booked: entitlement plus Resturlaub that has not expired, minus taken and
     * planned. Until [carryOverExpiry] passes this still counts [carryOverOpen], which only days up
     * to that date can use; a plan beyond it shows as overbooked the day after.
     */
    val remaining: VacationDays,
) {
    /** Resturlaub that is still usable and not yet used or planned. */
    val carryOverOpen: VacationDays get() = carryOver - carryOverUsed - carryOverLost
}

/**
 * Works out what marked days cost and the balance of a year.
 *
 * A marked day costs vacation only on a working day that is not a public holiday, so weekends
 * and holidays in a marked week are free. [isPublicHoliday] is a function, not a Land plus
 * extras and opt-outs, so the regional extras and the Bayern opt-out reach this code without
 * changing it. With `PublicHolidays.isHoliday` it throws for dates before 2019.
 *
 * "Today" comes from [clock]: marked days up to and including today count as taken, later ones
 * as planned.
 */
class VacationCalculator(
    private val clock: Clock,
    private val schedule: WorkSchedule = WorkSchedule(),
    private val isPublicHoliday: (LocalDate) -> Boolean,
) {
    /**
     * The vacation [mark] on [date] costs. A half working day (Heiligabend, say) caps the cost at
     * half a day, so a full and a half mark cost the same there.
     */
    fun cost(
        date: LocalDate,
        mark: DayMark = DayMark.FULL,
    ): VacationDays {
        if (isPublicHoliday(date)) return VacationDays.ZERO
        return minOf(mark.value, schedule.workingValue(date))
    }

    /** The balance of [entitlement]'s year. Marks from other years are ignored. */
    fun balance(
        entitlement: YearEntitlement,
        marks: Map<LocalDate, DayMark>,
    ): VacationBalance {
        val today = LocalDate.now(clock)
        val expiry = entitlement.carryOverExpiryDate
        val costs =
            marks
                .filterKeys { it.year == entitlement.year }
                .map { (date, mark) -> date to cost(date, mark) }

        fun total(include: (LocalDate) -> Boolean): VacationDays {
            var sum = VacationDays.ZERO
            for ((date, cost) in costs) if (include(date)) sum += cost
            return sum
        }

        val taken = total { it <= today }
        val planned = total { it > today }
        val carryOverUsed = minOf(entitlement.carryOver, total { it <= expiry })
        val carryOverLost =
            if (today > expiry) entitlement.carryOver - carryOverUsed else VacationDays.ZERO
        return VacationBalance(
            entitlement = entitlement.days,
            carryOver = entitlement.carryOver,
            carryOverExpiry = expiry,
            carryOverUsed = carryOverUsed,
            carryOverLost = carryOverLost,
            taken = taken,
            planned = planned,
            remaining = entitlement.days + entitlement.carryOver - carryOverLost - taken - planned,
        )
    }
}
