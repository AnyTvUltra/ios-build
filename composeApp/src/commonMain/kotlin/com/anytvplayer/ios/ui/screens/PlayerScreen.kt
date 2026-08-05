package com.anytvplayer.ios.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.anytvplayer.ios.LocalIptvViewModel
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.components.VideoPlayer

data class PlayerScreen(val streamUrl: String) : Screen {
    override val key = "player/${streamUrl.hashCode()}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = LocalIptvViewModel.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(
                url = streamUrl,
                modifier = Modifier.fillMaxSize(),
                onProgress = { position, duration ->
                    val channel = viewModel.findChannelByUrl(streamUrl)
                        ?: IptvChannel(id = streamUrl, name = streamUrl, streamUrl = streamUrl)
                    viewModel.saveWatchProgress(channel, position, duration)
                }
            )

            IconButton(
                onClick = { navigator.pop() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    }
}
