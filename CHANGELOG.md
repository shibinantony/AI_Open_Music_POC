# Changelog

All notable changes to **JSABMusic** are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [2.2.0] — 2026-09-18

### Added
- **Sovereign Blue & Doll Mascot Brand Overhaul:**
  - Removed legacy JioSaavn styling and green branding.
  - Introduced custom peaceful doll mascot vector icon (`ic_serene_doll_music.xml`) wearing studio headphones.
  - Updated launcher icons (`ic_launcher_foreground.xml`) to the new peaceful doll music emblem.
  - New brand tagline: *"Enjoy the beauty of sovereign music"*.
  - Serene celestial blue color palette (`SovereignBlue`, `SovereignBlueAccent`, `AmoledCard`, `PeacefulNightSurface`).
- **Playback Controls & Modes:**
  - **Shuffle All & Play All** buttons on Trending, Liked Music, and Recently Listened views.
  - **Repeat Mode Controls** (`Repeat Off`, `Repeat All`, `Repeat One`) with cycle action in `PlayerController` and `NowPlayingSheet`.
  - **Shuffle Mode Toggle** directly accessible on the `NowPlayingSheet` with glowing active state indicator.
- **Firebase Cloud Persistence & Google Authentication (`FirebaseSyncManager.kt`):**
  - **Google Sign-In:** Linked with Firebase Auth. Automatic anonymous session support for immediate guest listening without forced login walls.
  - **Cloud-Synced Liked Music Playlist:** Heart button in `NowPlayingSheet`, mini-player, and list rows; saves to Firestore (`users/{uid}/liked_songs`).
  - **7-Day Recently Listened History:** Automatic playback tracking with 7-day retention in Firestore (`users/{uid}/history`).
  - **Cloud Session Restoration:** Remembers last played song and queue in Firestore (`users/{uid}/session/last_played`); automatically restores library and queue upon reinstall.
- **Library Navigation:**
  - Added primary section switcher: **Explore** (Trending & Search), **Liked Music** (favorites playlist), and **Recent** (7-day history).
  - Added User Profile sheet showing Google avatar, display name, email, cloud sync status, and Sign-In/Out controls.

### Changed
- `PlayerController.kt`: Exposed `shuffleModeEnabled`, `repeatMode`, added `toggleShuffle()`, `cycleRepeatMode()`, `playAll()`, and `onSongStarted` callback.
- `NowPlayingSheet.kt`: Added Shuffle, Repeat, and Like buttons with Sovereign Blue accents.
- `MainActivity.kt`: Integrated FirebaseSyncManager, UserProfileSheet, Section switcher, and Play All / Shuffle All action rows.
- `app/build.gradle.kts`: Bumped `versionCode = 4`, `versionName = "2.2.0"`, added Firebase BoM 33.1.2 and Google Auth libraries.
- `proguard-rules.pro`: Added keep rules for Firebase, Firestore, and Google Play Services Auth.

---

## [2.1.0] — 2026-09-14

### Added
- **Bifurcated Search Engine** — Search results are now split into four dedicated tabs:
  - **Songs** — individual track results (existing endpoint, now in its own tab)
  - **Albums** — album-level results via `search.getAlbumResults`; shows artist, year, and track count badge
  - **Artists** — artist results via `search.getArtistResults`; shows circular avatar or fallback icon and formatted follower count (K/M)
  - **Playlists** — curated playlist results via `search.getPlaylistResults`; shows song count and follower count
- **Parallel API Queries** — `JioSaavnApiClient.searchAll()` fires all four search endpoints simultaneously using Kotlin `async`/`await` for near-instant results.
- **Drill-Down Playback** — Tapping any Album, Artist, or Playlist card:
  - Fetches the full song list from the appropriate JioSaavn endpoint
  - Loads the entire list into the `PlayerController` queue for gapless playback
  - Auto-plays the first track
- **New Data Models** — `AlbumItem`, `ArtistItem`, `PlaylistItem`, `SearchResults` aggregate wrapper.
- **New API Methods** — `getAlbumSongs(albumId)`, `getArtistSongs(artistId)`, `getPlaylistSongs(listId)`.
- **Stable LazyColumn Keys** — All list composables now use item ID as stable key for correct Compose diffing.
- **ProGuard** — Added `keep` rules for all new data models and OkHttp, ensuring release builds don't strip API response classes.

### Changed
- `JioSaavnApiClient` — Refactored to a shared private `getJson(url)` helper, eliminating repeated `OkHttpClient.newCall` boilerplate across all endpoints.
- `JioSaavnApiClient.getTrendingSongs()` — Now delegates to the new `getPlaylistSongs()` instead of duplicating playlist fetch logic.
- `MainActivity` — Search bar placeholder updated to mention "albums, playlists".
- `build.gradle.kts` — `versionCode` bumped from `1` → `3`; `versionName` from `"1.0.0"` → `"2.1.0"`.
- CI workflow (`build-apk.yml`) — Output APK filenames and release tag updated to `v2.1.0`.

### Fixed
- **Duplicate Listening-Time Bug** — Song-only search was returning the same track multiple times across different search contexts. Bifurcation into category tabs eliminates duplicates entirely.
- **`Modifier.weight(1f)` Compile Safety** — `weight()` modifier is now applied inside the `ColumnScope` caller (`MainPlayerScreen`) and threaded as a `Modifier` parameter to list composables, preventing a potential compile error on strict Compose versions.

---

## [2.0.2] — 2026-09-13

### Fixed
- Verified DES Key Implementation (`38346591`) for 320 kbps stream decryption.
- Deep `more_info` JSON schema extraction for `encrypted_media_url`, `primary_artists`, `duration`.
- Zero Startup ANR: non-blocking foreground service binding.

---

## [2.0.0] — 2026-09-12

### Added
- Complete rewrite: eliminated WebView abstraction, replaced with Pure Native AndroidX Media3 / ExoPlayer.
- Protocol-Level 0% Advertisements — direct Akamai/Cloudflare CDN streaming.
- 320 kbps DES-ECB hardware decryption (`MediaUrlResolver`).
- Samsung Hardware Audio HAL Equalizer — 5-band DSP + Sub-Bass Boost, 8 studio presets.
- Persistent Screen-Off Background MediaSessionService (Android 14/15/16 compliant).
- Gapless playlist queue management via Media3.
- Sleep Timer with acoustic fade-out.

---

## [1.0.4] — Milestone 1 (YouTube Music)

> See [AI-Governed-Music-Player-PoC](https://github.com/shibinantony/AI-Governed-Music-Player-PoC)
