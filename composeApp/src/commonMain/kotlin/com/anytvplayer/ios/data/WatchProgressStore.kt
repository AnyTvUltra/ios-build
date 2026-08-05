package com.anytvplayer.ios.data

import com.russhwolf.settings.Settings

/**
 * Persists last watched position and total duration per stream URL.
 */
class WatchProgressStore {

    private val settings = Settings()
    private val prefix = "anytv_watch_progress"

    data class Progress(val position: Long, val duration: Long)
    data class ProgressWithUrl(val streamUrl: String, val position: Long, val duration: Long)

    fun getProgress(streamUrl: String): Progress? {
        val key = keyFor(streamUrl)
        val position = settings.getLongOrNull("${key}_position") ?: -1L
        val duration = settings.getLongOrNull("${key}_duration") ?: -1L
        if (position < 0) return null
        return Progress(position, duration.coerceAtLeast(0L))
    }

    fun getProgressRatio(streamUrl: String): Float {
        val progress = getProgress(streamUrl) ?: return 0f
        if (progress.duration <= 0) return 0f
        return (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f)
    }

    fun saveProgress(streamUrl: String, position: Long, duration: Long) {
        if (streamUrl.isBlank() || position < 0) return
        val key = keyFor(streamUrl)
        settings.putLong("${key}_position", position)
        settings.putLong("${key}_duration", duration.coerceAtLeast(0L))
        settings.putString("${key}_url", streamUrl)
    }

    fun clearProgress(streamUrl: String) {
        val key = keyFor(streamUrl)
        settings.remove("${key}_position")
        settings.remove("${key}_duration")
        settings.remove("${key}_url")
    }

    fun getAllProgress(): List<ProgressWithUrl> {
        return settings.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull { key ->
                if (!key.endsWith("_url")) return@mapNotNull null
                val streamUrl = settings.getStringOrNull(key) ?: return@mapNotNull null
                val base = key.removeSuffix("_url")
                val position = settings.getLongOrNull("${base}_position") ?: -1L
                val duration = settings.getLongOrNull("${base}_duration") ?: -1L
                if (position < 0) return@mapNotNull null
                ProgressWithUrl(streamUrl, position, duration.coerceAtLeast(0L))
            }
    }

    private fun keyFor(streamUrl: String): String {
        // Use a stable, URL-safe key derived from the URL.
        val suffix = streamUrl.takeLast(240).replace("/", "_").replace(":", "_")
        return "$prefix:$suffix"
    }
}
