package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.admin.ShortClip
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.ConnectionState
import com.anytvplayer.ios.data.iptv.IptvCategory
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.components.GlassCard
import com.anytvplayer.ios.ui.theme.LiveRed
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.ui.theme.StarYellow
import com.anytvplayer.ios.ui.theme.WarningOrange
import com.anytvplayer.ios.viewmodel.IptvViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object HomeScreen : Screen {
    override val key = "home"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        LaunchedEffect(Unit) { viewModel.tryAutoConnect() }

        LaunchedEffect(viewModel.activationState?.isActivated) {
            val activation = viewModel.activationState
            if (activation != null && !activation.isActivated) {
                navigator.push(ActivationScreen)
            }
        }

        val brandColor = viewModel.brandColor

        val isLoadingInitialContent = viewModel.isLoadingChannels &&
            viewModel.banners.isEmpty() &&
            viewModel.brandingConfig.bannerImageUrl.isBlank() &&
            viewModel.liveChannels.isEmpty() &&
            viewModel.vodChannels.isEmpty() &&
            viewModel.seriesChannels.isEmpty()

        when (val state = viewModel.connectionState) {
            ConnectionState.Disconnected,
            ConnectionState.Connecting -> HomeLoading()
            is ConnectionState.Error -> HomeConnectionError(
                message = state.message,
                onRetry = viewModel::tryAutoConnect
            )
            is ConnectionState.Connected -> {
                if (isLoadingInitialContent) HomeLoading()
                else HomeContent(navigator, viewModel, brandColor)
            }
        }
    }
}

