package com.anytvplayer.ios.data.user

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val provider: String = "",
    val photoUrl: String = "",
    val isLoggedIn: Boolean = false,
    val verified: Boolean = false
) {
    fun toJson(): String = jsonSerializer.encodeToString(this)

    companion object {
        private val jsonSerializer = Json { ignoreUnknownKeys = true }
        fun fromJson(json: String): UserAccount? {
            if (json.isBlank()) return null
            return runCatching { jsonSerializer.decodeFromString<UserAccount>(json) }.getOrNull()
        }
    }
}
