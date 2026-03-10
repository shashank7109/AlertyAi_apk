package com.alertyai.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alertyai.app.ui.home.HomeScreen
import com.alertyai.app.ui.tasks.TasksScreen
import com.alertyai.app.ui.chat.ChatScreen
import com.alertyai.app.ui.settings.SettingsScreen
import com.alertyai.app.ui.teams.TeamsScreen
import com.alertyai.app.ui.teams.TeamChatScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Home",      Icons.Default.Home)
    object Tasks     : Screen("tasks",     "Tasks",     Icons.Default.CheckCircle)
    object Teams     : Screen("teams",     "Teams",     Icons.Default.Groups)
    object Chat      : Screen("chat",      "Assistant", Icons.Default.SmartToy)
    object Reminders : Screen("reminders", "Recalls",   Icons.Default.Notifications)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
    object Profile   : Screen("profile",   "Profile",   Icons.Default.Person)
    object TeamChat  : Screen("team_chat/{teamId}/{teamName}", "Chat", Icons.Default.Chat)
    object TeamDashboard : Screen("team_dashboard/{teamId}/{teamName}", "Team", Icons.Default.Dashboard)
}

private val bottomItems = listOf(
    Screen.Home, Screen.Tasks, Screen.Teams, Screen.Reminders, Screen.Settings
)

@Composable
fun AlertyNavGraph(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    startOnAddTask: Boolean = false,
    autoStartVoice: Boolean = false,
    onAddTaskConsumed: () -> Unit = {},
    onAutoStartVoiceConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Deep-link from widget + button → go to Tasks screen
    LaunchedEffect(startOnAddTask) {
        if (startOnAddTask) {
            navController.navigate(Screen.Tasks.route) {
                launchSingleTop = true
                restoreState = true
            }
            onAddTaskConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected    = selected,
                        onClick     = {
                            navController.navigate(screen.route) {
                                // Pop back to start destination, but keep state
                                popUpTo(navController.graph.findStartDestination().id) { 
                                    saveState = true 
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        icon        = { 
                            Icon(
                                screen.icon, 
                                contentDescription = screen.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ) 
                        },
                        label       = { 
                            Text(
                                screen.label.uppercase(), 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            val showFabOnRoutes = listOf(
                Screen.Home.route, 
                Screen.Tasks.route, 
                Screen.Teams.route, 
                Screen.Reminders.route, 
                Screen.Settings.route
            )
            if (currentRoute in showFabOnRoutes) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Chat.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "Assistant")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)      { 
                HomeScreen(
                    onNavigateToTasks = { 
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReminders = { 
                        navController.navigate(Screen.Reminders.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                ) 
            }
            composable(Screen.Tasks.route) { 
                TasksScreen(
                    autoStartVoice = autoStartVoice,
                    onAutoStartVoiceConsumed = onAutoStartVoiceConsumed
                ) 
            }
            composable(Screen.Reminders.route) { com.alertyai.app.ui.reminders.RemindersScreen() }
            composable(Screen.Teams.route)     {
                TeamsScreen(
                    onTeamClick = { teamId, teamName ->
                        val encodedName = java.net.URLEncoder.encode(teamName, "UTF-8")
                        navController.navigate("team_dashboard/$teamId/$encodedName")
                    }
                )
            }
            
            composable(
                route = Screen.TeamDashboard.route,
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("teamName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val encodedName = backStackEntry.arguments?.getString("teamName") ?: ""
                val teamName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                
                com.alertyai.app.ui.teams.TeamDashboardScreen(
                    teamId = teamId,
                    teamName = teamName,
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = {
                        navController.navigate("team_chat/$teamId/$encodedName")
                    }
                )
            }
            composable(
                route = Screen.TeamChat.route,
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("teamName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val encodedName = backStackEntry.arguments?.getString("teamName") ?: ""
                val teamName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                TeamChatScreen(teamId = teamId, teamName = teamName, onBack = { navController.popBackStack() })
            }
            composable(Screen.Chat.route)      { ChatScreen() }
            composable(Screen.Settings.route)  { 
                SettingsScreen(
                    isDark = isDark, 
                    onToggleTheme = onToggleTheme,
                    onLogout = onLogout,
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                ) 
            }
            composable(Screen.Profile.route) {
                com.alertyai.app.ui.profile.ProfileScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
