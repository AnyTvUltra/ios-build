package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.components.GlassCard
import com.anytvplayer.ios.ui.components.LiveBadge
import com.anytvplayer.ios.ui.theme.TwitiMint

data class IptvChannelsScreen(val type: String, val categoryId: String) : Screen {
    override val key = "iptv_channels/${type}/${categoryId}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val channelType = remember { runCatching { ChannelType.valueOf(type) }.getOrDefault(ChannelType.LIVE) }

        val categories = when (channelType) {
            ChannelType.LIVE -> viewModel.liveCategories
            ChannelType.VOD -> viewModel.vodCategories
            ChannelType.SERIES -> viewModel.seriesCategories
        }
        val category = categories.find { it.id == categoryId }

        LaunchedEffect(channelType, categoryId) {
            category?.let { viewModel.loadChannelsByCategory(channelType, categoryId) }
        }

        val channels = viewModel.categoryChannels
        val categoryName = category?.name ?: channels.firstOrNull()?.categoryName ?: "Channels"
        val scrollKey = "${channelType.name}:$categoryId"
        val savedScroll = viewModel.getChannelsScroll(scrollKey)
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = savedScroll.index,
            initialFirstVisibleItemScrollOffset = savedScroll.offset
        )

        DisposableEffect(scrollKey) {
            onDispose { viewModel.saveChannelsScroll(scrollKey, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
        }

        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Text("${channels.size} channels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (viewModel.isLoadingChannels) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TwitiMint)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = channels, key = { "${it.type}:${it.id}:${it.streamId}" }) { channel ->
                        ChannelListItem(
                            channel = if (channel.categoryName.isBlank()) channel.copy(categoryName = categoryName) else channel,
                            onClick = { openChannel(navigator, viewModel, channel) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: IptvChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel icon/thumbnail
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.streamIcon.isNotEmpty()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(channel.name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TwitiMint)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (channel.categoryName.isNotEmpty()) {
                    Text(channel.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (channel.type == ChannelType.LIVE) {
                    Spacer(Modifier.height(4.dp))
                    LiveBadge()
                }
            }

            // Play button
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, "Play", tint = TwitiMint, modifier = Modifier.size(28.dp))
            }
        }
    }
}
