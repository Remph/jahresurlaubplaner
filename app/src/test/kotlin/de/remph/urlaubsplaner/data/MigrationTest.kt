package de.remph.urlaubsplaner.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.remph.urlaubsplaner.core.balance.DayMark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Opens the committed schema of each version and checks that migrating to the current one keeps
 * the data. With only version 1 there is nothing to migrate yet; when
 * [UrlaubsplanerDatabase.VERSION] goes up, add a test that starts at the old version, fills it
 * with rows, runs the migration and reads the rows back.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            UrlaubsplanerDatabase::class.java,
        )

    @Test
    fun `there is one migration for every version step`() {
        assertEquals(UrlaubsplanerDatabase.VERSION - 1, UrlaubsplanerDatabase.MIGRATIONS.size)
        UrlaubsplanerDatabase.MIGRATIONS.forEachIndexed { index, migration ->
            assertEquals(index + 1, migration.startVersion)
            assertEquals(index + 2, migration.endVersion)
        }
    }

    @Test
    fun `the exported schema of the current version matches the code`() {
        helper.createDatabase(TEST_DB, UrlaubsplanerDatabase.VERSION).close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            UrlaubsplanerDatabase.VERSION,
            true,
            *UrlaubsplanerDatabase.MIGRATIONS.toTypedArray(),
        )
    }

    @Test
    fun `a database from version 1 opens with the real builder and keeps its rows`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO marked_day (date, mark) VALUES ('2026-08-14', 'HALF')")
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database =
            Room
                .databaseBuilder(context, UrlaubsplanerDatabase::class.java, TEST_DB)
                .addMigrations(*UrlaubsplanerDatabase.MIGRATIONS.toTypedArray())
                .build()
        try {
            val marks =
                runBlocking {
                    UserDataRepository(database)
                        .observeMarks(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
                        .first()
                }
            assertEquals(mapOf(LocalDate.of(2026, 8, 14) to DayMark.HALF), marks)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