@Composable
private fun HomeContent(
    navigator: cafe.adriel.voyager.navigator.Navigator,
    viewModel: IptvViewModel,
    brandColor: Color
) {
    val adminBanner = remember(viewModel.banners) {
        viewModel.banners
            .filter { it.imageUrl.isNotBlank() }
            .map { banner ->
                IptvChannel(
                    id = "admin_banner_${banner.id}",
                    name = banner.title.ifBlank { "Ad" },
                    streamIcon = banner.imageUrl,
                    coverUrl = banner.imageUrl,
                    categoryName = banner.subtitle,
                    streamUrl = banner.streamUrl,
                    type = ChannelType.VOD
                )
            }
            .ifEmpty {
                viewModel.brandingConfig.bannerImageUrl.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    listOf(
                        IptvChannel(
                            id = "admin_banner",
                            name = viewModel.brandingConfig.bannerTitle.ifBlank { "Ad" },
                            streamIcon = imageUrl,
                            coverUrl = imageUrl,
                            categoryName = viewModel.brandingConfig.bannerSubtitle,
                            streamUrl = viewModel.brandingConfig.bannerStreamUrl,
                            type = ChannelType.VOD
                        )
                    )
                } ?: emptyList()
            }
    }

    val featuredItems = if (adminBanner.isNotEmpty()) {
        adminBanner
    } else {
        remember(
            viewModel.vodChannels,
            viewModel.seriesChannels,
            viewModel.liveChannels,
        ) {
            val preferred = (
                viewModel.vodChannels.take(6) +
                    viewModel.seriesChannels.take(4) +
                    viewModel.liveChannels.take(2)
                )
                .distinctBy { "${it.type}:${it.id}:${it.streamId}" }
            preferred.filter { it.coverUrl.isNotBlank() || it.streamIcon.isNotBlank() }
                .ifEmpty { preferred }
                .take(8)
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.homeScroll.index,
        initialFirstVisibleItemScrollOffset = viewModel.homeScroll.offset
    )
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveHomeScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }
    val threshold = with(LocalDensity.current) { 300.dp.toPx() }
    val scrollAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                (listState.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
            } else 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp)
        ) {
            item(key = "featured_carousel") {
                FeaturedCarousel(
                    modifier = Modifier.fillParentMaxHeight(0.88f),
                    channels = featuredItems,
                    brandColor = brandColor,
                    onChannelClick = { channel ->
                        if (channel != null) {
                            openChannel(navigator, viewModel, channel)
                        } else {
                            navigator.push(PlaylistPickerScreen)
                        }
                    },
                )
            }

            viewModel.contentError?.let { error ->
                item {
                    Text(
                        text = error,
                        color = LiveRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            if (viewModel.availablePlaylists.size > 1) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { navigator.push(PlaylistPickerScreen) },
                        glassColor = brandColor.copy(alpha = 0.10f),
                        borderColor = brandColor.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, null, tint = brandColor)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text("Playlists", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    viewModel.currentPlaylist?.name ?: "Choose playlist",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("Change", color = brandColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (viewModel.shortClips.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Trending",
                        color = TwitiCyan,
                        onViewAll = { navigator.push(ShortsScreen) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.shortClips.take(8), key = { it.id }) { clip ->
                            ShortPreviewCard(
                                clip = clip,
                                onClick = { navigator.push(ShortsScreen) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (viewModel.isLoadingChannels) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = brandColor)
                    }
                }
            } else {
                channelSection(
                    title = "Live",
                    channels = viewModel.liveChannels,
                    iconColor = LiveRed,
                    onChannelClick = { openChannel(navigator, viewModel, it) },
                    onViewAll = { navigator.push(LiveHubScreen()) }
                )
                channelSection(
                    title = "Latest Movies",
                    channels = viewModel.vodChannels,
                    iconColor = brandColor,
                    onChannelClick = { openChannel(navigator, viewModel, it) }
                )
                channelSection(
                    title = "Series",
                    channels = viewModel.seriesChannels,
                    iconColor = TwitiCyan,
                    onChannelClick = { openChannel(navigator, viewModel, it) }
                )
                categoryContentSections(
                    navigator = navigator,
                    viewModel = viewModel,
                    categories = viewModel.liveCategories,
                    channels = viewModel.liveChannels,
                    iconColor = LiveRed,
                    onChannelClick = { openChannel(navigator, viewModel, it) }
                )
                categoryContentSections(
                    navigator = navigator,
                    viewModel = viewModel,
                    categories = viewModel.vodCategories,
                    channels = viewModel.vodChannels,
                    iconColor = brandColor,
                    onChannelClick = { openChannel(navigator, viewModel, it) }
                )
                categoryContentSections(
                    navigator = navigator,
                    viewModel = viewModel,
                    categories = viewModel.seriesCategories,
                    channels = viewModel.seriesChannels,
                    iconColor = TwitiCyan,
                    onChannelClick = { openChannel(navigator, viewModel, it) }
                )
            }

            if (
                viewModel.isConnected &&
                !viewModel.isLoadingChannels &&
                viewModel.liveChannels.isEmpty() &&
                viewModel.vodChannels.isEmpty() &&
                viewModel.seriesChannels.isEmpty()
            ) {
                item {
                    EmptyHome(
                        onPlaylists = { navigator.push(PlaylistPickerScreen) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Transparent
                        )
                    )
                )
        )

        StreamingTopBar(
            navigator = navigator,
            scrollAlpha = scrollAlpha,
            appName = viewModel.brandingConfig.appName,
            logoUrl = viewModel.brandingConfig.logoUrl,
            unreadCount = viewModel.unreadCount,
            brandColor = brandColor
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.channelSection(
    title: String,
    channels: List<IptvChannel>,
    iconColor: Color,
    onChannelClick: (IptvChannel) -> Unit,
    onViewAll: (() -> Unit)? = null
) {
    if (channels.isEmpty()) return
    item {
        SectionTitle(title, iconColor, onViewAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels.take(20), key = { it.id }) { channel ->
                IptvChannelCard(channel = channel, onClick = { onChannelClick(channel) })
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categoryContentSections(
    navigator: cafe.adriel.voyager.navigator.Navigator,
    viewModel: IptvViewModel,
    categories: List<IptvCategory>,
    channels: List<IptvChannel>,
    iconColor: Color,
    onChannelClick: (IptvChannel) -> Unit
) {
    if (categories.isEmpty() || channels.isEmpty()) return

    categories.take(6).forEach { category ->
        val categoryChannels = channels
            .filter { it.categoryId == category.id }
            .distinctBy { "${it.type}:${it.id}:${it.streamId}" }
            .take(10)

        if (categoryChannels.isEmpty()) return@forEach

        item {
            SectionTitle(
                title = category.name,
                color = iconColor,
                onViewAll = {
                    navigator.push(
                        IptvChannelsScreen(category.type.name, category.id)
                    )
                }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = categoryChannels,
                    key = { "${it.type}:${it.id}:${it.streamId}" }
                ) { channel ->
                    IptvChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun FeaturedCarousel(
    modifier: Modifier = Modifier,
    channels: List<IptvChannel>,
    brandColor: Color,
    onChannelClick: (IptvChannel?) -> Unit,
) {
    if (channels.isEmpty()) {
        FeaturedBanner(
            channel = null,
            brandColor = brandColor,
            onPrimaryAction = { onChannelClick(null) },
            modifier = modifier,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { channels.size })
    val scope = rememberCoroutineScope()
    val currentChannels by rememberUpdatedState(channels)

    // Auto-scroll for image banners
    val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pagerState, pagerIsDragged) {
        if (pagerIsDragged) return@LaunchedEffect
        while (true) {
            val size = currentChannels.size
            if (size <= 1) {
                delay(1_000)
                continue
            }

            if (pagerState.isScrollInProgress) {
                delay(200)
                continue
            }

            delay(5_000)

            if (pagerState.isScrollInProgress) {
                delay(200)
                continue
            }

            val current = pagerState.currentPage.coerceIn(0, size - 1)
            val nextPage = (current + 1) % size
            try {
                pagerState.animateScrollToPage(nextPage)
            } catch (_: CancellationException) {
                try {
                    pagerState.scrollToPage(pagerState.currentPage.coerceIn(0, size - 1))
                } catch (_: Exception) { }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> "${channels[page].type}:${channels[page].id}:${channels[page].streamId}" },
        ) { page ->
            val channel = channels[page]
            FeaturedBanner(
                channel = channel,
                brandColor = brandColor,
                onPrimaryAction = { onChannelClick(channel) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (channels.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                channels.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (index == pagerState.currentPage) 22.dp else 7.dp,
                                height = 7.dp,
                            )
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) {
                                    brandColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingTopBar(
    navigator: cafe.adriel.voyager.navigator.Navigator,
    scrollAlpha: Float,
    appName: String,
    logoUrl: String,
    unreadCount: Int,
    brandColor: Color
) {
    val backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = scrollAlpha)
    val contentColor = if (scrollAlpha > 0.5f) MaterialTheme.colorScheme.onBackground else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                TwitiLogo(
                    logoUrl = logoUrl,
                    appName = appName,
                    contentColor = contentColor
                )
            }

            IconButton(
                onClick = { navigator.push(NotificationsScreen) }
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )

                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp),
                            containerColor = LiveRed,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            Button(
                onClick = { navigator.push(ActivationScreen) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Subscribe",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TwitiLogo(
    logoUrl: String,
    appName: String,
    contentColor: Color
) {
    if (logoUrl.isNotBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = appName,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Text(
            text = appName.ifBlank { "AnyTV" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )
    }
}

@Composable
private fun FeaturedBanner(
    channel: IptvChannel?,
    brandColor: Color,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val imageUrl = channel?.coverUrl?.ifBlank { channel.streamIcon }.orEmpty()
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = channel?.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                brandColor.copy(alpha = 0.82f),
                                Color(0xFF3B1425),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            bgColor.copy(alpha = 0.92f),
                            bgColor
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 58.dp)
        ) {
            Text(
                text = if (channel != null) "Free Watch" else "Start Now",
                color = brandColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = channel?.name ?: "Add your playlist to start watching",
                color = Color.White,
                style = if (channel != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
            if (channel != null) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (channel.year.isNotBlank()) {
                        Text(
                            text = channel.year,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (channel.categoryName.isNotBlank()) {
                        Text(
                            text = channel.categoryName,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (channel.rating.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = channel.rating,
                            color = StarYellow,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    if (channel != null) Icons.Filled.PlayArrow else Icons.AutoMirrored.Outlined.PlaylistPlay,
                    contentDescription = null
                )
                Text(
                    if (channel != null) "Watch Now" else "Playlists",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color, onViewAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )
        if (onViewAll != null) {
            Text(
                "View All",
                color = color,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }
    }
}

@Composable
fun IptvChannelCard(
    channel: IptvChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(if (channel.type == ChannelType.LIVE) 150.dp else 132.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (channel.type == ChannelType.LIVE) 96.dp else 176.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TwitiMint,
                    modifier = Modifier.size(36.dp)
                )
            }
            if (channel.type == ChannelType.LIVE) {
                Text(
                    "LIVE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LiveRed)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Text(
            channel.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

@Composable
fun CategoryChip(name: String, primaryColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(primaryColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            color = primaryColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ShortPreviewCard(clip: ShortClip, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(138.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (clip.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = clip.thumbnailUrl,
                contentDescription = clip.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(TwitiMint.copy(alpha = 0.7f), MaterialTheme.colorScheme.surfaceVariant)
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(11.dp)
        ) {
            Text(
                clip.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${clip.views} views",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AnyTV",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TwitiMint
            )
            Spacer(Modifier.height(22.dp))
            CircularProgressIndicator(color = TwitiMint)
            Spacer(Modifier.height(14.dp))
            Text("Connecting...", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun HomeConnectionError(message: String, onRetry: () -> Unit) {
    val networkKeywords = listOf(
        "unable to resolve host",
        "no address associated",
        "unknownhost",
        "connect",
        "sockettimeout",
        "ssl",
        "timeout",
        "network"
    )
    val isNetworkError = networkKeywords.any { message.contains(it, ignoreCase = true) }
    val displayMessage = if (isNetworkError) {
        "Unable to connect. Check your internet connection and try again."
    } else {
        message
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(LiveRed.copy(alpha = 0.22f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(LiveRed, WarningOrange)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Text(
                    "Connection Error",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )

                Text(
                    displayMessage,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(TwitiMint, TwitiCyan))
                        )
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text(
                            "Try Again",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(onPlaylists: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(12.dp))
        Text("No content", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Text("Add content from admin panel", color = MaterialTheme.colorScheme.onSurface)
        Text(
            "View Playlists",
            color = TwitiMint,
            modifier = Modifier.clickable(onClick = onPlaylists).padding(12.dp)
        )
    }
}
