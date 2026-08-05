package com.anytvplayer.ios.data

import com.anytvplayer.ios.data.downloads.DownloadedItem
import com.anytvplayer.ios.data.user.UserAccount
import com.russhwolf.settings.Settings

class AppSettings {

    private val settings = Settings()

    var languageTag: String
        get() = settings.getStringOrNull(KEY_LANGUAGE) ?: "ar"
        set(value) { settings.putString(KEY_LANGUAGE, value) }

    var notificationsEnabled: Boolean
        get() = settings.getBooleanOrNull(KEY_NOTIFICATIONS) ?: true
        set(value) { settings.putBoolean(KEY_NOTIFICATIONS, value) }

    var autoplayEnabled: Boolean
        get() = settings.getBooleanOrNull(KEY_AUTOPLAY) ?: true
        set(value) { settings.putBoolean(KEY_AUTOPLAY, value) }

    var profileName: String
        get() = settings.getStringOrNull(KEY_PROFILE_NAME) ?: ""
        set(value) { settings.putString(KEY_PROFILE_NAME, value.trim().take(80)) }

    var profileAvatarUri: String
        get() = settings.getStringOrNull(KEY_PROFILE_AVATAR) ?: ""
        set(value) { settings.putString(KEY_PROFILE_AVATAR, value) }

    var profileId: String
        get() {
            val saved = settings.getStringOrNull(KEY_PROFILE_ID)
            if (!saved.isNullOrBlank()) return saved
            return regenerateProfileId()
        }
        set(value) { settings.putString(KEY_PROFILE_ID, value) }

    fun regenerateProfileId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val generated = "ATV-" + (1..8).map { chars.random() }.joinToString("")
        settings.putString(KEY_PROFILE_ID, generated)
        return generated
    }

    fun clearProfile() {
        settings.remove(KEY_PROFILE_NAME)
        settings.remove(KEY_PROFILE_AVATAR)
        settings.remove(KEY_PROFILE_ID)
    }

    var userAccount: UserAccount?
        get() = UserAccount.fromJson(settings.getStringOrNull(KEY_USER_ACCOUNT) ?: "")
        set(value) { settings.putString(KEY_USER_ACCOUNT, value?.toJson() ?: "") }

    var downloads: List<DownloadedItem>
        get() = DownloadedItem.listFromJson(settings.getStringOrNull(KEY_DOWNLOADS) ?: "")
        set(value) { settings.putString(KEY_DOWNLOADS, DownloadedItem.listToJson(value)) }

    var isDarkTheme: Boolean
        get() = settings.getBooleanOrNull(KEY_DARK_THEME) ?: true
        set(value) { settings.putBoolean(KEY_DARK_THEME, value) }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_AUTOPLAY = "autoplay"
        const val KEY_PROFILE_NAME = "profile_name"
        const val KEY_PROFILE_AVATAR = "profile_avatar"
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_USER_ACCOUNT = "user_account"
        const val KEY_DOWNLOADS = "downloads"
        const val KEY_DARK_THEME = "dark_theme"
    }
}
