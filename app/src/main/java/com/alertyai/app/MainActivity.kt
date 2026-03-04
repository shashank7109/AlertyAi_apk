package com.alertyai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.alertyai.app.navigation.AlertyNavGraph
import com.alertyai.app.ui.auth.LoginScreen
import com.alertyai.app.ui.theme.AlertyAITheme
import com.alertyai.app.network.TokenManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDark by remember { mutableStateOf(false) }
            // Auth gate: show LoginScreen if no JWT stored
            var isLoggedIn by remember { mutableStateOf(TokenManager.isLoggedIn(this)) }

            AlertyAITheme(darkTheme = isDark) {
                if (isLoggedIn) {
                    AlertyNavGraph(
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark }
                    )
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}
