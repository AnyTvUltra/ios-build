package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.viewmodel.gradientForType

data class IptvChannelsScreen(val type: String, val categoryId: String) : Screen {
    override val key = "iptv_channels/${type}/${categoryId}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val channelType = remember { runCatching { ChannelType.valueOf(type) }.getOrDefault(ChannelType.LIVE) }

        LaunchedEffect(channelType, categoryId) {
            viewModel.loadChannelsByCategory(channelType, categoryId)
        }

        val channels = when (channelType) {
            ChannelType.LIVE -> viewModel.allLiveChannels.filter { it.categoryId == categoryId }
            ChannelType.VOD -> viewModel.allVodChannels.filter { it.categoryId == categoryId }
            ChannelType.SERIES -> viewModel.allSeriesChannels.filter { it.categoryId == categoryId }
        }

        val categoryName = channels.firstOrNull()?.categoryName ?: "Channels"

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$categoryName (${channels.size})") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (channels.isEmpty() && !viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "No channels in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        ContentCard(
                            title = channel.name,
                            subtitle = channel.categoryName,
                            imageUrl = channel.coverUrl.ifBlank { channel.streamIcon },
                            gradient = channel.gradientForType(),
                            onClick = { openChannel(navigator, viewModel, channel) }
                        )
                    }
                }
            }
        }
    }
}
