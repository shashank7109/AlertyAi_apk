package com.alertyai.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isDark: Boolean, onToggleTheme: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // App info card
            Card(
                shape  = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AlertyAI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                    Text("v1.0.0 — Your local AI planner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Preferences", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

            // Dark mode toggle
            SettingsRow(
                icon  = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                label = "Dark Mode",
            ) {
                Switch(checked = isDark, onCheckedChange = { onToggleTheme() })
            }

            Spacer(Modifier.height(8.dp))
            Text("Data", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

            SettingsRow(
                icon  = Icons.Default.Storage,
                label = "All data stored locally on this device",
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            SettingsRow(
                icon  = Icons.Default.RecordVoiceOver,
                label = "Text-to-Speech (on-device)",
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            SettingsRow(
                icon  = Icons.Default.DocumentScanner,
                label = "OCR scanning (on-device ML Kit)",
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    trailing: @Composable () -> Unit
) {
    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            trailing()
        }
    }
}
