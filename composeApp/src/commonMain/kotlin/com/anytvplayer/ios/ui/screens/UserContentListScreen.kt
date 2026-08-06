package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.LibraryItem
import com.anytvplayer.ios.data.admin.SubscriptionContact
import com.anytvplayer.ios.data.admin.SubscriptionItem
import com.anytvplayer.ios.data.admin.WatchProgressItem
import com.anytvplayer.ios.data.admin.toIptvChannel
import com.anytvplayer.ios.data.iptv.IptvChannel

data class UserContentListScreen(val type: String) : Screen {
    override val key = "user_content/${type}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        val title = remember(type) {
            when (type.lowercase()) {
                "favorites" -> "Favorites"
                "watchlist" -> "Watchlist"
                "history", "continue" -> "Continue Watching"
                "subscriptions" -> "Subscriptions"
                else -> "My Content"
            }
        }

        val isSubscriptions = type.lowercase() == "subscriptions"

        val rawItems = when (type.lowercase()) {
            "favorites", "watchlist" -> viewModel.libraryItems
            "history", "continue" -> viewModel.watchProgressItems
            "subscriptions" -> viewModel.subscriptions
            else -> viewModel.libraryItems
        }

        val channels = rawItems.map { item ->
            when (item) {
                is LibraryItem -> item.toIptvChannel()
                is WatchProgressItem -> item.toIptvChannel()
                else -> IptvChannel(id = "", name = "", type = com.anytvplayer.ios.data.iptv.ChannelType.LIVE)
            }
        }

        val onRemove: (IptvChannel, Any) -> Unit = { channel, original ->
            when {
                original is WatchProgressItem -> viewModel.removeWatchProgress(original.contentId)
                original is LibraryItem -> viewModel.removeFromLibrary(channel)
                else -> viewModel.removeFromLibrary(channel)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
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
                if (channels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No items yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isSubscriptions) {
                    items(rawItems.size) { index ->
                        val item = rawItems[index] as? SubscriptionItem ?: return@items
                        Column {
                            SubscriptionCard(
                                item = item,
                                onContactClick = { contact ->
                                    // Platform links not implemented for iOS yet
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                } else {
                    items(channels.size) { index ->
                        val channel = channels[index]
                        val original = rawItems[index]
                        Column {
                            LibraryRow(
                                channel = channel,
                                onClick = { openChannel(navigator, viewModel, channel) },
                                onRemove = { onRemove(channel, original) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    channel: IptvChannel,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ContentCard(
                title = channel.name,
                subtitle = channel.categoryName,
                imageUrl = channel.coverUrl.ifBlank { channel.streamIcon },
                onClick = onClick
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove")
        }
    }
}
