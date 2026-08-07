package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Tv
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
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.StarYellow
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint

data class SeriesEpisodesScreen(val seriesId: Int) : Screen {
    override val key = "series_episodes/${seriesId}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        LaunchedEffect(seriesId) {
            if (seriesId > 0) viewModel.loadSeriesEpisodes(seriesId)
        }

        val series by remember(viewModel.seriesGroups, seriesId) {
            derivedStateOf { viewModel.seriesGroups.find { it.seriesId == seriesId } }
        }
        var selectedSeason by remember { mutableStateOf<String?>(viewModel.getSeriesSeason(seriesId)) }

        val savedScroll = viewModel.getSeriesScroll(seriesId)
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = savedScroll.index,
            initialFirstVisibleItemScrollOffset = savedScroll.offset
        )

        DisposableEffect(seriesId) {
            onDispose {
                viewModel.saveSeriesSeason(seriesId, selectedSeason)
                viewModel.saveSeriesScroll(seriesId, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
        }

        val episodes = viewModel.seriesEpisodes
        val sortedEpisodes = remember(episodes) {
            episodes.sortedWith(compareBy({ it.categoryName }, { it.name }))
        }
        val seasons = remember(sortedEpisodes) {
            sortedEpisodes.map { it.categoryName.ifBlank { "Episodes" } }.distinct()
        }
        val grouped = remember(sortedEpisodes, selectedSeason) {
            if (selectedSeason != null) {
                sortedEpisodes.filter { it.categoryName == selectedSeason }.groupBy { it.categoryName }
            } else {
                sortedEpisodes.groupBy { it.categoryName.ifBlank { "Episodes" } }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when {
                viewModel.isLoadingEpisodes && episodes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = TwitiMint) }

                episodes.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Tv, null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Text(viewModel.contentError ?: "Failed to load episodes", color = MaterialTheme.colorScheme.onSurface)
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item(key = "header") {
                            SeriesHeader(series = series, onBack = { navigator.pop() })
                        }

                        if (seasons.size > 1) {
                            item(key = "seasons") {
                                SeasonChips(seasons = seasons, selected = selectedSeason, onSelect = { selectedSeason = it })
                            }
                        }

                        grouped.forEach { (season, eps) ->
                            if (selectedSeason == null) {
                                item(key = "season_title_$season") { SeasonTitle(season, eps.size) }
                            }
                            items(items = eps, key = { "${it.id}:$season" }) { episode ->
                                EpisodeItem(
                                    episode = episode,
                                    fallbackImage = series?.coverUrl?.ifBlank { series?.streamIcon }.orEmpty(),
                                    onClick = {
                                        val url = viewModel.getStreamUrl(episode)
                                        if (url.isNotBlank()) navigator.push(PlayerScreen(url))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHeader(series: IptvChannel?, onBack: () -> Unit) {
    val imageUrl = series?.coverUrl?.ifBlank { series.streamIcon }.orEmpty()
    val title = series?.name ?: "Series"
    val plot = series?.plot.orEmpty()

    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(model = imageUrl, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent, Color.Black.copy(alpha = 0.85f)))
            )
        )

        IconButton(onClick = onBack, modifier = Modifier.padding(top = 40.dp, start = 8.dp).align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!series?.year.isNullOrBlank()) {
                    Text(series!!.year, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                }
                if (!series?.rating.isNullOrBlank()) {
                    Icon(Icons.Filled.Star, null, tint = StarYellow, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(series!!.rating, color = StarYellow, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (plot.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(plot, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SeasonChips(seasons: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TwitiMint, selectedLabelColor = Color.Black)
            )
        }
        items(seasons) { season ->
            FilterChip(
                selected = selected == season,
                onClick = { onSelect(season) },
                label = { Text(season) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TwitiMint, selectedLabelColor = Color.Black)
            )
        }
    }
}

@Composable
private fun SeasonTitle(season: String, count: Int) {
    Text(
        "$season  •  $count episodes",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun EpisodeItem(episode: IptvChannel, fallbackImage: String, onClick: () -> Unit) {
    val imageUrl = episode.streamIcon.ifBlank { fallbackImage }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(88.dp, 54.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(model = imageUrl, contentDescription = episode.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = TwitiMint)
            }

            Box(
                modifier = Modifier.align(Alignment.Center).size(28.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(episode.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (episode.categoryName.isNotBlank()) {
                Text(episode.categoryName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            if (episode.duration.isNotBlank()) {
                Text(episode.duration, color = TwitiCyan, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
