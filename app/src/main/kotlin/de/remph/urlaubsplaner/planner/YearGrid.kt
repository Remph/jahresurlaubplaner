package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.remph.urlaubsplaner.R
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * The whole year at a glance: twelve rows, one per month, 31 columns for the days. Too small to
 * tap a single day (a phone is about 10 dp per column), so a tap opens the tapped month.
 */
@Composable
fun YearGrid(
    view: YearView,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: androidx.compose.ui.unit.Dp = 20.dp,
) {
    val colors = dayColors()
    val locale = currentLocale()
    val ink = MaterialTheme.colorScheme.onSurface
    val faint = ink.copy(alpha = 0.07f)
    val looks =
        remember(view) {
            (1..12).map { month ->
                val first = YearMonth.of(view.year, month)
                (1..first.lengthOfMonth()).map { view.look(first.atDay(it)) }
            }
        }
    val description = stringResource(R.string.year_grid_description, view.year)
    // The canvas is one touch area, so TalkBack gets one action per month instead.
    val monthActions =
        (1..12).map { month ->
            val name =
                java.time.Month
                    .of(month)
                    .getDisplayName(TextStyle.FULL_STANDALONE, locale)
            CustomAccessibilityAction(name) {
                onMonthClick(YearMonth.of(view.year, month))
                true
            }
        }
    Row(
        modifier.fillMaxWidth().semantics {
            contentDescription = description
            customActions = monthActions
        },
    ) {
        Column {
            for (month in 1..12) {
                Text(
                    text =
                        java.time.Month
                            .of(month)
                            .getDisplayName(TextStyle.SHORT, locale)
                            .trimEnd('.'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp).height(rowHeight),
                    maxLines = 1,
                )
            }
        }
        Canvas(
            Modifier
                .weight(1f)
                .height(rowHeight * 12)
                .pointerInput(view.year) {
                    detectTapGestures { tap ->
                        val row = (tap.y / (size.height / 12f)).toInt().coerceIn(0, 11)
                        onMonthClick(YearMonth.of(view.year, row + 1))
                    }
                },
        ) {
            val column = size.width / 31f
            val row = size.height / 12f
            val gap = 1.dp.toPx()
            val corner = CornerRadius(2.dp.toPx())
            looks.forEachIndexed { monthIndex, days ->
                days.forEachIndexed { dayIndex, look ->
                    val topLeft = Offset(dayIndex * column + gap / 2, monthIndex * row + gap / 2)
                    val cell = Size(column - gap, row - gap)
                    val fill =
                        when {
                            look.taken -> colors.taken
                            look.holiday -> colors.holiday
                            look.school -> colors.school
                            !look.workDay -> faint
                            else -> null
                        }
                    if (fill != null) drawRoundRect(fill, topLeft, cell, corner)
                    if (look.mark != null && !look.taken) {
                        drawRoundRect(
                            colors.planned,
                            topLeft,
                            cell,
                            corner,
                            style = Stroke(1.5.dp.toPx()),
                        )
                    }
                    if (look.today) {
                        drawRoundRect(ink, topLeft, cell, corner, style = Stroke(1.5.dp.toPx()))
                    }
                }
            }
        }
    }
}
