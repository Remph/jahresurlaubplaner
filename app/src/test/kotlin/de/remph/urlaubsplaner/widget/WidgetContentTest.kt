package de.remph.urlaubsplaner.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.MappedNode
import androidx.glance.testing.unit.hasText
import androidx.glance.text.EmittableText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.MainActivity
import de.remph.urlaubsplaner.ui.Tab
import de.remph.urlaubsplaner.ui.TabRequest
import org.junit.Test
import org.junit.runner.RunWith

/** The layouts as Glance builds them; how they look on a launcher is for Nick's phone. */
@RunWith(AndroidJUnit4::class)
class WidgetContentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val status =
        WidgetModel.Status(
            number = "12,5",
            overbooked = false,
            unit = "Tage übrig",
            unitWithYear = "Tage übrig 2027",
            summary = "Nächster Urlaub 14.04.",
            tripLabel = "Nächster Urlaub",
            tripWhen = "14.–16. April",
            tripCost = "5 Urlaubstage",
            figures = listOf("Genommen 8", "Geplant 5,5", "Anspruch 26"),
            warning = null,
        )

    private fun open(tab: Tab) =
        Intent(context, MainActivity::class.java).setAction(TabRequest.action(tab))

    @Test
    fun `the wide widget shows the number, the trip, its cost and the three figures`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(WIDE_SIZE)
            provideComposable { WidgetContent(status, WidgetStyle()) }

            onNode(hasText("12,5")).assertExists()
            onNode(hasText("Tage übrig 2027")).assertExists()
            onNode(hasText("14.–16. April")).assertExists()
            onNode(hasText("5 Urlaubstage")).assertExists()
            onNode(hasText("Genommen 8")).assertExists()
            onNode(hasText("Geplant 5,5")).assertExists()
            onNode(hasText("Anspruch 26")).assertExists()
            // The one line of the small widget is not repeated.
            onNode(hasText("Nächster Urlaub 14.04.")).assertDoesNotExist()
        }

    @Test
    fun `the warning takes the place of the figures`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(WIDE_SIZE)
            val warned = status.copy(warning = "3 Tage Resturlaub verfallen am 31.03.")
            provideComposable { WidgetContent(warned, WidgetStyle()) }

            onNode(hasText("3 Tage Resturlaub verfallen am 31.03.")).assertExists()
            onNode(hasText("Genommen 8")).assertDoesNotExist()
        }

    @Test
    fun `the small widget shows the number and one line`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(SMALL_SIZE)
            provideComposable { WidgetContent(status, WidgetStyle()) }

            onNode(hasText("12,5")).assertExists()
            onNode(hasText("Tage übrig")).assertExists()
            onNode(hasText("Nächster Urlaub 14.04.")).assertExists()
            onNode(hasText("14.–16. April")).assertDoesNotExist()
            onNode(hasText("Genommen 8")).assertDoesNotExist()
        }

    @Test
    fun `a tap on a status opens the overview`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(SMALL_SIZE)
            provideComposable { WidgetContent(status, WidgetStyle()) }

            onNode(hasStartActivityClickAction(open(Tab.OVERVIEW))).assertExists()
            onNode(hasStartActivityClickAction(open(Tab.SETTINGS))).assertDoesNotExist()
        }

    @Test
    fun `a missing input is a prompt and a tap opens the settings`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(WIDE_SIZE)
            val prompt =
                WidgetModel.Prompt(
                    "Bundesland wählen",
                    "Dann zeigt das Widget deinen Urlaub.",
                )
            provideComposable { WidgetContent(prompt, WidgetStyle(transparencyPercent = 50)) }

            onNode(hasText("Bundesland wählen")).assertExists()
            onNode(hasText("Dann zeigt das Widget deinen Urlaub.")).assertExists()
            onNode(hasStartActivityClickAction(open(Tab.SETTINGS))).assertExists()
        }

    @Test
    fun `the small prompt is the title alone`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(SMALL_SIZE)
            val prompt = WidgetModel.Prompt("Urlaubstage eintragen", "Für 2027 fehlen sie noch.")
            provideComposable { WidgetContent(prompt, WidgetStyle()) }

            onNode(hasText("Urlaubstage eintragen")).assertExists()
            onNode(hasText("Für 2027 fehlen sie noch.")).assertDoesNotExist()
        }

    @Test
    fun `the widget draws at the size between the two layouts as the small one`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(
                androidx.compose.ui.unit
                    .DpSize(250.dp, 60.dp),
            )
            provideComposable { WidgetContent(status, WidgetStyle()) }

            onNode(hasText("Nächster Urlaub 14.04.")).assertExists()
        }

    private fun hasMaxLines(
        text: String,
        lines: Int,
    ) = GlanceNodeMatcher<MappedNode>("text \"$text\" with at most $lines lines") { node ->
        val emittable = node.value.emittable
        emittable is EmittableText && emittable.text == text && emittable.maxLines == lines
    }

    // Nick, 2026-10-26: "Noch kein Urlaub geplant" was cut to "Noch kein Urla..." on his phone.
    @Test
    fun `the trip texts of the wide widget may wrap to two lines`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(WIDE_SIZE)
            val none = status.copy(tripWhen = "Noch kein Urlaub geplant", tripCost = null)
            provideComposable { WidgetContent(none, WidgetStyle()) }

            onNode(hasMaxLines("Noch kein Urlaub geplant", 2)).assertExists()
            onNode(hasMaxLines("Nächster Urlaub", 2)).assertExists()
        }

    @Test
    fun `the Resturlaub warning may wrap to two lines`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(WIDE_SIZE)
            val warned = status.copy(warning = "3 Tage Resturlaub verfallen am 31.03.")
            provideComposable { WidgetContent(warned, WidgetStyle()) }

            onNode(hasMaxLines("3 Tage Resturlaub verfallen am 31.03.", 2)).assertExists()
        }
}
