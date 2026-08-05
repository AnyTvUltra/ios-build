package com.anytvplayer.ios.data.iptv

import com.anytvplayer.ios.data.network.appHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

private val M3U_USER_AGENTS = listOf(
    "VLC/3.0.18 LibVLC/3.0.18",
    BROWSER_USER_AGENT,
    "Kodi/20.2",
    "IPTVSmarters/1.0.0",
    null
)

class IptvRepository {

    private var xtreamApi: XtreamApi? = null
    private var cachedChannels: List<IptvChannel> = emptyList()
    private var cachedCategories: List<IptvCategory> = emptyList()

    suspend fun connectXtream(server: IptvServer): XtreamLoginResponse {
        disconnect()
        val api = XtreamApi(server)
        val response = api.login()
        if (response.userInfo.status.lowercase() != "active") {
            throw Exception("Account not active: ${response.userInfo.status}")
        }
        xtreamApi = api
        return response
    }

    suspend fun downloadM3u(url: String): String = withContext(Dispatchers.Default) {
        var lastError: Exception? = null
        for (ua in M3U_USER_AGENTS) {
            try {
                val response = appHttpClient.get(url) {
                    header("Accept", "*/*")
                    if (ua != null) header("User-Agent", ua)
                }
                if (!response.status.isSuccess()) throw Exception("Failed to load playlist (${response.status.value})")
                val body = response.bodyAsText()
                if (body.isBlank()) throw Exception("Empty M3U playlist")
                if (body.contains("#EXTM3U", ignoreCase = true) ||
                    body.contains("#EXTINF:", ignoreCase = true) ||
                    body.startsWith("http", ignoreCase = true)
                ) {
                    return@withContext body
                } else {
                    lastError = Exception("File is not a valid M3U playlist")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: Exception("Failed to load playlist")
    }

    suspend fun connectM3u(url: String): Int = withContext(Dispatchers.Default) {
        disconnect()
        val body = downloadM3u(url)
        parseM3uContent(body)
    }

    fun connectM3uContent(content: String): Int {
        disconnect()
        return parseM3uContent(content)
    }

    private fun parseM3uContent(content: String): Int {
        val channels = M3uParser.parse(content)
        val categories = M3uParser.extractCategories(channels)
        cachedChannels = channels
        cachedCategories = categories
        return channels.size
    }

    suspend fun getCategories(type: ChannelType): List<IptvCategory> {
        val api = xtreamApi
        return if (api != null) {
            when (type) {
                ChannelType.LIVE -> api.getLiveCategories()
                ChannelType.VOD -> api.getVodCategories()
                ChannelType.SERIES -> api.getSeriesCategories()
            }
        } else {
            cachedCategories.filter { it.type == type }
        }
    }

    suspend fun getChannels(type: ChannelType, categoryId: String? = null): List<IptvChannel> {
        val api = xtreamApi
        return if (api != null) {
            when (type) {
                ChannelType.LIVE -> api.getLiveStreams(categoryId)
                ChannelType.VOD -> api.getVodStreams(categoryId)
                ChannelType.SERIES -> api.getSeriesStreams(categoryId)
            }
        } else {
            val filtered = cachedChannels.filter { it.type == type }
            if (categoryId != null) filtered.filter { it.categoryId == categoryId } else filtered
        }
    }

    suspend fun searchChannels(query: String): List<IptvChannel> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        val api = xtreamApi
        if (api != null && cachedChannels.isEmpty()) {
            val live = try { api.getLiveStreams() } catch (_: Exception) { emptyList() }
            val vod = try { api.getVodStreams() } catch (_: Exception) { emptyList() }
            val series = try { api.getSeriesStreams() } catch (_: Exception) { emptyList() }
            cachedChannels = live + vod + series
        }
        return cachedChannels.filter { it.name.lowercase().contains(lowerQuery) }
    }

    suspend fun getSeriesEpisodes(seriesId: Int): List<IptvChannel> {
        val api = xtreamApi ?: return emptyList()
        return api.getSeriesEpisodes(seriesId)
    }

    fun getStreamUrl(channel: IptvChannel): String {
        if (channel.streamUrl.isNotEmpty()) return channel.streamUrl
        val api = xtreamApi ?: return ""
        return when (channel.type) {
            ChannelType.LIVE -> api.getLiveStreamUrl(channel.streamId)
            ChannelType.VOD -> api.getVodStreamUrl(channel.streamId, channel.containerExtension.ifEmpty { "mp4" })
            ChannelType.SERIES -> ""
        }
    }

    fun isConnected(): Boolean = xtreamApi != null || cachedChannels.isNotEmpty()

    fun currentServerUrl(): String? = xtreamApi?.currentServerUrl()

    fun disconnect() {
        xtreamApi = null
        cachedChannels = emptyList()
        cachedCategories = emptyList()
    }
}
