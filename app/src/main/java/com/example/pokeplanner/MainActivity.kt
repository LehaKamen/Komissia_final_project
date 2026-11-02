package com.example.pokeplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pokeplanner.ui.settings.SettingsVm
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: SettingsVm = hiltViewModel()
            val settings by viewModel.state.collectAsStateWithLifecycle()
            LifecycleStartEffect(Unit) {
                viewModel.foreground(true)
                onStopOrDispose { viewModel.foreground(false) }
            }
            MaterialTheme(colorScheme = if (settings.dark) darkColorScheme() else lightColorScheme()) {
                AppNav()
            }
        }
    }
}
