package de.remph.urlaubsplaner.planner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The colours of the calendar. A holiday is always gold and school holidays always light blue,
 * also with system colours (Nick, 2026-10-25): the meaning must not change with the wallpaper.
 * Taken days use the theme's primary colour, so they follow the wallpaper.
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
    return DayColors(
        holiday = if (dark) Color(0xFF6B5320) else Color(0xFFF7E3B5),
        onHoliday = if (dark) Color(0xFFF3D9A0) else Color(0xFF5A3C0A),
        school = if (dark) Color(0xFF244A68) else Color(0xFFD3E8F8),
        schoolBar = if (dark) Color(0xFF5B97C9) else Color(0xFF6FA9D8),
        taken = scheme.primary,
        onTaken = scheme.onPrimary,
        planned = scheme.primary,
        weekend = scheme.onSurfaceVariant,
    )
}
