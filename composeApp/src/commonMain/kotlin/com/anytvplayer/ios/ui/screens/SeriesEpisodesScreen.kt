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
import com.anytvplayer.ios.data.iptv.IptvChannel

data class SeriesEpisodesScreen(val seriesId: Int) : Screen {
    override val key = "series_episodes/${seriesId}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        var episodes by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(seriesId) {
            isLoading = true
            viewModel.loadSeriesEpisodes(seriesId) { result ->
                episodes = result
                isLoading = false
                if (result.isEmpty()) {
                    error = "No episodes found."
                }
            }
        }

        val title = episodes.firstOrNull()?.categoryName?.let { "Season $it" } ?: "Series"

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
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (!isLoading && error != null) {
                    item {
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                val grouped = episodes.groupBy { it.categoryName.ifBlank { "Episodes" } }
                for ((season, eps) in grouped) {
                    item {
                        SectionHeader(season)
                    }
                    items(eps) { episode ->
                        Column {
                            ContentCard(
                                title = episode.name,
                                subtitle = episode.plot.take(80),
                                imageUrl = episode.coverUrl.ifBlank { episode.streamIcon },
                                onClick = { openChannel(navigator, viewModel, episode) }
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
