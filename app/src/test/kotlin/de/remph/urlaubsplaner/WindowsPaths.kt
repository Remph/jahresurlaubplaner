package de.remph.urlaubsplaner

import org.junit.Assume.assumeFalse

/**
 * Skips the calling test on Windows. Some AndroidX libraries compare file paths after a
 * hard-coded '/': androidx.sqlite 2.6.2 (database name check in `SupportSQLiteDriver`, reached
 * through Room's `MigrationTestHelper`) and androidx.core 1.18.0 (`FileProvider.belongsToRoot`).
 * Robolectric's temp paths use '\' on Windows, so those calls throw there, while the same code
 * works on a phone and on Linux (the cloud runs these tests).
 */
fun assumeUnixPaths() {
    assumeFalse(
        "AndroidX assumes '/' as the path separator",
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true),
    )
}
