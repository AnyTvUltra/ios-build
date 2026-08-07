package com.anytvplayer.ios.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvCategory
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.StarYellow
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.IptvViewModel

enum class HubTab(val icon: ImageVector, val label: String) {
    Channels(Icons.Outlined.LiveTv, "Channels"),
    Movies(Icons.Outlined.Movie, "Movies"),
    Series(Icons.Outlined.Tv, "Series");
}

data class LiveHubScreen(
    val allowedTabs: List<HubTab> = listOf(HubTab.Channels),
    val defaultTab: HubTab = HubTab.Channels,
    val titleOverride: String? = null
) : Screen {
    override val key: String get() = if (allowedTabs == listOf(HubTab.Channels)) "live" else "vod"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val iptvViewModel = LocalIptvViewModel.current
        val brandColor = iptvViewModel.brandColor

        val initialTab = remember {
            val saved = HubTab.entries.find { it.name == iptvViewModel.hubSelectedTab }
            if (saved != null && saved in allowedTabs) saved else defaultTab
        }

        var selectedTab by remember { mutableStateOf(initialTab) }
        var selectedCategoryId by remember { mutableStateOf<String?>(iptvViewModel.hubSelectedCategory) }
        var searchQuery by remember { mutableStateOf(iptvViewModel.hubSearchQuery) }
        var selectedYears by remember { mutableStateOf(emptyList<String>()) }
        var selectedRatings by remember { mutableStateOf(emptyList<String>()) }
        var filterSheetOpen by remember { mutableStateOf(false) }

        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = iptvViewModel.hubScroll.index,
            initialFirstVisibleItemScrollOffset = iptvViewModel.hubScroll.offset
        )

        val channels = when (selectedTab) {
            HubTab.Channels -> iptvViewModel.liveChannels
            HubTab.Movies -> iptvViewModel.vodChannels
            HubTab.Series -> iptvViewModel.seriesGroups
        }
        val categories = when (selectedTab) {
            HubTab.Channels -> iptvViewModel.liveCategories
            HubTab.Movies -> iptvViewModel.vodCategories
            HubTab.Series -> iptvViewModel.seriesCategories
        }

        val years = remember(channels) {
            channels.map { it.year }.filter { it.isNotBlank() }.distinct().sortedByDescending { it.toIntOrNull() ?: 0 }
        }
        val ratings = remember(channels) {
            channels.map { it.rating }.filter { it.isNotBlank() }.distinct().sortedByDescending { it.toFloatOrNull() ?: 0f }
        }

        DisposableEffect(Unit) {
            onDispose {
                iptvViewModel.saveHubSelection(selectedTab.name, selectedCategoryId, searchQuery)
                iptvViewModel.saveHubScroll(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
            }
        }

        val items = remember(channels, selectedCategoryId, searchQuery, selectedYears, selectedRatings) {
            var result = if (selectedCategoryId == null) channels
            else channels.filter { it.categoryId == selectedCategoryId }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                result = result.filter {
                    it.name.lowercase().contains(q) || it.categoryName.lowercase().contains(q)
                }
            }
            if (selectedYears.isNotEmpty()) result = result.filter { it.year in selectedYears }
            if (selectedRatings.isNotEmpty()) result = result.filter { it.rating in selectedRatings }
            result.sortedBy { it.name }
        }

        val title = titleOverride ?: when {
            allowedTabs == listOf(HubTab.Channels) -> "Live"
            else -> "VOD"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                if (allowedTabs.size > 1) {
                    HubTabBar(
                        allowedTabs = allowedTabs,
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            selectedCategoryId = null
                            searchQuery = ""
                            selectedYears = emptyList()
                            selectedRatings = emptyList()
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                HubHeader(
                    title = title,
                    showFilter = selectedTab != HubTab.Channels,
                    onFilterClick = { filterSheetOpen = true }
                )

                Spacer(Modifier.height(18.dp))

                val placeholder = when (selectedTab) {
                    HubTab.Channels -> "Search channels..."
                    HubTab.Movies -> "Search movies..."
                    HubTab.Series -> "Search series..."
                }
                SearchField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = placeholder)
                Spacer(Modifier.height(16.dp))

                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it },
                    onAllSelected = { selectedCategoryId = null }
                )
                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (iptvViewModel.isLoadingChannels && items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TwitiMint)
                        }
                    } else if (items.isEmpty()) {
                        EmptyHubState(selectedTab)
                    } else {
                        Crossfade(targetState = selectedTab, label = "hub", modifier = Modifier.fillMaxSize()) { tab ->
                            LibraryGrid(
                                tab = tab,
                                items = items,
                                navigator = navigator,
                                viewModel = iptvViewModel,
                                gridState = gridState
                            )
                        }
                    }
                }
            }

            if (filterSheetOpen) {
                FilterBottomSheet(
                    years = years,
                    ratings = ratings,
                    selectedYears = selectedYears,
                    selectedRatings = selectedRatings,
                    onYearToggle = { y -> selectedYears = if (y in selectedYears) selectedYears - y else selectedYears + y },
                    onRatingToggle = { r -> selectedRatings = if (r in selectedRatings) selectedRatings - r else selectedRatings + r },
                    onDismiss = { filterSheetOpen = false }
                )
            }
        }
    }
}

