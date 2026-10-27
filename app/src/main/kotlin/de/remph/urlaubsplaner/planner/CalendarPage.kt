package de.remph.urlaubsplaner.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/**
 * What the calendar shows at one moment: a month (or year) together with the [view] of its year.
 * The ViewModel changes the month at once but delivers the new year's view a moment later, so the
 * screen draws from this pair instead of the two separate values, or it would show the new header
 * over the old year's body (the year without vacation days showed its empty state for a frame).
 */
class CalendarPage(
    val view: YearView,
    val month: YearMonth,
    val mode: CalendarMode,
) {
    /** Two pages with the same key are the same content (new data only redraws it, no slide). */
    val key =
        CalendarPageKey(
            mode,
            if (mode ==
                CalendarMode.YEAR
            ) {
                YearMonth.of(month.year, 1)
            } else {
                month
            },
        )
}

data class CalendarPageKey(
    val mode: CalendarMode,
    val month: YearMonth,
)

/**
 * The page for [month] and [mode] once [view] is the view of that month's year; until then the
 * page before, or null if there was none yet.
 */
@Composable
fun rememberCalendarPage(
    view: YearView?,
    month: YearMonth,
    mode: CalendarMode,
): CalendarPage? {
    var page by remember { mutableStateOf<CalendarPage?>(null) }
    if (view != null && view.year == month.year) {
        val last = page
        if (last == null || last.view !== view || last.month != month || last.mode != mode) {
            page = CalendarPage(view, month, mode)
        }
    }
    return page
}

/** The least and the most a row of the expanded year grid gets (the maintainer, 2026-10-27). */
val YearRowsMin = 36.dp
val YearRowsMax = 64.dp

/**
 * The row height of the expanded year grid: what fits when the rest of the page takes [others] of
 * the [available] height, so the page does not scroll, but not under [YearRowsMin] (then it
 * scrolls) and not over [YearRowsMax].
 */
fun expandedRowHeight(
    available: Dp,
    others: Dp,
): Dp = ((available - others) / MONTHS).coerceIn(YearRowsMin, YearRowsMax)

internal const val MONTHS = 12
