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
import com.anytvplayer.ios.data.iptv.ChannelType
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            item { HomeTopBar(onSearch = { navigator.push(SearchScreen()) }) }

            if (!viewModel.isConnected) {
                item { ConnectPrompt() }
            } else {
                if (viewModel.isLoading) {
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

                item { SectionHeader("Trending") }
                item { TrendingRow(SampleData.trendingMovies) }

                if (viewModel.watchProgressItems.isNotEmpty()) {
                    item { SectionHeader("Continue Watching") }
                    item { ChannelRow(viewModel.watchProgressItems, viewModel) { openChannel(navigator, viewModel, it) } }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onSearch: () -> Unit) {
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
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Person, contentDescription = "Profile")
            }
        }
    }
}

@Composable
private fun ConnectPrompt() {
    val viewModel = LocalIptvViewModel.current
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "Connect to your IPTV server",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Server URL or M3U") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val server = com.anytvplayer.ios.data.iptv.IptvServer(
                    serverUrl = url,
                    username = username,
                    password = password,
                    type = if (url.contains("m3u", ignoreCase = true) || url.contains("get.php", ignoreCase = true)) {
                        com.anytvplayer.ios.data.iptv.ServerType.M3U_URL
                    } else {
                        com.anytvplayer.ios.data.iptv.ServerType.XTREAM_CODES
                    }
                )
                viewModel.connectToServer(server)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
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

