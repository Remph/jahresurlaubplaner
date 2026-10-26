package de.remph.urlaubsplaner.core.ics

import de.remph.urlaubsplaner.core.balance.AmountText
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.Trip
import de.remph.urlaubsplaner.core.balance.VacationCalculator
import de.remph.urlaubsplaner.core.balance.VacationDays
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The vacation as an iCalendar file (RFC 5545) for a calendar app or to send to someone. Decided
 * with the maintainer on 2026-10-26: only the vacation, no public or school holidays, so the file needs no
 * ODbL credit; one all-day event per trip (the marked days of [VacationCalculator.trips], a
 * weekend between two weeks stays inside it), taken and planned days alike.
 *
 * The UID depends only on the first day of the trip, so importing a newer file again updates
 * the event in most calendar apps instead of adding a second one. "Now" for the stamp comes from
 * [clock].
 */
class IcsExport(
    private val clock: Clock,
    private val calculator: VacationCalculator,
) {
    /** How many events [encode] writes, for a screen that disables the export when it is zero. */
    fun tripCount(marks: Map<LocalDate, DayMark>): Int = calculator.trips(marks).size

    fun encode(
        marks: Map<LocalDate, DayMark>,
        language: IcsLanguage,
    ): String {
        val stamp = STAMP_FORMAT.format(clock.instant().atOffset(ZoneOffset.UTC))
        val lines =
            mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:$PRODID", "CALSCALE:GREGORIAN")
        for (trip in calculator.trips(marks)) {
            lines += event(trip, marks, language, stamp)
        }
        lines += "END:VCALENDAR"
        return lines.joinToString(separator = "") { IcsText.fold(it) + IcsText.NEWLINE }
    }

    fun writeTo(
        output: OutputStream,
        marks: Map<LocalDate, DayMark>,
        language: IcsLanguage,
    ) {
        output.write(encode(marks, language).toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun event(
        trip: Trip,
        marks: Map<LocalDate, DayMark>,
        language: IcsLanguage,
        stamp: String,
    ): List<String> {
        val halfDays =
            marks.count { (date, mark) ->
                date in trip && calculator.cost(date, mark) == VacationDays.HALF
            }
        return listOf(
            "BEGIN:VEVENT",
            "UID:urlaubsplaner-${trip.start}@$UID_DOMAIN",
            "DTSTAMP:$stamp",
            // The end of an all-day event is exclusive, so it is the day after the last day.
            "DTSTART;VALUE=DATE:${DATE_FORMAT.format(trip.start)}",
            "DTEND;VALUE=DATE:${DATE_FORMAT.format(trip.end.plusDays(1))}",
            "SUMMARY:${IcsText.escape(summary(language))}",
            "DESCRIPTION:${IcsText.escape(description(trip.cost, halfDays, language))}",
            "END:VEVENT",
        )
    }

    private fun summary(language: IcsLanguage) =
        when (language) {
            IcsLanguage.GERMAN -> "Urlaub"
            IcsLanguage.ENGLISH -> "Vacation"
        }

    /** "4,5 Urlaubstage, davon 1 halber Tag" or "4.5 vacation days, 1 of them a half day". */
    private fun description(
        cost: VacationDays,
        halfDays: Int,
        language: IcsLanguage,
    ): String {
        val one = cost == VacationDays.ONE
        return when (language) {
            IcsLanguage.GERMAN -> {
                val days =
                    AmountText.format(cost, Locale.GERMANY) +
                        if (one) " Urlaubstag" else " Urlaubstage"
                when (halfDays) {
                    0 -> days
                    1 -> "$days, davon 1 halber Tag"
                    else -> "$days, davon $halfDays halbe Tage"
                }
            }

            IcsLanguage.ENGLISH -> {
                val days =
                    AmountText.format(cost, Locale.ENGLISH) +
                        if (one) " vacation day" else " vacation days"
                when (halfDays) {
                    0 -> days
                    1 -> "$days, 1 of them a half day"
                    else -> "$days, $halfDays of them half days"
                }
            }
        }
    }

    companion object {
        const val MIME_TYPE = "text/calendar"

        private const val UID_DOMAIN = "de.remph.urlaubsplaner"
        private const val PRODID = "-//de.remph.urlaubsplaner//Urlaubsplaner//EN"
        private val DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE
        private val STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

        /** The name suggested to the file picker and used for the file that is shared. */
        fun fileName(exportedOn: LocalDate) = "urlaubsplaner-$exportedOn.ics"
    }
}
