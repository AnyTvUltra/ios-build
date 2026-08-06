package com.anytvplayer.ios.viewmodel

import androidx.compose.ui.graphics.Color
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.ui.theme.GradientEnd
import com.anytvplayer.ios.ui.theme.TwitiCyan
import com.anytvplayer.ios.ui.theme.TwitiGreen
import com.anytvplayer.ios.ui.theme.TwitiMint
import com.anytvplayer.ios.ui.theme.TwitiTeal

fun IptvChannel.gradientForType(): List<Color> {
    return when (type) {
        ChannelType.LIVE -> listOf(TwitiMint, TwitiCyan)
        ChannelType.VOD -> listOf(TwitiGreen, TwitiTeal)
        ChannelType.SERIES -> listOf(TwitiCyan, GradientEnd)
    }
}
