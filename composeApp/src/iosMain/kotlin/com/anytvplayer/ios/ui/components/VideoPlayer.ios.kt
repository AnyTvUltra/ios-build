package com.anytvplayer.ios.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.interop.UIKitView
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.UIKit.UIView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onProgress: (position: Long, duration: Long) -> Unit
) {
    UIKitView(
        factory = {
            val nsUrl = NSURL(string = url)
            if (nsUrl == null) {
                return@UIKitView UIView()
            }
            val player = AVPlayer(uRL = nsUrl)
            val controller = AVPlayerViewController()
            controller.player = player
            player.play()
            controller.view
        },
        modifier = modifier
    )
}
