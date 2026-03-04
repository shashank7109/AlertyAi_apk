package com.alertyai.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alertyai.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel()
    val state by vm.state.collectAsState()

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var profilePic by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadProfile(context)
    }

    LaunchedEffect(state.profile) {
        state.profile?.let {
            name = it.fullName ?: it.name ?: ""
            username = it.username ?: ""
            mobile = it.mobileNumber ?: ""
            profilePic = it.profilePicture ?: ""
        }
    }

    if (state.updateSuccess) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "CREDENTIALS UPDATED", Toast.LENGTH_SHORT).show()
            vm.resetUpdateSuccess()
            onBack()
        }
    }

    state.error?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it.uppercase(), Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "IDENTITY MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "PERSONNEL FILE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }

            // Profile Picture Section
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClayCard(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (profilePic.isNotBlank()) {
                            AsyncImage(
                                model = profilePic,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("NEURAL SIGNATURE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }

            Text("DATA OVERRIDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ClayCard(shape = RoundedCornerShape(16.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("FULL NAME") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }

                ClayCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text("ID_HANDLE") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            supportingText = { 
                                Text("REQUIRED FOR @MENTION PROTOCOLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) 
                            }
                        )
                    }
                }

                ClayCard(shape = RoundedCornerShape(16.dp)) {
                    TextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        placeholder = { Text("COMMUNICATION LINE") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }

                ClayCard(shape = RoundedCornerShape(16.dp)) {
                    TextField(
                        value = profilePic,
                        onValueChange = { profilePic = it },
                        placeholder = { Text("SIGNATURE URL") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            ClayButton(
                onClick = { vm.updateProfile(context, name, username, mobile, profilePic) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = !state.isLoading,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("SYNCHRONIZE PROFILE", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
