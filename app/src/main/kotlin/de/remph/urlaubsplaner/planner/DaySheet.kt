package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationDays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/**
 * What a tapped day is, and how to mark it: whole or half, one day or up to an end day ("Bis").
 * A range marks only the days that cost vacation, so the weekend inside it stays unmarked.
 * Before anything is stored the sheet says what it would cost and how many days would be left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    view: YearView,
    date: LocalDate,
    viewModel: PlannerViewModel,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    val colors = dayColors()
    val existing = view.markOn(date)
    var mark by remember(date) { mutableStateOf(existing ?: DayMark.FULL) }
    var end by remember(date) { mutableStateOf(date) }
    var picking by remember { mutableStateOf(false) }
    val days = view.costlyDays(date, end)
    val cost = days.fold(VacationDays.ZERO) { sum, day -> sum + view.cost(day, mark) }
    val remaining = view.remainingAfter(days, mark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(
                horizontal = 24.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                date.fullText(locale),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Facts(view, date, locale, colors.holiday, colors.school)
            if (view.canMark(date)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val marks =
                        listOf(
                            DayMark.FULL to R.string.mark_full,
                            DayMark.HALF to R.string.mark_half,
                        )
                    marks.forEachIndexed { index, (entry, label) ->
                        SegmentedButton(
                            selected = mark == entry,
                            onClick = { mark = entry },
                            shape = SegmentedButtonDefaults.itemShape(index, marks.size),
                        ) { Text(stringResource(label)) }
                    }
                }
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.until, end.mediumText(locale)))
                }
                Text(
                    text = costText(cost, remaining, locale),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = {
                        viewModel.mark(days, mark)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (existing ==
                                null
                            ) {
                                R.string.mark_save
                            } else {
                                R.string.mark_change
                            },
                        ),
                    )
                }
                if (days.any { view.markOn(it) != null }) {
                    TextButton(
                        onClick = {
                            viewModel.unmark(days)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.mark_remove)) }
                }
            } else {
                Text(
                    stringResource(R.string.costs_nothing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (picking) {
        EndDatePicker(date, end, onPick = {
            end = it
            picking = false
        }, onDismiss = {
            picking =
                false
        })
    }
}

@Composable
internal fun costText(
    cost: VacationDays,
    remaining: VacationDays?,
    locale: Locale,
): String {
    val quantity = if (cost == VacationDays.ONE) 1 else 2
    val costs = pluralStringResource(R.plurals.sheet_costs, quantity, cost.text(locale))
    return if (remaining == null) {
        costs
    } else {
        stringResource(R.string.sheet_costs_and_left, costs, remaining.text(locale))
    }
}

@Composable
private fun Facts(
    view: YearView,
    date: LocalDate,
    locale: Locale,
    holidayColor: Color,
    schoolColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        view.holidays[date]?.let { Fact(holidayColor, stringResource(it.labelRes())) }
        for (entry in view.schoolHolidaysOn(date)) Fact(schoolColor, entry.nameIn(locale))
        if (view.holidays[date] == null &&
            view.schoolHolidaysOn(date).isEmpty() &&
            !view.schoolHolidaysKnown(date, date)
        ) {
            Text(
                stringResource(R.string.school_holidays_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Fact(
    swatch: Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).background(swatch, MaterialTheme.shapes.extraSmall))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** The end day can be the start day or later in the same year; the balance is per year. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndDatePicker(
    start: LocalDate,
    current: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis =
                current
                    .atStartOfDay(
                        ZoneOffset.UTC,
                    ).toInstant()
                    .toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val day =
                            Instant
                                .ofEpochMilli(
                                    utcTimeMillis,
                                ).atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        return !day.isBefore(start) && day.year == start.year
                    }

                    override fun isSelectableYear(year: Int) = year == start.year
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        DatePicker(state)
    }
}
