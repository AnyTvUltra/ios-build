package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.gradientForType

object SportsScreen : Screen {
    override val key = "sports"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val channels = viewModel.liveChannels
        val categories = viewModel.liveCategories
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }

        when {
            viewModel.isLoadingChannels && channels.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TwitiMint)
            }

            channels.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .background(LiveRed.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LiveTv,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "لا توجد قنوات مباشرة",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "ستظهر القنوات هنا فور إضافتها إلى قائمة التشغيل",
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 154.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 46.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LiveHeader(channelCount = channels.size)
                }

                if (categories.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                "Categories",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(end = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories, key = { it.id }) { category ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                navigator.push(IptvChannelsScreen(category.type.name, category.id))
                                            }
                                            .padding(horizontal = 15.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(LiveRed, CircleShape)
                                        )
                                        Text(
                                            category.name,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            modifier = Modifier.padding(start = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All Channels",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${channels.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                itemsIndexed(
                    items = channels,
                    key = { index, channel -> "${channel.id}:$index" }
                ) { _, channel ->
                    LiveChannelTile(
                        channel = channel,
                        categoryName = categories.find { it.id == channel.categoryId }?.name.orEmpty(),
                        onClick = { openChannel(navigator, viewModel, channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveHeader(channelCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(LiveRed, CircleShape)
                )
                Text(
                    "Live",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 9.dp)
                )
            }
            Text(
                "$channelCount channel${if (channelCount != 1) "s" else ""}",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(LiveRed.copy(alpha = 0.13f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text("LIVE", color = LiveRed, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LiveChannelTile(
    channel: IptvChannel,
    categoryName: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(channel.gradientForType())),
            contentAlignment = Alignment.Center
        ) {
            if (channel.streamIcon.isNotBlank()) {
                AsyncImage(
                    model = channel.streamIcon,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    channel.name.take(2).uppercase(),
                    color = TwitiMint,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(8.dp)
                    .background(LiveRed, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.52f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
            }
        }
        Text(
            channel.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp, end = 3.dp, top = 9.dp)
        )
        Text(
            categoryName.ifBlank { channel.categoryName.ifBlank { "Live" } },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
        )
    }
}
