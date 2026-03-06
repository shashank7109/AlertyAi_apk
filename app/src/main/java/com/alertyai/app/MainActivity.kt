package com.alertyai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.alertyai.app.navigation.AlertyNavGraph
import com.alertyai.app.ui.auth.LoginScreen
import com.alertyai.app.ui.theme.AlertyAITheme
import com.alertyai.app.network.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import com.alertyai.app.data.local.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handle result */ }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        setContent {
            var isDark by remember { mutableStateOf(false) }
            // Auth gate: reactively observe login state
            val isLoggedIn by TokenManager.isLoggedInState.collectAsState(initial = TokenManager.isLoggedIn(this))

            AlertyAITheme(darkTheme = isDark) {
                if (isLoggedIn) {
                    AlertyNavGraph(
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark },
                        onLogout = {
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                TokenManager.clearToken(this@MainActivity)
                                AppDatabase.getInstance(this@MainActivity).clearAllTables()
                            }
                        }
                    )
                } else {
                    LoginScreen(onLoginSuccess = { TokenManager.isLoggedIn(this) })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Do nothing to token natively, let API calls catch 401 unauths.
    }
}
