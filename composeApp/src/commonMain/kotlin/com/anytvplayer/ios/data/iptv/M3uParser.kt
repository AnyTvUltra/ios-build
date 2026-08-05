package com.anytvplayer.ios.data.iptv

/**
 * Parses M3U/M3U8 playlist files into IptvChannel objects.
 */
object M3uParser {

    fun parse(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val cleaned = content.replace("\uFEFF", "")
        val lines = cleaned.lines()
        var i = 0
        var idCounter = 1

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF:")) {
                val attrs = parseAttributes(line)
                val name = extractDisplayName(line)
                val group = attrs["group-title"]?.trim().takeUnless { it.isNullOrBlank() } ?: "Other"

                var url = ""
                var j = i + 1
                while (j < lines.size) {
                    val nextLine = lines[j].trim()
                    if (nextLine.isNotEmpty() && !nextLine.startsWith("#")) {
                        url = nextLine
                        break
                    }
                    j++
                }

                if (url.isNotEmpty()) {
                    val type = guessChannelType(url, group)
                    val categoryId = "${type.name}:$group".hashCode().toString()
                    val sourceId = attrs["tvg-id"]?.takeIf { it.isNotBlank() } ?: "m3u"
                    val icon = attrs["tvg-logo"] ?: ""
                    channels.add(
                        IptvChannel(
                            id = "${sourceId}_${idCounter++}",
                            name = name,
                            streamIcon = icon,
                            categoryId = categoryId,
                            categoryName = group,
                            streamUrl = url,
                            type = type,
                            coverUrl = icon
                        )
                    )
                    i = j + 1
                    continue
                }
            }
            i++
        }

        return channels.distinctBy { channel ->
            val normalizedUrl = channel.streamUrl.trim()
            if (normalizedUrl.isNotBlank()) {
                "${channel.type}:url:$normalizedUrl"
            } else {
                "${channel.type}:meta:${channel.id}:${channel.name.trim().lowercase()}"
            }
        }
    }

    fun extractCategories(channels: List<IptvChannel>): List<IptvCategory> {
        return channels
            .groupBy { it.type to it.categoryName }
            .map { (key, items) ->
                IptvCategory(
                    id = items.first().categoryId,
                    name = key.second,
                    type = key.first
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val regex = Regex("""(\S+?)="([^"]*?)""")
        regex.findAll(line).forEach { match ->
            attrs[match.groupValues[1]] = match.groupValues[2]
        }
        return attrs
    }

    private fun extractDisplayName(line: String): String {
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex >= 0 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }
    }

    private fun guessChannelType(url: String, group: String): ChannelType {
        val lowerUrl = url.substringBefore('?').lowercase()
        val lowerGroup = group.lowercase()
        return when {
            lowerUrl.contains("/series/") || lowerGroup.contains("series") ||
                    lowerGroup.contains("مسلسل") || lowerGroup.contains("مسلسلات") -> ChannelType.SERIES
            lowerUrl.contains("/movie/") || lowerUrl.contains("/vod/") ||
                    lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") ||
                    lowerUrl.endsWith(".avi") || lowerGroup.contains("movie") ||
                    lowerGroup.contains("film") || lowerGroup.contains("vod") ||
                    lowerGroup.contains("فيلم") || lowerGroup.contains("أفلام") ||
                    lowerGroup.contains("افلام") -> ChannelType.VOD
            else -> ChannelType.LIVE
        }
    }
}
