package com.alertyai.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.alertyai.app.ui.home.HomeScreen
import com.alertyai.app.ui.reminders.RemindersScreen
import com.alertyai.app.ui.settings.SettingsScreen
import com.alertyai.app.ui.tasks.TasksScreen
import com.alertyai.app.ui.chat.ChatScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Home",      Icons.Default.Home)
    object Tasks     : Screen("tasks",     "Tasks",     Icons.Default.CheckCircle)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Notifications)
    object Chat      : Screen("chat",      "AI Chat",   Icons.Default.SmartToy)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
}

private val bottomItems = listOf(
    Screen.Home, Screen.Tasks, Screen.Reminders, Screen.Chat, Screen.Settings
)

@Composable
fun AlertyNavGraph(isDark: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
                bottomItems.forEach { screen ->
                    NavigationBarItem(
                        selected    = currentRoute == screen.route,
                        onClick     = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon        = { Icon(screen.icon, contentDescription = screen.label) },
                        label       = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)      { HomeScreen() }
            composable(Screen.Tasks.route)     { TasksScreen() }
            composable(Screen.Reminders.route) { RemindersScreen() }
            composable(Screen.Chat.route)      { ChatScreen() }
            composable(Screen.Settings.route)  { SettingsScreen(isDark = isDark, onToggleTheme = onToggleTheme) }
        }
    }
}
