package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvServer
import com.anytvplayer.ios.data.iptv.ServerType
import com.anytvplayer.ios.data.iptv.parseXtreamM3uUrl
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint

object LoginScreen : Screen {
    override val key = "login"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val scrollState = rememberScrollState()

        var serverUrl by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var selectedType by remember { mutableStateOf(ServerType.XTREAM_CODES) }

        if (viewModel.isConnected) {
            LaunchedEffect(Unit) { navigator.replaceAll(HomeScreen) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Connect", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Hero card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(TwitiMint.copy(alpha = 0.18f), TwitiCyan.copy(alpha = 0.10f))))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(CircleShape).background(TwitiMint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Wifi, null, tint = TwitiCyan, modifier = Modifier.size(54.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Welcome to AnyTV", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Enter your IPTV provider details to get started", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(28.dp))

            // Server type selector
            ServerTypeSelector(
                selected = selectedType,
                onSelect = { selectedType = it; if (it == ServerType.M3U_URL && serverUrl.isBlank()) serverUrl = "" }
            )

            Spacer(Modifier.height(16.dp))

            // Server URL
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(if (selectedType == ServerType.M3U_URL) "M3U / M3U8 URL" else "Server URL", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors(),
                placeholder = {
                    Text(
                        if (selectedType == ServerType.M3U_URL) "http://example.com/playlist.m3u" else "http://example.com:port",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )

            if (selectedType != ServerType.M3U_URL) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }

            viewModel.contentError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    val finalUrl = serverUrl.trim()
                    val smart = parseXtreamM3uUrl(finalUrl)
                    val server = if (smart != null) {
                        smart.copy(name = smart.name.ifBlank { finalUrl })
                    } else {
                        IptvServer(
                            serverUrl = finalUrl,
                            username = username,
                            password = password,
                            type = if (selectedType == ServerType.M3U_URL || finalUrl.contains(".m3u", ignoreCase = true)) ServerType.M3U_URL else selectedType
                        )
                    }
                    viewModel.connectToServer(server)
                },
                enabled = !viewModel.isLoading && serverUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = TwitiMint, contentColor = Color.White)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Connect", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))

            TextButton(
                onClick = { navigator.push(PlaylistPickerScreen) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Manage Playlists", fontWeight = FontWeight.SemiBold)
            }

            if (viewModel.isConnected) {
                Text("Connected!", color = TwitiMint, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TwitiMint,
    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    cursorColor = TwitiMint
)

@Composable
private fun ServerTypeSelector(selected: ServerType, onSelect: (ServerType) -> Unit) {
    val options = listOf(ServerType.XTREAM_CODES, ServerType.M3U_URL, ServerType.AUTO_DETECT)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { type ->
            val isSelected = selected == type
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        when (type) {
                            ServerType.XTREAM_CODES -> "Xtream"
                            ServerType.M3U_URL -> "M3U"
                            ServerType.AUTO_DETECT -> "Auto"
                            else -> type.name
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TwitiMint.copy(alpha = 0.18f),
                    selectedLabelColor = TwitiMint
                )
            )
        }
    }
}
