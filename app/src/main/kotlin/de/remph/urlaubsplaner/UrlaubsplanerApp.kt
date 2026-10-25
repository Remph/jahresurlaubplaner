package de.remph.urlaubsplaner

import android.app.Application
import de.remph.urlaubsplaner.data.BackupFileStore
import de.remph.urlaubsplaner.data.UrlaubsplanerDatabase
import de.remph.urlaubsplaner.data.UserDataRepository
import java.time.Clock

/**
 * Holds the one database and what is built on it. Screens and the ViewModels get their parts from
 * here; there is no dependency injection library because there are only four parts.
 */
class UrlaubsplanerApp : Application() {
    /** The only place that asks for "now", so every screen shares one clock. */
    val clock: Clock = Clock.systemDefaultZone()

    private val database by lazy { UrlaubsplanerDatabase.create(this) }

    val repository by lazy { UserDataRepository(database) }

    val backupStore by lazy { BackupFileStore(contentResolver) }
}
