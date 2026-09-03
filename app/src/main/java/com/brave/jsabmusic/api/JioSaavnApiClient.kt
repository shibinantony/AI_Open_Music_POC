package com.brave.jsabmusic.api

import com.brave.jsabmusic.api.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * High-performance, asynchronous REST Client communicating directly with JioSaavn's JSON API.
 * Bypasses webview barriers, fetches track metadata, and resolves direct 320 kbps CDN links.
 */
object JioSaavnApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://www.jiosaavn.com/api.php"

    /**
     * Searches JioSaavn for tracks matching the query and resolves 320 kbps stream links.
     */
    suspend fun searchSongs(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()
        if (query.trim().isEmpty()) return@withContext songs

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE_URL?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&p=1&n=25"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext songs
                val body = response.body?.string() ?: return@withContext songs

                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext songs

                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val song = parseSongJson(obj)
                    if (song != null) {
                        songs.add(song)
                    }
                }
            }
        } catch (e: Exception) {
            // Log notice and return whatever was parsed
        }
        return@withContext songs
    }

    /**
     * Fetches top trending / viral tracks currently charting on JioSaavn.
     */
    suspend fun getTrendingSongs(): List<SongItem> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongItem>()

        // Playlist 82914609 is JioSaavn's official "Trending Today" Top Chart
        val trendingPlaylistIds = listOf("82914609", "110858205", "51124653")

        for (listId in trendingPlaylistIds) {
            try {
                val url = "$BASE_URL?__call=playlist.getDetails&listid=$listId&_format=json&_marker=0&api_version=4"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            val list = json.optJSONArray("list") ?: json.optJSONArray("songs")
                            if (list != null && list.length() > 0) {
                                for (i in 0 until list.length()) {
                                    val song = parseSongJson(list.getJSONObject(i))
                                    if (song != null) {
                                        songs.add(song)
                                    }
                                }
                            }
                        }
                    }
                }

                if (songs.isNotEmpty()) {
                    break // Successfully obtained trending songs
                }
            } catch (e: Exception) {
                // Try fallback playlist
            }
        }

        // Fallback: search for top hits if playlist was unreachable
        if (songs.isEmpty()) {
            return@withContext searchSongs("Top Hindi Songs 2026")
        }

        return@withContext songs
    }

    private fun parseSongJson(obj: JSONObject): SongItem? {
        val id = obj.optString("id").ifEmpty { obj.optString("song_id") }
        if (id.isEmpty()) return null

        val rawTitle = obj.optString("song").ifEmpty { obj.optString("title") }
        val title = unescapeHtml(rawTitle)

        val rawArtist = obj.optString("primary_artists").ifEmpty {
            obj.optString("singers").ifEmpty { obj.optString("artist") }
        }
        val artist = unescapeHtml(rawArtist).ifEmpty { "JioSaavn Artist" }

        val album = unescapeHtml(obj.optString("album"))
        val duration = obj.optLong("duration", 180L)

        val rawImage = obj.optString("image").ifEmpty { obj.optString("artwork") }
        val artwork = MediaUrlResolver.upgradeArtworkUrl(rawImage)

        val encryptedMediaUrl = obj.optString("encrypted_media_url")
        val mediaPreviewUrl = obj.optString("media_preview_url")

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
