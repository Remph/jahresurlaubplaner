package de.remph.urlaubsplaner.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        val app = context.applicationContext as UrlaubsplanerApp
        val language = WidgetLanguage.of(context)
        // Read now, not inside the composition: a tap or a resize draws again from this model.
        val data = withContext(Dispatchers.IO) { app.repository.readAll() }
        val status = WidgetStatus.of(app.clock, data.settings, data.entitlements, data.marks)
        val model = WidgetModel.of(status, language.context, language.locale)
        provideContent {
            WidgetContent(model, WidgetStyle.from(currentState<Preferences>()))
        }
    }
}

class UrlaubsplanerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UrlaubsplanerWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
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
