package de.remph.urlaubsplaner

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.remph.urlaubsplaner.settings.SettingsRoute
import de.remph.urlaubsplaner.settings.SettingsViewModel

// AppCompatActivity, not ComponentActivity: AppCompatDelegate switches the app language with it.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as UrlaubsplanerApp
        setContent {
            UrlaubsplanerTheme {
                // PR 1 has only the settings page; the status home arrives with the next PR.
                SettingsRoute(
                    viewModel { SettingsViewModel(app.repository, app.backupStore, app.clock) },
                )
            }
        }
    }
}

@Composable
private fun UrlaubsplanerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Dynamic colour needs Android 12 (API 31); minSdk is 30.
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors =
        if (dynamic) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (dark) darkColorScheme() else lightColorScheme()
        }
    MaterialTheme(colorScheme = colors, content = content)
}
