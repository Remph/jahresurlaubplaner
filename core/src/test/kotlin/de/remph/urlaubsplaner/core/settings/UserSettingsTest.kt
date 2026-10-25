package de.remph.urlaubsplaner.core.settings

import de.remph.urlaubsplaner.core.balance.VacationDays
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.holidays.RegionalExtra
import de.remph.urlaubsplaner.core.holidays.RegionalOptOut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserSettingsTest {
    @Test
    fun `a new user has no Land and works Monday to Friday`() {
        val settings = UserSettings()

        assertNull(settings.land)
        assertEquals(VacationDays.ONE, settings.schedule.workingValue(LocalDate.of(2026, 10, 26)))
        assertEquals(VacationDays.ZERO, settings.schedule.workingValue(LocalDate.of(2026, 10, 25)))
        assertEquals(VacationDays.ONE, settings.schedule.workingValue(LocalDate.of(2026, 10, 30)))
    }

    @Test
    fun `without a Land there is no holiday check`() {
        assertNull(UserSettings().publicHolidayCheck())
    }

    @Test
    fun `the holiday check follows Land, extras and opt-outs`() {
        val assumption = LocalDate.of(2026, 8, 15)
        val friedensfest = LocalDate.of(2026, 8, 8)

        val bayern = UserSettings(land = Bundesland.BAYERN).publicHolidayCheck()!!
        assertTrue(bayern(assumption))
        assertFalse(bayern(friedensfest))
        assertTrue(bayern(LocalDate.of(2026, 12, 25)))
        assertFalse(bayern(LocalDate.of(2026, 12, 23)))

        val optedOut =
            UserSettings(
                land = Bundesland.BAYERN,
                optOuts = setOf(RegionalOptOut.MARIAE_HIMMELFAHRT_BAYERN),
                extras = setOf(RegionalExtra.AUGSBURGER_FRIEDENSFEST),
            ).publicHolidayCheck()!!
        assertFalse(optedOut(assumption))
        assertTrue(optedOut(friedensfest))
    }

    @Test
    fun `the holiday check answers for several years with one function`() {
        val check = UserSettings(land = Bundesland.BERLIN).publicHolidayCheck()!!

        assertTrue(check(LocalDate.of(2026, 1, 1)))
        assertTrue(check(LocalDate.of(2027, 1, 1)))
        assertFalse(check(LocalDate.of(2027, 1, 2)))
    }
}
