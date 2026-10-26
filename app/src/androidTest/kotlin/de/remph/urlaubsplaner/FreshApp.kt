package de.remph.urlaubsplaner

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** The app under test, the same instance the screens and the widget use. */
val targetApp: UrlaubsplanerApp
    get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as UrlaubsplanerApp

/**
 * Starts every test with an empty database and leaves it empty. The tests work on the app's real
 * database (the widget and the screens only know that one), so on a phone with real data they
 * would wipe it: the rule refuses to run anywhere but on an emulator, unless the run is started
 * with `-Pandroid.testInstrumentationRunnerArguments.allowRealDevice=true`.
 */
class FreshApp : TestWatcher() {
    override fun starting(description: Description) {
        val arguments = InstrumentationRegistry.getArguments()
        check(isEmulator() || arguments.getString(ALLOW_REAL_DEVICE) == "true") {
            "These tests empty the app's database. Run them on an emulator; on a device pass " +
                "$ALLOW_REAL_DEVICE=true only if its Urlaubsplaner data may be deleted (and make " +
                "a backup first)."
        }
        wipe()
    }

    override fun finished(description: Description) = wipe()

    private fun wipe() {
        // clearAllTables must not run on the main thread; the test thread is not the main thread,
        // but the IO dispatcher keeps that obvious.
        runBlocking(Dispatchers.IO) { targetApp.database.clearAllTables() }
    }

    private fun isEmulator() =
        Build.HARDWARE == "goldfish" ||
            Build.HARDWARE == "ranchu" ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.MODEL.contains("Emulator")

    private companion object {
        const val ALLOW_REAL_DEVICE = "allowRealDevice"
    }
}
