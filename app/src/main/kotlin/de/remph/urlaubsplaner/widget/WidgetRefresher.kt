package de.remph.urlaubsplaner.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import de.remph.urlaubsplaner.data.UrlaubsplanerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Keeps the widgets current. The app process is the only writer of the database, so while it
 * lives it watches the three tables and draws the widgets again after a change. Without that a
 * marked day would show on the home screen only at the next periodic update.
 */
@OptIn(FlowPreview::class)
class WidgetRefresher(
    private val context: Context,
    private val database: UrlaubsplanerDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watching: Job? = null

    private val _revision = MutableStateFlow(0)

    /** Rises with every [invalidate]; a running widget session reads its data again when it moves. */
    val revision: StateFlow<Int> = _revision

    /** Whether the user has put the widget on a home screen. */
    fun hasWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return WIDGET_RECEIVERS.any {
            manager
                .getAppWidgetIds(
                    ComponentName(context, it),
                ).isNotEmpty()
        }
    }

    /** Starts watching the data; calling it again does nothing. */
    fun start() {
        if (watching != null) return
        watching =
            scope.launch {
                // The first value comes at once, so the widgets are also drawn when the app starts.
                database.invalidationTracker
                    .createFlow("settings", "year_entitlement", "marked_day")
                    .debounce(DEBOUNCE_MILLIS)
                    .collect { redraw() }
            }
    }

    /** For changes that are not in the database: the language, the phone's light or dark mode. */
    fun refresh() {
        if (hasWidgets()) scope.launch { redraw() }
    }

    /** Marks what the widgets show as outdated, for a session that is still running. */
    fun invalidate() {
        _revision.update { it + 1 }
    }

    private suspend fun redraw() {
        invalidate()
        UrlaubsplanerWidget().updateAll(context)
        UrlaubsplanerBigWidget().updateAll(context)
    }

    private companion object {
        // Marking a range writes many rows; one redraw after they are all in is enough.
        const val DEBOUNCE_MILLIS = 500L
    }
}
