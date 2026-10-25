package de.remph.urlaubsplaner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
        /** Version 2: the setting that suggests the unused days as next year's Resturlaub. */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(connection: SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE settings ADD COLUMN suggestCarryOver INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        const val VERSION = 2
        const val FILE_NAME = "urlaubsplaner.db"

        /**
         * One entry per version step, in order. Add the new `Migration` here whenever [VERSION]
         * goes up; a test fails if one is missing. There is deliberately no fallback to
         * destructive migration: this is the user's vacation data.
         */
        val MIGRATIONS: List<Migration> = listOf(MIGRATION_1_2)

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
