package com.anytvplayer.ios.data.admin

/**
 * Data models for the Admin Panel REST API responses.
 * API Base: /api/v1/
 */

// ── Connection Config ──

data class AdminPanelConfig(
    val panelUrl: String = "",
    val apiKey: String = ""
)

// ── Device Registration ──

data class DeviceRegistration(
    val macAddress: String,
    val deviceKey: String,
    val recoveryToken: String = "",
    val deviceName: String,
    val model: String,
    val modelVersion: String,
    val appVersion: String,
    val ipAddress: String = "",
    val countryName: String = "",
    val countryCode: String = ""
)

data class RegisterResponse(
    val success: Boolean,
    val deviceId: String = "",
    val deviceKey: String = "",
    val isNew: Boolean = false
)

// ── Heartbeat ──

data class HeartbeatRequest(
    val macAddress: String,
    val deviceKey: String,
    val isOnline: Boolean
)

data class HeartbeatResponse(
    val success: Boolean,
    val serverTime: String = ""
)

// ── Playlist ──

data class PlaylistInfo(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val isProtected: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String = ""
)

data class PlaylistResponse(
    val success: Boolean,
    val playlist: PlaylistInfo? = null,
    val defaultPlaylistUrl: String = ""
)

data class PlaylistsResponse(
    val success: Boolean,
    val playlists: List<PlaylistInfo> = emptyList(),
    val total: Int = 0,
    val defaultPlaylistUrl: String = ""
)

data class PlaylistCheckResponse(
    val success: Boolean,
    val hasPlaylist: Boolean = false,
    val playlistId: String = "",
    val lastUpdated: String = "",
    val total: Int = 0
)

// ── User Library ──

data class LibraryItem(
    val id: String = "",
    val contentId: String = "",
    val contentType: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val groupTitle: String = ""
)

data class LibraryResponse(
    val success: Boolean,
    val items: List<LibraryItem> = emptyList()
)

// ── Continue Watching ──

data class WatchProgressItem(
    val id: String = "",
    val contentId: String = "",
    val contentType: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

data class WatchProgressResponse(
    val success: Boolean,
    val item: WatchProgressItem? = null,
    val items: List<WatchProgressItem> = emptyList()
)

// ── Subscription Packages ──

data class SubscriptionContact(
    val platform: String = "",
    val value: String = "",
    val label: String = ""
)

data class SubscriptionItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val currency: String = "",
    val durationDays: Int = 0,
    val contacts: List<SubscriptionContact> = emptyList(),
    val isActive: Boolean = true
)

data class SubscriptionsResponse(
    val success: Boolean,
    val items: List<SubscriptionItem> = emptyList()
)

// ── Activation ──

data class ActivationInfo(
    val status: String = "",
    val activatedAt: String = "",
    val expiresAt: String = "",
    val packageName: String = ""
)

data class ActivationResponse(
    val success: Boolean,
    val isActivated: Boolean = false,
    val activation: ActivationInfo? = null
)

// ── Config & Branding ──

data class PlayerConfigItem(
    val id: String = "",
    val type: String = "",
    val enabled: Boolean = true,
    val title: String = "",
    val category: String = "",
    val publisherName: String = "",
    val publisherAvatarUrl: String = "",
    val items: List<ShortClip> = emptyList()
)

data class ShortClip(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val creator: String = "",
    val creatorAvatarUrl: String = "",
    val views: Int = 0,
    val likes: Int = 0,
    val isActive: Boolean = true
)

data class ShortComment(
    val id: String = "",
    val parentId: String = "",
    val displayName: String = "",
    val profileId: String = "",
    val avatarUrl: String = "",
    val body: String = "",
    val createdAt: String = "",
    val editedAt: String = "",
    val isMine: Boolean = false
)

data class ShortSocialState(
    val success: Boolean = true,
    val liked: Boolean = false,
    val saved: Boolean = false,
    val likesCount: Int = 0,
    val savesCount: Int = 0,
    val viewsCount: Int = 0,
    val commentsCount: Int = 0,
    val comments: List<ShortComment> = emptyList()
)

data class BrandingConfig(
    val appName: String = "AnyTV Players",
    val logoUrl: String = "",
    val primaryColor: String = "#EA580C",
    val defaultLanguage: String = "ar",
    val bannerImageUrl: String = "",
    val bannerTitle: String = "",
    val bannerSubtitle: String = "",
    val bannerStreamUrl: String = ""
)

data class Banner(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

data class SubscriptionPackage(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val durationDays: Int = 0
)

data class ConfigResponse(
    val success: Boolean,
    val playerConfig: List<PlayerConfigItem> = emptyList(),
    val branding: BrandingConfig = BrandingConfig(),
    val banners: List<Banner> = emptyList(),
    val packages: List<SubscriptionPackage> = emptyList()
)

data class GiftCodeResponse(
    val success: Boolean,
    val expiresAt: String = "",
    val durationDays: Int = 0,
    val error: String = ""
)

data class PlaylistMutationResponse(
    val success: Boolean,
    val playlist: PlaylistInfo? = null,
    val deletedId: String = "",
    val error: String = ""
)

data class SupportTicket(
    val id: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "open",
    val adminReply: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class SupportTicketsResponse(
    val success: Boolean,
    val tickets: List<SupportTicket> = emptyList(),
    val ticket: SupportTicket? = null,
    val canCreateTicket: Boolean = true,
    val error: String = ""
)

class ApiException(val code: Int, override val message: String) : Exception(message)
