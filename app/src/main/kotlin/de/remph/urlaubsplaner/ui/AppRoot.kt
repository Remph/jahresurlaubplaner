package de.remph.urlaubsplaner.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.appearance.AppearanceStore
import de.remph.urlaubsplaner.planner.CalendarMode
import de.remph.urlaubsplaner.planner.CalendarScreen
import de.remph.urlaubsplaner.planner.OverviewScreen
import de.remph.urlaubsplaner.planner.PlannerViewModel
import de.remph.urlaubsplaner.settings.SettingsHost
import de.remph.urlaubsplaner.settings.SettingsViewModel

/** The three tabs along the bottom (Nick, 2026-10-25). */
enum class Tab(
    val label: Int,
    val icon: ImageVector,
) {
    OVERVIEW(R.string.nav_overview, Icons.Filled.Home),
    CALENDAR(R.string.nav_calendar, Icons.Filled.DateRange),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings),
}

/**
 * The frame of the app: the bottom navigation and the one place messages (snackbars) appear.
 * It starts on the overview; until a Land is chosen that points to the settings.
 */
@Composable
fun AppRoot(
    settingsViewModel: SettingsViewModel,
    plannerViewModel: PlannerViewModel,
    appearanceStore: AppearanceStore,
    request: TabRequest? = null,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    // A tap on the widget asks for a tab; it wins over where the user was.
    LaunchedEffect(request) { request?.let { tab = it.tab } }
    val snackbar = remember { SnackbarHostState() }
    // A phone keeps the app in memory for days; the status must follow the date.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { plannerViewModel.refreshToday() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                for (entry in Tab.entries) {
                    val label = stringResource(entry.label)
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.OVERVIEW -> {
                OverviewScreen(
                    plannerViewModel,
                    padding,
                    onOpenSettings = { tab = Tab.SETTINGS },
                    onOpenMonth = {
                        plannerViewModel.showMode(CalendarMode.MONTH)
                        plannerViewModel.showMonth(it)
                        tab = Tab.CALENDAR
                    },
                )
            }

            Tab.CALENDAR -> {
                CalendarScreen(plannerViewModel, padding, onOpenSettings = { tab = Tab.SETTINGS })
            }

            Tab.SETTINGS -> {
                SettingsHost(settingsViewModel, appearanceStore, snackbar, padding)
            }
        }
    }
}
