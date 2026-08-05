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
| `IptvModels` | in_progress | KMP serializable versions |
| `ContentData` / `SampleData` | in_progress | uses Compose `Color` |
| `AppSettings` | in_progress | `multiplatform-settings` |
| `SecurePreferences` | in_progress | uses `Settings`; iOS Keychain in future |
| `UserAccount` / `NotificationItem` | in_progress | to be implemented |
| `DownloadedItem` / `DownloadsManager` | not_started | `URLSessionDownloadTask` later |
| `WatchProgressStore` | not_started | cloud + local progress |
| `IptvPreferences` | in_progress | server persistence |
| `M3uParser` | in_progress | pure Kotlin parser |
| `XtreamApi` | in_progress | Ktor-based |
| `IptvRepository` | in_progress | unifies Xtream + M3U |
| `TmdbApi` / `TmdbModels` | in_progress | Ktor + JSON |
| `HttpClient` | in_progress | Ktor + Darwin engine |

## ViewModel

| Component | Status | Notes |
|-----------|--------|-------|
| `IptvViewModel` | in_progress | core state and methods |
| CompositionLocal provider | in_progress | shared across screens |

## Navigation

| Component | Status | Notes |
|-----------|--------|-------|
| Voyager setup | in_progress | `Navigator` in `App` |
| Screen routes | in_progress | 18 screens mapped |

## Screens

| Screen | Status | Notes |
|--------|--------|-------|
| SplashScreen | done | loading / auto-connect |
| HomeScreen | done | main dashboard |
| LiveHubScreen | in_progress | live channels hub (stub) |
| VodHubScreen | in_progress | movies/series hub (stub) |
| ProfileScreen | in_progress | user profile (stub) |
| ShortsScreen | in_progress | short videos (stub) |
| SupportScreen | in_progress | tickets (stub) |
| LoginScreen | in_progress | auth (stub) |
| NotificationsScreen | in_progress | notifications list (stub) |
| DownloadsScreen | in_progress | downloads (stub) |
| DetailScreen | in_progress | generic detail (stub) |
| MovieDetailScreen | done | movie info from TMDb |
| PlayerScreen | done | AVPlayer video |
| IptvChannelsScreen | done | category grid/list |
| ActivationScreen | in_progress | device activation (stub) |
| PlaylistPickerScreen | in_progress | playlist selection (stub) |
| SeriesEpisodesScreen | in_progress | episodes list (stub) |
| UserContentListScreen | in_progress | library / continue watching (stub) |

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
| Network permissions | not_started | `NSAppTransportSecurity` in `Info.plist` |
| Notifications | not_started | `UserNotifications` |
| Downloads | not_started | `URLSessionDownloadTask` |
| Deep links | not_started | URL scheme |
| Language / locale | not_started | iOS locale override |
| Dark/light theme | in_progress | matches system |

## Known Differences from Android

- **Navigation:** Jetpack Navigation replaced by Voyager.
- **Image loading:** Coil 2 replaced by Coil 3 Multiplatform.
- **HTTP client:** OkHttp replaced by Ktor + Darwin engine.
- **Video player:** ExoPlayer / Media3 replaced by AVPlayer.
- **SharedPreferences:** replaced by `multiplatform-settings` (NSUserDefaults on iOS).
- **Secure storage:** not yet using iOS Keychain; uses `Settings`.
- **Downloads:** not yet using `URLSessionDownloadTask`.
