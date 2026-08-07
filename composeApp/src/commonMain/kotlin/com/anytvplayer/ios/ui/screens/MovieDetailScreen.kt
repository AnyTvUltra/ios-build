package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.data.iptv.TmdbApi
import com.anytvplayer.ios.data.iptv.TmdbMovie
import com.anytvplayer.ios.ui.components.GlassCard
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
        LaunchedEffect(channel) { tmdb = TmdbApi.fetchMovie(channel) }

        val title = tmdb?.title?.ifBlank { null } ?: channel.name
        val year = tmdb?.year?.ifBlank { null } ?: channel.year.take(4)
        val rating = tmdb?.rating?.ifBlank { null } ?: channel.rating
        val runtime = tmdb?.runtime?.ifBlank { null } ?: channel.duration
        val plot = tmdb?.overview?.ifBlank { null } ?: channel.plot
        val cast = tmdb?.cast?.ifBlank { null } ?: channel.cast
        val director = tmdb?.director?.ifBlank { null } ?: channel.director
        val genres = tmdb?.genres?.ifBlank { null } ?: channel.genre
        val category = channel.categoryName

        val backdropUrl = tmdb?.backdropUrl?.ifBlank { null }
            ?: tmdb?.posterUrl?.ifBlank { null }
            ?: channel.coverUrl.ifBlank { channel.streamIcon }
        val posterUrl = tmdb?.posterUrl?.ifBlank { null } ?: channel.streamIcon

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Backdrop
            item {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    if (backdropUrl.isNotBlank()) {
                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title.take(2).uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent, MaterialTheme.colorScheme.background)
                                )
                            )
                    )

                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.padding(top = 40.dp, start = 8.dp).align(Alignment.TopStart)
                    ) {
                        GlassCard(cornerRadius = 12.dp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }

            // Content
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        if (posterUrl.isNotBlank()) {
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = title,
                                modifier = Modifier.width(110.dp).height(160.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (year.isNotBlank()) InfoChip(year)
                                if (rating.isNotBlank()) InfoChip("★ $rating")
                                if (runtime.isNotBlank()) InfoChip(runtime)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (genres.isNotBlank()) InfoChip(genres)
                        else if (category.isNotBlank()) InfoChip(category)
                    }

                    Spacer(Modifier.height(16.dp))

                    if (plot.isNotBlank()) {
                        Text("Plot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(6.dp))
                        Text(plot, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(16.dp))
                    }

                    if (cast.isNotBlank()) LabeledRow("Cast", cast)
                    if (director.isNotBlank()) LabeledRow("Director", director)

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { if (streamUrl.isNotBlank()) navigator.push(PlayerScreen(streamUrl)) },
                        enabled = streamUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TwitiMint),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
