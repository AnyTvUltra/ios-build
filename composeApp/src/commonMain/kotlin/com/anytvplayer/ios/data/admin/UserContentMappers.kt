package com.anytvplayer.ios.data.admin

import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel

private fun toChannelType(contentType: String): ChannelType = when (contentType.lowercase()) {
    "movie" -> ChannelType.VOD
    "series" -> ChannelType.SERIES
    else -> ChannelType.LIVE
}

fun toIptvChannel(
    contentType: String,
    contentId: String,
    title: String,
    posterUrl: String,
    streamUrl: String,
    groupTitle: String
): IptvChannel {
    val type = toChannelType(contentType)
    val seriesId = if (contentId.startsWith("SERIES_GROUP_")) {
        contentId.removePrefix("SERIES_GROUP_").toIntOrNull() ?: 0
    } else if (type == ChannelType.SERIES) {
        contentId.toIntOrNull() ?: 0
    } else {
        0
    }
    return IptvChannel(
        id = contentId,
        name = title,
        streamId = if (type == ChannelType.SERIES) 0 else contentId.toIntOrNull() ?: 0,
        streamIcon = posterUrl,
        streamUrl = streamUrl,
        type = type,
        categoryName = groupTitle,
        seriesId = seriesId,
        coverUrl = posterUrl
    )
}

fun LibraryItem.toIptvChannel(): IptvChannel = toIptvChannel(
    contentType = contentType,
    contentId = contentId,
    title = title,
    posterUrl = posterUrl,
    streamUrl = streamUrl,
    groupTitle = groupTitle
)

fun WatchProgressItem.toIptvChannel(): IptvChannel = toIptvChannel(
    contentType = contentType,
    contentId = contentId,
    title = title,
    posterUrl = posterUrl,
    streamUrl = streamUrl,
    groupTitle = ""
)
