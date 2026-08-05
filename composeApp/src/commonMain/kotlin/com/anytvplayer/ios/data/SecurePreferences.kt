package com.anytvplayer.ios.data

import com.russhwolf.settings.Settings

/**
 * Platform-agnostic secure(ish) string store.
 * On iOS this currently uses multiplatform-settings (NSUserDefaults) for compatibility.
 * A future pass will wrap the iOS Keychain for truly encrypted storage.
 */
class SecurePreferences(
    private val preferencesName: String,
    private val keyAlias: String
) {
    private val settings = Settings(name = preferencesName)

    fun putString(key: String, value: String) {
        if (value.isEmpty()) {
            remove(key)
            return
        }
        settings.putString(key, value)
    }

    fun getString(key: String): String? = settings.getStringOrNull(key)

    fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getString(key)?.toLongOrNull() ?: defaultValue
    }

    fun remove(key: String) {
        settings.remove(key)
    }

    fun clear() {
        settings.clear()
    }
}
