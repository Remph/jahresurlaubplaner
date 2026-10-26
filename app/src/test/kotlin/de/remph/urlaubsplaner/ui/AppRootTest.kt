package de.remph.urlaubsplaner.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.appearance.AppearanceStore
import de.remph.urlaubsplaner.data.BackupFileStore
import de.remph.urlaubsplaner.planner.PlannerFixture
import de.remph.urlaubsplaner.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppRootTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var fixture: PlannerFixture

    // What the widget asks for through MainActivity.
    private var request by mutableStateOf<TabRequest?>(null)

    @Before
    fun open() {
        fixture = PlannerFixture()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings =
            SettingsViewModel(
                fixture.repository,
                BackupFileStore(context.contentResolver),
                fixture.clock,
            )
        val appearance = AppearanceStore(context.getSharedPreferences("app-root-test", 0))
        compose.setContent { AppRoot(settings, fixture.viewModel(), appearance, request) }
    }

    @After
    fun close() = fixture.close()

    // The title "Einstellungen" is on the page too, so pick the tab by its role.
    private fun tab(text: String) =
        hasText(text) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    @Config(qualifiers = "de")
    fun `the app starts on the overview with three tabs and asks for the Land`() {
        waitForText("Einstellungen öffnen")

        compose.onNode(tab("Übersicht")).assertIsSelected()
        compose.onNode(tab("Kalender")).assertIsDisplayed()
        compose.onNode(tab("Einstellungen")).assertIsDisplayed()
        compose.onNodeWithText("Wähle zuerst dein Bundesland", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the button on the overview opens the settings`() {
        waitForText("Einstellungen öffnen")

        compose.onNodeWithText("Einstellungen öffnen").performClick()

        compose.onNode(tab("Einstellungen")).assertIsSelected()
        waitForText("Bundesland wählen")
    }

    @Test
    @Config(qualifiers = "de")
    fun `tapping a tab switches to it`() {
        compose.onNode(tab("Kalender")).performClick()

        compose.onNode(tab("Kalender")).assertIsSelected()
        waitForText("Einstellungen öffnen")
    }

    @Test
    @Config(qualifiers = "de")
    fun `a request from the widget opens its tab`() {
        waitForText("Einstellungen öffnen")

        request = TabRequest(Tab.SETTINGS)

        compose.waitForIdle()
        compose.onNode(tab("Einstellungen")).assertIsSelected()
        waitForText("Bundesland wählen")
    }

    @Test
    @Config(qualifiers = "de")
    fun `a second tap on the widget opens the tab again after the user has left it`() {
        request = TabRequest(Tab.SETTINGS)
        compose.waitForIdle()
        compose.onNode(tab("Übersicht")).performClick()
        compose.onNode(tab("Übersicht")).assertIsSelected()

        request = TabRequest(Tab.SETTINGS)

        compose.waitForIdle()
        compose.onNode(tab("Einstellungen")).assertIsSelected()
    }

    @Test
    @Config(qualifiers = "de")
    fun `an old request does not pull the user back after a rotation or a tab change`() {
        request = TabRequest(Tab.SETTINGS)
        compose.waitForIdle()
        compose.onNode(tab("Kalender")).performClick()

        compose.waitForIdle()

        compose.onNode(tab("Kalender")).assertIsSelected()
    }
}
