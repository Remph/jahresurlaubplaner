package de.remph.urlaubsplaner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.remph.urlaubsplaner.R
import de.remph.urlaubsplaner.appearance.AppearanceStore
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
 * Until the first Land is chosen the app has nothing to show elsewhere, so it starts on the
 * settings and the other tabs are placeholders until the next pieces arrive.
 */
@Composable
fun AppRoot(
    settingsViewModel: SettingsViewModel,
    appearanceStore: AppearanceStore,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.SETTINGS) }
    val snackbar = remember { SnackbarHostState() }
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
            Tab.OVERVIEW -> Placeholder(R.string.overview_placeholder, padding)
            Tab.CALENDAR -> Placeholder(R.string.calendar_placeholder, padding)
            Tab.SETTINGS -> SettingsHost(settingsViewModel, appearanceStore, snackbar, padding)
        }
    }
}

@Composable
private fun Placeholder(
    text: Int,
    padding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), Alignment.Center) {
        Text(stringResource(text))
    }
}
