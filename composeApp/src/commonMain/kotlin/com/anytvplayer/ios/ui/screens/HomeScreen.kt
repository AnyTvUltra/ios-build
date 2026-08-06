package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.ContentItem
import com.anytvplayer.ios.data.SampleData
import com.anytvplayer.ios.data.admin.toIptvChannel
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.ConnectionState
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.TwitiMint

object HomeScreen : Screen {
    override val key = "home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val categories = remember(viewModel.allCategories) { viewModel.allCategories }
        val live = remember(viewModel.allLiveChannels) { viewModel.allLiveChannels.take(15) }
        val vod = remember(viewModel.allVodChannels) { viewModel.allVodChannels.take(15) }

        if (viewModel.isAdminPanel && viewModel.activationState != null && viewModel.activationState?.isActivated == false) {
            ActivationPrompt()
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            item { HomeTopBar(
                onSearch = { navigator.push(SearchScreen) },
                onProfile = { navigator.push(ProfileScreen) }
            ) }

            when (val state = viewModel.connectionState) {
                ConnectionState.Disconnected,
                ConnectionState.Connecting -> item { LoadingItem() }
                is ConnectionState.Error -> item { ErrorItem(state.message) }
                is ConnectionState.Connected -> {
                    if (viewModel.isLoadingChannels) {
                        item { LoadingItem() }
                    }

                    if (categories.isNotEmpty()) {
                        item { SectionHeader("Categories") }
                        item { CategoryRow(categories) { category ->
                            navigator.push(IptvChannelsScreen(category.type.name, category.id))
                        } }
                    }

                    if (live.isNotEmpty()) {
                        item { SectionHeader("Live Channels") }
                        item { ChannelRow(live, viewModel) { openChannel(navigator, viewModel, it) } }
                    }

                    if (vod.isNotEmpty()) {
                        item { SectionHeader("Movies") }
                        item { ChannelRow(vod, viewModel) { openChannel(navigator, viewModel, it) } }
                    }

                    val series = remember(viewModel.allSeriesChannels) { viewModel.allSeriesChannels.take(15) }
                    if (series.isNotEmpty()) {
                        item { SectionHeader("Series") }
                        item { ChannelRow(series, viewModel) { openChannel(navigator, viewModel, it) } }
                    }

                    val continueWatching = viewModel.watchProgressItems.map { it.toIptvChannel() }
                    if (continueWatching.isNotEmpty()) {
                        item { SectionHeader("Continue Watching") }
                        item { ChannelRow(continueWatching, viewModel) { openChannel(navigator, viewModel, it) } }
                    }

                    item { SectionHeader("Trending") }
                    item { TrendingRow(SampleData.trendingMovies) }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ANYTV",
            style = MaterialTheme.typography.headlineMedium,
            color = TwitiMint
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = onProfile) {
                Icon(Icons.Filled.Person, contentDescription = "Profile")
            }
        }
    }
}

@Composable
private fun ActivationPrompt() {
    val viewModel = LocalIptvViewModel.current
    var code by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Device ID: ${viewModel.displayDeviceId}",
                style = MaterialTheme.typography.headlineSmall,
                color = TwitiMint
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Waiting for activation",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Please activate this device from the admin panel or redeem a gift code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Gift code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            viewModel.giftCodeMessage?.let {
                Text(text = it, color = if (it.contains("success", true)) Color.Green else MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { viewModel.redeemGiftCode(code) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Redeem")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.refreshActivation() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun ErrorItem(message: String) {
    val viewModel = LocalIptvViewModel.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { viewModel.tryAutoConnect() }) {
            Text("Retry")
        }
    }
}

@Composable
private fun LoadingItem() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TwitiMint)
    }
}

@Composable
private fun CategoryRow(
    categories: List<com.anytvplayer.ios.data.iptv.IptvCategory>,
    onClick: (com.anytvplayer.ios.data.iptv.IptvCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                TwitiMint.copy(alpha = 0.8f),
                                TwitiMint.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .clickable { onClick(category) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TrendingRow(items: List<ContentItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ContentCard(
                title = item.title,
                subtitle = item.category,
                imageUrl = "",
                gradient = item.gradientColors,
                onClick = {}
            )
        }
    }
}

