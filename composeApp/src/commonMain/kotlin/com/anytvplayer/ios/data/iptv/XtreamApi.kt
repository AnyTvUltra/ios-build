package com.anytvplayer.ios.data.iptv

import com.anytvplayer.ios.data.network.appHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class XtreamApi(private val server: IptvServer) {

    private val categoryNames = mutableMapOf<ChannelType, Map<String, String>>()
    private var serverInfo: XtreamServerInfo = XtreamServerInfo()

    fun currentServerUrl(): String = server.serverUrl

    private val baseUrl: String
        get() {
            val url = server.serverUrl.trimEnd('/')
            return if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        }

    private val baseHttpUrl: Url
        get() = runCatching { Url(baseUrl) }.getOrNull() ?: throw IllegalArgumentException("Invalid Xtream URL")

    private fun baseStreamHttpUrl(): Url {
        val info = serverInfo
        val base = baseHttpUrl
        if (info.url.isBlank()) return base

        val raw = info.url.trim()
        val hostPort = raw
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        val host = hostPort.substringBefore(":", "").trim()
        val explicitPort = hostPort.substringAfter(":", "").trim()
        if (host.isBlank()) return base

        if (host.equals(base.host, ignoreCase = true) && explicitPort.isBlank()) {
            return base
        }

        val scheme = when {
            raw.startsWith("http://", ignoreCase = true) -> "http"
            raw.startsWith("https://", ignoreCase = true) -> "https"
            else -> info.serverProtocol.ifBlank { base.protocol.name }.lowercase()
        }

        val port = when {
            explicitPort.isNotBlank() && explicitPort != "0" -> explicitPort
            scheme == "https" && info.httpsPort.isNotBlank() && info.httpsPort != "0" -> info.httpsPort
            info.port.isNotBlank() && info.port != "0" -> info.port
            else -> base.port.toString()
        }

        val portPart = if (port != "80" && port != "443") ":$port" else ""
        return runCatching { Url("$scheme://$host$portPart") }.getOrNull() ?: base
    }

    suspend fun login(): XtreamLoginResponse = withContext(Dispatchers.IO) {
        val body = fetchJson(apiUrl())
        val userObj = body["user_info"]?.jsonObject ?: JsonObject(emptyMap())
        val serverObj = body["server_info"]?.jsonObject ?: JsonObject(emptyMap())

        serverInfo = XtreamServerInfo(
            url = serverObj.getString("url"),
            port = serverObj.getString("port"),
            httpsPort = serverObj.getString("https_port"),
            serverProtocol = serverObj.getString("server_protocol"),
            timeNow = serverObj.getString("time_now")
        )

        XtreamLoginResponse(
            userInfo = XtreamUserInfo(
                username = userObj.getString("username"),
                password = userObj.getString("password"),
                status = userObj.getString("status"),
                expDate = userObj.getString("exp_date"),
                isTrial = userObj.getString("is_trial") == "1",
                activeCons = userObj.getInt("active_cons"),
                maxConnections = userObj.getInt("max_connections"),
                createdAt = userObj.getString("created_at")
            ),
            serverInfo = serverInfo
        )
    }

    suspend fun getLiveCategories(): List<IptvCategory> = withContext(Dispatchers.IO) {
        parseCategories(fetchJsonArray(apiUrl("get_live_categories")), ChannelType.LIVE)
    }

    suspend fun getVodCategories(): List<IptvCategory> = withContext(Dispatchers.IO) {
        parseCategories(fetchJsonArray(apiUrl("get_vod_categories")), ChannelType.VOD)
    }

    suspend fun getSeriesCategories(): List<IptvCategory> = withContext(Dispatchers.IO) {
        parseCategories(fetchJsonArray(apiUrl("get_series_categories")), ChannelType.SERIES)
    }

    suspend fun getLiveStreams(categoryId: String? = null): List<IptvChannel> = withContext(Dispatchers.IO) {
        parseStreams(fetchJsonArray(apiUrl("get_live_streams", categoryId)), ChannelType.LIVE)
    }

    suspend fun getVodStreams(categoryId: String? = null): List<IptvChannel> = withContext(Dispatchers.IO) {
        parseStreams(fetchJsonArray(apiUrl("get_vod_streams", categoryId)), ChannelType.VOD)
    }

    suspend fun getSeriesStreams(categoryId: String? = null): List<IptvChannel> = withContext(Dispatchers.IO) {
        parseStreams(fetchJsonArray(apiUrl("get_series", categoryId)), ChannelType.SERIES)
    }

    suspend fun getSeriesEpisodes(seriesId: Int): List<IptvChannel> = withContext(Dispatchers.IO) {
        val url = URLBuilder(baseHttpUrl).apply {
            path("player_api.php")
            parameters.append("username", server.username)
            parameters.append("password", server.password)
            parameters.append("action", "get_series_info")
            parameters.append("series_id", seriesId.toString())
        }.build()

        val body = fetchJson(url)
        val rootInfo = body["info"]?.jsonObject
        val seriesCover = rootInfo?.getString("cover").ifBlank {
            rootInfo?.getString("cover_big")
        }.ifBlank {
            rootInfo?.getString("stream_icon")
        }.ifBlank {
            rootInfo?.getString("thumb")
        }

        val episodesObject = body["episodes"]?.jsonObject ?: JsonObject(emptyMap())
        val episodes = mutableListOf<IptvChannel>()
        episodesObject.entries.forEach { (season, element) ->
            val seasonEpisodes = (element as? JsonArray) ?: return@forEach
            seasonEpisodes.forEachIndexed { index, ep ->
                val episode = ep.jsonObject
                val episodeId = episode.getString("id").toIntOrNull() ?: 0
                val extension = episode.getString("container_extension", "mp4")
                val info = episode["info"]?.jsonObject
                val episodeCover = info?.getString("movie_image").ifBlank {
                    info?.getString("cover")
                }.ifBlank {
                    info?.getString("thumbnail")
                }.ifBlank {
                    info?.getString("thumb")
                }.ifBlank {
                    seriesCover
                }

                episodes += IptvChannel(
                    id = "EPISODE_$episodeId",
                    name = episode.getString("title", "Episode ${index + 1}"),
                    streamId = episodeId,
                    streamIcon = episodeCover,
                    categoryId = season,
                    categoryName = "Season $season",
                    streamUrl = getSeriesStreamUrl(episodeId, extension),
                    type = ChannelType.VOD,
                    plot = info?.getString("plot"),
                    duration = info?.getString("duration"),
                    containerExtension = extension,
                    seriesId = seriesId
                )
            }
        }
        episodes
    }

    fun getLiveStreamUrl(streamId: Int, extension: String = "m3u8"): String =
        streamUrl("live", streamId, extension)

    fun getVodStreamUrl(streamId: Int, extension: String): String =
        streamUrl("movie", streamId, extension)

    fun getSeriesStreamUrl(streamId: Int, extension: String): String =
        streamUrl("series", streamId, extension)

    private fun streamUrl(kind: String, streamId: Int, extension: String): String {
        return URLBuilder(baseStreamHttpUrl()).apply {
            path(kind, server.username, server.password, "$streamId.${extension.ifBlank { "mp4" }}")
        }.buildString()
    }

    private suspend fun fetchJson(url: Url): JsonObject {
        val response = appHttpClient.get(url) {
            headers.append("User-Agent", BROWSER_USER_AGENT)
        }
        if (!response.status.isSuccess()) throw Exception("Xtream error (${response.status.value})")
        val body = response.bodyAsText()
        if (body.isBlank()) throw Exception("Empty Xtream response")
        return jsonParser.parseToJsonElement(body).jsonObject
    }

    private suspend fun fetchJsonArray(url: Url): JsonArray {
        val response = appHttpClient.get(url) {
            headers.append("User-Agent", BROWSER_USER_AGENT)
        }
        if (!response.status.isSuccess()) throw Exception("Xtream error (${response.status.value})")
        val body = response.bodyAsText()
        if (body.isBlank()) throw Exception("Empty Xtream response")
        return jsonParser.parseToJsonElement(body).jsonArray
    }

    private fun apiUrl(action: String? = null, categoryId: String? = null): Url {
        return URLBuilder(baseHttpUrl).apply {
            path("player_api.php")
            parameters.append("username", server.username)
            parameters.append("password", server.password)
            if (!action.isNullOrBlank()) parameters.append("action", action)
            if (!categoryId.isNullOrBlank()) parameters.append("category_id", categoryId)
        }.build()
    }

    private fun parseCategories(array: JsonArray, type: ChannelType): List<IptvCategory> {
        val list = mutableListOf<IptvCategory>()
        for (element in array) {
            val obj = element.jsonObject
            val id = obj.getString("category_id").trim()
            val name = obj.getString("category_name").trim()
            if (id.isBlank() || name.isBlank()) continue
            list.add(
                IptvCategory(
                    id = id,
                    name = name,
                    parentId = obj.getString("parent_id", "0"),
                    type = type
                )
            )
        }
        val normalized = list.distinctBy { it.id }
        categoryNames[type] = normalized.associate { it.id to it.name }
        return normalized
    }

    private fun parseStreams(array: JsonArray, type: ChannelType): List<IptvChannel> {
        val list = mutableListOf<IptvChannel>()
        for (objElement in array) {
            val obj = objElement.jsonObject
            val streamId = obj.getString("stream_id").toIntOrNull()
                ?: obj.getString("series_id").toIntOrNull() ?: 0
            val ext = obj.getString("container_extension", if (type == ChannelType.LIVE) "m3u8" else "mp4")
            val categoryId = obj.getString("category_id")

            val streamUrl = when (type) {
                ChannelType.LIVE -> getLiveStreamUrl(streamId, ext)
                ChannelType.VOD -> getVodStreamUrl(streamId, ext)
                ChannelType.SERIES -> ""
            }

            list.add(
                IptvChannel(
                    id = "${type.name}_$streamId",
                    name = obj.getString("name", "Unknown"),
                    streamId = streamId,
                    streamIcon = obj.getString("stream_icon").ifBlank { obj.getString("cover") },
                    categoryId = categoryId,
                    categoryName = categoryNames[type]?.get(categoryId).orEmpty(),
                    streamUrl = streamUrl,
                    type = type,
                    rating = obj.getString("rating"),
                    year = obj.getString("year"),
                    plot = obj.getString("plot"),
                    cast = obj.getString("cast"),
                    director = obj.getString("director"),
                    genre = obj.getString("genre"),
                    duration = obj.getString("duration"),
                    containerExtension = ext,
                    seriesId = obj.getString("series_id").toIntOrNull() ?: 0,
                    coverUrl = obj.getString("cover").ifBlank { obj.getString("stream_icon") }
                )
            )
        }
        return list
    }

    companion object {
        private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private fun JsonObject.getString(key: String, default: String = ""): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: default
}

private fun JsonObject.getInt(key: String, default: Int = 0): Int {
    return this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: default
}

private fun String?.ifBlank(default: () -> String?): String {
    return if (this.isNullOrBlank()) default().orEmpty() else this
}

private fun String?.ifBlank(default: String?): String {
    return if (this.isNullOrBlank()) default.orEmpty() else this
}
