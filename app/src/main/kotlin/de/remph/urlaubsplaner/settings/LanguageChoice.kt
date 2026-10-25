package de.remph.urlaubsplaner.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.remph.urlaubsplaner.R

/**
 * The app language is a setting of its own, not only the system language (Nick, 2026-10-25).
 * [AppCompatDelegate] keeps it, so it is not in the database or the backup file. With
 * [SYSTEM] the system language applies, and German when that is neither German nor English.
 */
enum class LanguageChoice(
    val tag: String,
) {
    SYSTEM(""),
    GERMAN("de"),
    ENGLISH("en"),
    ;

    @StringRes
    fun labelRes(): Int =
        when (this) {
            SYSTEM -> R.string.language_system
            GERMAN -> R.string.language_german
            ENGLISH -> R.string.language_english
        }

    companion object {
        /** The choice that belongs to the stored language tags; anything else counts as system. */
        fun fromTags(tags: String): LanguageChoice =
            entries.firstOrNull {
                it != SYSTEM &&
                    tags.substringBefore(',').substringBefore('-') == it.tag
            }
                ?: SYSTEM

        fun current(): LanguageChoice =
            fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        /** Restarts the visible activity in the new language. */
        fun apply(choice: LanguageChoice) =
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(choice.tag))
    }
}
