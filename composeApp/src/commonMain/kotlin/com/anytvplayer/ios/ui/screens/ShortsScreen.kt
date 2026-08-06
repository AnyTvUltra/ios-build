package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import com.anytvplayer.ios.data.admin.ShortClip
import com.anytvplayer.ios.data.admin.ShortSocialState
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.viewmodel.IptvViewModel

object ShortsScreen : Screen {
    override val key = "shorts"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        LaunchedEffect(Unit) {
            if (!viewModel.isConnected) {
                navigator.push(LoginScreen)
            } else {
                viewModel.loadAllContent()
            }
        }

        val shorts = viewModel.shortClips

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Twiti (${shorts.size})") },
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
                if (viewModel.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(shorts, key = { it.id }) { clip ->
                    ShortCard(
                        clip = clip,
                        viewModel = viewModel,
                        onClick = { navigator.push(PlayerScreen(clip.videoUrl)) },
                        onLike = { viewModel.toggleShortLike(clip.id) },
                        onSave = { viewModel.toggleShortSave(clip.id) }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (!viewModel.isLoading && shorts.isEmpty()) {
                    item {
                        Text(
                            "No short clips available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ShortCard(
    clip: ShortClip,
    viewModel: IptvViewModel,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit
) {
    val social = viewModel.shortSocialStates[clip.id] ?: ShortSocialState()
    val gradient = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))

    LaunchedEffect(clip.id) {
        viewModel.loadShortSocial(clip.id)
        viewModel.recordShortView(clip.id)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(24.dp))
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
                            Brush.linearGradient(listOf(TwitiMint, TwitiCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        clip.title.take(2).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.44f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    clip.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                if (clip.creator.isNotBlank()) {
                    Text(
                        "@${clip.creator}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${social.viewsCount} views",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (social.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (social.liked) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "${social.likesCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )

                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (social.saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (social.saved) TwitiMint else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "${social.savesCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
