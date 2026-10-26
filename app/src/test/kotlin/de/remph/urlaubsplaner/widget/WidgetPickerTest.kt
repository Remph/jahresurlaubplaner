package de.remph.urlaubsplaner.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The two widgets in the launcher's picker, and which layout a placed widget id gets. */
@RunWith(AndroidJUnit4::class)
class WidgetPickerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun info(receiver: Class<*>) =
        AppWidgetProviderInfo().apply { provider = ComponentName(context, receiver) }

    private fun place(
        id: Int,
        receiver: Class<*>,
    ) = shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(id, info(receiver))

    @Test
    fun `a widget placed from the big receiver gets the big layout`() {
        place(7, UrlaubsplanerBigWidgetReceiver::class.java)

        assertTrue(widgetFor(context, 7) is UrlaubsplanerBigWidget)
    }

    @Test
    fun `a widget placed from the old receiver keeps the small layout`() {
        place(8, UrlaubsplanerWidgetReceiver::class.java)

        assertEquals(UrlaubsplanerWidget::class.java, widgetFor(context, 8)::class.java)
    }

    @Test
    fun `an unknown widget id gets the small layout`() {
        assertEquals(UrlaubsplanerWidget::class.java, widgetFor(context, 99)::class.java)
    }

    @Test
    fun `both receivers are in the manifest with their own names in the picker`() {
        val packageManager = context.packageManager
        val labels =
            WIDGET_RECEIVERS.map {
                packageManager
                    .getReceiverInfo(ComponentName(context, it), PackageManager.GET_META_DATA)
                    .loadLabel(packageManager)
                    .toString()
            }

        assertEquals(
            listOf(
                context.getString(R.string.widget_label_small),
                context.getString(R.string.widget_label_big),
            ),
            labels,
        )
    }
}
