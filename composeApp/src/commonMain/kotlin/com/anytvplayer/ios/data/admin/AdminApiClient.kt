package com.anytvplayer.ios.data.admin

import com.anytvplayer.ios.data.network.appHttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Client for the AnyTV device API.
 *
 * Application authentication is sent in X-API-Key. Endpoints bound to a
 * registered device additionally receive X-Device-Key.
 */
class AdminApiClient(private val config: AdminPanelConfig) {

    private val baseUrl: String = config.panelUrl.trim().trimEnd('/')

    // ── Device ──

    suspend fun registerDevice(registration: DeviceRegistration): RegisterResponse =
        withContext(Dispatchers.Default) {
            val body = buildJsonObject {
                put("mac_address", registration.macAddress)
                put("device_name", registration.deviceName)
                put("model", registration.model)
                put("model_version", registration.modelVersion)
                put("app_version", registration.appVersion)
                if (registration.deviceKey.isNotBlank()) put("device_key", registration.deviceKey)
                if (registration.recoveryToken.isNotBlank()) {
                    put("recovery_token", registration.recoveryToken)
                }
                if (registration.ipAddress.isNotBlank()) put("ip_address", registration.ipAddress)
                if (registration.countryName.isNotBlank()) put("country_name", registration.countryName)
                if (registration.countryCode.isNotBlank()) put("country_code", registration.countryCode)
            }
            val json = requestJson(
                method = Method.POST,
                url = endpoint("/api/v1/device/register"),
                deviceKey = "",
                body = body,
                clientHeader = true
            )
            RegisterResponse(
                success = json.bool("success", false),
                deviceId = json.text("device_id"),
                deviceKey = json.text("device_key"),
                isNew = json.bool("is_new", false)
            )
        }

    suspend fun sendHeartbeat(request: HeartbeatRequest): HeartbeatResponse =
        withContext(Dispatchers.Default) {
            val body = buildJsonObject {
                put("mac_address", request.macAddress)
                put("device_key", request.deviceKey)
                put("is_online", request.isOnline)
            }
            val json = postJson(endpoint("/api/v1/device/heartbeat"), body, request.deviceKey)
            HeartbeatResponse(
                success = json.bool("success", false),
                serverTime = json.text("server_time")
            )
        }

    // ── Playlists ──

