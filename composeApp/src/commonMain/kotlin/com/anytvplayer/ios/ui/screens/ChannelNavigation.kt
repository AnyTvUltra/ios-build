package com.anytvplayer.ios.ui.screens

import cafe.adriel.voyager.navigator.Navigator
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel
import com.anytvplayer.ios.viewmodel.IptvViewModel

fun openChannel(navigator: Navigator, viewModel: IptvViewModel, channel: IptvChannel) {
    if (channel.type == ChannelType.SERIES) {
        val seriesId = channel.seriesId.takeIf { it > 0 } ?: channel.streamId
        if (seriesId > 0) {
            navigator.push(SeriesEpisodesScreen(seriesId))
        }
        return
    }

    val streamUrl = viewModel.getStreamUrl(channel)
    if (streamUrl.isBlank()) {
        return
    }

    if (channel.type == ChannelType.VOD) {
        navigator.push(MovieDetailScreen(channel.toJson()))
    } else {
        navigator.push(PlayerScreen(streamUrl))
    }
}
