package com.alertyai.app.ui.teams.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.data.model.Organization
import com.alertyai.app.ui.components.ClayCard

@Composable
fun OrgCard(org: Organization, onClick: () -> Unit) {
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(org.name.take(1).uppercase(), 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 24.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(org.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                org.description?.let {
                    if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            // Role badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (org.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (org.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
            ) {
                Text(org.myRole.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (org.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}
