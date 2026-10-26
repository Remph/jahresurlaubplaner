package de.remph.urlaubsplaner.planner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.balance.VacationDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** What a day is, put together once for the cell, the year grid and the spoken description. */
class DayLook(
    val holiday: Boolean,
    val school: Boolean,
    val mark: DayMark?,
    val taken: Boolean,
    val today: Boolean,
    val workDay: Boolean,
    /** A day to take for a possible Brückentag that is not marked yet. */
    val bridge: Boolean = false,
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
        bridge = mark == null && date in bridgeDays,
    )
}

/** The seven weekday names over the month grid, Monday first as in Germany. */
@Composable
private fun WeekdayHeader(locale: Locale) {
    Row(Modifier.fillMaxWidth()) {
        for (day in DayOfWeek.entries) {
            Text(
                text = day.shortText(locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The fade of a day's tile when it is marked or unmarked: short, so the status stays first. */
private val DayFade = tween<Color>(durationMillis = 150)

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
                        // Keyed by date: a cell that appears with a new month starts at its colours
                        // instead of fading from the previous month's day at the same place.
                        key(month.atDay(day)) {
                            DayCell(view, month.atDay(day), locale, onDayClick, Modifier.weight(1f))
                        }
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
    // The theme's large step, the same as the cards (design review, Nick, 2026-10-25).
    val shape = MaterialTheme.shapes.large
    val planned = look.mark != null && !look.taken
    // Every day is a tile of its own, so the days stand out from the background (Nick,
    // 2026-10-25). The tile's colour is the strongest thing the day is; a school holiday under a
    // taken day or a holiday still shows as a bar along the bottom (in the text colour on a taken
    // day, since the wallpaper's colour can be close to any fixed blue).
    val fillTarget =
        when {
            look.taken -> colors.taken
            look.holiday -> colors.holiday
            look.school -> colors.school
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    val textTarget =
        when {
            look.taken -> colors.onTaken
            look.holiday -> colors.onHoliday
            !look.workDay -> colors.weekend
            else -> MaterialTheme.colorScheme.onSurface
        }
    // Marking or removing a day fades the tile instead of flipping it. Compose follows Android's
    // "Remove animations" setting, so with it on the colours change at once.
    val fill by animateColorAsState(fillTarget, DayFade, label = "dayFill")
    val textColor by animateColorAsState(textTarget, DayFade, label = "dayText")
    val outline by animateColorAsState(
        if (planned) colors.planned else Color.Transparent,
        DayFade,
        label = "dayOutline",
    )
    val schoolBar = look.school && (look.taken || look.holiday || planned)
    // The touch area is the whole cell; the tile is drawn 2 dp inside it, so a narrow phone does
    // not lose 4 dp of the 48 dp target.
    Box(
        modifier =
            modifier
                .height(52.dp)
                .clickable { onClick(date) }
                .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(shape)
                    .background(fill)
                    .then(
                        when {
                            planned || outline.alpha > 0f -> {
                                Modifier.border(BorderStroke(2.dp, outline), shape)
                            }

                            look.bridge -> {
                                Modifier.dashedBorder(colors.planned, shape)
                            }

                            else -> {
                                Modifier
                            }
                        },
                    ),
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
                        .background(
                            if (look.taken) colors.onTaken else colors.schoolBar,
                            RoundedCornerShape(2.dp),
                        ),
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
    if (look.bridge) parts += stringResource(R.string.day_bridge)
    if (look.today) parts += stringResource(R.string.day_today)
    return parts.joinToString(", ")
}

/** The colour key under the calendar, in the same shapes as the days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(
    modifier: Modifier = Modifier,
    showBridge: Boolean = false,
    compact: Boolean = false,
) {
    val colors = dayColors()
    val shape = MaterialTheme.shapes.extraSmall
    val size = if (compact) 12.dp else 16.dp
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(R.string.legend_holiday, size, compact) {
            Modifier.background(colors.holiday, shape)
        }
        LegendItem(R.string.legend_school, size, compact) {
            Modifier.background(colors.school, shape)
        }
        LegendItem(R.string.legend_taken, size, compact) {
            Modifier.background(colors.taken, shape)
        }
        LegendItem(R.string.legend_planned, size, compact) {
            Modifier.border(BorderStroke(2.dp, colors.planned), shape)
        }
        if (showBridge) {
            LegendItem(R.string.legend_bridge, size, compact) {
                Modifier.dashedBorder(colors.planned, shape, dash = 4.dp, gap = 3.dp)
            }
        }
    }
}

/** A dashed outline in the shape of a tile: a day that could be marked, but is not yet. */
fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    dash: Dp = 6.dp,
    gap: Dp = 4.dp,
    width: Dp = 2.dp,
): Modifier =
    drawWithContent {
        drawContent()
        val stroke =
            Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
            )
        // The line is centred on the edge, so it is drawn half its width inside.
        inset(width.toPx() / 2) {
            drawOutline(shape.createOutline(size, layoutDirection, this), color, style = stroke)
        }
    }

/**
 * Says so when the school holiday file does not reach all of [from] to [to]: no colour then means
 * "not known", not "no school holidays". Nothing is shown while the file covers the time.
 */
@Composable
fun SchoolHolidaysUnknownNote(
    view: YearView,
    from: LocalDate,
    to: LocalDate,
) {
    if (view.schoolHolidaysKnown(from, to)) return
    Text(
        stringResource(R.string.school_holidays_unknown),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LegendItem(
    label: Int,
    size: Dp,
    compact: Boolean,
    swatch: @Composable () -> Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(size).then(swatch()))
        Text(
            text = stringResource(label),
            style =
                with(MaterialTheme.typography) { if (compact) labelSmall else bodySmall },
            modifier = Modifier.padding(start = if (compact) 4.dp else 6.dp),
        )
    }
}
