package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Composes the real calendar: the month, the colours in words, and marking through the sheet. */
@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var fixture: PlannerFixture

    @Before
    fun open() {
        fixture = PlannerFixture()
        fixture.fill()
    }

    @After
    fun close() = fixture.close()

    private fun show(waitFor: String = "März 2027") {
        compose.setContent {
            CalendarScreen(fixture.viewModel(), PaddingValues(), onOpenSettings = {})
        }
        waitForText(waitFor)
    }

    private fun waitForText(
        text: String,
        substring: Boolean = false,
    ) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text, substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun day(description: String) =
        compose.onNode(hasContentDescription(description, substring = true))

    @Test
    @Config(qualifiers = "de")
    fun `the calendar shows this month with weekday names`() {
        show()

        compose.onNodeWithText("Mo").assertIsDisplayed()
        compose.onNodeWithText("So").assertIsDisplayed()
        compose.onNodeWithText("Monat").assertIsDisplayed()
        compose.onNodeWithText("Jahr").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `days say in words what the colours say`() {
        show()
        waitForText("Karfreitag")

        day("Freitag, 26. März 2027, Feiertag: Karfreitag").assertExists()
        day("Montag, 8. März 2027, Urlaub genommen").assertExists()
        day("Montag, 15. März 2027, Urlaub geplant").assertExists()
        day("Mittwoch, 10. März 2027, heute").assertExists()
        day("Dienstag, 30. März 2027, Schulferien").assertExists()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the month lists its holidays and school holidays by name`() {
        show()
        waitForText("Karfreitag")

        compose.onNodeWithText("Karfreitag").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ostermontag").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Osterferien").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the month card adds up the days taken and planned in the month`() {
        show()
        waitForText("Karfreitag")

        compose.onNodeWithText("2 Tage").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5 Tage").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `stepping to the next month shows April`() {
        show()

        compose.onNodeWithContentDescription("Nächster Monat").performClick()

        waitForText("April 2027")
        compose.onNodeWithText("Osterferien").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `swiping left shows the next month and swiping right the previous one`() {
        show()

        compose.onRoot().performTouchInput { swipeLeft() }
        waitForText("April 2027")

        compose.onRoot().performTouchInput { swipeRight() }
        waitForText("März 2027")
        compose.onRoot().performTouchInput { swipeRight() }
        waitForText("Februar 2027")
    }

    @Test
    @Config(qualifiers = "de")
    fun `a swipe that starts on a day steps the month and does not open the sheet`() {
        show()
        waitForText("Karfreitag")

        day("Donnerstag, 25. März 2027").performTouchInput {
            swipeLeft(
                startX = centerX,
                endX =
                    centerX - 200f,
            )
        }

        waitForText("April 2027")
        compose.onNodeWithText("Ganzer Tag").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `a short drag does not change the month`() {
        show()

        compose.onRoot().performTouchInput { swipeLeft(startX = centerX, endX = centerX - 20f) }
        compose.waitForIdle()

        compose.onNodeWithText("März 2027").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `in the year mode a swipe steps a whole year`() {
        showYear()

        compose.onRoot().performTouchInput { swipeLeft() }
        waitForText("2028")
        compose.onRoot().performTouchInput { swipeRight() }
        compose.onRoot().performTouchInput { swipeRight() }
        waitForText("2026")
    }

    @Test
    @Config(qualifiers = "de")
    fun `swiping at the end of the calendar stays on the last month`() {
        show()
        repeat(10) { compose.onNodeWithContentDescription("Nächster Monat").performClick() }
        waitForText("Januar 2028")
        repeat(11) { compose.onNodeWithContentDescription("Nächster Monat").performClick() }
        waitForText("Dezember 2028")

        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitForIdle()

        compose.onNodeWithText("Dezember 2028").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `the English calendar swipes too`() {
        show("March 2027")

        compose.onRoot().performTouchInput { swipeLeft() }

        waitForText("April 2027")
    }

    @Test
    @Config(qualifiers = "de")
    fun `a month outside the school holiday file says they are unknown`() {
        show()

        repeat(10) { compose.onNodeWithContentDescription("Nächster Monat").performClick() }

        waitForText("Januar 2028")
        // The new year's data arrives a moment after the title changes.
        waitForText("noch keine Schulferien", substring = true)
        compose
            .onNodeWithText("noch keine Schulferien", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `tapping a working day opens the sheet and marking it stores the day`() {
        show()
        waitForText("Karfreitag")

        day("Donnerstag, 25. März 2027").performClick()
        waitForText("Donnerstag, 25. März 2027")
        compose.onNodeWithText("Ganzer Tag").assertIsDisplayed()
        compose.onNodeWithText("Bis: 25.03.2027").assertIsDisplayed()
        compose.onNodeWithText("Kostet 1 Urlaubstag, danach noch 32 übrig").assertIsDisplayed()
        compose.onNodeWithText("Eintragen").performClick()

        compose.waitUntil(5_000) {
            compose
                .onAllNodes(hasContentDescription("Donnerstag, 25. März 2027, Urlaub geplant"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    @Config(qualifiers = "de")
    fun `a half day shows what it costs`() {
        show()
        waitForText("Karfreitag")

        day("Donnerstag, 25. März 2027").performClick()
        compose.onNodeWithText("Halber Tag").performClick()

        compose.onNodeWithText("Kostet 0,5 Urlaubstage, danach noch 32,5 übrig").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `a marked day offers Ändern and Entfernen and removing clears it`() {
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027").performClick()
        compose.onNodeWithText("Ändern").assertIsDisplayed()
        compose.onNodeWithText("Entfernen").performClick()

        compose.waitUntil(5_000) {
            compose
                .onAllNodes(hasContentDescription("Montag, 15. März 2027, Urlaub geplant"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    @Config(qualifiers = "de")
    fun `a weekend day cannot be marked`() {
        show()
        waitForText("Karfreitag")

        day("Samstag, 13. März 2027").performClick()

        compose.onNodeWithText("kostet keinen Urlaub", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Eintragen").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `a public holiday names itself in the sheet and cannot be marked`() {
        show()
        waitForText("Karfreitag")

        day("Freitag, 26. März 2027").performClick()

        compose.onNodeWithText("kostet keinen Urlaub", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Eintragen").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the year mode shows the whole year and the way back to a month`() {
        show()

        compose.onNodeWithText("Jahr").performClick()

        compose
            .onNode(hasContentDescription("Jahresübersicht 2027", substring = true))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Nächstes Jahr").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the year mode says too when the school holidays of that year are unknown`() {
        show()
        compose.onNodeWithText("Jahr").performClick()

        compose.onNodeWithContentDescription("Nächstes Jahr").performClick()

        waitForText("noch keine Schulferien", substring = true)
        compose
            .onNodeWithText("noch keine Schulferien", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `the year grid offers a TalkBack action per month that opens it`() {
        show()
        compose.onNodeWithText("Jahr").performClick()
        waitForText("2027")

        val grid = compose.onNode(hasContentDescription("Jahresübersicht 2027", substring = true))
        val actions = grid.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(12, actions.size)
        assertEquals("April", actions[3].label)
        compose.runOnUiThread { actions[3].action() }

        waitForText("April 2027")
    }

    @Test
    @Config(qualifiers = "en")
    fun `the English calendar has English month names and day descriptions`() {
        show("March 2027")
        waitForText("Good Friday")

        day("Friday, March 26, 2027, Public holiday: Good Friday").assertExists()
        day("Monday, March 8, 2027, Vacation taken").assertExists()
        compose.onNodeWithText("Easter holidays").performScrollTo().assertIsDisplayed()
    }

    private fun yearGrid() =
        compose.onNode(hasContentDescription("Jahresübersicht 2027", substring = true))

    private fun showYear() {
        show()
        compose.onNodeWithText("Jahr").performClick()
        waitForText("Jan")
    }

    @Test
    @Config(qualifiers = "de")
    fun `the year grid starts compact and the handle expands and collapses it`() {
        showYear()
        yearGrid().assertHeightIsEqualTo(YearRowsCompact * 12)

        compose
            .onNodeWithContentDescription("Jahresübersicht größer anzeigen")
            .performScrollTo()
            .performClick()
        compose.mainClock.advanceTimeBy(ANIMATION_MILLIS)
        yearGrid().assertHeightIsEqualTo(YearRowsExpanded * 12)

        compose
            .onNodeWithContentDescription("Jahresübersicht kleiner anzeigen")
            .performScrollTo()
            .performClick()
        compose.mainClock.advanceTimeBy(ANIMATION_MILLIS)
        yearGrid().assertHeightIsEqualTo(YearRowsCompact * 12)
    }

    @Test
    @Config(qualifiers = "de-w411dp-h1250dp")
    fun `swiping down expands the year grid and swiping up makes it compact again`() {
        showYear()

        yearGrid().performTouchInput { swipeDown() }
        compose.waitForIdle()
        yearGrid().assertHeightIsEqualTo(YearRowsExpanded * 12)

        yearGrid().performTouchInput { swipeUp() }
        compose.waitForIdle()
        yearGrid().assertHeightIsEqualTo(YearRowsCompact * 12)
    }

    @Test
    @Config(qualifiers = "de-w411dp-h500dp")
    fun `on a short screen a swipe up scrolls the expanded grid before it makes it compact`() {
        showYear()
        compose
            .onNodeWithContentDescription("Jahresübersicht größer anzeigen")
            .performScrollTo()
            .performClick()
        compose.waitForIdle()

        yearGrid().performTouchInput { swipeUp() }
        compose.waitForIdle()

        yearGrid().assertHeightIsEqualTo(YearRowsExpanded * 12)
    }

    private companion object {
        const val ANIMATION_MILLIS = 1_000L
    }
}
