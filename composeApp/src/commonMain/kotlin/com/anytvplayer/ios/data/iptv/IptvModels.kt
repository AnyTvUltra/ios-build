package com.anytvplayer.ios.data.iptv

import androidx.compose.ui.graphics.Color
import io.ktor.http.*
import io.ktor.http.encodeURLQueryComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class IptvServer(
    val name: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val apiKey: String = "",
    val type: ServerType = ServerType.XTREAM_CODES
)

fun parseXtreamM3uUrl(url: String): IptvServer? {
    val trimmed = url.trim()
    if (!trimmed.contains("get.php", ignoreCase = true) &&
        !trimmed.contains("player_api.php", ignoreCase = true)
    ) {
        return null
    }

    val parsed = runCatching { Url(trimmed) }.getOrNull() ?: return null
    val username = parsed.parameters["username"]?.trim().orEmpty()
    val password = parsed.parameters["password"]?.trim().orEmpty()
    if (username.isBlank() || password.isBlank()) return null

    val scheme = parsed.protocol.name
    val host = parsed.host
    val port = parsed.port
    val defaultPort = if (scheme == "https") 443 else 80
    val portPart = if (port != defaultPort && port != 0) ":$port" else ""
    val baseUrl = "$scheme://$host$portPart"

    return IptvServer(
        name = host,
        serverUrl = baseUrl,
        username = username,
        password = password,
        type = ServerType.XTREAM_CODES
    )
}

fun IptvServer.toSmartServer(): IptvServer {
    if (type == ServerType.XTREAM_CODES || type == ServerType.AUTO_DETECT) {
        parseXtreamM3uUrl(serverUrl)?.let { smart ->
            return smart.copy(name = name.ifBlank { smart.name }, type = type)
        }
    }
    return this
}

fun buildXtreamM3uFallback(server: IptvServer): String? {
    val url = server.serverUrl.trim()
    if (url.contains("get.php", ignoreCase = true) &&
        url.contains("type=m3u", ignoreCase = true)
    ) {
        return url
    }

    val username = server.username.takeIf { it.isNotBlank() } ?: return null
    val password = server.password.takeIf { it.isNotBlank() } ?: return null
    val base = url.trimEnd('/')
    if (!base.startsWith("http://", ignoreCase = true) &&
        !base.startsWith("https://", ignoreCase = true)
    ) {
        return null
    }

    return "$base/get.php?username=${encodeURLQueryComponent(username)}&password=${encodeURLQueryComponent(password)}&type=m3u&output=ts"
}

@Serializable
enum class ServerType { XTREAM_CODES, M3U_URL, AUTO_DETECT, LOCAL_M3U_FILE, STALKER_PORTAL, ADMIN_PANEL }

@Serializable
data class XtreamUserInfo(
    val username: String = "",
    val password: String = "",
    val status: String = "",
    val expDate: String = "",
    val isTrial: Boolean = false,
    val activeCons: Int = 0,
    val maxConnections: Int = 0,
    val createdAt: String = ""
)

@Serializable
data class XtreamServerInfo(
    val url: String = "",
    val port: String = "",
    val httpsPort: String = "",
    val serverProtocol: String = "http",
    val timeNow: String = ""
)

@Serializable
data class XtreamLoginResponse(
    val userInfo: XtreamUserInfo = XtreamUserInfo(),
    val serverInfo: XtreamServerInfo = XtreamServerInfo()
)

@Serializable
data class IptvCategory(
    val id: String,
    val name: String,
    val parentId: String = "0",
    val type: ChannelType = ChannelType.LIVE
)

@Serializable
data class IptvChannel(
    val id: String,
    val name: String,
    val streamId: Int = 0,
    val streamIcon: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val streamUrl: String = "",
    val type: ChannelType = ChannelType.LIVE,
    val rating: String = "",
    val year: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val duration: String = "",
    val containerExtension: String = "",
    val seriesId: Int = 0,
    val coverUrl: String = "",
    @Transient
    val gradientColors: List<Color> = emptyList()
) {
    fun toJson(): String = jsonSerializer.encodeToString(this)

    companion object {
        private val jsonSerializer = Json { ignoreUnknownKeys = true; isLenient = true }
        fun fromJson(value: String): IptvChannel = jsonSerializer.decodeFromString(value)
    }
}

@Serializable
enum class ChannelType { LIVE, VOD, SERIES }

@Serializable
sealed class ConnectionState {
    @Serializable
    data object Disconnected : ConnectionState()
    @Serializable
    data object Connecting : ConnectionState()
    @Serializable
    data class Connected(val loginResponse: XtreamLoginResponse) : ConnectionState()
    @Serializable
    data class Error(val message: String) : ConnectionState()
}

sealed class LoadingState<out T> {
    data object Idle : LoadingState<Nothing>()
    data object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val message: String) : LoadingState<Nothing>()
}
