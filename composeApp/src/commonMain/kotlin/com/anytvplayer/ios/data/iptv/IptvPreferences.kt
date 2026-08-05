package com.anytvplayer.ios.data.iptv

import com.anytvplayer.ios.data.SecurePreferences

class IptvPreferences {

    private val legacySettings = com.russhwolf.settings.Settings(name = "Twiti_iptv")
    private val prefs = SecurePreferences(
        preferencesName = "anytv_iptv_secure",
        keyAlias = "anytv_iptv_credentials_key"
    )

    fun saveServer(server: IptvServer) {
        prefs.putString("server_name", server.name)
        prefs.putString("server_url", server.serverUrl)
        prefs.putString("username", server.username)
        prefs.putString("password", server.password)
        prefs.putString("api_key", server.apiKey)
        prefs.putString("server_type", server.type.name)
        legacySettings.clear()
    }

    fun loadServer(): IptvServer? {
        migrateLegacyIfNeeded()
        val url = prefs.getString("server_url") ?: return null
        if (url.isBlank()) return null

        return IptvServer(
            name = prefs.getString("server_name") ?: "",
            serverUrl = url,
            username = prefs.getString("username") ?: "",
            password = prefs.getString("password") ?: "",
            apiKey = prefs.getString("api_key") ?: "",
            type = runCatching {
                ServerType.valueOf(prefs.getString("server_type") ?: "XTREAM_CODES")
            }.getOrDefault(ServerType.XTREAM_CODES)
        )
    }

    fun clearServer() {
        prefs.clear()
        legacySettings.clear()
    }

    fun hasServer(): Boolean = loadServer() != null

    private fun migrateLegacyIfNeeded() {
        if (prefs.getString("server_url") != null) return
        val url = legacySettings.getStringOrNull("server_url") ?: return
        saveServer(
            IptvServer(
                name = legacySettings.getStringOrNull("server_name") ?: "",
                serverUrl = url,
                username = legacySettings.getStringOrNull("username") ?: "",
                password = legacySettings.getStringOrNull("password") ?: "",
                apiKey = legacySettings.getStringOrNull("api_key") ?: "",
                type = runCatching {
                    ServerType.valueOf(
                        legacySettings.getStringOrNull("server_type") ?: ServerType.XTREAM_CODES.name
                            ?: ServerType.XTREAM_CODES.name
                    )
                }.getOrDefault(ServerType.XTREAM_CODES)
            )
        )
    }
}
