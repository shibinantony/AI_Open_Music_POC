package com.brave.jsabmusic.firebase

import android.content.Context
import android.util.Log
import com.brave.jsabmusic.api.model.SongItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Enterprise Cloud Persistence and Authentication Manager for JSABMusic.
 * Orchestrates Google Sign-In, Firebase Auth, Real-Time Cloud Firestore Sync for
 * Liked Music, 7-Day Listening History retention, and cloud session restoration on app reinstall.
 */
class FirebaseSyncManager(private val context: Context) {

    private val tag = "FirebaseSyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val likedSongs: StateFlow<List<SongItem>> = _likedSongs.asStateFlow()

    private val _recentlyListened = MutableStateFlow<List<SongItem>>(emptyList())
    val recentlyListened: StateFlow<List<SongItem>> = _recentlyListened.asStateFlow()

    private var likedListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user
                if (user != null) {
                    attachFirestoreListeners(user.uid)
                } else {
                    // Sign in anonymously as a fallback guest session so data starts syncing immediately
                    auth?.signInAnonymously()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            task.result?.user?.let { guestUser ->
                                _currentUser.value = guestUser
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

    private fun attachFirestoreListeners(uid: String) {
        val db = firestore ?: return

        // 1. Real-time Liked Songs Sync
        likedListener?.remove()
        try {
            likedListener = db.collection("users")
                .document(uid)
                .collection("liked_songs")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Liked songs listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val songs = snapshot?.documents?.mapNotNull { doc ->
                        parseSongItem(doc.data)
                    } ?: emptyList()
                    _likedSongs.value = songs
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach liked songs listener", e)
        }

        // 2. 7-Day Recently Listened History Sync
        historyListener?.remove()
        try {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
            historyListener = db.collection("users")
                .document(uid)
                .collection("history")
                .whereGreaterThanOrEqualTo("playedAt", sevenDaysAgo)
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "History listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val songs = snapshot?.documents?.mapNotNull { doc ->
                        parseSongItem(doc.data)
                    } ?: emptyList()
                    _recentlyListened.value = songs
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach history listener", e)
        }
    }

    /**
     * Toggles a song's liked status in Firebase Cloud Firestore.
     * Updates local state optimistically for instant UI responsiveness.
     */
    fun toggleLike(song: SongItem) {
        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return

        val isCurrentlyLiked = isLiked(song.id)

        // Optimistic UI update
        if (isCurrentlyLiked) {
            _likedSongs.value = _likedSongs.value.filter { it.id != song.id }
        } else {
            _likedSongs.value = listOf(song) + _likedSongs.value
        }

        scope.launch {
            try {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("liked_songs")
                    .document(song.id)

                if (isCurrentlyLiked) {
                    docRef.delete().await()
                } else {
                    val map = songToMap(song).apply {
                        put("likedAt", System.currentTimeMillis())
                    }
                    docRef.set(map).await()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error updating liked status in Firestore", e)
            }
        }
    }

    fun isLiked(songId: String): Boolean {
        return _likedSongs.value.any { it.id == songId }
    }

    /**
     * Records a track playback to Firestore 7-Day History.
     */
    fun recordSongPlayed(song: SongItem) {
        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return

        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val map = songToMap(song).apply {
                    put("playedAt", now)
                }
                db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document(song.id)
                    .set(map)
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Failed to record played song in history", e)
            }
        }
    }

    /**
     * Saves active playback state to Firestore so the user can restore on fresh install.
     */
    fun saveLastSession(song: SongItem, queue: List<SongItem>) {
        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return

        scope.launch {
            try {
                val queueData = queue.take(30).map { songToMap(it) }
                val sessionMap = hashMapOf(
                    "lastSong" to songToMap(song),
                    "queue" to queueData,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users")
                    .document(uid)
                    .collection("session")
                    .document("last_played")
                    .set(sessionMap)
                    .await()
            } catch (e: Exception) {
                Log.e(tag, "Error saving last session", e)
            }
        }
    }

    /**
     * Restores last played song and queue from Firestore (used after reinstall or login).
     */
    suspend fun restoreLastSession(): Pair<SongItem, List<SongItem>>? {
        val uid = _currentUser.value?.uid ?: return null
        val db = firestore ?: return null

        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("session")
                .document("last_played")
                .get()
                .await()

            if (doc.exists()) {
                val lastSongMap = doc.get("lastSong") as? Map<String, Any>
                val lastSong = parseSongItem(lastSongMap)

                val queueList = doc.get("queue") as? List<Map<String, Any>>
                val queue = queueList?.mapNotNull { parseSongItem(it) } ?: emptyList()

                if (lastSong != null && queue.isNotEmpty()) {
                    Pair(lastSong, queue)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Error restoring session", e)
            null
        }
    }

    /**
     * Links or signs in using Google Auth credentials.
     */
    fun signInWithGoogle(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        val authInstance = auth ?: run {
            onComplete(false, "Auth service not ready")
            return
        }

        val current = authInstance.currentUser
        if (current != null && current.isAnonymous) {
            // Link existing anonymous data with the Google Account
            current.linkWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    _currentUser.value = user
                    user?.let { attachFirestoreListeners(it.uid) }
                    onComplete(true, null)
                } else {
                    // If account already exists with this Google credential, sign into that account directly
                    authInstance.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = signInTask.result?.user
                            _currentUser.value = user
                            user?.let { attachFirestoreListeners(it.uid) }
                            onComplete(true, null)
                        } else {
                            onComplete(false, signInTask.exception?.localizedMessage)
                        }
                    }
                }
            }
        } else {
            authInstance.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    _currentUser.value = user
                    user?.let { attachFirestoreListeners(it.uid) }
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage)
                }
            }
        }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
        _likedSongs.value = emptyList()
        _recentlyListened.value = emptyList()
        likedListener?.remove()
        historyListener?.remove()
    }

    private fun songToMap(song: SongItem): HashMap<String, Any> {
        return hashMapOf(
            "id" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "album" to song.album,
            "durationSeconds" to song.durationSeconds,
            "highResArtworkUrl" to song.highResArtworkUrl,
            "encryptedMediaUrl" to song.encryptedMediaUrl,
            "mediaPreviewUrl" to song.mediaPreviewUrl,
            "directStreamUrl" to song.directStreamUrl
        )
    }

    private fun parseSongItem(data: Map<String, Any>?): SongItem? {
        if (data == null) return null
        val id = data["id"] as? String ?: return null
        val title = data["title"] as? String ?: ""
        val artist = data["artist"] as? String ?: ""
        val album = data["album"] as? String ?: ""
        val durationSeconds = (data["durationSeconds"] as? Number)?.toLong() ?: 180L
        val highResArtworkUrl = data["highResArtworkUrl"] as? String ?: ""
        val encryptedMediaUrl = data["encryptedMediaUrl"] as? String ?: ""
        val mediaPreviewUrl = data["mediaPreviewUrl"] as? String ?: ""
        val directStreamUrl = data["directStreamUrl"] as? String ?: ""

        return SongItem(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            highResArtworkUrl = highResArtworkUrl,
            encryptedMediaUrl = encryptedMediaUrl,
            mediaPreviewUrl = mediaPreviewUrl,
            directStreamUrl = directStreamUrl
        )
    }
}
