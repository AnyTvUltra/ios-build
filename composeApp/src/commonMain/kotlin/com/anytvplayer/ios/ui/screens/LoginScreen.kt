package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvServer
import com.anytvplayer.ios.data.iptv.ServerType
import com.anytvplayer.ios.data.iptv.parseXtreamM3uUrl
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Connect") },
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
                Text(
                    "Enter your IPTV details",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(24.dp))

                ServerTypeSelector(
                    selected = selectedType,
                    onSelect = {
                        selectedType = it
                        if (it == ServerType.M3U_URL && serverUrl.isBlank()) {
                            serverUrl = ""
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(if (selectedType == ServerType.M3U_URL) "M3U / M3U8 URL" else "Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (selectedType == ServerType.M3U_URL) "http://example.com/playlist.m3u"
                            else "http://example.com:port"
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                if (selectedType != ServerType.M3U_URL) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (viewModel.contentError != null) {
                    Text(
                        viewModel.contentError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Connect")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navigator.push(PlaylistPickerScreen()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Playlists")
                }

                if (viewModel.isConnected) {
                    Spacer(Modifier.height(16.dp))
                    Text("Connected", color = TwitiMint)
                    LaunchedEffect(Unit) {
                        navigator.replaceAll(HomeScreen())
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerTypeSelector(selected: ServerType, onSelect: (ServerType) -> Unit) {
    val options = listOf(ServerType.XTREAM_CODES, ServerType.M3U_URL, ServerType.AUTO_DETECT)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { type ->
            val selectedType = selected == type
            FilterChip(
                selected = selectedType,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        when (type) {
                            ServerType.XTREAM_CODES -> "Xtream"
                            ServerType.M3U_URL -> "M3U"
                            ServerType.AUTO_DETECT -> "Auto"
                            else -> type.name
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
