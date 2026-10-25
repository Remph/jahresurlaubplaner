package de.remph.urlaubsplaner.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Only proves that Kotlin, JUnit and the injected-clock pattern work. Remove it once the
// first real rule test exists.
class ToolchainSmokeTest {
    @Test
    fun `a fixed clock gives the date the logic is told`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val clock = Clock.fixed(Instant.parse("2027-03-28T10:00:00Z"), berlin)

        assertEquals(LocalDate.of(2027, 3, 28), LocalDate.now(clock))
    }
}
