package de.remph.urlaubsplaner.planner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.core.balance.DayMark
import de.remph.urlaubsplaner.core.holidays.Bundesland
import de.remph.urlaubsplaner.core.settings.UserSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun show(
        waitFor: String = "März 2027",
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = fontScale),
            ) {
                CalendarScreen(fixture.viewModel(), PaddingValues(), onOpenSettings = {})
            }
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
            swipeLeft(startX = centerX, endX = centerX - 200f)
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
    @Config(qualifiers = "de")
    fun `in a right to left layout the swipe directions are mirrored`() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CalendarScreen(fixture.viewModel(), PaddingValues(), onOpenSettings = {})
            }
        }
        waitForText("März 2027")

        compose.onRoot().performTouchInput { swipeRight() }

        waitForText("April 2027")
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

    private fun showYear(fontScale: Float = 1f) {
        show(fontScale = fontScale)
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
        yearGrid().assertHeightIsEqualTo(YearRowsMin * 12)

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
        assertExpanded()

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

        yearGrid().assertHeightIsEqualTo(YearRowsMin * 12)
    }

    private fun gridHeight() = yearGrid().getBoundsInRoot().height

    /** More than compact, and within what the expanded rows may be. */
    private fun assertExpanded() {
        val height = gridHeight()
        assertTrue("$height", height > YearRowsCompact * 12)
        assertTrue("$height", height >= YearRowsMin * 12 && height <= YearRowsMax * 12)
    }

    private fun expandRows() {
        compose
            .onNodeWithContentDescription("Jahresübersicht größer anzeigen")
            .performScrollTo()
            .performClick()
        compose.waitForIdle()
    }

    /** Where the page ends and where the screen does, to see whether the page scrolls. */
    private fun assertPageFillsScreenWithoutScrolling() {
        val legendBottom = compose.onNodeWithText("Geplant").getBoundsInRoot().bottom
        val screen = compose.onRoot().getBoundsInRoot().height
        // 16 dp frame padding below the legend; the rows are whole dp, so up to 12 dp are left.
        assertTrue("$legendBottom of $screen", legendBottom <= screen - 16.dp)
        assertTrue("$legendBottom of $screen", legendBottom >= screen - 16.dp - 13.dp)
    }

    @Test
    @Config(qualifiers = "de-w411dp-h900dp")
    fun `the expanded rows fill the screen and the page does not scroll`() {
        showYear()
        expandRows()

        assertExpanded()
        assertTrue(gridHeight() > YearRowsMin * 12 && gridHeight() < YearRowsMax * 12)
        assertPageFillsScreenWithoutScrolling()
    }

    @Test
    @Config(qualifiers = "de-w411dp-h900dp")
    fun `the legend entry for granted days takes room from the rows`() {
        turnGrantedDaysOn()
        showYear()
        expandRows()

        assertExpanded()
        assertPageFillsScreenWithoutScrolling()
    }

    @Test
    @Config(qualifiers = "de-w411dp-h900dp")
    fun `a larger font gives the rows less room but the page still fits`() {
        showYear(fontScale = 1.3f)
        expandRows()

        assertExpanded()
        assertPageFillsScreenWithoutScrolling()
    }

    @Test
    @Config(qualifiers = "de-w360dp-h640dp")
    fun `on a small phone the rows stop at the minimum and the page scrolls`() {
        showYear()
        expandRows()

        yearGrid().assertHeightIsEqualTo(YearRowsMin * 12)
    }

    @Test
    @Config(qualifiers = "de-w800dp-h360dp")
    fun `in landscape the rows stop at the minimum`() {
        showYear()
        expandRows()

        yearGrid().assertHeightIsEqualTo(YearRowsMin * 12)
    }

    @Test
    @Config(qualifiers = "de-w411dp-h2400dp")
    fun `on a tall screen the rows stop at the maximum`() {
        showYear()
        expandRows()

        yearGrid().assertHeightIsEqualTo(YearRowsMax * 12)
    }

    @Test
    @Config(qualifiers = "de")
    fun `after a step the old month is gone and the new one is drawn`() {
        show()

        compose.onNodeWithContentDescription("Nächster Monat").performClick()
        waitForText("April 2027")
        compose.waitForIdle()

        compose.onAllNodesWithText("März 2027").assertCountEquals(0)
        compose.onAllNodesWithText("Karfreitag").assertCountEquals(0)
        compose.onNodeWithText("Osterferien").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun `stepping between years with and without vacation days never mixes their pages`() {
        showYear()
        compose.mainClock.autoAdvance = false

        // 2027 has Urlaubstage, 2028 does not: the other year's empty state must not show under
        // this year's title, in either direction.
        for (button in listOf("Nächstes Jahr", "Vorheriges Jahr")) {
            compose.onNodeWithContentDescription(button).performClick()
            repeat(80) {
                Thread.sleep(5)
                compose.mainClock.advanceTimeByFrame()
                assertEmptyStatesMatchTitles()
            }
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEmptyStatesMatchTitles()
        compose.onAllNodesWithText("Für 2028", substring = true).assertCountEquals(0)
    }

    private fun assertEmptyStatesMatchTitles() {
        for (year in 2026..2028) {
            val empty =
                compose
                    .onAllNodesWithText("Für $year sind noch keine", substring = true)
                    .fetchSemanticsNodes()
            val title = compose.onAllNodesWithText("$year").fetchSemanticsNodes()
            assertTrue(
                "the empty state of $year without its title",
                empty.isEmpty() || title.isNotEmpty(),
            )
        }
    }

    private companion object {
        const val ANIMATION_MILLIS = 1_000L
    }

    private fun grantedSwitch() = compose.onNode(hasText("Genehmigt") and isToggleable())

    private fun turnGrantedDaysOn() =
        runBlocking {
            fixture.repository.saveSettings(
                UserSettings(land = Bundesland.NORDRHEIN_WESTFALEN, grantedDays = true),
            )
        }

    @Test
    @Config(qualifiers = "de")
    fun `with granted days off there is no switch, no legend entry and no words for it`() {
        runBlocking { fixture.repository.setGranted(listOf(fixture.date("2027-03-15")), true) }
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027, Urlaub geplant").performClick()

        compose.onNodeWithText("Ändern").assertIsDisplayed()
        compose.onNode(hasText("Genehmigt")).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `with granted days on a granted day says so in words and the legend has the entry`() {
        runBlocking { fixture.repository.setGranted(listOf(fixture.date("2027-03-15")), true) }
        turnGrantedDaysOn()
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027, Urlaub genehmigt").assertExists()
        day("Dienstag, 16. März 2027, Urlaub geplant").assertExists()
        compose.onNodeWithText("Genehmigt").assertExists()
    }

    @Test
    @Config(qualifiers = "de")
    fun `granting a planned day in the sheet stores it`() {
        turnGrantedDaysOn()
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027, Urlaub geplant").performClick()
        grantedSwitch().assertIsOff().performClick()
        compose.onNodeWithText("Ändern").performClick()

        compose.waitUntil(5_000) {
            compose
                .onAllNodes(hasContentDescription("Montag, 15. März 2027, Urlaub genehmigt"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(
            setOf(fixture.date("2027-03-15")),
            runBlocking { fixture.repository.readAll() }.granted,
        )
    }

    @Test
    @Config(qualifiers = "de")
    fun `the sheet starts with the flag the day has and a new mark starts as planned`() {
        runBlocking { fixture.repository.setGranted(listOf(fixture.date("2027-03-15")), true) }
        turnGrantedDaysOn()
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027, Urlaub genehmigt").performClick()
        grantedSwitch().assertIsOn()
        compose.onNodeWithText("Entfernen").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(hasContentDescription("Montag, 15. März 2027, Urlaub genehmigt"))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        // Marking the same day again must not bring the old flag back.
        day("Montag, 15. März 2027").performClick()
        grantedSwitch().assertIsOff()
    }

    @Test
    @Config(qualifiers = "de")
    fun `a day that is taken has no granted switch`() {
        turnGrantedDaysOn()
        show()
        waitForText("Karfreitag")

        day("Montag, 8. März 2027, Urlaub genommen").performClick()

        compose.onNodeWithText("Ändern").assertIsDisplayed()
        grantedSwitch().assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `changing a granted day to a half day with the setting off keeps it granted`() {
        runBlocking { fixture.repository.setGranted(listOf(fixture.date("2027-03-15")), true) }
        show()
        waitForText("Karfreitag")

        day("Montag, 15. März 2027, Urlaub geplant").performClick()
        compose.onNodeWithText("Halber Tag").performClick()
        compose.onNodeWithText("Ändern").performClick()

        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasContentDescription("Montag, 15. März 2027, Halber Urlaubstag geplant"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        val stored = runBlocking { fixture.repository.readAll() }
        assertEquals(DayMark.HALF, stored.marks.getValue(fixture.date("2027-03-15")))
        assertEquals(setOf(fixture.date("2027-03-15")), stored.granted)
    }
}
