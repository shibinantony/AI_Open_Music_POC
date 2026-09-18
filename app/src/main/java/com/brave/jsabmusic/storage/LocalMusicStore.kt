package com.brave.jsabmusic.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.brave.jsabmusic.api.model.SongItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Robust local persistence store for JSABMusic.
 * Ensures liked songs, 7-day listening history, user profile, and last playback session
 * operate 100% reliably on-device offline and sync bidirectionally with Cloud Firestore.
 */
class LocalMusicStore(context: Context) {

    private val tag = "LocalMusicStore"
    private val prefs: SharedPreferences = context.getSharedPreferences("jsab_local_music_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LIKED_SONGS = "key_liked_songs"
        private const val KEY_HISTORY = "key_listening_history"
        private const val KEY_USER_UID = "key_user_uid"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHOTO = "key_user_photo"
        private const val KEY_IS_GUEST = "key_is_guest"
        private const val KEY_LAST_SONG = "key_last_song"
        private const val KEY_LAST_QUEUE = "key_last_queue"
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L
    }

    private val cachedLiked = mutableListOf<SongItem>()
    private val cachedHistory = mutableListOf<Pair<Long, SongItem>>()

    init {
        loadLikedFromDisk()
        loadHistoryFromDisk()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Liked Songs
    // ─────────────────────────────────────────────────────────────────────────

    @Synchronized
    fun getLikedSongs(): List<SongItem> {
        return cachedLiked.toList()
    }

    @Synchronized
    fun isLiked(songId: String): Boolean {
        return cachedLiked.any { it.id == songId }
    }

    @Synchronized
    fun toggleLike(song: SongItem): Boolean {
        val existingIndex = cachedLiked.indexOfFirst { it.id == song.id }
        val nowLiked = if (existingIndex >= 0) {
            cachedLiked.removeAt(existingIndex)
            false
        } else {
            cachedLiked.add(0, song)
            true
        }
        saveLikedToDisk()
        return nowLiked
    }

    @Synchronized
    fun setLikedSongs(songs: List<SongItem>) {
        cachedLiked.clear()
        val seen = mutableSetOf<String>()
        for (song in songs) {
            if (seen.add(song.id)) {
                cachedLiked.add(song)
            }
        }
        saveLikedToDisk()
    }

    private fun loadLikedFromDisk() {
        val raw = prefs.getString(KEY_LIKED_SONGS, null) ?: return
        try {
            val array = JSONArray(raw)
            cachedLiked.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                jsonToSong(obj)?.let { cachedLiked.add(it) }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load liked songs from disk", e)
        }
    }

    private fun saveLikedToDisk() {
        try {
            val array = JSONArray()
            for (song in cachedLiked) {
                array.put(songToJson(song))
            }
            prefs.edit().putString(KEY_LIKED_SONGS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save liked songs to disk", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7-Day Listening History
    // ─────────────────────────────────────────────────────────────────────────

    @Synchronized
    fun getRecentlyListened(): List<SongItem> {
        pruneOldHistory()
        val distinctSongs = mutableListOf<SongItem>()
        val seenIds = mutableSetOf<String>()
        for ((_, song) in cachedHistory) {
            if (seenIds.add(song.id)) {
                distinctSongs.add(song)
            }
        }
        return distinctSongs
    }

    @Synchronized
    fun recordSongPlayed(song: SongItem) {
        val now = System.currentTimeMillis()
        cachedHistory.removeAll { it.second.id == song.id }
        cachedHistory.add(0, Pair(now, song))
        pruneOldHistory()
        saveHistoryToDisk()
    }

    @Synchronized
    fun setHistorySongs(songs: List<SongItem>) {
        val now = System.currentTimeMillis()
        cachedHistory.clear()
        val seenIds = mutableSetOf<String>()
        var offset = 0L
        for (song in songs) {
            if (seenIds.add(song.id)) {
                cachedHistory.add(Pair(now - offset, song))
                offset += 60000L
            }
        }
        saveHistoryToDisk()
    }

    private fun pruneOldHistory() {
        val threshold = System.currentTimeMillis() - SEVEN_DAYS_MS
        cachedHistory.removeAll { it.first < threshold }
        if (cachedHistory.size > 100) {
            while (cachedHistory.size > 100) {
                cachedHistory.removeAt(cachedHistory.size - 1)
            }
        }
    }

    private fun loadHistoryFromDisk() {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return
        try {
            val array = JSONArray(raw)
            cachedHistory.clear()
            for (i in 0 until array.length()) {
                val itemObj = array.getJSONObject(i)
                val playedAt = itemObj.optLong("playedAt", System.currentTimeMillis())
                val songObj = itemObj.optJSONObject("song")
                if (songObj != null) {
                    jsonToSong(songObj)?.let { cachedHistory.add(Pair(playedAt, it)) }
                }
            }
            pruneOldHistory()
        } catch (e: Exception) {
            Log.e(tag, "Failed to load history from disk", e)
        }
    }

    private fun saveHistoryToDisk() {
        try {
            val array = JSONArray()
            for ((playedAt, song) in cachedHistory) {
                val itemObj = JSONObject().apply {
                    put("playedAt", playedAt)
                    put("song", songToJson(song))
                }
                array.put(itemObj)
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save history to disk", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User Profile
    // ─────────────────────────────────────────────────────────────────────────

    fun getOrCreateUserId(): String {
        var uid = prefs.getString(KEY_USER_UID, null)
        if (uid.isNullOrEmpty()) {
            uid = "user_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_USER_UID, uid).apply()
        }
        return uid
    }

    fun saveUserProfile(uid: String, name: String?, email: String?, photoUrl: String?, isGuest: Boolean) {
        prefs.edit()
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHOTO, photoUrl)
            .putBoolean(KEY_IS_GUEST, isGuest)
            .apply()
    }

    fun getUserProfile(): LocalUserProfile {
        val uid = getOrCreateUserId()
        val name = prefs.getString(KEY_USER_NAME, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val photo = prefs.getString(KEY_USER_PHOTO, null)
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, true)
        return LocalUserProfile(uid, name, email, photo, isGuest)
    }

    fun clearUserProfile() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .putBoolean(KEY_IS_GUEST, true)
            .apply()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Last Session Restoration
    // ─────────────────────────────────────────────────────────────────────────

    fun saveLastSession(song: SongItem, queue: List<SongItem>) {
        try {
            val songJson = songToJson(song).toString()
            val queueArray = JSONArray()
            for (q in queue.take(30)) {
                queueArray.put(songToJson(q))
            }
            prefs.edit()
                .putString(KEY_LAST_SONG, songJson)
                .putString(KEY_LAST_QUEUE, queueArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save session to disk", e)
        }
    }

    fun restoreLastSession(): Pair<SongItem, List<SongItem>>? {
        val songRaw = prefs.getString(KEY_LAST_SONG, null) ?: return null
        val queueRaw = prefs.getString(KEY_LAST_QUEUE, null) ?: return null
        return try {
            val song = jsonToSong(JSONObject(songRaw)) ?: return null
            val queueArray = JSONArray(queueRaw)
            val queue = mutableListOf<SongItem>()
            for (i in 0 until queueArray.length()) {
                jsonToSong(queueArray.getJSONObject(i))?.let { queue.add(it) }
            }
            if (queue.isNotEmpty()) Pair(song, queue) else null
        } catch (e: Exception) {
            Log.e(tag, "Failed to restore session from disk", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun songToJson(song: SongItem): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationSeconds", song.durationSeconds)
            put("highResArtworkUrl", song.highResArtworkUrl)
            put("encryptedMediaUrl", song.encryptedMediaUrl)
            put("mediaPreviewUrl", song.mediaPreviewUrl)
            put("directStreamUrl", song.directStreamUrl)
        }
    }

    fun jsonToSong(obj: JSONObject?): SongItem? {
        if (obj == null) return null
        val id = obj.optString("id").ifEmpty { return null }
        return SongItem(
            id = id,
            title = obj.optString("title", "Unknown Track"),
            artist = obj.optString("artist", "Unknown Artist"),
            album = obj.optString("album", ""),
            durationSeconds = obj.optLong("durationSeconds", 180L),
            highResArtworkUrl = obj.optString("highResArtworkUrl", ""),
            encryptedMediaUrl = obj.optString("encryptedMediaUrl", ""),
            mediaPreviewUrl = obj.optString("mediaPreviewUrl", ""),
            directStreamUrl = obj.optString("directStreamUrl", "")
        )
    }
}

data class LocalUserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isGuest: Boolean
)
