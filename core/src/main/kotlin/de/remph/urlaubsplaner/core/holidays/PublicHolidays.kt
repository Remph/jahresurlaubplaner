package de.remph.urlaubsplaner.core.holidays

import java.time.LocalDate

/** A public holiday on a date. The name is looked up from [holiday] in the app's string resources. */
data class PublicHoliday(
    val date: LocalDate,
    val holiday: Holiday,
)

object PublicHolidays {
    /** Earlier years differ (Reformationstag 2017, Weltkindertag and Frauentag came in 2019). */
    const val FIRST_SUPPORTED_YEAR = 2019

    /** The public holidays of [land] in [year], sorted by date. */
    fun of(
        land: Bundesland,
        year: Int,
        extras: Set<RegionalExtra> = emptySet(),
    ): List<PublicHoliday> {
        require(year >= FIRST_SUPPORTED_YEAR) {
            "Public holidays are supported from $FIRST_SUPPORTED_YEAR, got $year"
        }
        val statewide =
            HolidayRules.all
                .filter { land in it.lands && it.appliesInYear(year) }
                .map { it.holiday }
        val regional = extras.filter { land in it.lands }.map { it.holiday }
        return (statewide + regional)
            .distinct()
            .map { PublicHoliday(it.date(year), it) }
            .sortedBy { it.date }
    }

    fun isHoliday(
        land: Bundesland,
        date: LocalDate,
        extras: Set<RegionalExtra> = emptySet(),
    ): Boolean = of(land, date.year, extras).any { it.date == date }
}
