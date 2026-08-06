package com.anytvplayer.ios.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.anytvplayer.ios.data.AppSettings
import com.anytvplayer.ios.data.WatchProgressStore
import com.anytvplayer.ios.data.admin.*
import com.anytvplayer.ios.data.iptv.*
import com.anytvplayer.ios.data.user.NotificationItem
import com.anytvplayer.ios.data.user.UserAccount
import com.anytvplayer.ios.ui.theme.TwitiMint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.absoluteValue

private data class ContentSnapshot(
    val live: List<IptvChannel>,
    val vod: List<IptvChannel>,
    val series: List<IptvChannel>,
    val liveCategories: List<IptvCategory>,
    val vodCategories: List<IptvCategory>,
    val seriesCategories: List<IptvCategory>
)

data class ScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

class IptvViewModel(
    private val coroutineScope: CoroutineScope
) {

    private val appSettings = AppSettings()
    private val preferences = IptvPreferences()
    private val repository = IptvRepository()
    private val watchProgressStore = WatchProgressStore()
    private val deviceIdentity = DeviceIdentity()
    private val playlistCache = PlaylistCache()

    // Admin state
    private var adminApiClient: AdminApiClient? = null
    private var connectionJob: Job? = null
    private var searchJob: Job? = null
    private var heartbeatJob: Job? = null
    private var activationJob: Job? = null
    private var configJob: Job? = null
    private var playlistJob: Job? = null

    private var m3uSeriesEpisodes: Map<Int, List<IptvChannel>> = emptyMap()
    private val recordedShortViews = mutableSetOf<String>()

    // ── Connection ──
    var connectionState: ConnectionState by mutableStateOf(ConnectionState.Disconnected)
        private set

    var isLoading by mutableStateOf(false)
    var isLoadingChannels by mutableStateOf(false)
    var contentError: String? by mutableStateOf(null)

    // ── Content ──
    var liveCategories by mutableStateOf<List<IptvCategory>>(emptyList())
        private set
    var vodCategories by mutableStateOf<List<IptvCategory>>(emptyList())
        private set
    var seriesCategories by mutableStateOf<List<IptvCategory>>(emptyList())
        private set

    var liveChannels by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var vodChannels by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var seriesChannels by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var seriesGroups by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var seriesEpisodes by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var isLoadingEpisodes by mutableStateOf(false)
        private set

    var categoryChannels by mutableStateOf<List<IptvChannel>>(emptyList())
        private set
    var selectedCategory by mutableStateOf<IptvCategory?>(null)
        private set

    // Legacy aggregated lists used by many existing screens.
    // Live channels are exposed as liveChannels; keeping allCategories around
    // makes existing callers compile until screens are ported.
    val allCategories get() = liveCategories + vodCategories + seriesCategories
    val allLiveChannels get() = liveChannels
    val allVodChannels get() = vodChannels
    val allSeriesChannels get() = seriesChannels

    var searchResults by mutableStateOf<List<IptvChannel>>(emptyList())
    var searchQuery by mutableStateOf("")
    var isSearching by mutableStateOf(false)

    // ── Server ──
    var savedServer by mutableStateOf(preferences.loadServer())
        private set
    var currentPlaylist by mutableStateOf<PlaylistInfo?>(null)
        private set

    // ── Admin Panel State ──
    var activationState by mutableStateOf<ActivationResponse?>(null)
        private set
    var availablePlaylists by mutableStateOf<List<PlaylistInfo>>(emptyList())
        private set
    var brandingConfig by mutableStateOf(BrandingConfig())
        private set
    var banners by mutableStateOf<List<Banner>>(emptyList())
        private set
    var subscriptionPackages by mutableStateOf<List<SubscriptionPackage>>(emptyList())
        private set
    var shortClips by mutableStateOf<List<ShortClip>>(emptyList())
        private set
    var shortSocialStates by mutableStateOf<Map<String, ShortSocialState>>(emptyMap())
    var isRefreshingActivation by mutableStateOf(false)
        private set
    var giftCodeMessage by mutableStateOf<String?>(null)
        private set

    // ── User Content ──
    var libraryItems by mutableStateOf<List<LibraryItem>>(emptyList())
        private set
    var watchProgressItems by mutableStateOf<List<WatchProgressItem>>(emptyList())
        private set
    var subscriptions by mutableStateOf<List<SubscriptionItem>>(emptyList())
        private set
    var isUserContentLoading by mutableStateOf(false)
        private set

    // ── Profile / Settings ──
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

    var supportTickets by mutableStateOf<List<SupportTicket>>(emptyList())
        private set
    var canCreateSupportTicket by mutableStateOf(true)
        private set
    var supportMessage by mutableStateOf<String?>(null)
        private set
    var isSupportBusy by mutableStateOf(false)
        private set

    var playlistActionMessage by mutableStateOf<String?>(null)
        private set
    var isPlaylistActionBusy by mutableStateOf(false)
        private set

    // ── Navigation state ──
    var homeScroll by mutableStateOf(ScrollPosition())
    var hubSelectedTab by mutableStateOf("Channels")
    var hubSelectedCategory by mutableStateOf<String?>(null)
    var hubScroll by mutableStateOf(ScrollPosition())
    var hubSearchQuery by mutableStateOf("")
    private val _channelsScroll = mutableMapOf<String, ScrollPosition>()
    private val _seriesSelectedSeason = mutableMapOf<Int, String>()
    private val _seriesScroll = mutableMapOf<Int, ScrollPosition>()

    // ── Device identity ──
    var deviceMac by mutableStateOf("")
        private set
    var deviceKey by mutableStateOf("")
        private set
    var registerResponse by mutableStateOf<RegisterResponse?>(null)
        private set

    val displayDeviceId: String
        get() {
            val hash = deviceMac.sumOf { it.code }
            return (152000 + (hash % 1000).absoluteValue).toString()
        }

    val isConnected: Boolean
        get() {
            val connected = connectionState is ConnectionState.Connected || repository.isConnected()
            return connected && (!isAdminPanel || activationState?.isActivated == true)
        }

    val isAdminPanel: Boolean
        get() = savedServer?.type == ServerType.ADMIN_PANEL

    val brandColor: Color
        get() = runCatching {
            Color(parseColorString(brandingConfig.primaryColor).toULong())
        }.getOrDefault(TwitiMint)

    init {
        deviceMac = deviceIdentity.getMacAddress()
        deviceKey = deviceIdentity.getDeviceKey()
        tryAutoConnect()
    }

    // ── Connection ──

    fun connectToServer(server: IptvServer) {
        connectionJob?.cancel()
        connectionState = ConnectionState.Connecting
        contentError = null
        isLoading = true

        val smartServer = server.toSmartServer()
        connectionJob = coroutineScope.launch {
            try {
                when (smartServer.type) {
                    ServerType.XTREAM_CODES -> {
                        val response = repository.connectXtream(smartServer)
                        connectionState = ConnectionState.Connected(response)
                        preferences.saveServer(smartServer)
                        savedServer = smartServer
                        loadAllContent()
                    }
                    ServerType.M3U_URL -> {
                        val count = repository.connectM3u(smartServer.serverUrl)
                        connectionState = ConnectionState.Connected(
                            XtreamLoginResponse(
                                userInfo = XtreamUserInfo(
                                    username = "M3U",
                                    status = "Active ($count channels)"
                                )
                            )
                        )
                        preferences.saveServer(smartServer)
                        savedServer = smartServer
                        loadAllContent()
                    }
                    ServerType.AUTO_DETECT -> {
                        val fallbackUrl = buildXtreamM3uFallback(smartServer)
                        try {
                            val response = repository.connectXtream(smartServer)
                            connectionState = ConnectionState.Connected(response)
                            preferences.saveServer(smartServer.copy(type = ServerType.XTREAM_CODES))
                            savedServer = smartServer.copy(type = ServerType.XTREAM_CODES)
                            loadAllContent()
                        } catch (xtreamError: Exception) {
                            if (fallbackUrl != null) {
                                try {
                                    val count = repository.connectM3u(fallbackUrl)
                                    val m3uServer = smartServer.copy(
                                        serverUrl = fallbackUrl,
                                        type = ServerType.M3U_URL,
                                        name = server.name.ifBlank { smartServer.name }
                                    )
                                    connectionState = ConnectionState.Connected(
                                        XtreamLoginResponse(
                                            userInfo = XtreamUserInfo(
                                                username = "M3U",
                                                status = "Active via M3U ($count channels)"
                                            )
                                        )
                                    )
                                    preferences.saveServer(m3uServer)
                                    savedServer = m3uServer
                                    loadAllContent()
                                } catch (m3uError: Exception) {
                                    connectionState = ConnectionState.Error(
                                        buildDetail(xtreamError, m3uError)
                                    )
                                }
                            } else {
                                connectionState = ConnectionState.Error(
                                    xtreamError.message ?: "Connection failed"
                                )
                            }
                        }
                    }
                    ServerType.LOCAL_M3U_FILE -> {
                        throw Exception("Local M3U files are not supported on this platform")
                    }
                    ServerType.STALKER_PORTAL -> {
                        throw Exception("Stalker portal is not supported")
                    }
                    ServerType.ADMIN_PANEL -> {
                        connectAdminPanel(server)
                    }
                }
            } catch (e: Exception) {
                val message = e.message ?: "Connection failed"
                val xtreamHint = if (
                    server.type == ServerType.M3U_URL &&
                    (server.serverUrl.contains("get.php", ignoreCase = true) ||
                            server.serverUrl.contains("player_api.php", ignoreCase = true))
                ) {
                    " Try using the Xtream Codes option instead."
                } else ""
                connectionState = ConnectionState.Error(message + xtreamHint)
            } finally {
                isLoading = false
            }
        }
    }

    private fun buildDetail(xtreamError: Exception, m3uError: Exception): String {
        val a = xtreamError.message?.takeIf { it.isNotBlank() } ?: "Xtream connection failed"
        val b = m3uError.message?.takeIf { it.isNotBlank() } ?: "M3U fallback failed"
        return "$a | $b"
    }

    private suspend fun connectAdminPanel(server: IptvServer) {
        val apiClient = AdminApiClient(
            AdminPanelConfig(panelUrl = server.serverUrl, apiKey = server.apiKey)
        )
        adminApiClient = apiClient

        // 1. Register device
        val registration = deviceIdentity.toRegistration()
        val regResponse = apiClient.registerDevice(registration)
        registerResponse = regResponse

        if (!regResponse.success) {
            throw Exception("Device registration failed")
        }

        val registeredDeviceKey = regResponse.deviceKey.ifBlank { deviceIdentity.getDeviceKey() }
        if (registeredDeviceKey.isBlank()) throw Exception("No device key received")
        deviceIdentity.saveDeviceKey(registeredDeviceKey)
        deviceKey = registeredDeviceKey

        // 2. Check activation
        val activation = apiClient.checkActivation(deviceMac, registeredDeviceKey)
        activationState = activation
        startActivationWatcher(apiClient, registeredDeviceKey, activation)

        // 3. Save server
        preferences.saveServer(server)
        savedServer = server

        // 4. Fetch config & branding
        try {
            val config = apiClient.getConfig()
            if (config.success) {
                brandingConfig = config.branding
                banners = config.banners
                    .filter { it.isActive && (it.imageUrl.isNotBlank() || it.streamUrl.isNotBlank()) }
                    .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                subscriptionPackages = config.packages
                shortClips = config.playerConfig
                    .filter { it.enabled && it.type.equals("shorts", ignoreCase = true) }
                    .flatMap { it.items }
                    .filter { it.isActive && it.videoUrl.isNotBlank() }
            }
        } catch (_: Exception) { /* Non-critical */ }

        connectionState = ConnectionState.Connected(
            XtreamLoginResponse(
                userInfo = XtreamUserInfo(
                    username = server.name.ifBlank { brandingConfig.appName },
                    status = if (activation.isActivated) "Active" else "Not Activated",
                    expDate = activation.activation?.expiresAt ?: ""
                )
            )
        )

        startHeartbeat(apiClient, registeredDeviceKey)

        if (!activation.isActivated) {
            clearContent()
            return
        }

        // 5. Fetch playlists
        val playlistsResponse = apiClient.getAllPlaylists(deviceMac, registeredDeviceKey)
        availablePlaylists = playlistsResponse.playlists

        // 6. Load playlist content
        if (playlistsResponse.playlists.isNotEmpty()) {
            loadPlaylistContent(playlistsResponse.playlists.first())
        } else if (playlistsResponse.defaultPlaylistUrl.isValidNetworkUrl()) {
            loadM3uFromUrl(playlistsResponse.defaultPlaylistUrl)
        } else {
            currentPlaylist = null
            playlistCache.clear()
            clearContent()
        }

        // 7. Start playlist watcher
        startPlaylistWatcher(apiClient, registeredDeviceKey)

        // 8. Start config watcher
        startConfigWatcher(apiClient)
    }

    // ── Playlists ──

    fun selectPlaylist(playlist: PlaylistInfo) {
        coroutineScope.launch {
            loadPlaylistContent(playlist)
        }
    }

    fun addPlaylist(name: String, url: String) {
        val api = adminApiClient ?: run {
            playlistActionMessage = "Admin panel unavailable"
            return
        }
        if (activationState?.isActivated != true) {
            playlistActionMessage = "Please activate your device first"
            return
        }
        val cleanedName = name.trim()
        val cleanedUrl = url.trim()
        if (cleanedName.isBlank() || !cleanedUrl.isValidNetworkUrl()) {
            playlistActionMessage = "Invalid name or URL"
            return
        }
        coroutineScope.launch {
            isPlaylistActionBusy = true
            playlistActionMessage = null
            try {
                val response = api.addPlaylist(deviceMac, deviceKey, cleanedName, cleanedUrl)
                if (!response.success) throw Exception(response.error.ifBlank { "Failed to add playlist" })
                refreshPlaylists(selectPlaylistId = response.playlist?.id)
                playlistActionMessage = "Playlist added"
            } catch (error: Exception) {
                playlistActionMessage = error.message ?: "Failed to add playlist"
            } finally {
                isPlaylistActionBusy = false
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistInfo) {
        val api = adminApiClient ?: run {
            playlistActionMessage = "Admin panel unavailable"
            return
        }
        coroutineScope.launch {
            isPlaylistActionBusy = true
            playlistActionMessage = null
            try {
                val response = api.deletePlaylist(deviceMac, deviceKey, playlist.id)
                if (!response.success) throw Exception(response.error.ifBlank { "Failed to delete playlist" })
                val removedCurrent = currentPlaylist?.id == playlist.id
                refreshPlaylists(selectPlaylistId = if (removedCurrent) null else currentPlaylist?.id)
                playlistActionMessage = "Playlist deleted"
            } catch (error: Exception) {
                playlistActionMessage = error.message ?: "Failed to delete playlist"
            } finally {
                isPlaylistActionBusy = false
            }
        }
    }

    private suspend fun refreshPlaylists(selectPlaylistId: String? = null) {
        val api = adminApiClient ?: return
        val response = api.getAllPlaylists(deviceMac, deviceKey)
        availablePlaylists = response.playlists
        val selected = response.playlists.firstOrNull { it.id == selectPlaylistId }
            ?: response.playlists.firstOrNull()
        if (selected != null) {
            loadPlaylistContent(selected)
        } else {
            currentPlaylist = null
            playlistCache.clear()
            clearContent()
        }
    }

    private suspend fun loadPlaylistContent(playlist: PlaylistInfo) {
        isLoadingChannels = true
        currentPlaylist = playlist

        try {
            val cache = withContext(Dispatchers.Default) {
                Triple(
                    playlistCache.loadChannels(),
                    playlistCache.getCachedUrl(),
                    playlistCache.isFresh()
                )
            }
            val cachedChannels = cache.first
            val cachedUrl = cache.second

            if (cachedChannels != null && cachedUrl == playlist.url && cache.third) {
                applyChannels(cachedChannels)
                isLoadingChannels = false
                return
            }

            loadM3uFromUrl(playlist.url)
        } catch (e: Exception) {
            val cached = withContext(Dispatchers.Default) { playlistCache.loadChannels() }
            if (cached != null) applyChannels(cached)
        } finally {
            isLoadingChannels = false
        }
    }

    private suspend fun loadM3uFromUrl(url: String) {
        try {
            require(url.isValidNetworkUrl()) { "Invalid playlist URL" }

            val smart = parseXtreamM3uUrl(url)
            if (smart != null) {
                try {
                    repository.connectXtream(smart)
                    loadAllContent().join()
                    return
                } catch (_: Exception) {
                    // Fall through to direct M3U loading
                }
            }

            isLoadingChannels = true
            val m3uContent = withContext(Dispatchers.Default) {
                repository.downloadM3u(url)
            }

            val channels = withContext(Dispatchers.Default) {
                M3uParser.parse(m3uContent)
            }

            withContext(Dispatchers.Default) {
                playlistCache.saveChannels(channels, url)
            }

            applyChannels(channels)
        } catch (e: Exception) {
            contentError = e.message ?: "Failed to load playlist"
            throw e
        } finally {
            isLoadingChannels = false
        }
    }

    private suspend fun applyChannels(channels: List<IptvChannel>) {
        val snapshot = withContext(Dispatchers.Default) {
            val uniqueChannels = channels.distinctBy { channel ->
                val normalizedUrl = channel.streamUrl.trim()
                if (normalizedUrl.isNotBlank()) {
                    "${channel.type}:url:$normalizedUrl"
                } else {
                    "${channel.type}:meta:${channel.id}:${channel.name.trim().lowercase()}"
                }
            }
            val live = uniqueChannels.filter { it.type == ChannelType.LIVE }
            val vod = uniqueChannels.filter { it.type == ChannelType.VOD }
            val series = uniqueChannels.filter { it.type == ChannelType.SERIES }
            ContentSnapshot(
                live = live,
                vod = vod,
                series = series,
                liveCategories = M3uParser.extractCategories(live),
                vodCategories = M3uParser.extractCategories(vod),
                seriesCategories = M3uParser.extractCategories(series)
            )
        }
        val (groups, m3uMap) = buildSeriesGroupsAndMap(snapshot.series)
        liveCategories = snapshot.liveCategories
        vodCategories = snapshot.vodCategories
        seriesCategories = snapshot.seriesCategories
        liveChannels = snapshot.live
        vodChannels = snapshot.vod
        seriesChannels = snapshot.series
        seriesGroups = groups
        m3uSeriesEpisodes = m3uMap

        // Update legacy fields too
        allLiveChannels
        allVodChannels
        allSeriesChannels
    }

    private fun buildSeriesGroupsAndMap(
        series: List<IptvChannel>
    ): Pair<List<IptvChannel>, Map<Int, List<IptvChannel>>> {
        if (series.isEmpty()) return emptyList<IptvChannel>() to emptyMap()

        val first = series.first()
        val isXtream = first.type == ChannelType.SERIES &&
                first.streamUrl.isBlank() &&
                first.seriesId > 0

        return if (isXtream) {
            series to emptyMap()
        } else {
            val grouped = series.groupBy { it.categoryId }
            val groups = grouped.map { (categoryId, episodes) ->
                val lead = episodes.maxByOrNull {
                    it.coverUrl.isNotBlank() || it.streamIcon.isNotBlank()
                } ?: episodes.first()
                IptvChannel(
                    id = "SERIES_GROUP_$categoryId",
                    name = lead.categoryName.ifBlank { "Series" },
                    streamId = 0,
                    streamIcon = lead.streamIcon,
                    categoryId = categoryId,
                    categoryName = lead.categoryName,
                    streamUrl = "",
                    type = ChannelType.SERIES,
                    rating = lead.rating,
                    year = lead.year,
                    plot = lead.plot,
                    cast = lead.cast,
                    director = lead.director,
                    genre = lead.genre,
                    duration = lead.duration,
                    coverUrl = lead.coverUrl.ifBlank { lead.streamIcon },
                    seriesId = categoryId.toIntOrNull() ?: categoryId.hashCode()
                )
            }
            val map = grouped.mapKeys { (categoryId, _) ->
                categoryId.toIntOrNull() ?: categoryId.hashCode()
            }.mapValues { (_, episodes) ->
                episodes.sortedBy { it.name }
            }
            groups to map
        }
    }

    // ── Watchers / Services ──

    private fun startHeartbeat(apiClient: AdminApiClient, registeredDeviceKey: String) {
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(60_000L)
                    apiClient.sendHeartbeat(
                        HeartbeatRequest(deviceMac, registeredDeviceKey, isOnline = true)
                    )
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Ignore and retry next interval
                }
            }
        }
    }

    private fun startActivationWatcher(
        apiClient: AdminApiClient,
        registeredDeviceKey: String,
        initialResponse: ActivationResponse
    ) {
        activationJob?.cancel()
        activationState = initialResponse
        activationJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(30_000L)
                    val response = apiClient.checkActivation(deviceMac, registeredDeviceKey)
                    val wasActive = activationState?.isActivated == true
                    activationState = response
                    when {
                        !wasActive && response.isActivated -> refreshActivation()
                        wasActive && !response.isActivated -> {
                            playlistJob?.cancel()
                            availablePlaylists = emptyList()
                            currentPlaylist = null
                            playlistCache.clear()
                            clearContent()
                        }
                    }
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Retry next interval
                }
            }
        }
    }

    private fun startConfigWatcher(apiClient: AdminApiClient) {
        configJob?.cancel()
        configJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(15_000L)
                    val config = apiClient.getConfig()
                    if (config.success) {
                        banners = config.banners
                            .filter { it.isActive && (it.imageUrl.isNotBlank() || it.streamUrl.isNotBlank()) }
                            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                    }
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Retry next interval
                }
            }
        }
    }

    private fun startPlaylistWatcher(apiClient: AdminApiClient, registeredDeviceKey: String) {
        playlistJob?.cancel()
        playlistJob = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(60_000L)
                    val check = apiClient.checkPlaylistChange(deviceMac, registeredDeviceKey)
                    if (check.success && check.hasPlaylist) {
                        val lastVersion = playlistCache.getPlaylistVersion()
                        val newVersion = "${check.hasPlaylist}|${check.playlistId}|${check.lastUpdated}|${check.total}"
                        if (lastVersion != newVersion) {
                            playlistCache.savePlaylistVersion(
                                check.hasPlaylist,
                                check.playlistId,
                                check.lastUpdated,
                                check.total
                            )
                            val response = apiClient.getAllPlaylists(deviceMac, registeredDeviceKey)
                            availablePlaylists = response.playlists
                            if (response.playlists.isNotEmpty()) {
                                loadPlaylistContent(response.playlists.first())
                            } else {
                                currentPlaylist = null
                                playlistCache.clear()
                                clearContent()
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Retry
                }
            }
        }
    }

    // ── Auto Connect ──

    fun tryAutoConnect() {
        if (isConnected || connectionState is ConnectionState.Connecting) return
        val server = IptvServer(
            name = "AnyTV Players",
            serverUrl = AnyTvConfig.BASE_URL,
            apiKey = AnyTvConfig.DEVICE_API_KEY,
            type = ServerType.ADMIN_PANEL
        )
        connectToServer(server)
    }

    fun refreshActivation() {
        if (isRefreshingActivation) return
        isRefreshingActivation = true
        tryAutoConnect()
        coroutineScope.launch {
            connectionJob?.join()
            isRefreshingActivation = false
        }
    }

    fun redeemGiftCode(code: String) {
        val api = adminApiClient ?: return
        if (code.isBlank()) {
            giftCodeMessage = "Please enter a gift code"
            return
        }
        coroutineScope.launch {
            giftCodeMessage = null
            val response = api.redeemGift(deviceMac, code)
            if (response.success) {
                giftCodeMessage = "Gift code redeemed"
                refreshActivation()
            } else {
                giftCodeMessage = response.error.ifBlank { "Gift code redemption failed" }
            }
        }
    }

    // ── Shorts social ──

    fun loadShortSocial(clipId: String) {
        val api = adminApiClient ?: return
        coroutineScope.launch {
            runCatching {
                api.getShortSocial(clipId, deviceMac, deviceKey)
            }.getOrNull()?.takeIf { it.success }?.let { state ->
                shortSocialStates = shortSocialStates + (clipId to state)
            }
        }
    }

    fun recordShortView(clipId: String) {
        val api = adminApiClient ?: return
        if (!recordedShortViews.add(clipId)) return
        coroutineScope.launch {
            runCatching {
                api.setShortAction(clipId, deviceMac, deviceKey, action = "view", enabled = null)
            }.getOrNull()?.takeIf { it.success }?.let { state ->
                shortSocialStates = shortSocialStates + (clipId to state)
            }
        }
    }

    fun toggleShortLike(clipId: String) {
        val api = adminApiClient ?: return
        val previous = shortSocialStates[clipId] ?: ShortSocialState()
        val optimistic = previous.copy(
            liked = !previous.liked,
            likesCount = (previous.likesCount + if (previous.liked) -1 else 1).coerceAtLeast(0)
        )
        shortSocialStates = shortSocialStates + (clipId to optimistic)
        coroutineScope.launch {
            val result = runCatching {
                api.setShortAction(
                    clipId,
                    deviceMac,
                    deviceKey,
                    action = "like",
                    enabled = optimistic.liked
                )
            }.getOrNull()
            shortSocialStates = shortSocialStates + (
                    clipId to (result?.takeIf { it.success } ?: previous)
                    )
        }
    }

    fun toggleShortSave(clipId: String) {
        val api = adminApiClient ?: return
        val previous = shortSocialStates[clipId] ?: ShortSocialState()
        val optimistic = previous.copy(
            saved = !previous.saved,
            savesCount = (previous.savesCount + if (previous.saved) -1 else 1).coerceAtLeast(0)
        )
        shortSocialStates = shortSocialStates + (clipId to optimistic)
        coroutineScope.launch {
            val result = runCatching {
                api.setShortAction(
                    clipId,
                    deviceMac,
                    deviceKey,
                    action = "save",
                    enabled = optimistic.saved
                )
            }.getOrNull()
            shortSocialStates = shortSocialStates + (
                    clipId to (result?.takeIf { it.success } ?: previous)
                    )
        }
    }

    fun addShortComment(clipId: String, text: String, parentId: String = "") {
        val api = adminApiClient ?: return
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        coroutineScope.launch {
            runCatching {
                api.addShortComment(
                    clipId = clipId,
                    macAddress = deviceMac,
                    deviceKey = deviceKey,
                    comment = cleaned,
                    parentId = parentId,
                    profileName = profileName,
                    profileId = profileId,
                    avatarUrl = profileAvatarUri
                )
            }.getOrNull()?.takeIf { it.success }?.let { state ->
                shortSocialStates = shortSocialStates + (clipId to state)
            }
        }
    }

    fun editShortComment(clipId: String, commentId: String, text: String) {
        val api = adminApiClient ?: return
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        coroutineScope.launch {
            runCatching {
                api.editShortComment(clipId, deviceMac, deviceKey, commentId, cleaned)
            }.getOrNull()?.takeIf { it.success }?.let { state ->
                shortSocialStates = shortSocialStates + (clipId to state)
            }
        }
    }

    fun deleteShortComment(clipId: String, commentId: String) {
        val api = adminApiClient ?: return
        val previous = shortSocialStates[clipId] ?: return
        shortSocialStates = shortSocialStates + (
                clipId to previous.copy(
                    comments = previous.comments.filterNot { it.id == commentId || it.parentId == commentId },
                    commentsCount = (
                            previous.commentsCount - previous.comments.count {
                                it.id == commentId || it.parentId == commentId
                            }
                            ).coerceAtLeast(0)
                )
                )
        coroutineScope.launch {
            val result = runCatching {
                api.deleteShortComment(clipId, deviceMac, deviceKey, commentId)
            }.getOrNull()
            shortSocialStates = shortSocialStates + (
                    clipId to (result?.takeIf { it.success } ?: previous)
                    )
        }
    }

    // ── Content Loading (legacy / non-admin) ──

    fun loadAllContent(): Job = coroutineScope.launch {
        isLoadingChannels = true
        try {
            val categorySnapshot = coroutineScope {
                val liveCategoriesJob = async {
                    runCatching { repository.getCategories(ChannelType.LIVE) }.getOrDefault(emptyList())
                }
                val vodCategoriesJob = async {
                    runCatching { repository.getCategories(ChannelType.VOD) }.getOrDefault(emptyList())
                }
                val seriesCategoriesJob = async {
                    runCatching { repository.getCategories(ChannelType.SERIES) }.getOrDefault(emptyList())
                }
                Triple(liveCategoriesJob.await(), vodCategoriesJob.await(), seriesCategoriesJob.await())
            }
            val snapshot = coroutineScope {
                val liveJob = async {
                    runCatching { repository.getChannels(ChannelType.LIVE) }.getOrDefault(emptyList())
                }
                val vodJob = async {
                    runCatching { repository.getChannels(ChannelType.VOD) }.getOrDefault(emptyList())
                }
                val seriesJob = async {
                    runCatching { repository.getChannels(ChannelType.SERIES) }.getOrDefault(emptyList())
                }
                ContentSnapshot(
                    live = liveJob.await(),
                    vod = vodJob.await(),
                    series = seriesJob.await(),
                    liveCategories = categorySnapshot.first,
                    vodCategories = categorySnapshot.second,
                    seriesCategories = categorySnapshot.third
                )
            }
            liveCategories = snapshot.liveCategories
            vodCategories = snapshot.vodCategories
            seriesCategories = snapshot.seriesCategories
            liveChannels = snapshot.live
            vodChannels = snapshot.vod
            seriesChannels = snapshot.series
        } finally {
            isLoadingChannels = false
        }
    }

    fun loadChannelsByCategory(type: ChannelType, categoryId: String? = null) {
        if (!isConnected) return
        isLoadingChannels = true
        coroutineScope.launch {
            try {
                val channels = repository.getChannels(type, categoryId)
                when (type) {
                    ChannelType.LIVE -> liveChannels = channels
                    ChannelType.VOD -> vodChannels = channels
                    ChannelType.SERIES -> seriesChannels = channels
                }
            } catch (e: Exception) {
                contentError = e.message
            } finally {
                isLoadingChannels = false
            }
        }
    }

    fun loadChannelsByCategory(category: IptvCategory) {
        selectedCategory = category
        categoryChannels = emptyList()
        coroutineScope.launch {
            isLoadingChannels = true
            try {
                if (isAdminPanel) {
                    val source = when (category.type) {
                        ChannelType.LIVE -> allLiveChannels
                        ChannelType.VOD -> allVodChannels
                        ChannelType.SERIES -> allSeriesChannels
                    }
                    categoryChannels = source.filter { it.categoryId == category.id }
                } else {
                    categoryChannels = repository.getChannels(category.type, category.id)
                }
            } finally {
                isLoadingChannels = false
            }
        }
    }

    fun loadSeriesEpisodes(seriesId: Int, onResult: (List<IptvChannel>) -> Unit = {}) {
        coroutineScope.launch {
            isLoadingEpisodes = true
            contentError = null
            seriesEpisodes = try {
                repository.getSeriesEpisodes(seriesId).ifEmpty {
                    m3uSeriesEpisodes[seriesId] ?: emptyList()
                }
            } catch (e: Exception) {
                contentError = e.message ?: "Failed to load episodes"
                m3uSeriesEpisodes[seriesId] ?: emptyList()
            } finally {
                isLoadingEpisodes = false
            }
            onResult(seriesEpisodes)
        }
    }

    // ── Search ──

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }
        searchQuery = query
        isSearching = true
        searchJob = coroutineScope.launch {
            try {
                val normalizedQuery = query.trim().lowercase()
                val allChannels = allLiveChannels + allVodChannels + allSeriesChannels
                searchResults = withContext(Dispatchers.Default) {
                    allChannels.filter { channel ->
                        channel.name.contains(normalizedQuery, ignoreCase = true) ||
                                channel.categoryName.contains(normalizedQuery, ignoreCase = true) ||
                                channel.genre.contains(normalizedQuery, ignoreCase = true) ||
                                channel.year.contains(normalizedQuery, ignoreCase = true) ||
                                channel.plot.contains(normalizedQuery, ignoreCase = true)
                    }
                }
            } catch (_: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    // ── Stream URL ──

    fun getStreamUrl(channel: IptvChannel): String {
        if (channel.streamUrl.isNotEmpty()) return channel.streamUrl
        return repository.getStreamUrl(channel)
    }

    fun findChannelByUrl(url: String): IptvChannel? {
        return (allLiveChannels + allVodChannels + allSeriesChannels + seriesGroups +
                m3uSeriesEpisodes.values.flatten()).find {
            it.streamUrl == url || it.streamId.toString() in url
        }
    }

    fun getPlayerHeaders(streamUrl: String): Map<String, String> {
        val headers = mutableMapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9"
        )

        val parsed = runCatching { io.ktor.http.Url(streamUrl) }.getOrNull()
        if (parsed != null && parsed.host.isNotBlank()) {
            val base = buildString {
                append(parsed.protocol.name)
                append("://")
                append(parsed.host)
                if (parsed.port != 0 && parsed.port != 80 && parsed.port != 443) append(":${parsed.port}")
            }
            headers["Referer"] = "$base/"
            headers["Origin"] = base
        }

        repository.currentServerUrl()?.let { serverUrl ->
            headers["Referer"] = "${serverUrl.trimEnd('/')}/"
            headers["Origin"] = serverUrl.trimEnd('/')
        }

        return headers
    }

    // ── User Content ──

    fun loadUserContent() {
        coroutineScope.launch {
            isUserContentLoading = true
            try {
                val client = adminApiClient
                if (client != null && deviceMac.isNotBlank() && deviceKey.isNotBlank()) {
                    libraryItems = client.getLibrary(deviceMac, deviceKey).items
                    subscriptions = client.getSubscriptions(deviceMac, deviceKey).items
                    val cloudProgress = client.getWatchProgress(deviceMac, deviceKey, null).items
                    watchProgressItems = mergeWatchProgress(cloudProgress, localWatchProgress())
                } else {
                    watchProgressItems = localWatchProgress()
                }
            } catch (_: Exception) {
                // Keep local data on failure
            } finally {
                isUserContentLoading = false
            }
        }
    }

    private fun localWatchProgress(): List<WatchProgressItem> {
        return watchProgressStore.getAllProgress().mapNotNull { (streamUrl, position, duration) ->
            findChannelByUrl(streamUrl)?.toUserContentWatchProgress(position, duration)
        }
    }

    private fun mergeWatchProgress(
        cloud: List<WatchProgressItem>,
        local: List<WatchProgressItem>
    ): List<WatchProgressItem> {
        return (cloud + local)
            .groupBy { it.contentId }
            .map { (_, items) -> items.maxByOrNull { it.positionMs } ?: items.first() }
    }

    fun addToLibrary(channel: IptvChannel) {
        val client = adminApiClient ?: run {
            if (libraryItems.none { it.contentId == channel.userContentId() }) {
                libraryItems = libraryItems + channel.toUserContentItem()
            }
            return
        }
        if (deviceMac.isBlank() || deviceKey.isBlank()) return
        val item = channel.toUserContentItem()
        libraryItems = libraryItems + item
        coroutineScope.launch {
            try {
                client.addToLibrary(deviceMac, deviceKey, item)
            } catch (_: Exception) {
                libraryItems = libraryItems.filter { it.contentId != item.contentId }
            }
        }
    }

    fun removeFromLibrary(contentId: String) {
        val client = adminApiClient ?: run {
            libraryItems = libraryItems.filter { it.contentId != contentId }
            return
        }
        if (deviceMac.isBlank() || deviceKey.isBlank()) return
        val previous = libraryItems
        libraryItems = previous.filter { it.contentId != contentId }
        coroutineScope.launch {
            try {
                client.removeFromLibrary(deviceMac, deviceKey, contentId)
            } catch (_: Exception) {
                libraryItems = previous
            }
        }
    }

    fun removeFromLibrary(channel: IptvChannel) {
        removeFromLibrary(channel.userContentId())
    }

    fun isInLibrary(channel: IptvChannel): Boolean {
        return libraryItems.any { it.contentId == channel.userContentId() }
    }

    fun saveWatchProgress(channel: IptvChannel, positionMs: Long, durationMs: Long) {
        val item = channel.toUserContentWatchProgress(positionMs, durationMs)
        watchProgressStore.saveProgress(channel.streamUrl, positionMs, durationMs)
        val previous = watchProgressItems.filter { it.contentId != item.contentId }
        watchProgressItems = previous + item

        val client = adminApiClient ?: return
        if (deviceMac.isBlank() || deviceKey.isBlank()) return
        coroutineScope.launch {
            try {
                client.saveWatchProgress(deviceMac, deviceKey, item)
            } catch (_: Exception) {
                // Keep local entry even if cloud fails
            }
        }
    }

    fun removeWatchProgress(contentId: String) {
        val client = adminApiClient ?: run {
            watchProgressItems = watchProgressItems.filter { it.contentId != contentId }
            return
        }
        if (deviceMac.isBlank() || deviceKey.isBlank()) return
        val previous = watchProgressItems
        watchProgressItems = previous.filter { it.contentId != contentId }
        coroutineScope.launch {
            try {
                client.removeWatchProgress(deviceMac, deviceKey, contentId)
            } catch (_: Exception) {
                watchProgressItems = previous
            }
        }
    }

    fun removeWatchProgressByUrl(streamUrl: String) {
        val channel = findChannelByUrl(streamUrl)
        if (channel != null) {
            removeWatchProgress(channel.userContentId())
        }
        watchProgressStore.clearProgress(streamUrl)
    }

    fun getLocalWatchProgress(streamUrl: String): WatchProgressStore.Progress? {
        return watchProgressStore.getProgress(streamUrl)
    }

    private fun IptvChannel.userContentId(): String = id.ifBlank { streamId.toString() }

    private fun IptvChannel.toUserContentType(): String = when (type) {
        ChannelType.LIVE -> "live"
        ChannelType.VOD -> "movie"
        ChannelType.SERIES -> "series"
    }

    private fun IptvChannel.toUserContentItem(): LibraryItem =
        LibraryItem(
            contentId = userContentId(),
            contentType = toUserContentType(),
            title = name,
            posterUrl = streamIcon.ifBlank { coverUrl },
            streamUrl = streamUrl,
            groupTitle = categoryName
        )

    private fun IptvChannel.toUserContentWatchProgress(
        positionMs: Long,
        durationMs: Long
    ): WatchProgressItem =
        WatchProgressItem(
            contentId = userContentId(),
            contentType = toUserContentType(),
            title = name,
            posterUrl = streamIcon.ifBlank { coverUrl },
            streamUrl = streamUrl,
            positionMs = positionMs,
            durationMs = durationMs
        )

    // ── Support ──

    fun loadSupportTickets() {
        val api = adminApiClient ?: return
        coroutineScope.launch {
            isSupportBusy = true
            try {
                val response = api.getSupportTickets(deviceMac, deviceKey)
                if (response.success) {
                    supportTickets = response.tickets
                    canCreateSupportTicket = response.canCreateTicket
                }
            } catch (error: Exception) {
                supportMessage = error.message
            }
            isSupportBusy = false
        }
    }

    fun submitSupportTicket(subject: String, message: String) {
        val api = adminApiClient ?: run {
            supportMessage = "Support unavailable"
            return
        }
        if (subject.isBlank() || message.isBlank()) {
            supportMessage = "Please fill in all fields"
            return
        }
        if (!canCreateSupportTicket) {
            supportMessage = "Cannot create support ticket"
            return
        }
        coroutineScope.launch {
            isSupportBusy = true
            supportMessage = null
            try {
                val response = api.submitSupportTicket(
                    macAddress = deviceMac,
                    deviceKey = deviceKey,
                    profileName = profileName,
                    profileId = profileId,
                    subject = subject,
                    message = message
                )
                if (response.success) {
                    response.ticket?.let { supportTickets = listOf(it) + supportTickets }
                    canCreateSupportTicket = false
                    supportMessage = "Ticket sent"
                } else {
                    supportMessage = response.error.ifBlank { "Failed to send ticket" }
                }
            } catch (error: Exception) {
                supportMessage = error.message ?: "Error sending ticket"
            }
            isSupportBusy = false
        }
    }

    // ── Settings / Profile ──

    fun updateNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        appSettings.notificationsEnabled = enabled
    }

    fun updateAutoplayEnabled(enabled: Boolean) {
        autoplayEnabled = enabled
        appSettings.autoplayEnabled = enabled
    }

    fun setLanguage(tag: String) {
        languageTag = tag
        appSettings.languageTag = tag
    }

    fun updateProfile(name: String, avatarUri: String = "") {
        profileName = name.trim().take(80)
        profileAvatarUri = avatarUri
        appSettings.profileName = profileName
        appSettings.profileAvatarUri = profileAvatarUri
        if (userAccount.isLoggedIn) {
            userAccount = userAccount.copy(name = profileName)
            appSettings.userAccount = userAccount
        }
    }

    fun uploadProfileAvatar(bytes: ByteArray, contentType: String = "image/jpeg"): String {
        val api = adminApiClient ?: return ""
        coroutineScope.launch {
            val uploaded = runCatching {
                api.uploadProfileAvatar(deviceMac, deviceKey, bytes, contentType)
            }.getOrNull().orEmpty()
            if (uploaded.startsWith("https://")) {
                profileAvatarUri = uploaded
                appSettings.profileAvatarUri = uploaded
            }
        }
        return ""
    }

    fun signIn(account: UserAccount) {
        val previous = appSettings.userAccount
        val isNewAccount = account.uid.isNotBlank() && account.uid != previous?.uid

        appSettings.userAccount = account
        userAccount = account

        if (isNewAccount) {
            if (account.photoUrl.isNotBlank()) {
                profileAvatarUri = account.photoUrl
            } else {
                profileAvatarUri = ""
                appSettings.profileAvatarUri = ""
            }
            appSettings.profileId = appSettings.regenerateProfileId()
            profileId = appSettings.profileId
        }

        if (account.name.isNotBlank()) {
            profileName = account.name
            appSettings.profileName = account.name
        }
    }

    fun signOut() {
        appSettings.userAccount = null
        userAccount = UserAccount()
    }

    fun toggleDarkTheme() {
        isDarkTheme = !isDarkTheme
        appSettings.isDarkTheme = isDarkTheme
    }

    // ── Navigation helpers ──

    fun saveHomeScroll(index: Int, offset: Int) { homeScroll = ScrollPosition(index, offset) }
    fun saveHubScroll(index: Int, offset: Int) { hubScroll = ScrollPosition(index, offset) }
    fun saveHubSelection(tab: String, categoryId: String?, search: String = "") {
        hubSelectedTab = tab
        hubSelectedCategory = categoryId
        hubSearchQuery = search
    }
    fun saveChannelsScroll(key: String, index: Int, offset: Int) {
        _channelsScroll[key] = ScrollPosition(index, offset)
    }
    fun getChannelsScroll(key: String) = _channelsScroll[key] ?: ScrollPosition()
    fun saveSeriesSeason(seriesId: Int, season: String?) {
        if (season != null) _seriesSelectedSeason[seriesId] = season
    }
    fun getSeriesSeason(seriesId: Int) = _seriesSelectedSeason[seriesId]
    fun saveSeriesScroll(seriesId: Int, index: Int, offset: Int) {
        _seriesScroll[seriesId] = ScrollPosition(index, offset)
    }
    fun getSeriesScroll(seriesId: Int) = _seriesScroll[seriesId] ?: ScrollPosition()

    // ── Notifications ──

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

    // ── Disconnect ──

    fun disconnect() {
        heartbeatJob?.cancel()
        activationJob?.cancel()
        configJob?.cancel()
        playlistJob?.cancel()
        connectionJob?.cancel()
        adminApiClient = null

        repository.disconnect()
        preferences.clearServer()
        savedServer = null
        playlistCache.clear()

        connectionState = ConnectionState.Disconnected
        activationState = null
        availablePlaylists = emptyList()
        currentPlaylist = null
        shortSocialStates = emptyMap()
        recordedShortViews.clear()
        categoryChannels = emptyList()
        selectedCategory = null
        registerResponse = null
        clearContent()
    }

    private fun clearContent() {
        liveCategories = emptyList()
        vodCategories = emptyList()
        seriesCategories = emptyList()
        liveChannels = emptyList()
        vodChannels = emptyList()
        seriesChannels = emptyList()
        seriesGroups = emptyList()
        m3uSeriesEpisodes = emptyMap()
        seriesEpisodes = emptyList()
        searchResults = emptyList()
        categoryChannels = emptyList()
        selectedCategory = null
    }

    // ── Cleanup ──

    fun onCleared() {
        heartbeatJob?.cancel()
        activationJob?.cancel()
        configJob?.cancel()
        playlistJob?.cancel()
        connectionJob?.cancel()
        searchJob?.cancel()
    }

    // ── Helpers ──

    private fun String.isValidNetworkUrl(): Boolean {
        return startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)
    }

    private fun String.ifBlank(default: String): String =
        if (isBlank()) default else this
}

private fun parseColorString(color: String): Long {
    val trimmed = color.trim().removePrefix("#")
    return when (trimmed.length) {
        6 -> ("FF$trimmed").toLong(16)
        8 -> trimmed.toLong(16)
        else -> throw IllegalArgumentException("Invalid color: $color")
    }
}
