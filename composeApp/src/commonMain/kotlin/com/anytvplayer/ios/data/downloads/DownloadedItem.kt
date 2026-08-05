package com.anytvplayer.ios.data.downloads

import com.anytvplayer.ios.data.iptv.ChannelType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DownloadedItem(
    val id: String,
    val channelId: String,
    val name: String,
    val coverUrl: String,
    val streamUrl: String,
    val downloadId: Long,
    val type: ChannelType,
    val status: Int = STATUS_PENDING,
    val localUri: String = "",
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0
) {
    val isComplete: Boolean
        get() = status == STATUS_SUCCESSFUL

    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    fun toJson(): String = jsonSerializer.encodeToString(this)

    companion object {
        const val STATUS_PENDING = 1
        const val STATUS_RUNNING = 2
        const val STATUS_PAUSED = 4
        const val STATUS_SUCCESSFUL = 8
        const val STATUS_FAILED = 16

        private val jsonSerializer = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): DownloadedItem? {
            if (json.isBlank()) return null
            return runCatching { jsonSerializer.decodeFromString<DownloadedItem>(json) }.getOrNull()
        }

        fun listToJson(items: List<DownloadedItem>): String =
            jsonSerializer.encodeToString(items)

        fun listFromJson(json: String): List<DownloadedItem> {
            if (json.isBlank()) return emptyList()
            return runCatching { jsonSerializer.decodeFromString<List<DownloadedItem>>(json) }.getOrDefault(emptyList())
        }
    }
}
