package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** What a day is, put together once for the cell, the year grid and the spoken description. */
class DayLook(
    val holiday: Boolean,
    val school: Boolean,
    val mark: DayMark?,
    val taken: Boolean,
    val today: Boolean,
    val workDay: Boolean,
)

/** [mark] counts only when it costs something, so a mark left on a Sunday shows nothing. */
fun YearView.look(date: LocalDate): DayLook {
    val mark = markOn(date)?.takeIf { cost(date, it) > VacationDays.ZERO }
    return DayLook(
        holiday = date in holidays,
        school = schoolHolidaysOn(date).isNotEmpty(),
        mark = mark,
        taken = mark != null && !date.isAfter(today),
        today = date == today,
        workDay = settings.schedule.workingValue(date) > VacationDays.ZERO,
    )
}

/** The seven weekday names over the month grid, Monday first as in Germany. */
@Composable
private fun WeekdayHeader(locale: Locale) {
    Row(Modifier.fillMaxWidth()) {
        for (day in DayOfWeek.entries) {
            Text(
                text = day.getDisplayName(TextStyle.SHORT, locale).trimEnd('.'),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One month as a grid of days, six rows at most. Days outside the month stay empty. */
@Composable
fun MonthGrid(
    view: YearView,
    month: YearMonth,
    onDayClick: (LocalDate) -> Unit,
) {
    val locale = currentLocale()
    val blanks = month.atDay(1).dayOfWeek.value - 1
    val cells = blanks + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WeekdayHeader(locale)
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val day = row * 7 + column - blanks + 1
                    if (day < 1 || day > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).height(52.dp))
                    } else {
                        DayCell(view, month.atDay(day), locale, onDayClick, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    view: YearView,
    date: LocalDate,
    locale: Locale,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val colors = dayColors()
    val look = view.look(date)
    val description = dayDescription(view, date, look, locale)
    val shape = RoundedCornerShape(10.dp)
    val planned = look.mark != null && !look.taken
    // Every day is a tile of its own, so the days stand out from the background (Nick,
    // 2026-10-25). The tile's colour is the strongest thing the day is; a school holiday under a
    // taken day or a holiday still shows as a bar along the bottom.
    val fill =
        when {
            look.taken -> colors.taken
            look.holiday -> colors.holiday
            look.school -> colors.school
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    val textColor =
        when {
            look.taken -> colors.onTaken
            look.holiday -> colors.onHoliday
            !look.workDay -> colors.weekend
            else -> MaterialTheme.colorScheme.onSurface
        }
    val schoolBar = look.school && (look.taken || look.holiday || planned)
    Box(
        modifier =
            modifier
                .height(52.dp)
                .padding(2.dp)
                .clip(shape)
                .background(fill)
                .then(
                    if (planned) {
                        Modifier.border(BorderStroke(2.dp, colors.planned), shape)
                    } else {
                        Modifier
                    },
                ).clickable { onClick(date) }
                .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (look.today) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (look.mark == DayMark.HALF) {
            Text(
                text = "½",
                color = if (look.taken) colors.onTaken else colors.planned,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 5.dp, top = 2.dp),
            )
        }
        if (schoolBar) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(width = 22.dp, height = 3.dp)
                    .background(colors.schoolBar, RoundedCornerShape(2.dp)),
            )
        }
        if (look.today) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (schoolBar) 9.dp else 5.dp)
                    .size(width = 14.dp, height = 2.dp)
                    .background(textColor),
            )
        }
    }
}

/** What TalkBack says for a day: the date, then what is special about it. */
@Composable
fun dayDescription(
    view: YearView,
    date: LocalDate,
    look: DayLook,
    locale: Locale,
): String {
    val parts = mutableListOf(date.fullText(locale))
    view.holidays[date]?.let {
        parts +=
            stringResource(R.string.day_holiday, stringResource(it.labelRes()))
    }
    if (look.school) parts += stringResource(R.string.day_school_holiday)
    when {
        look.mark == null -> Unit
        look.mark == DayMark.HALF && look.taken -> parts += stringResource(R.string.day_taken_half)
        look.mark == DayMark.HALF -> parts += stringResource(R.string.day_planned_half)
        look.taken -> parts += stringResource(R.string.day_taken)
        else -> parts += stringResource(R.string.day_planned)
    }
    if (look.today) parts += stringResource(R.string.day_today)
    return parts.joinToString(", ")
}

/** The colour key under the calendar, in the same shapes as the days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(modifier: Modifier = Modifier) {
    val colors = dayColors()
    val shape = RoundedCornerShape(5.dp)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(R.string.legend_holiday) { Modifier.background(colors.holiday, shape) }
        LegendItem(R.string.legend_school) { Modifier.background(colors.school, shape) }
        LegendItem(R.string.legend_taken) { Modifier.background(colors.taken, shape) }
        LegendItem(R.string.legend_planned) {
            Modifier.border(BorderStroke(2.dp, colors.planned), shape)
        }
    }
}

@Composable
private fun LegendItem(
    label: Int,
    swatch: @Composable () -> Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).then(swatch()))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
