package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.balance.VacationDays
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Status first: how many days are left, what was taken and planned, a warning when Resturlaub
 * is about to expire, the next trip and the next holiday, and the year at a glance.
 */
@Composable
fun OverviewScreen(
    viewModel: PlannerViewModel,
    outerPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val view by viewModel.overview.collectAsStateWithLifecycle()
    val shown = view ?: return
    ScreenFrame(outerPadding) {
        when {
            !shown.hasLand -> {
                EmptyState(
                    stringResource(R.string.overview_no_land),
                    R.string.go_to_settings,
                    onOpenSettings,
                )
            }

            shown.balance == null -> {
                EmptyState(
                    stringResource(R.string.overview_no_entitlement, shown.year),
                    R.string.enter_vacation_days,
                    onOpenSettings,
                )
            }

            else -> {
                Status(shown, viewModel, onOpenMonth)
            }
        }
    }
}

@Composable
fun ScreenFrame(
    outerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .consumeWindowInsets(outerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
fun EmptyState(
    text: String,
    buttonText: Int,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(buttonText))
        }
    }
}

@Composable
private fun Status(
    view: YearView,
    viewModel: PlannerViewModel,
    onOpenMonth: (YearMonth) -> Unit,
) {
    // The bridge is found again by its first day, so the sheet survives a turn of the phone.
    var sheetStart by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val locale = currentLocale()
    val balance = view.balance ?: return
    val overbooked = balance.remaining < VacationDays.ZERO

    // The one number that matters, first.
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.days_left_label, view.year),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = balance.remaining.text(locale),
            style = MaterialTheme.typography.displayLarge,
            color =
                if (overbooked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
        if (overbooked) {
            Text(
                text = stringResource(R.string.overbooked),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Figure(R.string.taken, balance.taken.text(locale), Modifier.weight(1f))
        Figure(R.string.planned, balance.planned.text(locale), Modifier.weight(1f))
        val total = (balance.entitlement + balance.carryOver).text(locale)
        Figure(R.string.of_total, total, Modifier.weight(1f))
    }

    val open = balance.carryOverOpen
    if (open > VacationDays.ZERO && !view.today.isAfter(balance.carryOverExpiry)) {
        CarryOverWarning(open, balance.carryOverExpiry.mediumText(locale), locale)
    }

    Card {
        NextTrip(view, locale)
        NextHoliday(view, locale)
    }

    BridgesCard(view) { sheetStart = it.leave.first() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = view.year.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        YearGrid(view, { onOpenMonth(it) })
        Legend()
        SchoolHolidaysUnknownNote(
            view,
            LocalDate.of(view.year, 1, 1),
            LocalDate.of(view.year, 12, 31),
        )
    }

    val chosen =
        view.bridgeGroups.flatMap { it.bridges }.firstOrNull {
            it.leave.first() ==
                sheetStart
        }
    if (chosen != null) {
        BridgeSheet(view, chosen, viewModel, onDismiss = { sheetStart = null })
    }
}

@Composable
private fun Figure(
    label: Int,
    value: String,
    modifier: Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CarryOverWarning(
    open: VacationDays,
    expiry: String,
    locale: Locale,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = dayColors().holiday,
        contentColor = dayColors().onHoliday,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(
                stringResource(R.string.carry_over_warning, open.text(locale), expiry),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
internal fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@Composable
private fun NextTrip(
    view: YearView,
    locale: Locale,
) {
    val trip = view.nextTrip
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(R.string.next_trip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trip == null) {
            Text(stringResource(R.string.no_trip), style = MaterialTheme.typography.bodyLarge)
        } else {
            val range =
                if (trip.start == trip.end) {
                    trip.start.mediumText(locale)
                } else {
                    rangeText(trip.start, trip.end, locale)
                }
            Text(range, style = MaterialTheme.typography.bodyLarge)
            Text(
                text =
                    if (view.today in trip) {
                        stringResource(R.string.trip_now)
                    } else {
                        pluralStringResource(
                            R.plurals.trip_costs,
                            if (trip.cost == VacationDays.ONE) 1 else 2,
                            trip.cost.text(locale),
                        )
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextHoliday(
    view: YearView,
    locale: Locale,
) {
    val next = view.settings.nextPublicHoliday(view.today) ?: return
    val days = ChronoUnit.DAYS.between(view.today, next.date).toInt()
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(R.string.next_holiday),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(next.holiday.labelRes()), style = MaterialTheme.typography.bodyLarge)
        Text(
            text =
                if (days == 0) {
                    stringResource(R.string.holiday_today)
                } else {
                    val date = next.date.mediumText(locale)
                    pluralStringResource(R.plurals.holiday_in_days, days, date, days)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
