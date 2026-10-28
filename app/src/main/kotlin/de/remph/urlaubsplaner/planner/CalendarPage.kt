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
    /**
     * Counts the steps: it grows when the month (the year, in year mode) changes and stays when
     * only the data or the mode changes. The screen slides when it changes; a switch between month
     * and year view is no step and swaps in place, because AnimatedContent keeps the old content
     * for one frame when its key changes, even with no animation, and that frame showed both
     * titles and both bodies on top of each other.
     */
    val generation: Int = 0,
) {
    /** The month, or January of the year in year mode: what a step changes. */
    private val position: YearMonth =
        if (mode == CalendarMode.YEAR) YearMonth.of(month.year, 1) else month

    /** The next page: a step if it shows another month (or year) in the same mode. */
    fun next(
        view: YearView,
        month: YearMonth,
        mode: CalendarMode,
    ): CalendarPage {
        val page = CalendarPage(view, month, mode, generation)
        val step = mode == this.mode && page.position != position
        return if (step) CalendarPage(view, month, mode, generation + 1) else page
    }
}

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
        if (last == null) {
            page = CalendarPage(view, month, mode)
        } else if (last.view !== view || last.month != month || last.mode != mode) {
            page = last.next(view, month, mode)
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
