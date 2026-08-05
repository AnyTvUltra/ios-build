package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel

object ShortsScreen : Screen {
    override val key = "shorts"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        LaunchedEffect(Unit) {
            if (!viewModel.isConnected) {
                navigator.push(LoginScreen)
            } else {
                viewModel.loadAllContent()
            }
        }

        val shorts = viewModel.allVodChannels.shuffled().take(20)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Shorts") }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                if (viewModel.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(shorts) { channel ->
                    Column {
                        ContentCard(
                            title = channel.name,
                            subtitle = channel.categoryName,
                            imageUrl = channel.coverUrl.ifBlank { channel.streamIcon },
                            onClick = { openChannel(navigator, viewModel, channel) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (!viewModel.isLoading && shorts.isEmpty()) {
                    item {
                        Text(
                            "No short clips available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