object VodHubScreen : Screen {
    override val key = "vod"

    @Composable
    override fun Content() {
        LiveHubScreen(
            allowedTabs = listOf(HubTab.Movies, HubTab.Series),
            defaultTab = HubTab.Movies,
            titleOverride = "VOD"
        ).Content()
    }
}

@Composable
private fun HubHeader(title: String, showFilter: Boolean, onFilterClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(LiveRed, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }
        if (showFilter) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = TwitiMint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50)),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = {
            Box(
                modifier = Modifier.size(36.dp).background(TwitiMint.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, null, tint = TwitiMint, modifier = Modifier.size(20.dp))
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            cursorColor = TwitiMint
        )
    )
}

@Composable
private fun HubTabBar(allowedTabs: List<HubTab>, selectedTab: HubTab, onTabSelected: (HubTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        allowedTabs.forEach { tab ->
            val selected = tab == selectedTab
            val tabBackground = if (selected) Modifier.background(Brush.horizontalGradient(listOf(TwitiMint, TwitiCyan)))
            else Modifier
            val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .then(tabBackground)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(tab.icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Text(
                        tab.label, color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<IptvCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onAllSelected: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            FilterChip(name = "All", selected = selectedCategoryId == null, onClick = onAllSelected)
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(name = category.name, selected = selectedCategoryId == category.id, onClick = { onCategorySelected(category.id) })
        }
    }
}

@Composable
private fun FilterChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundModifier = if (selected) Modifier.background(Brush.horizontalGradient(listOf(TwitiMint, TwitiCyan)))
    else Modifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(backgroundModifier)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun LibraryGrid(
    tab: HubTab,
    items: List<IptvChannel>,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    viewModel: IptvViewModel,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    val columns = when (tab) {
        HubTab.Channels -> GridCells.Adaptive(150.dp)
        else -> GridCells.Adaptive(116.dp)
    }

    LazyVerticalGrid(
        state = gridState,
        columns = columns,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items, key = { "${it.type}:${it.id}:${it.streamId}" }, contentType = { if (it.type == ChannelType.LIVE) "channel" else "media" }) { channel ->
            when (tab) {
                HubTab.Channels -> ChannelCard(channel, navigator, viewModel)
                else -> MediaCard(channel, navigator, viewModel)
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: IptvChannel,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    viewModel: IptvViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .clickable { openChannel(navigator, viewModel, channel) }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF20232A)),
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
                    text = channel.name.take(2).uppercase(),
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
            text = channel.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp, end = 3.dp, top = 9.dp)
        )
        Text(
            text = channel.categoryName.ifBlank { "Live" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MediaCard(
    channel: IptvChannel,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    viewModel: IptvViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .clickable { openChannel(navigator, viewModel, channel) }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF20232A)),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = channel.coverUrl.ifBlank { channel.streamIcon }

            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = TwitiMint,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }

            val hasMeta = channel.year.isNotBlank() || channel.rating.isNotBlank()
            if (hasMeta) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (channel.year.isNotBlank()) {
                            Text(channel.year, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        if (channel.year.isNotBlank() && channel.rating.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                        }
                        if (channel.rating.isNotBlank()) {
                            Icon(Icons.Filled.Star, null, tint = StarYellow, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(channel.rating, color = StarYellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (channel.type == ChannelType.VOD) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.52f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            if (channel.type != ChannelType.LIVE) {
                val inLibrary = viewModel.isInLibrary(channel)
                IconButton(
                    onClick = { if (inLibrary) viewModel.removeFromLibrary(channel) else viewModel.addToLibrary(channel) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (inLibrary) TwitiMint.copy(alpha = 0.9f)
                                else Color.Black.copy(alpha = 0.52f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (inLibrary) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Library",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = channel.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp, end = 3.dp, top = 9.dp)
        )
        Text(
            text = channel.categoryName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyHubState(tab: HubTab) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(tab.icon, null, tint = TwitiMint, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No ${tab.label}",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Content will appear here once loaded",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    years: List<String>,
    ratings: List<String>,
    selectedYears: List<String>,
    selectedRatings: List<String>,
    onYearToggle: (String) -> Unit,
    onRatingToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(24.dp)
        ) {
            Text(
                text = "Filter Content",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(20.dp))
            FilterSection(label = "Year", options = years, selected = selectedYears, onToggle = onYearToggle)
            Spacer(Modifier.height(18.dp))
            FilterSection(label = "Rating", options = ratings, selected = selectedRatings, onToggle = onRatingToggle)
        }
    }
}

@Composable
private fun FilterSection(label: String, options: List<String>, selected: List<String>, onToggle: (String) -> Unit) {
    Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    if (options.isEmpty()) {
        Text("No options available", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(options, key = { it }) { option ->
                FilterChip(name = option, selected = selected.contains(option), onClick = { onToggle(option) })
            }
        }
    }
}
