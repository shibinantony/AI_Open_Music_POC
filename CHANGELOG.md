# Changelog

All notable changes to **JSABMusic** are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

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
