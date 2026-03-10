package com.alertyai.app

import android.Manifest
import android.content.Intent
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
import com.alertyai.app.widget.TaskWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import com.alertyai.app.data.local.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Tracks whether we should deep-link to the Add Task screen on launch
    private var widgetAction: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled passively */ }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Request RECORD_AUDIO permission — called when widget notification toggle is tapped */
    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        // Handle widget deep-link intent
        widgetAction = intent?.action
        handleWidgetIntent(intent)

        setContent {
            var isDark by remember { mutableStateOf(false) }
            val isLoggedIn by TokenManager.isLoggedInState.collectAsState(initial = TokenManager.isLoggedIn(this))

            var deepLinkToAddTask by remember { 
                mutableStateOf(
                    widgetAction == TaskWidgetReceiver.ACTION_ADD_TASK || 
                    widgetAction == com.alertyai.app.widget.QuickSettingsVoiceTileService.ACTION_QUICK_SETTINGS_VOICE
                ) 
            }
            
            var autoStartVoice by remember {
                mutableStateOf(widgetAction == com.alertyai.app.widget.QuickSettingsVoiceTileService.ACTION_QUICK_SETTINGS_VOICE)
            }

            AlertyAITheme(darkTheme = isDark) {
                if (isLoggedIn) {
                    AlertyNavGraph(
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark },
                        startOnAddTask = deepLinkToAddTask,
                        autoStartVoice = autoStartVoice,
                        onAddTaskConsumed = { 
                            deepLinkToAddTask = false
                            autoStartVoice = false 
                        },
                        onLogout = {
                            GlobalScope.launch(Dispatchers.IO) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.action) {
            TaskWidgetReceiver.ACTION_TOGGLE_NOTIF -> requestMicPermission()
            // ACTION_ADD_TASK and ACTION_QUICK_SETTINGS_VOICE are handled in setContent
        }
    }
}
