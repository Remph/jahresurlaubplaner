package de.remph.urlaubsplaner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.remph.urlaubsplaner.appearance.UrlaubsplanerTheme
import de.remph.urlaubsplaner.planner.PlannerViewModel
import de.remph.urlaubsplaner.settings.SettingsViewModel
import de.remph.urlaubsplaner.ui.AppRoot

// AppCompatActivity, not ComponentActivity: AppCompatDelegate switches the app language and the
// light/dark mode with it.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // With three-button navigation the system lays a translucent scrim over its bar by
        // default, which showed as a second tone under our navigation bar (Nick, 2026-10-26).
        // Our bar reaches under the system bar, so without the scrim it is one colour.
        window.isNavigationBarContrastEnforced = false
        val app = application as UrlaubsplanerApp
        setContent {
            val appearance by app.appearanceStore.state.collectAsStateWithLifecycle()
            UrlaubsplanerTheme(appearance) {
                AppRoot(
                    settingsViewModel =
                        viewModel {
                            SettingsViewModel(
                                app.repository,
                                app.backupStore,
                                app.calendarExporter,
                                app.clock,
                            )
                        },
                    plannerViewModel =
                        viewModel {
                            PlannerViewModel(
                                app.repository,
                                app.schoolHolidays,
                                app.clock,
                            )
                        },
                    appearanceStore = app.appearanceStore,
                )
            }
        }
    }
}
