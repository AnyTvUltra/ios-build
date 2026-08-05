package com.anytvplayer.ios.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onProgress: (position: Long, duration: Long) -> Unit
) {
    UIKitView(
        factory = {
            val nsUrl = NSURL(string = url)
            val player = if (nsUrl != null) AVPlayer(uRL = nsUrl) else null
            val controller = AVPlayerViewController()
            controller.player = player
            player?.play()
            controller.view
        },
        modifier = modifier
    )
}
