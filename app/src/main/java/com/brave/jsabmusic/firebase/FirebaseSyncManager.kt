package com.brave.jsabmusic.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.brave.jsabmusic.api.model.SongItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Unified user model representing active session credentials.
 */
data class SovereignUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

/**
 * Enterprise Cloud Persistence and Authentication Manager for JSABMusic.
 * Orchestrates Google Sign-In, Firebase Auth, Real-Time Cloud Firestore Sync,
 * and robust Local Persistence (SharedPreferences) for Liked Music, 7-Day History, and Session Restoration.
 */
class FirebaseSyncManager(private val context: Context) {

    private val tag = "FirebaseSyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // SharedPreferences for local persistence (replaces LocalMusicStore)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jsab_local_store", Context.MODE_PRIVATE)

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<SovereignUser?>(null)
    val currentUser: StateFlow<SovereignUser?> = _currentUser.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<SongItem>>(getLikedSongs())
    val likedSongs: StateFlow<List<SongItem>> = _likedSongs.asStateFlow()

    private val _recentlyListened = MutableStateFlow<List<SongItem>>(getRecentlyListened())
    val recentlyListened: StateFlow<List<SongItem>> = _recentlyListened.asStateFlow()

    private var likedListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    init {
        initLocalSession()
        initializeFirebase()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Local Persistence (SharedPreferences — replaces LocalMusicStore)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getOrCreateUserId(): String {
        var uid = prefs.getString("user_uid", null)
        if (uid == null) {
            uid = UUID.randomUUID().toString()
            prefs.edit().putString("user_uid", uid).apply()
        }
        return uid
    }

    private fun saveUserProfile(uid: String, displayName: String?, email: String?, photoUrl: String?, isGuest: Boolean) {
        prefs.edit().apply {
            putString("user_uid", uid)
            putString("user_display_name", displayName)
            putString("user_email", email)
            putString("user_photo_url", photoUrl)
            putBoolean("user_is_guest", isGuest)
            apply()
        }
    }

    private fun clearUserProfile() {
        prefs.edit().apply {
            remove("user_display_name")
            remove("user_email")
            remove("user_photo_url")
            remove("user_is_guest")
            apply()
        }
    }

    private data class LocalUserProfile(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
        val isGuest: Boolean
    )

    private fun getUserProfile(): LocalUserProfile {
        val uid = getOrCreateUserId()
        return LocalUserProfile(
            uid = uid,
            displayName = prefs.getString("user_display_name", null),
            email = prefs.getString("user_email", null),
            photoUrl = prefs.getString("user_photo_url", null),
            isGuest = prefs.getBoolean("user_is_guest", true)
        )
    }

    private fun getLikedSongs(): List<SongItem> {
        return try {
            val json = prefs.getString("liked_songs", null) ?: return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { parseSongFromJson(array.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun setLikedSongs(songs: List<SongItem>) {
        try {
            val array = JSONArray()
            songs.forEach { array.put(songToJson(it)) }
            prefs.edit().putString("liked_songs", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun isLiked(songId: String): Boolean = getLikedSongs().any { it.id == songId }

    fun toggleLike(song: SongItem) {
        val current = getLikedSongs().toMutableList()
        val existing = current.find { it.id == song.id }
        if (existing != null) current.remove(existing) else current.add(0, song)
        setLikedSongs(current)
        _likedSongs.value = getLikedSongs()

        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                val docRef = db.collection("users").document(uid).collection("liked_songs").document(song.id)
                if (existing != null) {
                    docRef.delete().await()
                } else {
                    docRef.set(songToMap(song).apply { put("likedAt", System.currentTimeMillis()) }).await()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error updating liked status in Firestore", e)
            }
        }
    }

    private fun getRecentlyListened(): List<SongItem> {
        return try {
            val json = prefs.getString("history_songs", null) ?: return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { parseSongFromJson(array.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    private fun setHistorySongs(songs: List<SongItem>) {
        try {
            val array = JSONArray()
            songs.forEach { array.put(songToJson(it)) }
            prefs.edit().putString("history_songs", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun recordSongPlayed(song: SongItem) {
        val current = getRecentlyListened().toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        // Keep only 7 days worth (max 200 entries)
        val pruned = current.take(200)
        setHistorySongs(pruned)
        _recentlyListened.value = pruned

        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                db.collection("users").document(uid).collection("history").document(song.id)
                    .set(songToMap(song).apply { put("playedAt", System.currentTimeMillis()) }).await()
            } catch (e: Exception) {
                Log.e(tag, "Failed to record played song in history", e)
            }
        }
    }

    fun saveLastSession(song: SongItem, queue: List<SongItem>) {
        try {
            val sessionJson = JSONObject().apply {
                put("last_song", songToJson(song))
                put("queue", JSONArray().also { arr -> queue.take(30).forEach { arr.put(songToJson(it)) } })
            }
            prefs.edit().putString("last_session", sessionJson.toString()).apply()
        } catch (_: Exception) {}

        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                val sessionMap = hashMapOf(
                    "lastSong" to songToMap(song),
                    "queue" to queue.take(30).map { songToMap(it) },
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).collection("session").document("last_played")
                    .set(sessionMap).await()
            } catch (e: Exception) {
                Log.e(tag, "Error saving last session to Firestore", e)
            }
        }
    }

    suspend fun restoreLastSession(): Pair<SongItem, List<SongItem>>? {
        // Try local first
        try {
            val json = prefs.getString("last_session", null)
            if (json != null) {
                val obj = JSONObject(json)
                val song = parseSongFromJson(obj.optJSONObject("last_song"))
                val queueArr = obj.optJSONArray("queue")
                val queue = if (queueArr != null) {
                    (0 until queueArr.length()).mapNotNull { parseSongFromJson(queueArr.getJSONObject(it)) }
                } else emptyList()
                if (song != null && queue.isNotEmpty()) return Pair(song, queue)
            }
        } catch (_: Exception) {}

        // Try Firestore
        val uid = _currentUser.value?.uid ?: return null
        val db = firestore ?: return null
        return try {
            val doc = db.collection("users").document(uid).collection("session").document("last_played").get().await()
            if (doc.exists()) {
                val lastSong = parseSongItem(doc.get("lastSong") as? Map<*, *>)
                val queueList = doc.get("queue") as? List<*>
                val queue = queueList?.mapNotNull { (it as? Map<*, *>)?.let { m -> parseSongItem(m) } } ?: emptyList()
                if (lastSong != null && queue.isNotEmpty()) Pair(lastSong, queue) else null
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Error restoring session from Firestore", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Firebase Auth & Firestore
    // ─────────────────────────────────────────────────────────────────────────

    private fun initLocalSession() {
        val profile = getUserProfile()
        val name = profile.displayName ?: if (profile.isGuest) "Sovereign Guest" else "Sovereign Listener"
        _currentUser.value = SovereignUser(
            uid = profile.uid,
            displayName = name,
            email = profile.email,
            photoUrl = profile.photoUrl,
            isAnonymous = profile.isGuest
        )
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            auth?.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    val sovUser = SovereignUser(
                        uid = fbUser.uid,
                        displayName = fbUser.displayName?.ifEmpty { null } ?: "Sovereign Listener",
                        email = fbUser.email,
                        photoUrl = fbUser.photoUrl?.toString(),
                        isAnonymous = fbUser.isAnonymous
                    )
                    _currentUser.value = sovUser
                    saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, sovUser.isAnonymous)
                    syncAllToCloud(fbUser.uid)
                    attachFirestoreListeners(fbUser.uid)
                } else {
                    auth?.signInAnonymously()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            task.result?.user?.let { guestUser ->
                                val guestSov = SovereignUser(
                                    uid = guestUser.uid,
                                    displayName = "Sovereign Guest",
                                    email = null,
                                    photoUrl = null,
                                    isAnonymous = true
                                )
                                _currentUser.value = guestSov
                                saveUserProfile(guestSov.uid, guestSov.displayName, null, null, true)
                                syncAllToCloud(guestUser.uid)
                                attachFirestoreListeners(guestUser.uid)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase initialization error: ${e.message}")
        }
    }

    fun syncAllToCloud(uid: String) {
        val db = firestore ?: return
        scope.launch {
            try {
                val user = _currentUser.value
                val profileMap = hashMapOf(
                    "uid" to uid,
                    "displayName" to (user?.displayName ?: "Sovereign Listener"),
                    "email" to (user?.email ?: ""),
                    "photoUrl" to (user?.photoUrl ?: ""),
                    "isAnonymous" to (user?.isAnonymous ?: true),
                    "lastActive" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(profileMap, SetOptions.merge()).await()

                for (song in getLikedSongs()) {
                    db.collection("users").document(uid).collection("liked_songs").document(song.id)
                        .set(songToMap(song).apply { put("likedAt", System.currentTimeMillis()) })
                }
                for (song in getRecentlyListened()) {
                    db.collection("users").document(uid).collection("history").document(song.id)
                        .set(songToMap(song).apply { put("playedAt", System.currentTimeMillis()) })
                }
            } catch (e: Exception) {
                Log.e(tag, "Error syncing data to cloud: ${e.message}")
            }
        }
    }

    private fun attachFirestoreListeners(uid: String) {
        val db = firestore ?: return

        likedListener?.remove()
        try {
            likedListener = db.collection("users").document(uid).collection("liked_songs")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { Log.e(tag, "Liked songs listener error: ${error.message}"); return@addSnapshotListener }
                    val remoteSongs = snapshot?.documents?.mapNotNull { parseSongItem(it.data) } ?: emptyList()
                    if (remoteSongs.isNotEmpty()) {
                        setLikedSongs(remoteSongs)
                        _likedSongs.value = getLikedSongs()
                    }
                }
        } catch (e: Exception) { Log.e(tag, "Failed to attach liked songs listener", e) }

        historyListener?.remove()
        try {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
            historyListener = db.collection("users").document(uid).collection("history")
                .whereGreaterThanOrEqualTo("playedAt", sevenDaysAgo)
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { Log.e(tag, "History listener error: ${error.message}"); return@addSnapshotListener }
                    val remoteSongs = snapshot?.documents?.mapNotNull { parseSongItem(it.data) } ?: emptyList()
                    if (remoteSongs.isNotEmpty()) {
                        setHistorySongs(remoteSongs)
                        _recentlyListened.value = getRecentlyListened()
                    }
                }
        } catch (e: Exception) { Log.e(tag, "Failed to attach history listener", e) }
    }

    fun signInWithProfile(displayName: String, email: String, photoUrl: String? = null) {
        val uid = _currentUser.value?.uid ?: getOrCreateUserId()
        val sovUser = SovereignUser(uid = uid, displayName = displayName, email = email, photoUrl = photoUrl, isAnonymous = false)
        _currentUser.value = sovUser
        saveUserProfile(uid, displayName, email, photoUrl, false)
        syncAllToCloud(uid)
        attachFirestoreListeners(uid)
    }

    fun signInWithGoogle(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        val authInstance = auth ?: run { onComplete(false, "Auth service not initialized"); return }

        val current = authInstance.currentUser
        if (current != null && current.isAnonymous) {
            current.linkWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val sovUser = SovereignUser(
                        uid = user?.uid ?: getOrCreateUserId(),
                        displayName = user?.displayName ?: "Google Listener",
                        email = user?.email, photoUrl = user?.photoUrl?.toString(), isAnonymous = false
                    )
                    _currentUser.value = sovUser
                    saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                    syncAllToCloud(sovUser.uid); attachFirestoreListeners(sovUser.uid); onComplete(true, null)
                } else {
                    authInstance.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = signInTask.result?.user
                            val sovUser = SovereignUser(
                                uid = user?.uid ?: getOrCreateUserId(),
                                displayName = user?.displayName ?: "Google Listener",
                                email = user?.email, photoUrl = user?.photoUrl?.toString(), isAnonymous = false
                            )
                            _currentUser.value = sovUser
                            saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                            syncAllToCloud(sovUser.uid); attachFirestoreListeners(sovUser.uid); onComplete(true, null)
                        } else { onComplete(false, signInTask.exception?.localizedMessage) }
                    }
                }
            }
        } else {
            authInstance.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val sovUser = SovereignUser(
                        uid = user?.uid ?: getOrCreateUserId(),
                        displayName = user?.displayName ?: "Google Listener",
                        email = user?.email, photoUrl = user?.photoUrl?.toString(), isAnonymous = false
                    )
                    _currentUser.value = sovUser
                    saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                    syncAllToCloud(sovUser.uid); attachFirestoreListeners(sovUser.uid); onComplete(true, null)
                } else { onComplete(false, task.exception?.localizedMessage) }
            }
        }
    }

    fun signOut() {
        try { auth?.signOut() } catch (_: Exception) {}
        likedListener?.remove()
        historyListener?.remove()
        clearUserProfile()
        val guestUid = getOrCreateUserId()
        _currentUser.value = SovereignUser(guestUid, "Sovereign Guest", null, null, true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Serialization helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun songToMap(song: SongItem): HashMap<String, Any> = hashMapOf(
        "id" to song.id, "title" to song.title, "artist" to song.artist,
        "album" to song.album, "durationSeconds" to song.durationSeconds,
        "highResArtworkUrl" to song.highResArtworkUrl,
        "encryptedMediaUrl" to song.encryptedMediaUrl,
        "mediaPreviewUrl" to song.mediaPreviewUrl,
        "directStreamUrl" to song.directStreamUrl
    )

    private fun songToJson(song: SongItem): JSONObject = JSONObject().apply {
        put("id", song.id); put("title", song.title); put("artist", song.artist)
        put("album", song.album); put("durationSeconds", song.durationSeconds)
        put("highResArtworkUrl", song.highResArtworkUrl)
        put("encryptedMediaUrl", song.encryptedMediaUrl)
        put("mediaPreviewUrl", song.mediaPreviewUrl)
        put("directStreamUrl", song.directStreamUrl)
    }

    private fun parseSongFromJson(obj: JSONObject?): SongItem? {
        if (obj == null) return null
        val id = obj.optString("id").ifEmpty { return null }
        return SongItem(
            id = id,
            title = obj.optString("title"),
            artist = obj.optString("artist"),
            album = obj.optString("album"),
            durationSeconds = obj.optLong("durationSeconds", 180L),
            highResArtworkUrl = obj.optString("highResArtworkUrl"),
            encryptedMediaUrl = obj.optString("encryptedMediaUrl"),
            mediaPreviewUrl = obj.optString("mediaPreviewUrl"),
            directStreamUrl = obj.optString("directStreamUrl")
        )
    }

    private fun parseSongItem(data: Map<*, *>?): SongItem? {
        if (data == null) return null
        val id = data["id"] as? String ?: return null
        return SongItem(
            id = id,
            title = data["title"] as? String ?: "",
            artist = data["artist"] as? String ?: "",
            album = data["album"] as? String ?: "",
            durationSeconds = (data["durationSeconds"] as? Number)?.toLong() ?: 180L,
            highResArtworkUrl = data["highResArtworkUrl"] as? String ?: "",
            encryptedMediaUrl = data["encryptedMediaUrl"] as? String ?: "",
            mediaPreviewUrl = data["mediaPreviewUrl"] as? String ?: "",
            directStreamUrl = data["directStreamUrl"] as? String ?: ""
        )
    }
}
