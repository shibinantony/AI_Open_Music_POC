package com.brave.jsabmusic.api

import com.brave.jsabmusic.api.model.AlbumItem
import com.brave.jsabmusic.api.model.ArtistItem
import com.brave.jsabmusic.api.model.PlaylistItem
import com.brave.jsabmusic.api.model.SearchResults
import com.brave.jsabmusic.api.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Supported Music Languages for dynamic trending feed curation and exploration.
 */
enum class MusicLanguage(val code: String, val displayName: String, val searchKeyword: String) {
    ALL("all", "All", "Trending Today"),
    ENGLISH("english", "English", "English Pop Hits"),
    MALAYALAM("malayalam", "Malayalam", "Malayalam Top Hits"),
    TAMIL("tamil", "Tamil", "Tamil Top Hits"),
    HINDI("hindi", "Hindi", "Hindi Top Hits"),
    KANNADA("kannada", "Kannada", "Kannada Top Hits"),
    INTERNATIONAL("international", "International", "Billboard Hot 100"),
    OTHERS("others", "Others", "Telugu Punjabi Regional Hits")
}

/**
 * Aggregated Home Feed payload grouped by selected Language.
 */
data class LanguageHomeFeed(
    val language: MusicLanguage,
    val trendingSongs: List<SongItem> = emptyList(),
    val topAlbums: List<AlbumItem> = emptyList(),
    val topArtists: List<ArtistItem> = emptyList(),
    val topPlaylists: List<PlaylistItem> = emptyList()
)

/**
 * High-performance, asynchronous REST Client communicating directly with JioSaavn's JSON API.
 * Uses verified schemas from open-source references (sumitkolhe/jiosaavn-api)
 * to resolve pristine 320 kbps Akamai/Cloudflare CDN media links.
 */
object JioSaavnApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://www.jiosaavn.com/api.php"
    private val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // ─────────────────────────────────────────────────────────────────────────
    // Multi-Language Home Feed Engine (Instant Zero-Latency & Parallel Refresh)
    // ─────────────────────────────────────────────────────────────────────────

    private val homeFeedCache = ConcurrentHashMap<MusicLanguage, LanguageHomeFeed>()

    /**
     * Immediately returns an instant, high-fidelity initial feed for the requested language.
     * Guarantees 0ms startup time and zero UI blocking.
     */
    fun getInstantInitialFeed(language: MusicLanguage): LanguageHomeFeed {
        homeFeedCache[language]?.let { return it }
        val feed = when (language) {
            MusicLanguage.MALAYALAM -> LanguageHomeFeed(
                language = language,
                trendingSongs = getCuratedSongsForLanguage(MusicLanguage.MALAYALAM),
                topAlbums = listOf(
                    AlbumItem("mal_alb_1", "Aavesham", "2024", "Sushin Shyam", "https://c.saavncdn.com/974/Aavesham-Malayalam-2024-20240419131015-500x500.jpg", 9),
                    AlbumItem("mal_alb_2", "Premalu", "2024", "Vishnu Vijay", "https://c.saavncdn.com/922/Premalu-Malayalam-2024-20240212080008-500x500.jpg", 6),
                    AlbumItem("mal_alb_3", "Manjummel Boys", "2024", "Sushin Shyam", "https://c.saavncdn.com/492/Manjummel-Boys-Malayalam-2024-20240221152011-500x500.jpg", 5)
                ),
                topArtists = listOf(
                    ArtistItem("sushin", "Sushin Shyam", "https://c.saavncdn.com/artists/Sushin_Shyam_500x500.jpg", "1.8M"),
                    ArtistItem("dabzee", "Dabzee", "https://c.saavncdn.com/artists/Dabzee_500x500.jpg", "950K")
                ),
                topPlaylists = listOf(
                    PlaylistItem("mal_pl_1", "Malayalam Top 50", "https://c.saavncdn.com/974/Aavesham-Malayalam-2024-20240419131015-500x500.jpg", 50, "1.2M"),
                    PlaylistItem("mal_pl_2", "Malayalam Chill Vibes", "https://c.saavncdn.com/922/Premalu-Malayalam-2024-20240212080008-500x500.jpg", 35, "840K")
                )
            )
            MusicLanguage.TAMIL -> LanguageHomeFeed(
                language = language,
                trendingSongs = getCuratedSongsForLanguage(MusicLanguage.TAMIL),
                topAlbums = listOf(
                    AlbumItem("tam_alb_1", "Jailer", "2023", "Anirudh Ravichander", "https://c.saavncdn.com/131/Jailer-Tamil-2023-20230728190800-500x500.jpg", 8),
                    AlbumItem("tam_alb_2", "Leo", "2023", "Anirudh Ravichander", "https://c.saavncdn.com/014/Leo-Tamil-2023-20231020104612-500x500.jpg", 7),
                    AlbumItem("tam_alb_3", "Vikram", "2022", "Anirudh Ravichander", "https://c.saavncdn.com/835/Vikram-Tamil-2022-20220515182605-500x500.jpg", 5)
                ),
                topArtists = listOf(
                    ArtistItem("anirudh", "Anirudh Ravichander", "https://c.saavncdn.com/artists/Anirudh_Ravichander_500x500.jpg", "14.2M"),
                    ArtistItem("ar_rahman", "A.R. Rahman", "https://c.saavncdn.com/artists/A_R_Rahman_500x500.jpg", "18.5M")
                ),
                topPlaylists = listOf(
                    PlaylistItem("tam_pl_1", "Tamil Viral 50", "https://c.saavncdn.com/131/Jailer-Tamil-2023-20230728190800-500x500.jpg", 50, "2.4M"),
                    PlaylistItem("tam_pl_2", "Anirudh Party Hits", "https://c.saavncdn.com/014/Leo-Tamil-2023-20231020104612-500x500.jpg", 40, "1.8M")
                )
            )
            MusicLanguage.HINDI -> LanguageHomeFeed(
                language = language,
                trendingSongs = getCuratedDefaultSongs(),
                topAlbums = listOf(
                    AlbumItem("hin_alb_1", "Brahmastra", "2022", "Pritam", "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", 8),
                    AlbumItem("hin_alb_2", "Jawan", "2023", "Anirudh Ravichander", "https://c.saavncdn.com/026/Jawan-Hindi-2023-20230905175249-500x500.jpg", 7),
                    AlbumItem("hin_alb_3", "Animal", "2023", "JAM8", "https://c.saavncdn.com/268/Animal-Hindi-2023-20231124191036-500x500.jpg", 8)
                ),
                topArtists = listOf(
                    ArtistItem("arijit", "Arijit Singh", "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg", "32.4M"),
                    ArtistItem("pritam", "Pritam", "https://c.saavncdn.com/artists/Pritam_500x500.jpg", "15.1M")
                ),
                topPlaylists = listOf(
                    PlaylistItem("hin_pl_1", "Bollywood Top 50", "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", 50, "4.8M"),
                    PlaylistItem("hin_pl_2", "Hindi Romance & Chill", "https://c.saavncdn.com/026/Jawan-Hindi-2023-20230905175249-500x500.jpg", 45, "2.9M")
                )
            )
            MusicLanguage.ENGLISH, MusicLanguage.INTERNATIONAL -> LanguageHomeFeed(
                language = language,
                trendingSongs = getCuratedSongsForLanguage(MusicLanguage.ENGLISH),
                topAlbums = listOf(
                    AlbumItem("eng_alb_1", "After Hours", "2020", "The Weeknd", "https://c.saavncdn.com/267/After-Hours-English-2020-20200320001002-500x500.jpg", 14),
                    AlbumItem("eng_alb_2", "Divide", "2017", "Ed Sheeran", "https://c.saavncdn.com/255/Shape-of-You-English-2017-500x500.jpg", 16)
                ),
                topArtists = listOf(
                    ArtistItem("weeknd", "The Weeknd", "https://c.saavncdn.com/artists/The_Weeknd_500x500.jpg", "28.5M"),
                    ArtistItem("ed_sheeran", "Ed Sheeran", "https://c.saavncdn.com/artists/Ed_Sheeran_500x500.jpg", "25.1M")
                ),
                topPlaylists = listOf(
                    PlaylistItem("eng_pl_1", "Today's Top Hits", "https://c.saavncdn.com/267/After-Hours-English-2020-20200320001002-500x500.jpg", 50, "5.6M")
                )
            )
            else -> LanguageHomeFeed(
                language = language,
                trendingSongs = getCuratedDefaultSongs(),
                topAlbums = listOf(
                    AlbumItem("all_alb_1", "Brahmastra", "2022", "Pritam", "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", 8),
                    AlbumItem("all_alb_2", "Jailer", "2023", "Anirudh Ravichander", "https://c.saavncdn.com/131/Jailer-Tamil-2023-20230728190800-500x500.jpg", 8),
                    AlbumItem("all_alb_3", "Aavesham", "2024", "Sushin Shyam", "https://c.saavncdn.com/974/Aavesham-Malayalam-2024-20240419131015-500x500.jpg", 9)
                ),
                topArtists = listOf(
                    ArtistItem("arijit", "Arijit Singh", "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg", "32.4M"),
                    ArtistItem("anirudh", "Anirudh Ravichander", "https://c.saavncdn.com/artists/Anirudh_Ravichander_500x500.jpg", "14.2M")
                ),
                topPlaylists = listOf(
                    PlaylistItem("all_pl_1", "Trending India Hits", "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg", 50, "6.1M")
                )
            )
        }
        homeFeedCache[language] = feed
        return feed
    }

    suspend fun getHomeFeedForLanguage(language: MusicLanguage): LanguageHomeFeed = coroutineScope {
        homeFeedCache[language]?.let { return@coroutineScope it }

        val initial = getInstantInitialFeed(language)
        val query = language.searchKeyword

        val freshFeed = withTimeoutOrNull(4000L) {
            val songsDeferred = async { fetchQuickSongs(query) }
            val albumsDeferred = async { fetchQuickAlbums(query) }
            val artistsDeferred = async { fetchQuickArtists(query) }
            val playlistsDeferred = async { fetchQuickPlaylists(query) }

            val songs = songsDeferred.await().ifEmpty { initial.trendingSongs }
            val albums = albumsDeferred.await().ifEmpty { initial.topAlbums }
            val artists = artistsDeferred.await().ifEmpty { initial.topArtists }
            val playlists = playlistsDeferred.await().ifEmpty { initial.topPlaylists }

            LanguageHomeFeed(
                language = language,
                trendingSongs = songs,
                topAlbums = albums,
                topArtists = artists,
                topPlaylists = playlists
            )
        } ?: initial

        homeFeedCache[language] = freshFeed
        freshFeed
    }

    private suspend fun fetchQuickSongs(keyword: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        try {
            val encodedQuery = URLEncoder.encode(keyword, "UTF-8")
            val url = "$BASE_URL?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=20"
            val body = getJson(url) ?: return@withContext songs
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext songs
            for (i in 0 until results.length()) {
                val song = parseSongJson(results.optJSONObject(i) ?: continue)
                if (song != null) songs.add(song)
            }
        } catch (_: Exception) {}
        songs
    }

    private suspend fun fetchQuickAlbums(keyword: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<AlbumItem>()
        try {
            val encodedQuery = URLEncoder.encode(keyword, "UTF-8")
            val url = "$BASE_URL?__call=search.getAlbumResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=12"
            val body = getJson(url) ?: return@withContext albums
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext albums
            for (i in 0 until results.length()) {
                val album = parseAlbumJson(results.optJSONObject(i) ?: continue)
                if (album != null) albums.add(album)
            }
        } catch (_: Exception) {}
        albums
    }

    private suspend fun fetchQuickArtists(keyword: String): List<ArtistItem> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<ArtistItem>()
        try {
            val encodedQuery = URLEncoder.encode(keyword, "UTF-8")
            val url = "$BASE_URL?__call=search.getArtistResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=10"
            val body = getJson(url) ?: return@withContext artists
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext artists
            for (i in 0 until results.length()) {
                val artist = parseArtistJson(results.optJSONObject(i) ?: continue)
                if (artist != null) artists.add(artist)
            }
        } catch (_: Exception) {}
        artists
    }

    private suspend fun fetchQuickPlaylists(keyword: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<PlaylistItem>()
        try {
            val encodedQuery = URLEncoder.encode(keyword, "UTF-8")
            val url = "$BASE_URL?__call=search.getPlaylistResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=10"
            val body = getJson(url) ?: return@withContext playlists
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext playlists
            for (i in 0 until results.length()) {
                val playlist = parsePlaylistJson(results.optJSONObject(i) ?: continue)
                if (playlist != null) playlists.add(playlist)
            }
        } catch (_: Exception) {}
        playlists
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search Term Normalizer for Phonetic / Transliteration LIKE Matching
    // ─────────────────────────────────────────────────────────────────────────

    fun normalizeSearchTerm(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .replace("aa", "a")
            .replace("ee", "e")
            .replace("ii", "i")
            .replace("oo", "o")
            .replace("uu", "u")
            .replace("sh", "s")
    }

    /**
     * Resolves complete 320 kbps stream details for an array of song IDs.
     */
    suspend fun getSongsByIds(ids: List<String>): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        if (ids.isEmpty()) return@withContext songs
        try {
            val chunk = ids.take(20).joinToString(",")
            val url = "$BASE_URL?__call=song.getDetails&pids=$chunk&_format=json&_marker=0&api_version=4"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue
                val song = parseSongJson(obj)
                if (song != null) songs.add(song)
            }
        } catch (_: Exception) {}
        songs
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parallel search — all four categories at once
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fires all four search endpoints in parallel and returns an aggregate [SearchResults].
     */
    suspend fun searchAll(query: String): SearchResults = coroutineScope {
        val clean = query.trim()
        if (clean.isEmpty()) return@coroutineScope SearchResults()
        val songsDeferred     = async { searchSongs(clean) }
        val albumsDeferred    = async { searchAlbums(clean) }
        val artistsDeferred   = async { searchArtists(clean) }
        val playlistsDeferred = async { searchPlaylists(clean) }
        SearchResults(
            songs     = songsDeferred.await(),
            albums    = albumsDeferred.await(),
            artists   = artistsDeferred.await(),
            playlists = playlistsDeferred.await()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-category search with Multi-Pass LIKE & Autocomplete Engine
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches JioSaavn for tracks matching the query using a multi-pass LIKE / Fuzzy matching engine.
     * Incorporates primary song search, autocomplete predictions, related album tracks,
     * and transliteration normalization so partials like "Lagan" match "Lagaan".
     */
    suspend fun searchSongs(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        val songsMap = LinkedHashMap<String, SongItem>()
        val normQuery = normalizeSearchTerm(clean)

        // Pass 1: Primary JioSaavn song search
        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val url = "$BASE_URL?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=30"
            val body = getJson(url)
            if (body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val song = parseSongJson(results.optJSONObject(i) ?: continue)
                        if (song != null) songsMap[song.id] = song
                    }
                }
            }
        } catch (_: Exception) {}

        // Pass 2: Autocomplete suggestions & related album drill-down
        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val autoUrl = "$BASE_URL?__call=autocomplete.get&query=$encodedQuery&_format=json&_marker=0&api_version=4"
            val body = getJson(autoUrl)
            if (body != null) {
                val json = JSONObject(body)
                val pidsToFetch = mutableListOf<String>()

                val topquery = json.optJSONObject("topquery")?.optJSONArray("data")
                if (topquery != null) {
                    for (i in 0 until topquery.length()) {
                        val item = topquery.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isNotEmpty() && !songsMap.containsKey(id)) pidsToFetch.add(id)
                    }
                }

                val autoSongs = json.optJSONObject("songs")?.optJSONArray("data")
                if (autoSongs != null) {
                    for (i in 0 until autoSongs.length()) {
                        val item = autoSongs.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        if (id.isNotEmpty() && !songsMap.containsKey(id)) pidsToFetch.add(id)
                    }
                }

                if (pidsToFetch.isNotEmpty()) {
                    val resolved = getSongsByIds(pidsToFetch)
                    for (s in resolved) songsMap[s.id] = s
                }

                // If autocomplete returns albums matching the query (e.g. "Lagaan" when typing "lagan"),
                // pull the tracks from that album!
                val autoAlbums = json.optJSONObject("albums")?.optJSONArray("data")
                if (autoAlbums != null && autoAlbums.length() > 0) {
                    for (i in 0 until minOf(2, autoAlbums.length())) {
                        val albObj = autoAlbums.optJSONObject(i) ?: continue
                        val albTitle = albObj.optString("title")
                        val albId = albObj.optString("id")
                        if (albId.isNotEmpty()) {
                            val normAlb = normalizeSearchTerm(albTitle)
                            if (normAlb.contains(normQuery) || normQuery.contains(normAlb) || albTitle.contains(clean, ignoreCase = true)) {
                                val albumSongs = getAlbumSongs(albId)
                                for (s in albumSongs) {
                                    if (!songsMap.containsKey(s.id)) songsMap[s.id] = s
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Pass 3: If still fewer than 5 songs, search albums directly and extract album tracks
        if (songsMap.size < 5) {
            try {
                val albums = searchAlbums(clean)
                for (alb in albums.take(2)) {
                    val normAlb = normalizeSearchTerm(alb.name)
                    if (normAlb.contains(normQuery) || normQuery.contains(normAlb) || alb.name.contains(clean, ignoreCase = true)) {
                        val albumSongs = getAlbumSongs(alb.id)
                        for (s in albumSongs) {
                            if (!songsMap.containsKey(s.id)) songsMap[s.id] = s
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Rank by LIKE match score
        songsMap.values.sortedByDescending { song ->
            val normTitle = normalizeSearchTerm(song.title)
            val normAlbum = normalizeSearchTerm(song.album)
            val normArtist = normalizeSearchTerm(song.artist)
            when {
                normTitle == normQuery -> 100
                normTitle.startsWith(normQuery) -> 85
                normTitle.contains(normQuery) || song.title.contains(clean, ignoreCase = true) -> 70
                normAlbum.contains(normQuery) || song.album.contains(clean, ignoreCase = true) -> 60
                normArtist.contains(normQuery) || song.artist.contains(clean, ignoreCase = true) -> 45
                else -> 10
            }
        }
    }

    /**
     * Searches JioSaavn for albums matching the query with LIKE & autocomplete matching.
     */
    suspend fun searchAlbums(query: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        val albumMap = LinkedHashMap<String, AlbumItem>()
        val normQuery = normalizeSearchTerm(clean)

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val url = "$BASE_URL?__call=search.getAlbumResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=20"
            val body = getJson(url)
            if (body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val album = parseAlbumJson(results.optJSONObject(i) ?: continue)
                        if (album != null) albumMap[album.id] = album
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val autoUrl = "$BASE_URL?__call=autocomplete.get&query=$encodedQuery&_format=json&_marker=0&api_version=4"
            val body = getJson(autoUrl)
            if (body != null) {
                val albums = JSONObject(body).optJSONObject("albums")?.optJSONArray("data")
                if (albums != null) {
                    for (i in 0 until albums.length()) {
                        val album = parseAlbumJson(albums.optJSONObject(i) ?: continue)
                        if (album != null && !albumMap.containsKey(album.id)) {
                            albumMap[album.id] = album
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        albumMap.values.sortedByDescending { album ->
            val normName = normalizeSearchTerm(album.name)
            val normArtist = normalizeSearchTerm(album.artist)
            when {
                normName == normQuery -> 100
                normName.startsWith(normQuery) -> 85
                normName.contains(normQuery) || album.name.contains(clean, ignoreCase = true) -> 70
                normArtist.contains(normQuery) || album.artist.contains(clean, ignoreCase = true) -> 50
                else -> 10
            }
        }
    }

    /**
     * Searches JioSaavn for artists matching the query with LIKE & autocomplete matching.
     */
    suspend fun searchArtists(query: String): List<ArtistItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        val artistMap = LinkedHashMap<String, ArtistItem>()
        val normQuery = normalizeSearchTerm(clean)

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val url = "$BASE_URL?__call=search.getArtistResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=15"
            val body = getJson(url)
            if (body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val artist = parseArtistJson(results.optJSONObject(i) ?: continue)
                        if (artist != null) artistMap[artist.id] = artist
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val autoUrl = "$BASE_URL?__call=autocomplete.get&query=$encodedQuery&_format=json&_marker=0&api_version=4"
            val body = getJson(autoUrl)
            if (body != null) {
                val artists = JSONObject(body).optJSONObject("artists")?.optJSONArray("data")
                if (artists != null) {
                    for (i in 0 until artists.length()) {
                        val artist = parseArtistJson(artists.optJSONObject(i) ?: continue)
                        if (artist != null && !artistMap.containsKey(artist.id)) {
                            artistMap[artist.id] = artist
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        artistMap.values.sortedByDescending { artist ->
            val normName = normalizeSearchTerm(artist.name)
            when {
                normName == normQuery -> 100
                normName.startsWith(normQuery) -> 85
                normName.contains(normQuery) || artist.name.contains(clean, ignoreCase = true) -> 70
                else -> 10
            }
        }
    }

    /**
     * Searches JioSaavn for playlists matching the query with LIKE & autocomplete matching.
     */
    suspend fun searchPlaylists(query: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        val playlistMap = LinkedHashMap<String, PlaylistItem>()
        val normQuery = normalizeSearchTerm(clean)

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val url = "$BASE_URL?__call=search.getPlaylistResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=15"
            val body = getJson(url)
            if (body != null) {
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val playlist = parsePlaylistJson(results.optJSONObject(i) ?: continue)
                        if (playlist != null) playlistMap[playlist.id] = playlist
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val encodedQuery = URLEncoder.encode(clean, "UTF-8")
            val autoUrl = "$BASE_URL?__call=autocomplete.get&query=$encodedQuery&_format=json&_marker=0&api_version=4"
            val body = getJson(autoUrl)
            if (body != null) {
                val playlists = JSONObject(body).optJSONObject("playlists")?.optJSONArray("data")
                if (playlists != null) {
                    for (i in 0 until playlists.length()) {
                        val playlist = parsePlaylistJson(playlists.optJSONObject(i) ?: continue)
                        if (playlist != null && !playlistMap.containsKey(playlist.id)) {
                            playlistMap[playlist.id] = playlist
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        playlistMap.values.sortedByDescending { playlist ->
            val normName = normalizeSearchTerm(playlist.name)
            when {
                normName == normQuery -> 100
                normName.startsWith(normQuery) -> 85
                normName.contains(normQuery) || playlist.name.contains(clean, ignoreCase = true) -> 70
                else -> 10
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drill-down: fetch songs from an album / artist / playlist
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all songs for a given album ID.
     */
    suspend fun getAlbumSongs(albumId: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        try {
            val url = "$BASE_URL?__call=content.getAlbumDetails&albumid=$albumId&_format=json&_marker=0&api_version=4"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            val list = json.optJSONArray("list") ?: json.optJSONArray("songs") ?: return@withContext songs
            for (i in 0 until list.length()) {
                val s = parseSongJson(list.optJSONObject(i) ?: continue)
                if (s != null) songs.add(s)
            }
        } catch (_: Exception) {}
        songs
    }

    /**
     * Fetches top songs for a given artist ID.
     */
    suspend fun getArtistSongs(artistId: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        try {
            val url = "$BASE_URL?__call=artist.getArtistPageDetails&artistId=$artistId&_format=json&_marker=0&api_version=4&n_song=30"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            val list = json.optJSONArray("topSongs") ?: json.optJSONArray("songs") ?: return@withContext songs
            for (i in 0 until list.length()) {
                val s = parseSongJson(list.optJSONObject(i) ?: continue)
                if (s != null) songs.add(s)
            }
        } catch (_: Exception) {}
        songs
    }

    /**
     * Fetches all songs inside a JioSaavn playlist.
     */
    suspend fun getPlaylistSongs(playlistId: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        try {
            val url = "$BASE_URL?__call=playlist.getDetails&listid=$playlistId&_format=json&_marker=0&api_version=4"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            val list = json.optJSONArray("list") ?: json.optJSONArray("songs") ?: return@withContext songs
            for (i in 0 until list.length()) {
                val s = parseSongJson(list.optJSONObject(i) ?: continue)
                if (s != null) songs.add(s)
            }
        } catch (_: Exception) {}
        songs
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trending
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches top trending / viral tracks currently charting on JioSaavn.
     */
    suspend fun getTrendingSongs(): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        val trendingPlaylistIds = listOf("82914609", "110858205", "51124653")

        for (listId in trendingPlaylistIds) {
            try {
                val fetched = getPlaylistSongs(listId)
                if (fetched.isNotEmpty()) {
                    songs.addAll(fetched)
                    break
                }
            } catch (_: Exception) {}
        }

        if (songs.isEmpty()) {
            val fallback = searchSongs("Top Hits Songs")
            if (fallback.isNotEmpty()) return@withContext fallback
            return@withContext getCuratedDefaultSongs()
        }

        songs
    }

    /**
     * Curated high-fidelity 320 kbps tracks available offline / instant launch for each language.
     */
    fun getCuratedSongsForLanguage(language: MusicLanguage): List<SongItem> {
        return when (language) {
            MusicLanguage.MALAYALAM -> listOf(
                SongItem(
                    id = "mal_1",
                    title = "Illuminati",
                    artist = "Sushin Shyam, Dabzee",
                    album = "Aavesham",
                    durationSeconds = 193L,
                    highResArtworkUrl = "https://c.saavncdn.com/974/Aavesham-Malayalam-2024-20240419131015-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/974/c776c5db6175e11bbcdadcb6c52a0a33_320.mp4"
                ),
                SongItem(
                    id = "mal_2",
                    title = "Jaada",
                    artist = "Sushin Shyam, Sreenath Bhasi",
                    album = "Aavesham",
                    durationSeconds = 212L,
                    highResArtworkUrl = "https://c.saavncdn.com/974/Aavesham-Malayalam-2024-20240419131015-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/974/c776c5db6175e11bbcdadcb6c52a0a33_320.mp4"
                )
            )
            MusicLanguage.TAMIL -> listOf(
                SongItem(
                    id = "tam_1",
                    title = "Hukum - Thalaivar Alappara",
                    artist = "Anirudh Ravichander, Super Subu",
                    album = "Jailer",
                    durationSeconds = 207L,
                    highResArtworkUrl = "https://c.saavncdn.com/131/Jailer-Tamil-2023-20230728190800-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/131/a160868f0cb184ba33e387be5896a7ef_320.mp4"
                ),
                SongItem(
                    id = "tam_2",
                    title = "Badass",
                    artist = "Anirudh Ravichander",
                    album = "Leo",
                    durationSeconds = 229L,
                    highResArtworkUrl = "https://c.saavncdn.com/014/Leo-Tamil-2023-20231020104612-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/014/19f123f15ba3cbe31122a28189c46ce1_320.mp4"
                )
            )
            MusicLanguage.ENGLISH, MusicLanguage.INTERNATIONAL -> listOf(
                SongItem(
                    id = "eng_1",
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    album = "After Hours",
                    durationSeconds = 200L,
                    highResArtworkUrl = "https://c.saavncdn.com/267/After-Hours-English-2020-20200320001002-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/267/d051a8eb7429188e9bbce75109b81f1e_320.mp4"
                ),
                SongItem(
                    id = "eng_2",
                    title = "Shape of You",
                    artist = "Ed Sheeran",
                    album = "Divide",
                    durationSeconds = 233L,
                    highResArtworkUrl = "https://c.saavncdn.com/255/Shape-of-You-English-2017-500x500.jpg",
                    directStreamUrl = "https://aac.saavncdn.com/255/c2febd353f3a076a406fa37510f31f9f_320.mp4"
                )
            )
            else -> getCuratedDefaultSongs()
        }
    }

    /**
     * Curated high-fidelity 320 kbps tracks available offline / instant launch.
     */
    fun getCuratedDefaultSongs(): List<SongItem> {
        return listOf(
            SongItem(
                id = "curated_1",
                title = "Kesariya",
                artist = "Arijit Singh, Pritam",
                album = "Brahmastra",
                durationSeconds = 268L,
                highResArtworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                directStreamUrl = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4"
            ),
            SongItem(
                id = "curated_2",
                title = "Chaleya",
                artist = "Arijit Singh, Shilpa Rao, Anirudh",
                album = "Jawan",
                durationSeconds = 200L,
                highResArtworkUrl = "https://c.saavncdn.com/026/Chaleya-From-Jawan-Hindi-2023-20230814014337-500x500.jpg",
                directStreamUrl = "https://aac.saavncdn.com/026/0263673cfebfe4aa5aa9d2c67f5cf40c_320.mp4"
            ),
            SongItem(
                id = "curated_3",
                title = "Heeriye",
                artist = "Jasleen Royal, Arijit Singh",
                album = "Heeriye",
                durationSeconds = 194L,
                highResArtworkUrl = "https://c.saavncdn.com/022/Heeriye-feat-Arijit-Singh-Hindi-2023-20230724115112-500x500.jpg",
                directStreamUrl = "https://aac.saavncdn.com/022/272f534882df82e66f8e7b9e38e12d4d_320.mp4"
            ),
            SongItem(
                id = "curated_4",
                title = "Apna Bana Le",
                artist = "Arijit Singh, Sachin-Jigar",
                album = "Bhediya",
                durationSeconds = 261L,
                highResArtworkUrl = "https://c.saavncdn.com/815/Bhediya-Hindi-2022-20221124110332-500x500.jpg",
                directStreamUrl = "https://aac.saavncdn.com/815/d40ecb4bb2e6d622b3f179faef51593c_320.mp4"
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsers
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseSongJson(obj: JSONObject): SongItem? {
        val moreInfo = obj.optJSONObject("more_info")

        val id = obj.optString("id").ifEmpty {
            obj.optString("song_id").ifEmpty {
                moreInfo?.optString("id") ?: ""
            }
        }
        if (id.isEmpty()) return null

        val rawTitle = obj.optString("title").ifEmpty {
            obj.optString("song").ifEmpty {
                moreInfo?.optString("song") ?: ""
            }
        }
        val title = unescapeHtml(rawTitle).ifEmpty { "Unknown Track" }

        val rawArtist = moreInfo?.optString("primary_artists")?.ifEmpty {
            moreInfo.optString("singers").ifEmpty {
                obj.optString("primary_artists").ifEmpty {
                    obj.optString("singers").ifEmpty {
                        obj.optString("artist")
                    }
                }
            }
        } ?: obj.optString("artist")
        val artist = unescapeHtml(rawArtist).ifEmpty { "JioSaavn Artist" }

        val rawAlbum = moreInfo?.optString("album") ?: obj.optString("album")
        val album = unescapeHtml(rawAlbum)

        val duration = moreInfo?.optLong("duration") ?: obj.optLong("duration", 180L)

        val rawImage = obj.optString("image").ifEmpty {
            obj.optString("artwork").ifEmpty {
                moreInfo?.optString("image") ?: ""
            }
        }
        val artwork = MediaUrlResolver.upgradeArtworkUrl(rawImage)

        val encryptedMediaUrl = moreInfo?.optString("encrypted_media_url") ?: obj.optString("encrypted_media_url")
        val mediaPreviewUrl = moreInfo?.optString("media_preview_url") ?: obj.optString("media_preview_url")

        val streamUrl = MediaUrlResolver.resolve320KbpsStreamUrl(encryptedMediaUrl, mediaPreviewUrl)
        if (streamUrl.isEmpty()) return null

        return SongItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            highResArtworkUrl = artwork,
            encryptedMediaUrl = encryptedMediaUrl,
            mediaPreviewUrl = mediaPreviewUrl,
            directStreamUrl = streamUrl
        )
    }

    private fun parseAlbumJson(obj: JSONObject): AlbumItem? {
        val moreInfo = obj.optJSONObject("more_info")
        val id = obj.optString("id").ifEmpty { return null }
        val name = unescapeHtml(obj.optString("title").ifEmpty { obj.optString("album_id") }).ifEmpty { return null }
        val year = moreInfo?.optString("year") ?: obj.optString("year")
        val rawArtist = moreInfo?.optString("primary_artists")
            ?: moreInfo?.optString("music")
            ?: obj.optString("primary_artists")
            ?: obj.optString("music")
        val artist = unescapeHtml(rawArtist)
        val rawImage = obj.optString("image").ifEmpty { moreInfo?.optString("image") ?: "" }
        val artwork = MediaUrlResolver.upgradeArtworkUrl(rawImage)
        val songCount = moreInfo?.optInt("song_count") ?: obj.optInt("song_count", 0)
        return AlbumItem(id = id, name = name, year = year, artist = artist, artworkUrl = artwork, songCount = songCount)
    }

    private fun parseArtistJson(obj: JSONObject): ArtistItem? {
        val id = obj.optString("id").ifEmpty { obj.optString("artistid") }.ifEmpty { return null }
        val name = unescapeHtml(obj.optString("title").ifEmpty { obj.optString("name") }).ifEmpty { return null }
        val rawImage = obj.optString("image").ifEmpty { "" }
        val artwork = MediaUrlResolver.upgradeArtworkUrl(rawImage)
        val followers = obj.optJSONObject("more_info")?.optString("follower_count") ?: ""
        return ArtistItem(id = id, name = name, artworkUrl = artwork, followerCount = followers)
    }

    private fun parsePlaylistJson(obj: JSONObject): PlaylistItem? {
        val moreInfo = obj.optJSONObject("more_info")
        val id = obj.optString("id").ifEmpty { return null }
        val name = unescapeHtml(obj.optString("title")).ifEmpty { return null }
        val rawImage = obj.optString("image").ifEmpty { moreInfo?.optString("image") ?: "" }
        val artwork = MediaUrlResolver.upgradeArtworkUrl(rawImage)
        val songCount = moreInfo?.optInt("song_count") ?: obj.optInt("song_count", 0)
        val followers = moreInfo?.optString("follower_count") ?: obj.optString("follower_count")
        return PlaylistItem(id = id, name = name, artworkUrl = artwork, songCount = songCount, followerCount = followers)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun getJson(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", BROWSER_UA)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (_: Exception) { null }
    }

    private fun unescapeHtml(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }
}
