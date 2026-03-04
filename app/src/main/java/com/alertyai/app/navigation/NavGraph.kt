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
import com.alertyai.app.ui.teams.OrgMembersScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Home",      Icons.Default.Home)
    object Tasks     : Screen("tasks",     "Tasks",     Icons.Default.CheckCircle)
    object Teams     : Screen("teams",     "Teams",     Icons.Default.Groups)
    object Chat      : Screen("chat",      "Assistant", Icons.Default.SmartToy)
    object Reminders : Screen("reminders", "Recalls",   Icons.Default.Notifications)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
    object Profile   : Screen("profile",   "Profile",   Icons.Default.Person)
    object TeamChat  : Screen("team_chat/{orgId}/{teamName}", "Chat", Icons.Default.Chat)
    object OrgMembers: Screen("org_members/{orgId}/{orgName}/{isAdmin}/{joinCode}", "Members", Icons.Default.People)
}

private val bottomItems = listOf(
    Screen.Home, Screen.Tasks, Screen.Teams, Screen.Reminders, Screen.Settings
)

@Composable
fun AlertyNavGraph(isDark: Boolean, onToggleTheme: () -> Unit, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

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
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
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
                    onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                ) 
            }
            composable(Screen.Tasks.route)     { TasksScreen() }
            composable(Screen.Reminders.route) { com.alertyai.app.ui.reminders.RemindersScreen() }
            composable(Screen.Teams.route)     {
                TeamsScreen(
                    onTeamClick = { orgId, teamId, teamName ->
                        val encodedName = java.net.URLEncoder.encode(teamName, "UTF-8")
                        navController.navigate("team_chat/$orgId/$encodedName")
                    },
                    onMembersClick = { orgId, orgName, isAdmin, joinCode ->
                        val encodedName = java.net.URLEncoder.encode(orgName, "UTF-8")
                        val encodedCode = java.net.URLEncoder.encode(joinCode, "UTF-8")
                        navController.navigate("org_members/$orgId/$encodedName/$isAdmin/$encodedCode")
                    }
                )
            }
            composable(
                route = Screen.TeamChat.route,
                arguments = listOf(
                    navArgument("orgId") { type = NavType.StringType },
                    navArgument("teamName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orgId = backStackEntry.arguments?.getString("orgId") ?: ""
                val encodedName = backStackEntry.arguments?.getString("teamName") ?: ""
                val teamName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                TeamChatScreen(orgId = orgId, teamName = teamName, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.OrgMembers.route,
                arguments = listOf(
                    navArgument("orgId") { type = NavType.StringType },
                    navArgument("orgName") { type = NavType.StringType },
                    navArgument("isAdmin") { type = NavType.BoolType },
                    navArgument("joinCode") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val orgId = backStackEntry.arguments?.getString("orgId") ?: ""
                val encodedName = backStackEntry.arguments?.getString("orgName") ?: ""
                val orgName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false
                val encodedCode = backStackEntry.arguments?.getString("joinCode") ?: ""
                val joinCode = java.net.URLDecoder.decode(encodedCode, "UTF-8")
                OrgMembersScreen(
                    orgId = orgId,
                    orgName = orgName,
                    isAdmin = isAdmin,
                    initialJoinCode = joinCode,
                    onBack = { navController.popBackStack() }
                )
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
