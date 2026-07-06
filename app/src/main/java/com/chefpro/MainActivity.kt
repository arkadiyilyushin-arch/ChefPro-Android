package com.chefpro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.localization.rememberAppStrings
import com.chefpro.ui.navigation.AppNavigation
import com.chefpro.ui.theme.ChefProTheme
import com.chefpro.ui.viewmodel.ChefProViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — app works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val app = ChefProApplication.get(application)

        setContent {
            val chefProViewModel: ChefProViewModel = viewModel(
                factory = ChefProViewModelFactory(app),
            )
            val state by chefProViewModel.state.collectAsState()
            val strings = rememberAppStrings(state.appLanguage)

            CompositionLocalProvider(LocalAppStrings provides strings) {
                ChefProTheme(colorScheme = state.appColorScheme) {
                    AppNavigation(chefProViewModel)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

class ChefProViewModelFactory(
    private val app: ChefProApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChefProViewModel::class.java)) {
            return ChefProViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
