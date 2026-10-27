package de.remph.urlaubsplaner.planner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.settings.daysWithUnit
import de.remph.urlaubsplaner.ui.rememberAnimationsRemoved
import de.remph.urlaubsplaner.ui.rememberSharedAxis
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.floor

/**
 * The calendar: one month with every day tappable, or the whole year at a glance. Tapping a
 * day opens the day sheet to mark it. The month and the mode live in the ViewModel, so they
 * survive a tab change and a rotation.
 *
 * A step slides the title and the body a short way, like the tabs (a switch to the year or month
 * view swaps in place). Both are drawn from one
 * [CalendarPage], so the header and the body always belong to the same month.
 */
@Composable
fun CalendarScreen(
    viewModel: PlannerViewModel,
    outerPadding: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    val view by viewModel.calendar.collectAsStateWithLifecycle()
    val month by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val mode by viewModel.calendarMode.collectAsStateWithLifecycle()
    val rowsExpanded by viewModel.calendarYearRowsExpanded.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    var sheetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val page = rememberCalendarPage(view, month, mode) ?: return
    val shown = page.view
    val locale = currentLocale()
    val motion = rememberPageMotion()
    var headerPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints {
        // What the page can fill without scrolling: the screen without the bars and the frame.
        val visible =
            maxHeight - outerPadding.calculateTopPadding() -
                outerPadding.calculateBottomPadding() - FramePadding * 2
        ScreenFrame(outerPadding, scroll, Modifier.swipeToStep(viewModel::step)) {
            if (!shown.hasLand) {
                EmptyState(
                    stringResource(R.string.overview_no_land),
                    R.string.go_to_settings,
                    onOpenSettings,
                )
                return@ScreenFrame
            }
            Header(
                viewModel,
                page,
                motion,
                locale,
                Modifier.onSizeChanged { headerPx = it.height },
            )
            AnimatedContent(
                page,
                transitionSpec = motion.spec,
                contentKey = motion.key,
                label = "calendarPage",
            ) { shownPage ->
                CalendarBody(
                    shownPage,
                    rowsExpanded,
                    // The header and the gap under it are above the body.
                    with(LocalDensity.current) { headerPx.toDp() } + FrameSpacing,
                    visible,
                    scroll,
                    locale,
                    viewModel,
                    onOpenSettings,
                    onDay = { sheetDate = it },
                )
            }
        }
    }

    val date = sheetDate
    if (date != null && date.year == shown.year) {
        DaySheet(shown, date, viewModel, onDismiss = { sheetDate = null })
    }
}

private typealias PageTransition =
    AnimatedContentTransitionScope<CalendarPage>.() -> ContentTransform

private val FramePadding = 16.dp
private val FrameSpacing = 16.dp

/**
 * The slide of a step, the direction follows the date. With Android's animations off nothing
 * slides and the key never changes, so the content swaps in place (see [CalendarPage.generation]).
 */
private class PageMotion(
    val spec: PageTransition,
    val key: (CalendarPage) -> Int,
)

@Composable
private fun rememberPageMotion(): PageMotion {
    val axis = rememberSharedAxis<CalendarPage>()
    val removed = rememberAnimationsRemoved()
    return PageMotion(
        spec = {
            axis(targetState.month > initialState.month).using(SizeTransform(clip = false))
        },
        key = { if (removed) 0 else it.generation },
    )
}

