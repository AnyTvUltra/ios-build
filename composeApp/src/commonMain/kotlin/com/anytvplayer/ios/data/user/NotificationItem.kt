package com.anytvplayer.ios.data.user

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toString()
}

@Serializable
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val screen: String = "",
    val itemId: String = "",
    val imageUrl: String = "",
    val receivedAt: Long = 0L,
    val read: Boolean = false,
    val timestamp: String = formatTimestamp(receivedAt)
) {
    fun toJson(): String = jsonSerializer.encodeToString(this)

    companion object {
        private val jsonSerializer = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): NotificationItem? {
            if (json.isBlank()) return null
            return runCatching { jsonSerializer.decodeFromString<NotificationItem>(json) }.getOrNull()
        }

        fun listToJson(items: List<NotificationItem>): String =
            jsonSerializer.encodeToString(items)

        fun listFromJson(json: String): List<NotificationItem> {
            if (json.isBlank()) return emptyList()
            return runCatching { jsonSerializer.decodeFromString<List<NotificationItem>>(json) }.getOrDefault(emptyList())
        }
    }
}
