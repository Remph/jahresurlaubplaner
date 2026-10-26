package de.remph.urlaubsplaner.core.ics

import java.util.Locale

/** The language of the texts in the calendar file: the app's own two languages. */
enum class IcsLanguage {
    GERMAN,
    ENGLISH,
    ;

    companion object {
        /**
         * English for an English locale, German for everything else: the app has only these two
         * sets of texts and falls back to German for any other system language (Nick, 2026-10-26).
         */
        fun of(locale: Locale): IcsLanguage = if (locale.language == "en") ENGLISH else GERMAN
    }
}
