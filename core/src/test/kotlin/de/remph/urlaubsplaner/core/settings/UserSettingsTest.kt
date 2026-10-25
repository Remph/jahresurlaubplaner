package de.remph.urlaubsplaner.core.settings

import de.remph.urlaubsplaner.core.balance.VacationDays
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
}
