package com.anytvplayer.ios.data.admin

import com.anytvplayer.ios.data.SecurePreferences
import com.anytvplayer.ios.data.iptv.ChannelType
import com.anytvplayer.ios.data.iptv.IptvChannel
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Local cache for parsed playlist channels. TTL = 30 minutes, matching the
 * Android client.
 */
class PlaylistCache {

    private val prefs = SecurePreferences(
        preferencesName = "anytv_playlist_cache_secure",
        keyAlias = "anytv_playlist_cache_key"
    )

    fun saveChannels(channels: List<IptvChannel>, playlistUrl: String) {
        val array = JsonArray(
            channels.map { channel ->
                buildJsonObject {
                    put("id", channel.id)
                    put("name", channel.name)
                    put("streamIcon", channel.streamIcon)
                    put("categoryId", channel.categoryId)
                    put("categoryName", channel.categoryName)
                    put("streamUrl", channel.streamUrl)
                    put("type", channel.type.name)
                }
            }
        )

        prefs.putString(KEY_CHANNELS, array.toString())
        prefs.putString(KEY_URL, playlistUrl)
        prefs.putLong(KEY_TIMESTAMP, Clock.System.now().toEpochMilliseconds())
    }

    fun loadChannels(): List<IptvChannel>? {
        val raw = prefs.getString(KEY_CHANNELS) ?: return null
        return runCatching {
            jsonParser.parseToJsonElement(raw).jsonArray.map { element ->
                val obj = element.jsonObject
                IptvChannel(
                    id = obj.text("id"),
                    name = obj.text("name"),
                    streamIcon = obj.text("streamIcon"),
                    categoryId = obj.text("categoryId"),
                    categoryName = obj.text("categoryName"),
                    streamUrl = obj.text("streamUrl"),
                    type = runCatching { ChannelType.valueOf(obj.text("type")) }
                        .getOrDefault(ChannelType.LIVE)
                )
            }
        }.getOrNull()
    }

    fun isFresh(): Boolean {
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val age = Clock.System.now().toEpochMilliseconds() - timestamp
        return age in 0 until CACHE_TTL_MS
    }

    fun getCachedUrl(): String? = prefs.getString(KEY_URL)

    fun savePlaylistVersion(
        hasPlaylist: Boolean,
        id: String,
        lastUpdated: String,
        total: Int
    ) {
        prefs.putString(KEY_PLAYLIST_VERSION, "$hasPlaylist|$id|$lastUpdated|$total")
    }

    fun getPlaylistVersion(): String? = prefs.getString(KEY_PLAYLIST_VERSION)

    fun clear() {
        prefs.remove(KEY_CHANNELS)
        prefs.remove(KEY_URL)
        prefs.remove(KEY_TIMESTAMP)
        prefs.remove(KEY_PLAYLIST_VERSION)
    }

    private fun JsonObject.text(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
        const val KEY_CHANNELS = "cached_channels"
        const val KEY_URL = "cached_url"
        const val KEY_TIMESTAMP = "cached_timestamp"
        const val KEY_PLAYLIST_VERSION = "last_playlist_version"

        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
