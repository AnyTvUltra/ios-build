package com.anytvplayer.ios.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.anytvplayer.ios.data.AppSettings
import com.anytvplayer.ios.data.WatchProgressStore
import com.anytvplayer.ios.data.iptv.*
import com.anytvplayer.ios.data.user.NotificationItem
import com.anytvplayer.ios.data.user.UserAccount
import kotlinx.coroutines.*

class IptvViewModel(
    private val coroutineScope: CoroutineScope
) {

    private val appSettings = AppSettings()
    private val preferences = IptvPreferences()
    private val repository = IptvRepository()
    private val watchProgressStore = WatchProgressStore()

    var connectionState: ConnectionState by mutableStateOf(ConnectionState.Disconnected)
        private set

    var isLoading by mutableStateOf(false)
    var contentError: String? by mutableStateOf(null)

    var allCategories by mutableStateOf<List<IptvCategory>>(emptyList())
    var allLiveChannels by mutableStateOf<List<IptvChannel>>(emptyList())
    var allVodChannels by mutableStateOf<List<IptvChannel>>(emptyList())
    var allSeriesChannels by mutableStateOf<List<IptvChannel>>(emptyList())

    var searchResults by mutableStateOf<List<IptvChannel>>(emptyList())
    var searchQuery by mutableStateOf("")

    var savedServer by mutableStateOf(preferences.loadServer())
    var currentPlaylist by mutableStateOf<IptvServer?>(null)

    var isDarkTheme by mutableStateOf(appSettings.isDarkTheme)
    var notificationsEnabled by mutableStateOf(appSettings.notificationsEnabled)
    var autoplayEnabled by mutableStateOf(appSettings.autoplayEnabled)
    var languageTag by mutableStateOf(appSettings.languageTag)
    var userAccount by mutableStateOf(appSettings.userAccount ?: UserAccount())
    var profileName by mutableStateOf(appSettings.profileName)
    var profileAvatarUri by mutableStateOf(appSettings.profileAvatarUri)
    var profileId by mutableStateOf(appSettings.profileId)

    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
    var unreadCount by mutableStateOf(0)

    var libraryItems by mutableStateOf<List<IptvChannel>>(emptyList())
    var watchProgressItems by mutableStateOf<List<IptvChannel>>(emptyList())

    var shortClips by mutableStateOf<List<IptvChannel>>(emptyList())
    var shortSocialStates by mutableStateOf<Map<String, Any>>(emptyMap())

    var homeScroll by mutableStateOf(0)
    var hubSelectedTab by mutableStateOf("Channels")
    var hubSelectedCategory by mutableStateOf<String?>(null)
    var hubScroll by mutableStateOf(0)
    var hubSearchQuery by mutableStateOf("")

    val isConnected: Boolean
        get() = connectionState is ConnectionState.Connected || repository.isConnected()

    fun tryAutoConnect() {
        val server = savedServer ?: return
        if (isConnected) return
        connectToServer(server)
    }

    fun connectToServer(server: IptvServer) {
        contentError = null
        isLoading = true
        connectionState = ConnectionState.Connecting
        coroutineScope.launch {
            try {
                val smart = server.toSmartServer()
                val response = when (smart.type) {
                    ServerType.XTREAM_CODES, ServerType.AUTO_DETECT -> repository.connectXtream(smart)
                    ServerType.M3U_URL -> {
                        repository.connectM3u(smart.serverUrl)
                        XtreamLoginResponse()
                    }
                    else -> throw IllegalStateException("Unsupported server type")
                }
                preferences.saveServer(smart)
                currentPlaylist = smart
                savedServer = smart
                connectionState = ConnectionState.Connected(response)
                loadAllContent()
            } catch (e: Exception) {
                connectionState = ConnectionState.Error(e.message ?: "Connection failed")
                contentError = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun disconnect() {
        repository.disconnect()
        connectionState = ConnectionState.Disconnected
        allCategories = emptyList()
        allLiveChannels = emptyList()
        allVodChannels = emptyList()
        allSeriesChannels = emptyList()
        searchResults = emptyList()
    }

    fun loadAllContent() {
        if (!isConnected) return
        isLoading = true
        contentError = null
        coroutineScope.launch {
            try {
                val live = async { repository.getChannels(ChannelType.LIVE) }
                val vod = async { repository.getChannels(ChannelType.VOD) }
                val series = async { repository.getChannels(ChannelType.SERIES) }
                allLiveChannels = live.await()
                allVodChannels = vod.await()
                allSeriesChannels = series.await()

                val liveCat = async { repository.getCategories(ChannelType.LIVE) }
                val vodCat = async { repository.getCategories(ChannelType.VOD) }
                val seriesCat = async { repository.getCategories(ChannelType.SERIES) }
                allCategories = liveCat.await() + vodCat.await() + seriesCat.await()
            } catch (e: Exception) {
                contentError = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun loadChannelsByCategory(type: ChannelType, categoryId: String? = null) {
        if (!isConnected) return
        isLoading = true
        coroutineScope.launch {
            try {
                val channels = repository.getChannels(type, categoryId)
                when (type) {
                    ChannelType.LIVE -> allLiveChannels = channels
                    ChannelType.VOD -> allVodChannels = channels
                    ChannelType.SERIES -> allSeriesChannels = channels
                }
            } catch (e: Exception) {
                contentError = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun search(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        coroutineScope.launch {
            isLoading = true
            try {
                searchResults = repository.searchChannels(query)
            } catch (e: Exception) {
                contentError = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun getStreamUrl(channel: IptvChannel): String {
        return repository.getStreamUrl(channel)
    }

    fun findChannelByUrl(url: String): IptvChannel? {
        return (allLiveChannels + allVodChannels + allSeriesChannels).find {
            it.streamUrl == url || it.streamId.toString() in url
        }
    }

    fun loadSeriesEpisodes(seriesId: Int, onResult: (List<IptvChannel>) -> Unit = {}) {
        coroutineScope.launch {
            try {
                val episodes = repository.getSeriesEpisodes(seriesId)
                onResult(episodes)
            } catch (e: Exception) {
                contentError = e.message
            }
        }
    }

    fun saveWatchProgress(channel: IptvChannel, positionMs: Long, durationMs: Long) {
        watchProgressStore.saveProgress(channel.streamUrl, positionMs, durationMs)
    }

    fun removeWatchProgress(streamUrl: String) {
        watchProgressStore.clearProgress(streamUrl)
    }

    fun getLocalWatchProgress(streamUrl: String): WatchProgressStore.Progress? {
        return watchProgressStore.getProgress(streamUrl)
    }

    fun addToLibrary(channel: IptvChannel) {
        if (libraryItems.none { it.id == channel.id }) {
            libraryItems = libraryItems + channel
        }
    }

    fun removeFromLibrary(channel: IptvChannel) {
        libraryItems = libraryItems.filter { it.id != channel.id }
    }

    fun isInLibrary(channel: IptvChannel): Boolean {
        return libraryItems.any { it.id == channel.id }
    }

    fun toggleDarkTheme() {
        isDarkTheme = !isDarkTheme
        appSettings.isDarkTheme = isDarkTheme
    }

    fun setLanguage(tag: String) {
        languageTag = tag
        appSettings.languageTag = tag
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        appSettings.notificationsEnabled = enabled
    }

    fun updateAutoplayEnabled(enabled: Boolean) {
        autoplayEnabled = enabled
        appSettings.autoplayEnabled = enabled
    }

    fun signIn(account: UserAccount) {
        userAccount = account
        appSettings.userAccount = account
    }

    fun signOut() {
        userAccount = UserAccount()
        appSettings.userAccount = null
    }

    fun markNotificationsRead() {
        unreadCount = 0
        notifications = notifications.map { it.copy(read = true) }
    }

    fun clearNotifications() {
        notifications = emptyList()
        unreadCount = 0
    }

    fun deleteNotification(id: String) {
        notifications = notifications.filter { it.id != id }
    }

    fun updateProfile(name: String, avatarUri: String = "") {
        profileName = name
        profileAvatarUri = avatarUri
        appSettings.profileName = name
        appSettings.profileAvatarUri = avatarUri
    }

    // ── Stubs for advanced features to be fully ported ──

    fun tryAutoConnectLegacy() {}
    fun connectAdminPanel(baseUrl: String, key: String) {}
    fun refreshPlaylists() {}
    fun addPlaylist(name: String, url: String) {}
    fun deletePlaylist(playlist: IptvServer) {}
    fun selectPlaylist(playlist: IptvServer) {}
    fun loadM3uFromUrl(url: String) { connectToServer(IptvServer(serverUrl = url, type = ServerType.M3U_URL)) }
    fun startHeartbeat() {}
    fun startActivationWatcher() {}
    fun startConfigWatcher() {}
    fun startPlaylistWatcher() {}
    fun refreshActivation() {}
    fun redeemGiftCode(code: String): String? = null
    fun loadShortSocial() {}
    fun recordShortView(clipId: String) {}
    fun toggleShortLike(clipId: String) {}
    fun toggleShortSave(clipId: String) {}
    fun addShortComment(clipId: String, text: String) {}
    fun editShortComment(clipId: String, commentId: String, text: String) {}
    fun deleteShortComment(clipId: String, commentId: String) {}
    fun loadSupportTickets() {}
    fun submitSupportTicket(subject: String, body: String) {}
    fun startDownload(channel: IptvChannel) {}
    fun refreshDownloads() {}
    fun deleteDownload(id: String) {}
    fun hasDownloaded(channel: IptvChannel): Boolean = false
    fun uploadProfileAvatar(bytes: ByteArray): String = ""

    fun onCleared() {
        coroutineScope.cancel()
    }

    data class ScrollPosition(val index: Int = 0, val offset: Int = 0)
}

fun IptvChannel.gradientForType(): List<Color> {
    return gradientColors.ifEmpty {
        when (type) {
            ChannelType.LIVE -> listOf(Color(0xFFFF6D00), Color(0xFFFF9100))
            ChannelType.VOD -> listOf(Color(0xFF1A237E), Color(0xFF0D47A1))
            ChannelType.SERIES -> listOf(Color(0xFF880E4F), Color(0xFFAD1457))
        }
    }
}
