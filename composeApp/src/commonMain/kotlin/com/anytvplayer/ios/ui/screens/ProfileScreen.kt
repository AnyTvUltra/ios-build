package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.user.UserAccount

object ProfileScreen : Screen {
    override val key = "profile"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.profileAvatarUri.isNotBlank()) {
                        // coil avatar
                    }
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    viewModel.profileName.ifBlank { viewModel.userAccount.name.ifBlank { "Guest" } },
                    style = MaterialTheme.typography.headlineSmall
                )

                if (viewModel.userAccount.email.isNotBlank()) {
                    Text(
                        viewModel.userAccount.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = viewModel.profileName,
                    onValueChange = { viewModel.updateProfile(it) },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                SettingsSwitchRow(
                    title = "Dark Theme",
                    icon = Icons.Filled.DarkMode,
                    checked = viewModel.isDarkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme() }
                )

                Spacer(Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Notifications",
                    icon = Icons.Filled.Notifications,
                    checked = viewModel.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = "Language", modifier = Modifier.padding(end = 12.dp))
                        Text("Language", style = MaterialTheme.typography.bodyLarge)
                    }
                    LanguageSelector(
                        selected = viewModel.languageTag,
                        onSelect = { viewModel.setLanguage(it) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { navigator.push(UserContentListScreen("favorites")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("My Library")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { navigator.push(DownloadsScreen()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Downloads")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { navigator.push(LoginScreen()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect / Switch Server")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.signOut()
                        viewModel.disconnect()
                        navigator.replaceAll(HomeScreen())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Sign out", modifier = Modifier.padding(end = 8.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("en" to "English", "ar" to "Arabic", "ckb" to "Kurdish")
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(options.find { it.first == selected }?.second ?: selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, modifier = Modifier.padding(end = 12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
