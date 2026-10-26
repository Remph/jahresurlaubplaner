package de.remph.urlaubsplaner.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import de.remph.urlaubsplaner.settings.LanguageChoice
import java.util.Locale

/** The app language as the widget needs it: a [Context] to read the texts from, and its locale. */
class WidgetLanguage(
    val context: Context,
    val locale: Locale,
) {
    companion object {
        /**
         * The widget lives outside any activity, so the language the user chose in the app is not
         * applied to its context; this builds one. With the system language it is the system's,
         * and German when that is neither German nor English, the same rule as in the app
         * (Nick, 2026-10-26).
         */
        fun of(context: Context): WidgetLanguage = of(context, LanguageChoice.stored(context))

        // Not an app bundle split by language (the APK carries both languages), so nothing to
        // download that lint looks for.
        @SuppressLint("AppBundleLocaleChanges")
        fun of(
            context: Context,
            choice: LanguageChoice,
        ): WidgetLanguage {
            val system = context.resources.configuration.locales[0]
            val locale =
                when (choice) {
                    LanguageChoice.GERMAN -> {
                        Locale.GERMAN
                    }

                    LanguageChoice.ENGLISH -> {
                        Locale.ENGLISH
                    }

                    LanguageChoice.SYSTEM -> {
                        if (system.language == "de" || system.language == "en") {
                            system
                        } else {
                            Locale.GERMAN
                        }
                    }
                }
            val configuration =
                Configuration(context.resources.configuration).apply {
                    setLocales(LocaleList(locale))
                }
            return WidgetLanguage(context.createConfigurationContext(configuration), locale)
        }
    }
}
