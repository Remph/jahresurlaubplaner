package de.remph.urlaubsplaner.planner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The colours of the calendar. A holiday is always gold and school holidays always lilac,
 * also with system colours (the maintainer, 2026-10-25): the meaning must not change with the wallpaper.
 * Taken days use the theme's primary colour, so they follow the wallpaper. School holidays were
 * light blue until 0.9.1; with a blue wallpaper they looked like taken and planned days, so the
 * maintainer chose lilac over green, a thin stripe and a fixed taken colour (2026-10-26).
 */
@Immutable
class DayColors(
    val holiday: Color,
    val onHoliday: Color,
    val school: Color,
    val schoolBar: Color,
    val taken: Color,
    val onTaken: Color,
    val planned: Color,
    val weekend: Color,
)

@Composable
fun dayColors(): DayColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val school = if (dark) Color(0xFF3D3560) else Color(0xFFE6DFF7)
    return DayColors(
        holiday = if (dark) Color(0xFF6B5320) else Color(0xFFF7E3B5),
        onHoliday = if (dark) Color(0xFFF3D9A0) else Color(0xFF5A3C0A),
        school = school,
        schoolBar = if (dark) Color(0xFFBBA8F0) else Color(0xFF6B50B5),
        taken = scheme.primary,
        onTaken = scheme.onPrimary,
        planned = scheme.primary,
        weekend =
            weekendTextColor(
                scheme.onSurfaceVariant,
                listOf(scheme.surfaceContainerLow, school),
                if (dark) Color(0xFFB9C5CF) else Color(0xFF44525E),
            ),
    )
}

/** The least contrast (WCAG AA) text needs against its background. */
internal const val MIN_TEXT_CONTRAST = 4.5f

/**
 * The colour of a weekend's day number: the theme's soft grey while it can be read on every tile
 * the number can sit on, else [fallback]. On the school holiday tile the soft grey had 3.7:1 in
 * the dark theme and 4.4:1 in the light one (design review, the maintainer, 2026-10-25).
 */
internal fun weekendTextColor(
    soft: Color,
    backgrounds: List<Color>,
    fallback: Color,
): Color = if (backgrounds.all { contrastRatio(soft, it) >= MIN_TEXT_CONTRAST }) soft else fallback

/** WCAG contrast ratio of two opaque colours, 1 to 21. */
internal fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val (lighter, darker) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (lighter + 0.05f) / (darker + 0.05f)
}
