package de.remph.urlaubsplaner.planner

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/** The page the calendar draws: month and body always from the same year. */
@RunWith(AndroidJUnit4::class)
class CalendarPageTest {
    @get:Rule
    val compose = createComposeRule()

    private val fixture = PlannerFixture().also { it.fill() }
    private val viewModels = ViewModelStore()

    @After
    fun close() {
        viewModels.clear()
        fixture.close()
    }

    private fun viewOf(year: Int): YearView {
        val model = fixture.viewModel().also { viewModels.put("planner$year", it) }
        model.showMonth(YearMonth.of(year, 1))
        return runBlocking {
            withTimeout(5_000) { model.calendar.filterNotNull().first { it.year == year } }
        }
    }

    @Test
    fun `a new year keeps the page of the old year until its own view has arrived`() {
        val view2027 = viewOf(2027)
        val view2028 = viewOf(2028)
        var view by mutableStateOf<YearView?>(view2027)
        var month by mutableStateOf(YearMonth.of(2027, 12))
        compose.setContent {
            val page = rememberCalendarPage(view, month, CalendarMode.MONTH)
            Text("${page?.month} ${page?.view?.year}")
        }
        compose.onNodeWithText("2027-12 2027").assertExists()

        // The header's month is already January, the view is still 2027's.
        month = YearMonth.of(2028, 1)
        compose.waitForIdle()
        compose.onNodeWithText("2027-12 2027").assertExists()

        view = view2028
        compose.waitForIdle()
        compose.onNodeWithText("2028-01 2028").assertExists()
    }

    @Test
    fun `there is no page for a view of another year`() {
        val view2028 = viewOf(2028)
        compose.setContent {
            val page = rememberCalendarPage(view2028, YearMonth.of(2027, 5), CalendarMode.MONTH)
            Text(page?.month?.toString() ?: "none")
        }

        compose.onNodeWithText("none").assertExists()
    }

    @Test
    fun `a step counts as a new generation but new data and a change of mode do not`() {
        val view = viewOf(2027)
        val first = CalendarPage(view, YearMonth.of(2027, 5), CalendarMode.MONTH)

        val nextMonth = first.next(view, YearMonth.of(2027, 6), CalendarMode.MONTH)
        val newData = first.next(viewOf(2027), YearMonth.of(2027, 5), CalendarMode.MONTH)
        val yearView = first.next(view, YearMonth.of(2027, 5), CalendarMode.YEAR)
        val sameYear = yearView.next(view, YearMonth.of(2027, 9), CalendarMode.YEAR)
        val nextYear = yearView.next(view, YearMonth.of(2028, 5), CalendarMode.YEAR)
        val backToMonth = sameYear.next(view, YearMonth.of(2027, 9), CalendarMode.MONTH)

        assertEquals(0, first.generation)
        assertEquals(1, nextMonth.generation)
        assertEquals(0, newData.generation)
        // The month or year view of the same place is the same content, swapped in place.
        assertEquals(0, yearView.generation)
        assertEquals(0, sameYear.generation)
        assertEquals(1, nextYear.generation)
        assertEquals(0, backToMonth.generation)
    }

    @Test
    fun `the expanded rows are what fits, between the minimum and the maximum`() {
        val screen = Dp(800f)
        val rest = Dp(368f)

        // 432 dp left for 12 rows.
        assertEquals(36f, expandedRowHeight(screen, rest).value, 0.001f)
        assertEquals(YearRowsMin, expandedRowHeight(Dp(600f), rest))
        assertEquals(YearRowsMax, expandedRowHeight(Dp(2000f), rest))
        assertEquals(50f, expandedRowHeight(Dp(968f), rest).value, 0.001f)
    }
}
