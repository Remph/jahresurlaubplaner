package de.remph.urlaubsplaner.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.settings.UserSettings
import de.remph.urlaubsplaner.data.BackupFileStore
import de.remph.urlaubsplaner.data.UrlaubsplanerDatabase
import de.remph.urlaubsplaner.data.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Composes the real page on the JVM (Robolectric) and looks at what it shows. It does not replace
 * trying the app on a phone: pickers, the keyboard and TalkBack are not covered.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var database: UrlaubsplanerDatabase
    private lateinit var repository: UserDataRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrlaubsplanerDatabase::class.java).build()
        repository = UserDataRepository(database)
        val clock = Clock.fixed(Instant.parse("2027-01-15T10:00:00Z"), ZoneOffset.UTC)
        viewModel = SettingsViewModel(repository, BackupFileStore(context.contentResolver), clock)
    }

    @After
    fun close() = database.close()

    private fun show(title: String) {
        compose.setContent { SettingsRoute(viewModel) }
        waitForText(title)
    }

    /** Room reports from its own threads, so the page appears a moment after composing. */
    private fun waitForText(
        text: String,
        substring: Boolean = false,
    ) = compose.waitUntil(timeoutMillis = 5_000) {
        compose.onAllNodesWithText(text, substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun chooseLand(
        land: Bundesland,
        shownAs: String,
    ) {
        runBlocking { repository.saveSettings(UserSettings(land = land)) }
        waitForText(shownAs)
    }

    @Test
    @Config(qualifiers = "de")
    fun `a fresh start in German asks for the Land and shows nothing else about vacation`() {
        show("Einstellungen")

        compose.onNodeWithText("Einstellungen").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Welches Bundesland gilt für dich?",
                substring = true,
            ).assertIsDisplayed()
        compose.onNodeWithText("Bundesland wählen").assertIsDisplayed()
        compose.onNodeWithText("Urlaubstage im Jahr").assertDoesNotExist()
        // Restoring a backup is possible from the start, saving one is pointless with no data.
        compose.onNodeWithText("Sicherung wiederherstellen").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sicherung speichern").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en")
    fun `a fresh start in English has the English texts`() {
        show("Settings")

        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Choose state").assertIsDisplayed()
        compose.onNodeWithText("Restore backup").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `tapping the button opens the list of all 16 Länder`() {
        show("Einstellungen")

        compose.onNodeWithText("Bundesland wählen").performClick()

        compose.onNodeWithText("Baden-Württemberg").assertIsDisplayed()
        compose.onNodeWithText("Bayern").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `with a Land the vacation, working days and language sections appear`() {
        show("Einstellungen")
        chooseLand(Bundesland.NORDRHEIN_WESTFALEN, "Nordrhein-Westfalen")

        compose.onNodeWithText("Nordrhein-Westfalen").assertIsDisplayed()
        compose.onNodeWithText("Urlaubstage im Jahr").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Resturlaub").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Heiligabend (24.12.)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Deutsch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sicherung speichern").performScrollTo().assertIsDisplayed()
        // The year stepper has descriptions for TalkBack.
        compose.onNodeWithContentDescription("Vorheriges Jahr").assertExists()
        compose.onNodeWithContentDescription("Nächstes Jahr").assertExists()
        // The Bayern-only holiday does not show for another Land.
        compose.onNodeWithText("Mariä Himmelfahrt", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the Bayern holiday switch only shows for Bayern`() {
        show("Einstellungen")
        chooseLand(Bundesland.BAYERN, "Bayern")

        compose
            .onNodeWithText(
                "Mariä Himmelfahrt",
                substring = true,
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `typing days enables Speichern and a wrong amount shows the error`() {
        show("Einstellungen")
        chooseLand(Bundesland.NORDRHEIN_WESTFALEN, "Nordrhein-Westfalen")
        compose.onNodeWithText("Speichern").performScrollTo().assertIsNotEnabled()

        compose.onNodeWithText("Urlaubstage im Jahr").performScrollTo().performTextInput("12,3")
        compose.onNodeWithText("Bitte ganze oder halbe Tage", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Speichern").assertIsNotEnabled()

        compose.onNodeWithText("Urlaubstage im Jahr").performTextClearance()
        compose.onNodeWithText("Urlaubstage im Jahr").performTextInput("12,5")
        compose.onNodeWithText("Speichern").assertIsEnabled()
    }
}
