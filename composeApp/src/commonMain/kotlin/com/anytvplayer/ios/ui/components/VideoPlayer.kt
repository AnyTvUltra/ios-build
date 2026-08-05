package com.anytvplayer.ios.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onProgress: (position: Long, duration: Long) -> Unit = { _, _ -> }
)
