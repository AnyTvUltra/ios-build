package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvCategory

object LiveHubScreen : Screen {
    override val key = "live"

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

        val liveCategories = viewModel.allCategories.filter { it.type == ChannelType.LIVE }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Live TV") },
                    actions = {
                        TextButton(onClick = { navigator.push(SportsScreen) }) {
                            Text("Sports")
                        }
                        TextButton(onClick = { navigator.push(SearchScreen) }) {
                            Text("Search")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (viewModel.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (liveCategories.isEmpty() && !viewModel.isLoading) {
                    item {
                        Text(
                            "No live categories found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(liveCategories) { category ->
                    Column {
                        SectionHeader(category.name)
                        val channels = viewModel.allLiveChannels.filter { it.categoryId == category.id }
                        ChannelRow(channels, viewModel) { openChannel(navigator, viewModel, it) }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}
