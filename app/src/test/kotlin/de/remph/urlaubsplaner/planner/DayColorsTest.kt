package de.remph.urlaubsplaner.planner

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.appearance.FixedDark
import de.remph.urlaubsplaner.appearance.FixedLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The colours of the calendar days, in particular that a weekend number can be read. */
@RunWith(AndroidJUnit4::class)
class DayColorsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun colorsFor(scheme: ColorScheme): DayColors {
        lateinit var colors: DayColors
        compose.setContent { MaterialTheme(colorScheme = scheme) { colors = dayColors() } }
        compose.waitForIdle()
        return colors
    }

    private fun assertReadable(scheme: ColorScheme) {
        val colors = colorsFor(scheme)

        // The school holiday tile was the one that failed with the theme's soft grey.
        assertTrue(contrastRatio(colors.weekend, colors.school) >= MIN_TEXT_CONTRAST)
        assertTrue(contrastRatio(colors.weekend, scheme.surfaceContainerLow) >= MIN_TEXT_CONTRAST)
    }

    @Test
    fun `weekend numbers can be read on every tile in the dark theme`() = assertReadable(FixedDark)

    @Test
    fun `weekend numbers can be read on every tile in the light theme`() =
        assertReadable(FixedLight)

    @Test
    fun `the theme's soft grey is kept where it can be read and replaced where it cannot`() {
        val white = Color.White
        val black = Color.Black
        val grey = Color(0xFF6B6B6B)

        assertEquals(grey, weekendTextColor(grey, listOf(white), black))
        assertEquals(black, weekendTextColor(grey, listOf(white, Color(0xFF888888)), black))
    }

    @Test
    fun `contrast of black on white is 21 to 1 and of a colour with itself 1 to 1`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), 0.001f)
    }
}
