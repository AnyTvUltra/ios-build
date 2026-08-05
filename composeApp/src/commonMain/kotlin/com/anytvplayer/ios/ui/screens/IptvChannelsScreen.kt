package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
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
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel

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
                    title = { Text(categoryName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(channels) { channel ->
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
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
