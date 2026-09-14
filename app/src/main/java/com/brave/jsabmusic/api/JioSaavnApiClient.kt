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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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
    // Parallel search — all four categories at once
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fires all four search endpoints in parallel and returns an aggregate [SearchResults].
     */
    suspend fun searchAll(query: String): SearchResults = coroutineScope {
        if (query.trim().isEmpty()) return@coroutineScope SearchResults()
        val songsDeferred     = async { searchSongs(query) }
        val albumsDeferred    = async { searchAlbums(query) }
        val artistsDeferred   = async { searchArtists(query) }
        val playlistsDeferred = async { searchPlaylists(query) }
        SearchResults(
            songs     = songsDeferred.await(),
            albums    = albumsDeferred.await(),
            artists   = artistsDeferred.await(),
            playlists = playlistsDeferred.await()
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-category search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches JioSaavn for tracks matching the query and resolves 320 kbps stream links.
     */
    suspend fun searchSongs(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        if (query.trim().isEmpty()) return@withContext songs
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE_URL?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=25"
            val body = getJson(url) ?: return@withContext songs
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext songs
            for (i in 0 until results.length()) {
                val song = parseSongJson(results.optJSONObject(i) ?: continue)
                if (song != null) songs.add(song)
            }
        } catch (_: Exception) {}
        songs
    }

    /**
     * Searches JioSaavn for albums matching the query.
     */
    suspend fun searchAlbums(query: String): List<AlbumItem> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<AlbumItem>()
        if (query.trim().isEmpty()) return@withContext albums
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE_URL?__call=search.getAlbumResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=15"
            val body = getJson(url) ?: return@withContext albums
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext albums
            for (i in 0 until results.length()) {
                val album = parseAlbumJson(results.optJSONObject(i) ?: continue)
                if (album != null) albums.add(album)
            }
        } catch (_: Exception) {}
        albums
    }

    /**
     * Searches JioSaavn for artists matching the query.
     */
    suspend fun searchArtists(query: String): List<ArtistItem> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<ArtistItem>()
        if (query.trim().isEmpty()) return@withContext artists
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
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

    /**
     * Searches JioSaavn for playlists matching the query.
     */
    suspend fun searchPlaylists(query: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<PlaylistItem>()
        if (query.trim().isEmpty()) return@withContext playlists
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE_URL?__call=search.getPlaylistResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=15"
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
                val song = parseSongJson(list.optJSONObject(i) ?: continue)
                if (song != null) songs.add(song)
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
            val url = "$BASE_URL?__call=artist.getArtistPageDetails&artistId=$artistId&_format=json&_marker=0&api_version=4&page=0&category=latest&sort_order=desc&includeMetaTags=0"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            // Top songs live inside topSongs array
            val topSongsObj = json.optJSONObject("topSongs")
            val list = topSongsObj?.optJSONArray("songs")
                ?: json.optJSONArray("topSongs")
                ?: json.optJSONArray("songs")
                ?: return@withContext songs
            for (i in 0 until list.length()) {
                val song = parseSongJson(list.optJSONObject(i) ?: continue)
                if (song != null) songs.add(song)
            }
        } catch (_: Exception) {}
        songs
    }

    /**
     * Fetches all songs for a given playlist ID (list ID).
     */
    suspend fun getPlaylistSongs(listId: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        try {
            val url = "$BASE_URL?__call=playlist.getDetails&listid=$listId&_format=json&_marker=0&api_version=4"
            val body = getJson(url) ?: return@withContext songs
            val json = JSONObject(body)
            val list = json.optJSONArray("list")
                ?: json.optJSONArray("songs")
                ?: json.optJSONObject("more_info")?.optJSONArray("songs")
                ?: return@withContext songs
            for (i in 0 until list.length()) {
                val song = parseSongJson(list.optJSONObject(i) ?: continue)
                if (song != null) songs.add(song)
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

        // Official JioSaavn Top Chart Playlists: Trending Today, Weekly Top 20, Hindi Hitlist
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

        // Fallback: search for top hits if playlist endpoint had transient issue
        if (songs.isEmpty()) {
            val fallback = searchSongs("Top Hindi Songs")
            if (fallback.isNotEmpty()) return@withContext fallback
            return@withContext getCuratedDefaultSongs()
        }

        songs
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
