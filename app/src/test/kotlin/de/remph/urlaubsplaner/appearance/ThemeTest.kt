package de.remph.urlaubsplaner.appearance

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {
    private val wallpaper = darkColorScheme(primary = Color(0xFFFF00FF))

    @Test
    fun `system colours are used when on and available`() {
        val colors = appColorScheme(true, Appearance(), wallpaper)

        assertEquals(wallpaper.primary, colors.primary)
    }

    @Test
    fun `the app's own colours are used when system colours are off`() {
        val colors = appColorScheme(true, Appearance(systemColors = false), wallpaper)

        assertEquals(FixedDark.primary, colors.primary)
    }

    @Test
    fun `the app's own colours are used where the phone has no system colours`() {
        assertEquals(FixedLight.primary, appColorScheme(false, Appearance(), null).primary)
        assertEquals(FixedDark.primary, appColorScheme(true, Appearance(), null).primary)
    }

    @Test
    fun `pure black makes the dark background black but cards stay apart from it`() {
        val colors = appColorScheme(true, Appearance(oled = true, systemColors = false), null)

        assertEquals(Color.Black, colors.background)
        assertEquals(Color.Black, colors.surface)
        assertNotEquals(Color.Black, colors.surfaceContainerLow)
        assertNotEquals(colors.surfaceContainerLow, colors.surfaceContainerHigh)
    }

    @Test
    fun `pure black also applies on top of the system colours`() {
        val colors = appColorScheme(true, Appearance(oled = true), wallpaper)

        assertEquals(Color.Black, colors.background)
        assertEquals(wallpaper.primary, colors.primary)
    }

    @Test
    fun `pure black does nothing in the light theme`() {
        val colors = appColorScheme(false, Appearance(oled = true, systemColors = false), null)

        assertEquals(FixedLight.background, colors.background)
    }
}
