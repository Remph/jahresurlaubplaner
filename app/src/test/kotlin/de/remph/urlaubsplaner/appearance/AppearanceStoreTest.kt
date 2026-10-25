package de.remph.urlaubsplaner.appearance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = context.getSharedPreferences("appearance-store-test", 0)

    @Before
    fun clear() {
        preferences.edit().clear().commit()
    }

    @Test
    fun `a fresh store follows the system with the system colours on and no pure black`() {
        assertEquals(Appearance(ThemeMode.SYSTEM, oled = false, systemColors = true), state())
    }

    @Test
    fun `a change is kept for the next start`() {
        AppearanceStore(preferences).update {
            it.copy(mode = ThemeMode.DARK, oled = true, systemColors = false)
        }

        assertEquals(Appearance(ThemeMode.DARK, oled = true, systemColors = false), state())
    }

    @Test
    fun `an unknown stored mode falls back to the system`() {
        preferences.edit().putString("theme_mode", "SEPIA").commit()

        assertEquals(ThemeMode.SYSTEM, state().mode)
    }

    private fun state() = AppearanceStore(preferences).state.value
}
