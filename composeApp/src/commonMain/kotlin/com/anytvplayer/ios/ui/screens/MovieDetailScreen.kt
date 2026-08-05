package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.data.iptv.TmdbApi
import com.anytvplayer.ios.data.iptv.TmdbMovie
import com.anytvplayer.ios.ui.theme.TwitiMint

data class MovieDetailScreen(val encodedChannel: String) : Screen {
    override val key = "movie_detail/${encodedChannel.hashCode()}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current
        val channel = remember { IptvChannel.fromJson(encodedChannel) }
        val streamUrl = remember(channel) { viewModel.getStreamUrl(channel) }

        var tmdb by remember { mutableStateOf<TmdbMovie?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        LaunchedEffect(channel) {
            isLoading = true
            tmdb = TmdbApi.fetchMovie(channel)
            isLoading = false
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tmdb?.title ?: channel.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = tmdb?.backdropUrl?.ifBlank { channel.coverUrl.ifBlank { channel.streamIcon } } ?: channel.coverUrl.ifBlank { channel.streamIcon },
                        contentDescription = tmdb?.title ?: channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    FilledTonalButton(
                        onClick = {
                            if (streamUrl.isNotBlank()) {
                                navigator.push(PlayerScreen(streamUrl))
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        Spacer(Modifier.width(4.dp))
                        Text("Play")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    tmdb?.title ?: channel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (tmdb?.year?.isNotBlank() == true || tmdb?.rating?.isNotBlank() == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (tmdb?.year?.isNotBlank() == true) {
                            Text(tmdb?.year ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (tmdb?.rating?.isNotBlank() == true) {
                            Text("★ ${tmdb?.rating}", style = MaterialTheme.typography.bodyMedium, color = TwitiMint)
                        }
                        if (tmdb?.runtime?.isNotBlank() == true) {
                            Text(tmdb?.runtime ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (tmdb?.genres?.isNotBlank() == true) {
                    Text(
                        tmdb?.genres ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (tmdb?.director?.isNotBlank() == true) {
                    Text(
                        "Director: ${tmdb?.director}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (tmdb?.cast?.isNotBlank() == true) {
                    Text(
                        "Cast: ${tmdb?.cast}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (tmdb?.overview?.isNotBlank() == true || channel.plot.isNotBlank()) {
                    Text(
                        tmdb?.overview ?: channel.plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLoading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = TwitiMint)
                }
            }
        }
    }
}
