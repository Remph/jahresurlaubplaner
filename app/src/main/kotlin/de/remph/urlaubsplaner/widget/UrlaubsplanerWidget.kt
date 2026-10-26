package de.remph.urlaubsplaner.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import de.remph.urlaubsplaner.UrlaubsplanerApp
import de.remph.urlaubsplaner.core.widget.WidgetStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * The home screen widget, one widget with two layouts: the launcher picks by the size the user
 * gives it (small 2x1, wide 4x2). Each widget keeps its own [WidgetStyle] in Glance's state.
 */
class UrlaubsplanerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val refresher = (context.applicationContext as UrlaubsplanerApp).widgetRefresher
        // Taken before the first read, so a change in between reads the data again.
        val seen = refresher.revision.value
        val first = load(context)
        provideContent {
            val model = liveModel(first, seen, refresher.revision) { load(context) }
            WidgetContent(model, WidgetStyle.from(currentState<Preferences>()))
        }
    }

    private suspend fun load(context: Context): WidgetModel {
        val app = context.applicationContext as UrlaubsplanerApp
        val language = WidgetLanguage.of(context)
        val data = withContext(Dispatchers.IO) { app.repository.readAll() }
        val status = WidgetStatus.of(app.clock, data.settings, data.entitlements, data.marks)
        return WidgetModel.of(status, language.context, language.locale)
    }
}

/**
 * The model of a widget whose data stays current. Glance keeps a session alive for about a minute
 * after a draw, and while it lives an update only changes the widget's state:
 * [UrlaubsplanerWidget.provideGlance] does not run again. So a second change within that minute
 * (marking two days one after the other) would show the numbers of the first. Each
 * [WidgetRefresher.invalidate] raises [revision]; the model is read again when it moves past
 * [seen], the value [first] was read at.
 */
@Composable
internal fun liveModel(
    first: WidgetModel,
    seen: Int,
    revision: StateFlow<Int>,
    load: suspend () -> WidgetModel,
): WidgetModel {
    val current by revision.collectAsState()
    val model by produceState(first, current) {
        if (current != seen) value = load()
    }
    return model
}

class UrlaubsplanerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UrlaubsplanerWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // A running Glance session would keep showing the old date or language (see [liveModel]).
        if (intent.action in DAY_CHANGES || intent.action == Intent.ACTION_LOCALE_CHANGED) {
            (context.applicationContext as UrlaubsplanerApp).widgetRefresher.invalidate()
        }
        if (intent.action in DAY_CHANGES) {
            // The date is what "next vacation" and "days left" depend on. Glance already redraws
            // on a language change; the same path serves these.
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
            onUpdate(context, manager, ids)
        } else {
            super.onReceive(context, intent)
        }
    }

    private companion object {
        val DAY_CHANGES =
            setOf(
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
    }
}
