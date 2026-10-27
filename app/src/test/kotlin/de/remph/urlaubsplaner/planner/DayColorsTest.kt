package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.appearance.FixedDark
import de.remph.urlaubsplaner.appearance.FixedLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The colours of the calendar days, in particular that a weekend number can be read. */
@RunWith(AndroidJUnit4::class)
class DayColorsTest {
    @get:Rule
    val compose = createComposeRule()

    /** One composition for all schemes: a compose rule can set its content only once per test. */
    private fun colorsFor(vararg schemes: ColorScheme): List<DayColors> {
        val colors = mutableListOf<DayColors>()
        compose.setContent {
            Column {
                schemes.forEach { scheme ->
                    MaterialTheme(colorScheme = scheme) { colors += dayColors() }
                }
            }
        }
        compose.waitForIdle()
        return colors
    }

    private fun assertReadable(vararg schemes: ColorScheme) {
        colorsFor(*schemes).zip(schemes).forEach { (colors, scheme) ->
            // The school holiday tile was the one that failed with the theme's soft grey.
            assertTrue(contrastRatio(colors.weekend, colors.school) >= MIN_TEXT_CONTRAST)
            assertTrue(
                contrastRatio(colors.weekend, scheme.surfaceContainerLow) >= MIN_TEXT_CONTRAST,
            )
        }
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

    // A Pixel "blue" wallpaper: the worst case, since taken and planned days take its primary.
    private val blueLight = lightColorScheme(primary = Color(0xFF0061A4), onPrimary = Color.White)
    private val blueDark =
        darkColorScheme(primary = Color(0xFF9FCAFF), onPrimary = Color(0xFF003258))

    @Test
    fun `weekend numbers can be read on the school holiday tile with a blue wallpaper`() {
        assertReadable(blueLight, blueDark)
    }

    @Test
    fun `school holidays are not drawn in blue, which is what wallpapers are most often`() {
        for (colors in colorsFor(FixedLight, FixedDark, blueLight, blueDark)) {
            for (color in listOf(colors.school, colors.schoolBar)) {
                assertFalse("hue ${hueOf(color)}", hueOf(color) in BLUE_HUES)
            }
        }
    }

    @Test
    fun `the school holiday tile differs from taken days by hue with a blue wallpaper`() {
        for (colors in colorsFor(blueLight, blueDark)) {
            assertTrue(hueDistance(hueOf(colors.school), hueOf(colors.taken)) >= MIN_HUE_GAP)
        }
    }

    @Test
    fun `the school holiday bar can be seen on its tile`() {
        for (colors in colorsFor(FixedLight, FixedDark)) {
            assertTrue(contrastRatio(colors.schoolBar, colors.school) >= MIN_GRAPHIC_CONTRAST)
        }
    }
}

private val BLUE_HUES = 180f..250f
private const val MIN_HUE_GAP = 30f
private const val MIN_GRAPHIC_CONTRAST = 3f

private fun hueOf(color: Color): Float {
    val (r, g, b) = listOf(color.red, color.green, color.blue)
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    val hue =
        when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
    return (hue + 360f) % 360f
}

private fun hueDistance(
    a: Float,
    b: Float,
): Float = minOf(kotlin.math.abs(a - b), 360f - kotlin.math.abs(a - b))