@Composable
private fun CalendarBody(
    page: CalendarPage,
    rowsExpanded: Boolean,
    above: Dp,
    visible: Dp,
    scroll: ScrollState,
    locale: Locale,
    viewModel: PlannerViewModel,
    onOpenSettings: () -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val shown = page.view
    val month = page.month
    val density = LocalDensity.current
    // The expanded rows are what is left of the screen once the rest of the page has its height.
    // That rest is measured (the page minus the grid), and kept over a tab change.
    var savedOthers by rememberSaveable { mutableIntStateOf(-1) }
    var bodyPx by remember { mutableIntStateOf(0) }
    var gridPx by remember { mutableIntStateOf(0) }
    val othersPx = if (bodyPx > 0 && gridPx > 0) bodyPx - gridPx else savedOthers
    SideEffect { savedOthers = othersPx }
    val expanded =
        if (othersPx < 0) {
            YearRowsMin
        } else {
            val others = with(density) { othersPx.toDp() } + above
            // Whole dp, rounded down, so the page never ends a pixel too long and scrolls.
            floor(expandedRowHeight(visible, others).value).dp
        }
    val rowHeight by animateDpAsState(
        if (rowsExpanded) expanded else YearRowsCompact,
        label = "yearRowHeight",
    )
    Column(
        Modifier.fillMaxWidth().onSizeChanged { bodyPx = it.height },
        verticalArrangement = Arrangement.spacedBy(FrameSpacing),
    ) {
        if (page.mode == CalendarMode.MONTH) {
            MonthGrid(shown, month) { onDay(it) }
            MonthCard(shown, month, locale)
            Legend(
                showBridge = shown.bridgeDays.any { YearMonth.from(it) == month },
                showGranted = shown.grantedEnabled,
                compact = true,
            )
        } else {
            Column {
                YearGrid(
                    shown,
                    { viewModel.showMonth(it).also { viewModel.showMode(CalendarMode.MONTH) } },
                    Modifier
                        .onSizeChanged { gridPx = it.height }
                        .expandBySwipe(
                            rowsExpanded,
                            pageAtEnd = { scroll.value >= scroll.maxValue },
                            onChange = viewModel::expandYearRows,
                        ),
                    rowHeight = rowHeight,
                )
                YearRowsHandle(rowsExpanded) { viewModel.expandYearRows(!rowsExpanded) }
            }
            Legend(
                showBridge = shown.bridgeDays.isNotEmpty(),
                showGranted = shown.grantedEnabled,
            )
            SchoolHolidaysUnknownNote(
                shown,
                LocalDate.of(shown.year, 1, 1),
                LocalDate.of(shown.year, 12, 31),
            )
        }
        if (shown.entitlement == null) {
            EmptyState(
                stringResource(R.string.overview_no_entitlement, shown.year),
                R.string.enter_vacation_days,
                onOpenSettings,
            )
        }
    }
}

@Composable
private fun Header(
    viewModel: PlannerViewModel,
    page: CalendarPage,
    motion: PageMotion,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val mode = page.mode
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { viewModel.step(-1) }, enabled = viewModel.canStep(-1)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription =
                        stringResource(
                            if (mode == CalendarMode.MONTH) {
                                R.string.month_previous
                            } else {
                                R.string.year_previous
                            },
                        ),
                )
            }
            AnimatedContent(
                page,
                transitionSpec = motion.spec,
                contentKey = motion.key,
                label = "calendarTitle",
            ) { shownPage ->
                val month = shownPage.month
                Text(
                    text =
                        if (shownPage.mode == CalendarMode.MONTH) {
                            month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) +
                                " " + month.year
                        } else {
                            month.year.toString()
                        },
                    style = MaterialTheme.typography.titleLarge,
                    modifier =
                        Modifier.semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
            IconButton(onClick = { viewModel.step(1) }, enabled = viewModel.canStep(1)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription =
                        stringResource(
                            if (mode == CalendarMode.MONTH) {
                                R.string.month_next
                            } else {
                                R.string.year_next
                            },
                        ),
                )
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val modes =
                listOf(
                    CalendarMode.MONTH to R.string.mode_month,
                    CalendarMode.YEAR to R.string.mode_year,
                )
            modes.forEachIndexed { index, (entry, label) ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { viewModel.showMode(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) { Text(stringResource(label)) }
            }
        }
    }
}

/**
 * Under the month: the vacation days of the month, then its holidays and school holidays by
 * name, since colours alone say little (design review, the maintainer, 2026-10-25).
 */
@Composable
private fun MonthCard(
    view: YearView,
    month: YearMonth,
    locale: Locale,
) {
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val holidays = view.holidays.filterKeys { it.year == month.year && it.month == month.month }
    val school = view.schoolHolidaysBetween(first, last)
    val colors = dayColors()
    val vacation = view.vacationIn(month)
    val resources = LocalResources.current

    fun amount(days: VacationDays) =
        daysWithUnit(
            days,
            locale,
            one = { resources.getString(R.string.days_one, it) },
            other = { resources.getString(R.string.days_other, it) },
        )
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountRow(stringResource(R.string.legend_taken), amount(vacation.taken))
            AmountRow(stringResource(R.string.legend_planned), amount(vacation.planned))
        }
        if (holidays.isNotEmpty() || school.isNotEmpty()) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((date, holiday) in holidays.toSortedMap()) {
                    NoteRow(
                        colors.holiday,
                        date.mediumText(locale),
                        stringResource(holiday.labelRes()),
                    )
                }
                for (entry in school) {
                    NoteRow(
                        colors.school,
                        rangeText(entry.from, entry.to, locale),
                        entry.nameIn(locale),
                    )
                }
            }
        }
    }
    SchoolHolidaysUnknownNote(view, first, last)
}

@Composable
private fun AmountRow(
    label: String,
    amount: String,
) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            amount,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteRow(
    swatch: androidx.compose.ui.graphics.Color,
    date: String,
    name: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).background(swatch, MaterialTheme.shapes.extraSmall))
        Column(Modifier.padding(start = 12.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(
                date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
