package de.remph.urlaubsplaner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    val snackbar = remember { SnackbarHostState() }
    // Leaving a tab drops its composables; this keeps what each tab remembered (the open settings
    // page, scroll positions) for the way back.
    val tabState = rememberSaveableStateHolder()
    // A button or the widget that says "settings" means the list, not the page left open earlier;
    // only the tab bar brings back where the user was.
    // The old settings screen is still composed while it fades out and saves its state when it
    // goes, so removing the state is not enough; a new key makes the next settings screen start fresh.
    var settingsGeneration by rememberSaveable { mutableIntStateOf(0) }
    val show = { target: Tab ->
        if (target == Tab.SETTINGS && tab != Tab.SETTINGS) {
            tabState.removeState(stateKey(Tab.SETTINGS, settingsGeneration))
            settingsGeneration++
        }
        tab = target
    }
    // A tap on the widget asks for a tab; it wins over where the user was.
    LaunchedEffect(request) { request?.let { show(it.tab) } }
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
        // The tabs slide a short way in the direction of the tab bar, with a fade.
        val axis = rememberSharedAxis<Tab>()
        AnimatedContent(
            tab,
            transitionSpec = {
                axis(targetState.ordinal > initialState.ordinal).using(SizeTransform(clip = false))
            },
            label = "tab",
        ) { shown ->
            tabState.SaveableStateProvider(stateKey(shown, settingsGeneration)) {
                when (shown) {
                    Tab.OVERVIEW -> {
                        OverviewScreen(
                            plannerViewModel,
                            padding,
                            onOpenSettings = { show(Tab.SETTINGS) },
                            onOpenMonth = {
                                plannerViewModel.showMode(CalendarMode.MONTH)
                                plannerViewModel.showMonth(it)
                                tab = Tab.CALENDAR
                            },
                        )
                    }

                    Tab.CALENDAR -> {
                        CalendarScreen(
                            plannerViewModel,
                            padding,
                            onOpenSettings = { show(Tab.SETTINGS) },
                        )
                    }

                    Tab.SETTINGS -> {
                        SettingsHost(settingsViewModel, appearanceStore, snackbar, padding)
                    }
                }
            }
        }
    }
}

private fun stateKey(
    tab: Tab,
    settingsGeneration: Int,
) = if (tab ==
    Tab.SETTINGS
) {
    "SETTINGS$settingsGeneration"
} else {
    tab.name
}
