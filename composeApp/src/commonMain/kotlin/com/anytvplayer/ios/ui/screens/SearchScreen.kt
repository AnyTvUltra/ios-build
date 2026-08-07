package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import kotlinx.coroutines.delay
import kotlin.math.max

object SearchScreen : Screen {
    override val key = "search"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        var query by remember { mutableStateOf("") }

        val browseItems = remember(
            viewModel.liveChannels,
            viewModel.vodChannels,
            viewModel.seriesChannels,
        ) {
            interleaveContent(
                live = viewModel.liveChannels,
                vod = viewModel.vodChannels,
                series = viewModel.seriesChannels,
            )
        }
        val visibleItems = if (query.isBlank()) browseItems else viewModel.searchResults

        LaunchedEffect(query) {
            delay(180)
            viewModel.search(query)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            SearchHeader(
                query = query,
                totalItems = browseItems.size,
                onQueryChange = { query = it },
                onClear = { query = "" },
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    viewModel.isLoadingChannels && browseItems.isEmpty() -> {
                        CircularProgressIndicator(
                            color = TwitiMint,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    visibleItems.isEmpty() -> {
                        EmptyDiscovery(
                            isSearching = query.isNotBlank(),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        DiscoveryGrid(
                            channels = visibleItems,
                            query = query,
                            onChannelClick = { openChannel(navigator, viewModel, it) },
                        )
                    }
                }

                if (viewModel.isSearching && query.isNotBlank()) {
                    CircularProgressIndicator(
                        color = TwitiMint,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    totalItems: Int,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Discover",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Browse your entire library",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (totalItems > 0) {
                Text(
                    "$totalItems items",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(20.dp)),
            placeholder = { Text("Search channels, movies, series...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TwitiMint.copy(alpha = 0.13f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Search, null, tint = TwitiMint, modifier = Modifier.size(20.dp))
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = TwitiMint,
            ),
        )
    }
}

@Composable
private fun DiscoveryGrid(
    channels: List<IptvChannel>,
    query: String,
    onChannelClick: (IptvChannel) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = if (query.isBlank()) "Browse All" else "${channels.size} results",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        items(
            items = channels,
            key = { "${it.type}:${it.id}:${it.streamId}" },
        ) { channel ->
            DiscoveryCard(channel = channel, onClick = { onChannelClick(channel) })
        }
    }
}

@Composable
private fun DiscoveryCard(channel: IptvChannel, onClick: () -> Unit) {
    val imageUrl = channel.coverUrl.ifBlank { channel.streamIcon }
    val accent = when (channel.type) {
        ChannelType.LIVE -> LiveRed
        ChannelType.VOD -> TwitiMint
        ChannelType.SERIES -> TwitiCyan
    }
    val typeLabel = when (channel.type) {
        ChannelType.LIVE -> "Live"
        ChannelType.VOD -> "Movie"
        ChannelType.SERIES -> "Series"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(194.dp)
                .background(
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.42f), Color(0xFF171A20))),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (channel.type == ChannelType.LIVE) ContentScale.Fit else ContentScale.Crop,
                )
            } else {
                Text(
                    channel.name.take(2).uppercase(),
                    color = accent,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))),
            )
            Text(
                typeLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(accent.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.92f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(21.dp))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                channel.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            Text(
                channel.categoryName.ifBlank { channel.genre.ifBlank { typeLabel } },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyDiscovery(isSearching: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (isSearching) "No results" else "Your library is empty",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            if (isSearching) "Try different keywords" else "Connect to a provider to get started",
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun interleaveContent(
    live: List<IptvChannel>,
    vod: List<IptvChannel>,
    series: List<IptvChannel>,
): List<IptvChannel> {
    val largest = max(live.size, max(vod.size, series.size))
    return buildList(live.size + vod.size + series.size) {
        for (index in 0 until largest) {
            vod.getOrNull(index)?.let(::add)
            series.getOrNull(index)?.let(::add)
            live.getOrNull(index)?.let(::add)
        }
    }
}
