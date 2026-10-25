package de.remph.urlaubsplaner.ui

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.appearance.AppearanceStore
import de.remph.urlaubsplaner.data.BackupFileStore
import de.remph.urlaubsplaner.data.UrlaubsplanerDatabase
import de.remph.urlaubsplaner.data.UserDataRepository
import de.remph.urlaubsplaner.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppRootTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var database: UrlaubsplanerDatabase

    @Before
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrlaubsplanerDatabase::class.java).build()
        val clock = Clock.fixed(Instant.parse("2027-01-15T10:00:00Z"), ZoneOffset.UTC)
        val viewModel =
            SettingsViewModel(
                UserDataRepository(database),
                BackupFileStore(context.contentResolver),
                clock,
            )
        val appearance = AppearanceStore(context.getSharedPreferences("app-root-test", 0))
        compose.setContent { AppRoot(viewModel, appearance) }
    }

    @After
    fun close() = database.close()

    // The title "Einstellungen" is on the page too, so pick the tab by its role.
    private fun tab(text: String) =
        hasText(text) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test
    @Config(qualifiers = "de")
    fun `the app starts on the settings tab with three tabs along the bottom`() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Bundesland wählen").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(tab("Übersicht")).assertIsDisplayed()
        compose.onNode(tab("Kalender")).assertIsDisplayed()
        compose.onNode(tab("Einstellungen")).assertIsSelected()
    }

    @Test
    @Config(qualifiers = "de")
    fun `tapping a tab switches to it`() {
        compose.onNode(tab("Kalender")).performClick()

        compose.onNode(tab("Kalender")).assertIsSelected()
        compose.onNodeWithText("Der Kalender kommt als Nächstes.").assertIsDisplayed()
    }
}
