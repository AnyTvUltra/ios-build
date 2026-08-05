# iOS Migration Status

This file tracks the porting progress from the Android `MyApplication` project to the independent iOS KMP project `AnyTViOS`.

## Legend

- `not_started` — not implemented yet
- `in_progress` — work started, not finalized
- `done` — implemented, not fully tested
- `tested` — built and verified on GitHub Actions / device

## Project Infrastructure

| Component | Status | Notes |
|-----------|--------|-------|
| KMP project scaffold | done | `composeApp` + `iosApp` |
| Gradle / version catalog | done | KMP + Compose Multiplatform |
| GitHub Actions build + IPA | done | macOS runner, unsigned |
| README | done | basic instructions |
| `.gitignore` | done | excludes derived data |
| App icon / assets | in_progress | uses Android `ic_launcher.png` |

## Theme & Resources

| Component | Status | Notes |
|-----------|--------|-------|
| Color palette | done | brand + dark/light schemes |
| Typography | done | `SpedaFontFamily` loaded from resource |
| Theme | done | `TwitiTheme` with dark/light support |
| Images / fonts | in_progress | `ic_launcher.png` and `speda_bold.ttf` copied |
| Remaining drawables | not_started | XML vector drawables need conversion |

## Data Layer

| Component | Status | Notes |
|-----------|--------|-------|
| `IptvModels` | done | KMP serializable versions |
| `ContentData` / `SampleData` | done | uses Compose `Color` |
| `AppSettings` | done | `multiplatform-settings` |
| `SecurePreferences` | in_progress | uses `Settings`; iOS Keychain in future |
| `UserAccount` / `NotificationItem` | done | KMP serializable models |
| `DownloadedItem` / `DownloadsManager` | not_started | `URLSessionDownloadTask` later |
| `WatchProgressStore` | not_started | cloud + local progress |
| `IptvPreferences` | done | server persistence |
| `M3uParser` | done | pure Kotlin parser |
| `XtreamApi` | done | Ktor-based |
| `IptvRepository` | done | unifies Xtream + M3U |
| `TmdbApi` / `TmdbModels` | done | Ktor + JSON |
| `HttpClient` | done | Ktor + Darwin engine |

## ViewModel

| Component | Status | Notes |
|-----------|--------|-------|
| `IptvViewModel` | done | core state and methods |
| CompositionLocal provider | done | shared across screens |

## Navigation

| Component | Status | Notes |
|-----------|--------|-------|
| Voyager setup | done | `Navigator` in `App` |
| Screen routes | done | 18 screens mapped |

## Screens

| Screen | Status | Notes |
|--------|--------|-------|
| SplashScreen | done | loading / auto-connect |
| HomeScreen | done | main dashboard |
| LiveHubScreen | done | live channels hub |
| VodHubScreen | done | movies/series hub |
| ProfileScreen | done | user profile |
| ShortsScreen | done | short videos |
| SupportScreen | done | tickets UI |
| LoginScreen | done | connect via Xtream / M3U |
| NotificationsScreen | done | notifications list UI |
| DownloadsScreen | done | downloads UI |
| DetailScreen | done | generic detail |
| MovieDetailScreen | done | movie info from TMDb |
| PlayerScreen | done | AVPlayer video |
| IptvChannelsScreen | done | category grid/list |
| ActivationScreen | done | device activation UI |
| PlaylistPickerScreen | done | playlist selection |
| SeriesEpisodesScreen | done | episodes list |
| UserContentListScreen | done | library / continue watching |

## Components

| Component | Status | Notes |
|-----------|--------|-------|
| AppBottomBar | done | bottom navigation |
| ContentCard | done | item card (in HomeScreen) |
| GlassCard | in_progress | glassmorphism card (stub) |
| MatchCard | in_progress | live match card (stub) |
| SectionHeader | done | section title |

## Platform Services

| Component | Status | Notes |
|-----------|--------|-------|
| Video player (AVPlayer) | done | `UIKitView` wrapper |
| Network permissions | done | `NSAppTransportSecurity` in `Info.plist` |
| Notifications | not_started | `UserNotifications` |
| Downloads | not_started | `URLSessionDownloadTask` |
| Deep links | not_started | URL scheme |
| Language / locale | not_started | iOS locale override |
| Dark/light theme | done | matches system |

## Known Differences from Android

- **Navigation:** Jetpack Navigation replaced by Voyager.
- **Image loading:** Coil 2 replaced by Coil 3 Multiplatform.
- **HTTP client:** OkHttp replaced by Ktor + Darwin engine.
- **Video player:** ExoPlayer / Media3 replaced by AVPlayer.
- **SharedPreferences:** replaced by `multiplatform-settings` (NSUserDefaults on iOS).
- **Secure storage:** not yet using iOS Keychain; uses `Settings`.
- **Downloads:** not yet using `URLSessionDownloadTask`.
