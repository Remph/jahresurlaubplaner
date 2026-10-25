package de.remph.urlaubsplaner.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageChoiceTest {
    @Test
    fun `no stored language means the system language`() {
        assertEquals(LanguageChoice.SYSTEM, LanguageChoice.fromTags(""))
    }

    @Test
    fun `German and English are found with or without a region`() {
        assertEquals(LanguageChoice.GERMAN, LanguageChoice.fromTags("de"))
        assertEquals(LanguageChoice.GERMAN, LanguageChoice.fromTags("de-DE"))
        assertEquals(LanguageChoice.ENGLISH, LanguageChoice.fromTags("en"))
        assertEquals(LanguageChoice.ENGLISH, LanguageChoice.fromTags("en-GB,de"))
    }

    @Test
    fun `any other language counts as system, not as a guess`() {
        assertEquals(LanguageChoice.SYSTEM, LanguageChoice.fromTags("fr"))
        assertEquals(LanguageChoice.SYSTEM, LanguageChoice.fromTags("dev"))
    }

    @Test
    fun `the tag written is the tag read`() {
        for (choice in LanguageChoice.entries) {
            assertEquals(choice, LanguageChoice.fromTags(choice.tag))
        }
    }
}
