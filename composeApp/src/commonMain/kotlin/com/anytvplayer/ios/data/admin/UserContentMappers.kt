package com.anytvplayer.ios.data.admin

import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel

private fun String.toChannelType(): ChannelType = when (lowercase()) {
    "live" -> ChannelType.LIVE
    "movie" -> ChannelType.VOD
    "series" -> ChannelType.SERIES
    else -> ChannelType.LIVE
}

fun LibraryItem.toIptvChannel(): IptvChannel = IptvChannel(
    id = contentId,
    name = title,
    streamUrl = streamUrl,
    streamIcon = posterUrl,
    coverUrl = posterUrl,
    categoryName = groupTitle,
    type = contentType.toChannelType()
)

fun WatchProgressItem.toIptvChannel(): IptvChannel = IptvChannel(
    id = contentId,
    name = title,
    streamUrl = streamUrl,
    streamIcon = posterUrl,
    coverUrl = posterUrl,
    categoryName = "",
    type = contentType.toChannelType()
)
