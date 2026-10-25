package de.remph.urlaubsplaner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities =
        [
            SettingsEntity::class,
            YearEntitlementEntity::class,
            MarkedDayEntity::class,
        ],
    version = UrlaubsplanerDatabase.VERSION,
    exportSchema = true,
)
abstract class UrlaubsplanerDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    abstract fun entitlementDao(): EntitlementDao

    abstract fun markedDayDao(): MarkedDayDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "urlaubsplaner.db"

        /**
         * One entry per version step, in order. Add the new `Migration` here whenever [VERSION]
         * goes up; a test fails if one is missing. There is deliberately no fallback to
         * destructive migration: this is the user's vacation data.
         */
        val MIGRATIONS: List<Migration> = emptyList()

        fun create(context: Context): UrlaubsplanerDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    UrlaubsplanerDatabase::class.java,
                    FILE_NAME,
                ).addMigrations(*MIGRATIONS.toTypedArray())
                .build()
    }
}
