package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remph.urlaubsplaner.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The calendar: one month with every day tappable, or the whole year at a glance. Tapping a
 * day opens the day sheet to mark it. The month and the mode live in the ViewModel, so they
 * survive a tab change and a rotation.
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
    var sheetDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val shown = view ?: return
    val locale = currentLocale()

    ScreenFrame(outerPadding) {
        if (!shown.hasLand) {
            EmptyState(
                stringResource(R.string.overview_no_land),
                R.string.go_to_settings,
                onOpenSettings,
            )
            return@ScreenFrame
        }
        Header(viewModel, month, mode, locale)
        if (shown.year == month.year) {
            if (mode == CalendarMode.MONTH) {
                MonthGrid(shown, month) { sheetDate = it }
                Legend(showBridge = shown.bridgeDays.any { YearMonth.from(it) == month })
                MonthNotes(shown, month, locale)
            } else {
                YearGrid(
                    shown,
                    { viewModel.showMonth(it).also { viewModel.showMode(CalendarMode.MONTH) } },
                    rowHeight = 28.dp,
                )
                Legend()
                SchoolHolidaysUnknownNote(
                    shown,
                    LocalDate.of(shown.year, 1, 1),
                    LocalDate.of(shown.year, 12, 31),
                )
            }
        }
        if (shown.entitlement == null) {
            EmptyState(
                stringResource(R.string.overview_no_entitlement, shown.year),
                R.string.enter_vacation_days,
                onOpenSettings,
            )
        }
    }

    val date = sheetDate
    if (date != null && date.year == shown.year) {
        DaySheet(shown, date, viewModel, onDismiss = { sheetDate = null })
    }
}

@Composable
private fun Header(
    viewModel: PlannerViewModel,
    month: YearMonth,
    mode: CalendarMode,
    locale: Locale,
) {
    val title =
        if (mode == CalendarMode.MONTH) {
            month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + month.year
        } else {
            month.year.toString()
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            if (mode ==
                                CalendarMode.MONTH
                            ) {
                                R.string.month_previous
                            } else {
                                R.string.year_previous
                            },
                        ),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            IconButton(onClick = { viewModel.step(1) }, enabled = viewModel.canStep(1)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription =
                        stringResource(
                            if (mode ==
                                CalendarMode.MONTH
                            ) {
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

/** Under the month: its holidays and school holidays by name, since colours alone say little. */
@Composable
private fun MonthNotes(
    view: YearView,
    month: YearMonth,
    locale: Locale,
) {
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val holidays = view.holidays.filterKeys { it.year == month.year && it.month == month.month }
    val school = view.schoolHolidaysBetween(first, last)
    val colors = dayColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((date, holiday) in holidays.toSortedMap()) {
            NoteRow(colors.holiday, date.mediumText(locale), stringResource(holiday.labelRes()))
        }
        for (entry in school) {
            NoteRow(
                colors.school,
                rangeText(entry.from, entry.to, locale),
                entry.nameIn(locale),
            )
        }
        SchoolHolidaysUnknownNote(view, first, last)
    }
}

@Composable
private fun NoteRow(
    swatch: androidx.compose.ui.graphics.Color,
    date: String,
    name: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).background(swatch, RoundedCornerShape(5.dp)))
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