    suspend fun getPlaylist(macAddress: String, deviceKey: String): PlaylistResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/playlist", macAddress), deviceKey)
            PlaylistResponse(
                success = json.bool("success", false),
                playlist = json.obj("playlist")?.let(::parsePlaylistInfo),
                defaultPlaylistUrl = json.text("default_playlist_url")
            )
        }

    suspend fun getAllPlaylists(macAddress: String, deviceKey: String): PlaylistsResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/playlists", macAddress), deviceKey)
            val playlists = json.array("playlists").map { parsePlaylistInfo(it.jsonObject) }
            PlaylistsResponse(
                success = json.bool("success", false),
                playlists = playlists,
                total = json.int("total", playlists.size),
                defaultPlaylistUrl = json.text("default_playlist_url")
            )
        }

    suspend fun checkPlaylistChange(macAddress: String, deviceKey: String): PlaylistCheckResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/playlist/check", macAddress), deviceKey)
            PlaylistCheckResponse(
                success = !json.containsKey("error"),
                hasPlaylist = json.bool("has_playlist", false),
                playlistId = json.text("playlist_id"),
                lastUpdated = json.text("last_updated"),
                total = json.int("total", 0)
            )
        }

    suspend fun addPlaylist(
        macAddress: String,
        deviceKey: String,
        name: String,
        url: String
    ): PlaylistMutationResponse = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("mac_address", macAddress)
            put("device_key", deviceKey)
            put("name", name.trim())
            put("url", url.trim())
        }
        val json = postJson(endpoint("/api/v1/device/playlist-add"), body, deviceKey)
        PlaylistMutationResponse(
            success = json.bool("success", false),
            playlist = json.obj("playlist")?.let(::parsePlaylistInfo),
            error = json.text("error")
        )
    }

    suspend fun deletePlaylist(
        macAddress: String,
        deviceKey: String,
        playlistId: String
    ): PlaylistMutationResponse = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("mac_address", macAddress)
            put("device_key", deviceKey)
            put("playlist_id", playlistId)
        }
        val json = postJson(endpoint("/api/v1/device/playlist-delete"), body, deviceKey)
        PlaylistMutationResponse(
            success = json.bool("success", false),
            deletedId = json.text("deleted_id"),
            error = json.text("error")
        )
    }

    // ── User Library ──

    suspend fun getLibrary(macAddress: String, deviceKey: String): LibraryResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/library", macAddress), deviceKey)
            LibraryResponse(
                success = json.bool("success", false),
                items = json.array("items").map { parseLibraryItem(it.jsonObject) }
            )
        }

    suspend fun addToLibrary(
        macAddress: String,
        deviceKey: String,
        item: LibraryItem
    ): LibraryResponse = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("content_id", item.contentId)
            put("content_type", item.contentType)
            put("title", item.title)
            put("poster_url", item.posterUrl)
            put("stream_url", item.streamUrl)
            put("group_title", item.groupTitle)
        }
        val json = postJson(deviceEndpoint("/api/v1/device/library", macAddress), body, deviceKey)
        LibraryResponse(
            success = json.bool("success", false),
            items = json.obj("item")?.let { listOf(parseLibraryItem(it)) } ?: emptyList()
        )
    }

    suspend fun removeFromLibrary(
        macAddress: String,
        deviceKey: String,
        contentId: String
    ): Boolean = withContext(Dispatchers.Default) {
        val url = deviceEndpoint("/api/v1/device/library", macAddress)
            .withQuery("content_id", contentId)
        runCatching {
            requestJson(Method.DELETE, url, deviceKey, null, false).bool("success", false)
        }.getOrElse { false }
    }

    // ── Continue Watching ──

    suspend fun getWatchProgress(
        macAddress: String,
        deviceKey: String,
        contentId: String?
    ): WatchProgressResponse = withContext(Dispatchers.Default) {
        var url = deviceEndpoint("/api/v1/device/watch-progress", macAddress)
        if (!contentId.isNullOrBlank()) url = url.withQuery("content_id", contentId)
        val json = getJson(url, deviceKey)
        WatchProgressResponse(
            success = json.bool("success", false),
            item = json.obj("item")?.let(::parseWatchProgressItem),
            items = json.array("items").map { parseWatchProgressItem(it.jsonObject) }
        )
    }

    suspend fun saveWatchProgress(
        macAddress: String,
        deviceKey: String,
        item: WatchProgressItem
    ): WatchProgressResponse = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("content_id", item.contentId)
            put("content_type", item.contentType)
            put("title", item.title)
            put("poster_url", item.posterUrl)
            put("stream_url", item.streamUrl)
            put("position_ms", item.positionMs)
            put("duration_ms", item.durationMs)
        }
        val json = postJson(deviceEndpoint("/api/v1/device/watch-progress", macAddress), body, deviceKey)
        WatchProgressResponse(
            success = json.bool("success", false),
            item = json.obj("item")?.let(::parseWatchProgressItem)
        )
    }

    suspend fun removeWatchProgress(
        macAddress: String,
        deviceKey: String,
        contentId: String
    ): Boolean = withContext(Dispatchers.Default) {
        val url = deviceEndpoint("/api/v1/device/watch-progress", macAddress)
            .withQuery("content_id", contentId)
        runCatching {
            requestJson(Method.DELETE, url, deviceKey, null, false).bool("success", false)
        }.getOrElse { false }
    }

    // ── Subscriptions & Activation ──

    suspend fun getSubscriptions(macAddress: String, deviceKey: String): SubscriptionsResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/subscriptions", macAddress), deviceKey)
            SubscriptionsResponse(
                success = json.bool("success", false),
                items = json.array("items").map { parseSubscription(it.jsonObject) }
            )
        }

    suspend fun checkActivation(macAddress: String, deviceKey: String): ActivationResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/activation", macAddress), deviceKey)
            ActivationResponse(
                success = json.bool("success", false),
                isActivated = json.bool("is_activated", false),
                activation = json.obj("activation")?.let {
                    ActivationInfo(
                        status = it.text("status"),
                        activatedAt = it.text("activated_at"),
                        expiresAt = it.text("expires_at"),
                        packageName = it.text("package_name")
                    )
                }
            )
        }

    suspend fun redeemGift(macAddress: String, code: String): GiftCodeResponse =
        withContext(Dispatchers.Default) {
            val body = buildJsonObject {
                put("mac_address", macAddress)
                put("code", code.trim())
            }
            runCatching {
                val json = postJson(endpoint("/api/checkout/redeem-gift"), body, "")
                GiftCodeResponse(
                    success = json.bool("success", false),
                    expiresAt = json.text("expires_at"),
                    durationDays = json.int("duration_days", 0),
                    error = json.text("error")
                )
            }.getOrElse {
                GiftCodeResponse(success = false, error = it.message ?: "Gift code redemption failed")
            }
        }

    // ── Support ──

    suspend fun getSupportTickets(macAddress: String, deviceKey: String): SupportTicketsResponse =
        withContext(Dispatchers.Default) {
            val json = getJson(deviceEndpoint("/api/v1/device/support-tickets", macAddress), deviceKey)
            SupportTicketsResponse(
                success = json.bool("success", false),
                tickets = json.array("tickets").map { parseSupportTicket(it.jsonObject) },
                canCreateTicket = json.bool("can_create_ticket", true)
            )
        }

    suspend fun submitSupportTicket(
        macAddress: String,
        deviceKey: String,
        profileName: String,
        profileId: String,
        subject: String,
        message: String
    ): SupportTicketsResponse = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("mac_address", macAddress)
            put("device_key", deviceKey)
            put("profile_name", profileName.trim())
            put("profile_id", profileId.trim())
            put("subject", subject.trim())
            put("message", message.trim())
        }
        val json = postJson(endpoint("/api/v1/device/support-tickets"), body, deviceKey)
        SupportTicketsResponse(
            success = json.bool("success", false),
            ticket = json.obj("ticket")?.let(::parseSupportTicket),
            canCreateTicket = false,
            error = json.text("error")
        )
    }

    suspend fun uploadProfileAvatar(
        macAddress: String,
        deviceKey: String,
        bytes: ByteArray,
        contentType: String
    ): String = withContext(Dispatchers.Default) {
        val safeContentType = when (contentType.lowercase()) {
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val extension = when (safeContentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val response = appHttpClient.post(endpoint("/api/v1/device/profile-avatar")) {
            header("X-API-Key", config.apiKey)
            if (deviceKey.isNotBlank()) header("X-Device-Key", deviceKey)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("mac_address", macAddress)
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, safeContentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"profile.$extension\""
                                )
                            }
                        )
                    }
                )
            )
        }
        readJson(response).text("url")
    }

    // ── Shorts social ──

    suspend fun getShortSocial(
        clipId: String,
        macAddress: String,
        deviceKey: String
    ): ShortSocialState = withContext(Dispatchers.Default) {
        val url = endpoint("/api/v1/shorts/social")
            .withQuery("clip_id", clipId)
            .withQuery("mac", macAddress)
        parseShortSocial(getJson(url, deviceKey))
    }

    suspend fun setShortAction(
        clipId: String,
        macAddress: String,
        deviceKey: String,
        action: String,
        enabled: Boolean?
    ): ShortSocialState = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("clip_id", clipId)
            put("mac_address", macAddress)
            put("action", action)
            if (enabled != null) put("enabled", enabled)
        }
        parseShortSocial(postJson(endpoint("/api/v1/shorts/action"), body, deviceKey))
    }

    suspend fun addShortComment(
        clipId: String,
        macAddress: String,
        deviceKey: String,
        comment: String,
        parentId: String,
        profileName: String,
        profileId: String,
        avatarUrl: String
    ): ShortSocialState = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("clip_id", clipId)
            put("mac_address", macAddress)
            put("body", comment)
            if (parentId.isNotBlank()) put("parent_id", parentId)
            if (profileName.isNotBlank()) put("profile_name", profileName.trim())
            if (profileId.isNotBlank()) put("profile_id", profileId.trim())
            if (avatarUrl.startsWith("https://")) put("avatar_url", avatarUrl)
        }
        parseShortSocial(postJson(endpoint("/api/v1/shorts/comment"), body, deviceKey))
    }

    suspend fun editShortComment(
        clipId: String,
        macAddress: String,
        deviceKey: String,
        commentId: String,
        comment: String
    ): ShortSocialState = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("clip_id", clipId)
            put("mac_address", macAddress)
            put("comment_id", commentId)
            put("body", comment)
        }
        parseShortSocial(requestJson(Method.PATCH, endpoint("/api/v1/shorts/comment"), deviceKey, body, false))
    }

    suspend fun deleteShortComment(
        clipId: String,
        macAddress: String,
        deviceKey: String,
        commentId: String
    ): ShortSocialState = withContext(Dispatchers.Default) {
        val body = buildJsonObject {
            put("clip_id", clipId)
            put("mac_address", macAddress)
            put("comment_id", commentId)
        }
        parseShortSocial(requestJson(Method.DELETE, endpoint("/api/v1/shorts/comment"), deviceKey, body, false))
    }

    // ── Config ──

    suspend fun getConfig(): ConfigResponse = withContext(Dispatchers.Default) {
        val json = requestJson(
            method = Method.GET,
            url = endpoint("/api/v1/device/config"),
            deviceKey = "",
            body = null,
            clientHeader = true
        )

        val configItems = json.array("player_config").map { element ->
            val item = element.jsonObject
            val publisherName = item.text("publisher_name")
            val publisherAvatarUrl = item.text("publisher_avatar_url")
            PlayerConfigItem(
                id = item.text("id"),
                type = item.text("type"),
                enabled = item.bool("enabled", true),
                title = item.text("title"),
                category = item.text("category"),
                publisherName = publisherName,
                publisherAvatarUrl = publisherAvatarUrl,
                items = parseShortClips(item.array("items"), publisherName, publisherAvatarUrl)
            )
        }

        val packages = json.array("packages").map { element ->
            val item = element.jsonObject
            SubscriptionPackage(
                id = item.text("id"),
                name = item.text("name"),
                description = item.text("description"),
                price = item.double("price", 0.0),
                durationDays = item.int("duration_days", 0)
            )
        }

        val banners = json.array("banners").map { element ->
            val item = element.jsonObject
            Banner(
                id = item.text("id"),
                title = item.text("title"),
                subtitle = item.text("subtitle"),
                imageUrl = item.text("image_url"),
                streamUrl = item.text("stream_url"),
                isActive = item.bool("is_active", true),
                sortOrder = item.int("sort_order", 0)
            )
        }

        val branding = json.obj("branding")
        ConfigResponse(
            success = json.bool("success", false),
            playerConfig = configItems,
            branding = BrandingConfig(
                appName = branding?.textOr("app_name", "AnyTV Players") ?: "AnyTV Players",
                logoUrl = branding?.text("logo_url").orEmpty(),
                primaryColor = branding?.textOr("primary_color", "#EA580C") ?: "#EA580C",
                defaultLanguage = branding?.textOr("default_language", "ar") ?: "ar",
                bannerImageUrl = branding?.text("banner_image_url").orEmpty(),
                bannerTitle = branding?.text("banner_title").orEmpty(),
                bannerSubtitle = branding?.text("banner_subtitle").orEmpty(),
                bannerStreamUrl = branding?.text("banner_stream_url").orEmpty()
            ),
            banners = banners,
            packages = packages
        )
    }

    // ── Transport ──

    private enum class Method { GET, POST, PATCH, DELETE }

    private suspend fun getJson(url: Url, deviceKey: String): JsonObject =
        requestJson(Method.GET, url, deviceKey, null, false)

    private suspend fun postJson(url: Url, body: JsonObject, deviceKey: String): JsonObject =
        requestJson(Method.POST, url, deviceKey, body, false)

    private suspend fun requestJson(
        method: Method,
        url: Url,
        deviceKey: String,
        body: JsonObject?,
        clientHeader: Boolean
    ): JsonObject {
        val response: HttpResponse = when (method) {
            Method.GET -> appHttpClient.get(url) {
                applyHeaders(deviceKey, clientHeader)
            }
            Method.POST -> appHttpClient.post(url) {
                applyHeaders(deviceKey, clientHeader)
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            Method.PATCH -> appHttpClient.patch(url) {
                applyHeaders(deviceKey, clientHeader)
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            Method.DELETE -> appHttpClient.delete(url) {
                applyHeaders(deviceKey, clientHeader)
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
        }
        return readJson(response)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyHeaders(
        deviceKey: String,
        clientHeader: Boolean
    ) {
        header("X-API-Key", config.apiKey)
        if (deviceKey.isNotBlank()) header("X-Device-Key", deviceKey)
        if (clientHeader) header("X-AnyTV-Client", "official-ios")
    }

    private suspend fun readJson(response: HttpResponse): JsonObject {
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val serverMessage = runCatching {
                jsonParser.parseToJsonElement(text).jsonObject.text("error")
            }.getOrNull().orEmpty()
            throw ApiException(
                code = response.status.value,
                message = serverMessage.ifBlank { "Server error (${response.status.value})" }
            )
        }
        if (text.isBlank()) throw ApiException(code = 0, message = "Empty response from server")
        return runCatching { jsonParser.parseToJsonElement(text).jsonObject }
            .getOrElse { throw ApiException(code = 0, message = "Malformed response from server") }
    }

    private fun endpoint(path: String): Url =
        runCatching { Url("$baseUrl$path") }
            .getOrElse { throw ApiException(code = 0, message = "Invalid server URL") }

    private fun deviceEndpoint(path: String, macAddress: String): Url =
        endpoint(path).withQuery("mac", macAddress)

    private fun Url.withQuery(key: String, value: String): Url =
        URLBuilder(this).apply { parameters.append(key, value) }.build()

    // ── Parsers ──

    private fun parsePlaylistInfo(obj: JsonObject): PlaylistInfo = PlaylistInfo(
        id = obj.text("id"),
        name = obj.text("name"),
        url = obj.text("url"),
        isProtected = obj.bool("is_protected", false),
        isActive = obj.bool("is_active", true),
        createdAt = obj.text("created_at")
    )

    private fun parseSupportTicket(obj: JsonObject): SupportTicket = SupportTicket(
        id = obj.text("id"),
        subject = obj.text("subject"),
        message = obj.text("message"),
        status = obj.textOr("status", "open"),
        adminReply = obj.text("admin_reply"),
        createdAt = obj.text("created_at"),
        updatedAt = obj.text("updated_at")
    )

    private fun parseLibraryItem(obj: JsonObject): LibraryItem = LibraryItem(
        id = obj.text("id"),
        contentId = obj.text("content_id"),
        contentType = obj.text("content_type"),
        title = obj.text("title"),
        posterUrl = obj.text("poster_url"),
        streamUrl = obj.text("stream_url"),
        groupTitle = obj.text("group_title")
    )

    private fun parseWatchProgressItem(obj: JsonObject): WatchProgressItem = WatchProgressItem(
        id = obj.text("id"),
        contentId = obj.text("content_id"),
        contentType = obj.text("content_type"),
        title = obj.text("title"),
        posterUrl = obj.text("poster_url"),
        streamUrl = obj.text("stream_url"),
        positionMs = obj.long("position_ms", 0),
        durationMs = obj.long("duration_ms", 0)
    )

    private fun parseSubscriptionContact(obj: JsonObject): SubscriptionContact = SubscriptionContact(
        platform = obj.text("platform"),
        value = obj.text("value"),
        label = obj.text("label")
    )

    private fun parseSubscription(obj: JsonObject): SubscriptionItem = SubscriptionItem(
        id = obj.text("id"),
        title = obj.text("title"),
        description = obj.text("description"),
        price = obj.text("price"),
        currency = obj.text("currency"),
        durationDays = obj.int("duration_days", 0),
        contacts = obj.array("contacts").map { parseSubscriptionContact(it.jsonObject) },
        isActive = obj.bool("is_active", true)
    )

    private fun parseShortClips(
        array: JsonArray,
        publisherName: String,
        publisherAvatarUrl: String
    ): List<ShortClip> = array.mapIndexed { index, element ->
        val item = element.jsonObject
        ShortClip(
            id = item.text("id").ifBlank { "short_$index" },
            title = item.text("title"),
            description = item.text("description"),
            videoUrl = item.text("video_url").ifBlank { item.text("videoUrl") },
            thumbnailUrl = item.text("thumbnail_url").ifBlank { item.text("thumbnailUrl") },
            creator = item.text("creator").ifBlank { publisherName },
            creatorAvatarUrl = item.text("creator_avatar_url").ifBlank { publisherAvatarUrl },
            views = item.int("views", 0),
            likes = item.int("likes", 0),
            isActive = item.bool("is_active", item.bool("isActive", true))
        )
    }

    private fun parseShortSocial(json: JsonObject): ShortSocialState {
        val comments = json.array("comments").map { element ->
            val item = element.jsonObject
            ShortComment(
                id = item.text("id"),
                parentId = item.text("parent_id"),
                displayName = item.text("display_name"),
                profileId = item.text("profile_id"),
                avatarUrl = item.text("avatar_url"),
                body = item.text("body"),
                createdAt = item.text("created_at"),
                editedAt = item.text("edited_at"),
                isMine = item.bool("is_mine", false)
            )
        }
        return ShortSocialState(
            success = json.bool("success", false),
            liked = json.bool("liked", false),
            saved = json.bool("saved", false),
            likesCount = json.int("likes_count", 0),
            savesCount = json.int("saves_count", 0),
            viewsCount = json.int("views_count", 0),
            commentsCount = json.int("comments_count", comments.size),
            comments = comments
        )
    }

    private companion object {
        val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

// Default arguments are avoided on these extensions: they previously tripped a
// Kotlin/Native linker assertion in this module.

private fun JsonObject.text(key: String): String {
    val primitive = this[key] as? JsonPrimitive ?: return ""
    if (primitive is JsonNull) return ""
    val content = primitive.contentOrNull ?: return ""
    return if (content.equals("null", ignoreCase = true)) "" else content
}

private fun JsonObject.textOr(key: String, default: String): String =
    text(key).ifBlank { default }

private fun JsonObject.bool(key: String, default: Boolean): Boolean {
    val primitive = this[key] as? JsonPrimitive ?: return default
    primitive.booleanOrNull?.let { return it }
    return when (primitive.contentOrNull?.lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> default
    }
}

private fun JsonObject.int(key: String, default: Int): Int =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: default

private fun JsonObject.long(key: String, default: Long): Long =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: default

private fun JsonObject.double(key: String, default: Double): Double =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: default

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): JsonArray =
    this[key] as? JsonArray ?: JsonArray(emptyList())
