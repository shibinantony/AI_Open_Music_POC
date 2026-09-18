package com.brave.jsabmusic.firebase

import android.content.Context
import android.util.Log
import com.brave.jsabmusic.api.model.SongItem
import com.brave.jsabmusic.storage.LocalMusicStore
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
 * and robust Local Persistence fallback for Liked Music, 7-Day History, and Session Restoration.
 */
class FirebaseSyncManager(private val context: Context) {

    private val tag = "FirebaseSyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    val localStore = LocalMusicStore(context)

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _currentUser = MutableStateFlow<SovereignUser?>(null)
    val currentUser: StateFlow<SovereignUser?> = _currentUser.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<SongItem>>(localStore.getLikedSongs())
    val likedSongs: StateFlow<List<SongItem>> = _likedSongs.asStateFlow()

    private val _recentlyListened = MutableStateFlow<List<SongItem>>(localStore.getRecentlyListened())
    val recentlyListened: StateFlow<List<SongItem>> = _recentlyListened.asStateFlow()

    private var likedListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    init {
        initLocalSession()
        initializeFirebase()
    }

    private fun initLocalSession() {
        val profile = localStore.getUserProfile()
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
                    localStore.saveUserProfile(
                        sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, sovUser.isAnonymous
                    )
                    syncAllToCloud(fbUser.uid)
                    attachFirestoreListeners(fbUser.uid)
                } else {
                    // Try anonymous fallback session if network / config allows
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
                                localStore.saveUserProfile(guestSov.uid, guestSov.displayName, null, null, true)
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

                // Upload local liked songs
                val likes = localStore.getLikedSongs()
                for (song in likes) {
                    val map = songToMap(song).apply {
                        put("likedAt", System.currentTimeMillis())
                    }
                    db.collection("users").document(uid)
                        .collection("liked_songs").document(song.id)
                        .set(map)
                }

                // Upload local history
                val history = localStore.getRecentlyListened()
                for (song in history) {
                    val map = songToMap(song).apply {
                        put("playedAt", System.currentTimeMillis())
                    }
                    db.collection("users").document(uid)
                        .collection("history").document(song.id)
                        .set(map)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error syncing data to cloud: ${e.message}")
            }
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
                    val remoteSongs = snapshot?.documents?.mapNotNull { doc ->
                        parseSongItem(doc.data)
                    } ?: emptyList()

                    if (remoteSongs.isNotEmpty()) {
                        localStore.setLikedSongs(remoteSongs)
                        _likedSongs.value = localStore.getLikedSongs()
                    }
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
                    val remoteSongs = snapshot?.documents?.mapNotNull { doc ->
                        parseSongItem(doc.data)
                    } ?: emptyList()

                    if (remoteSongs.isNotEmpty()) {
                        localStore.setHistorySongs(remoteSongs)
                        _recentlyListened.value = localStore.getRecentlyListened()
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach history listener", e)
        }
    }

    /**
     * Toggles a song's liked status.
     * Persists immediately to local store, then synchronizes to Cloud Firestore if connected.
     */
    fun toggleLike(song: SongItem) {
        val wasLiked = localStore.isLiked(song.id)
        localStore.toggleLike(song)
        _likedSongs.value = localStore.getLikedSongs()

        val uid = _currentUser.value?.uid ?: return
        val db = firestore ?: return

        scope.launch {
            try {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("liked_songs")
                    .document(song.id)

                if (wasLiked) {
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
        return localStore.isLiked(songId)
    }

    /**
     * Records a track playback to 7-Day History.
     * Persists immediately to local storage, then syncs to Cloud Firestore.
     */
    fun recordSongPlayed(song: SongItem) {
        localStore.recordSongPlayed(song)
        _recentlyListened.value = localStore.getRecentlyListened()

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
     * Saves active playback state to local storage and Firestore.
     */
    fun saveLastSession(song: SongItem, queue: List<SongItem>) {
        localStore.saveLastSession(song, queue)

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
                Log.e(tag, "Error saving last session to Firestore", e)
            }
        }
    }

    /**
     * Restores last played song and queue (from local storage or Firestore).
     */
    suspend fun restoreLastSession(): Pair<SongItem, List<SongItem>>? {
        val local = localStore.restoreLastSession()
        if (local != null) return local

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
                val lastSongMap = doc.get("lastSong") as? Map<*, *>
                val lastSong = parseSongItem(lastSongMap)

                val queueList = doc.get("queue") as? List<*>
                val queue = queueList?.mapNotNull { item -> (item as? Map<*, *>)?.let { parseSongItem(it) } } ?: emptyList()

                if (lastSong != null && queue.isNotEmpty()) {
                    Pair(lastSong, queue)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Error restoring session from Firestore", e)
            null
        }
    }

    /**
     * Signs in with a Sovereign Profile (custom/test or fallback).
     * Creates user record in Firestore and immediately syncs local likes & history.
     */
    fun signInWithProfile(displayName: String, email: String, photoUrl: String? = null) {
        val uid = _currentUser.value?.uid ?: localStore.getOrCreateUserId()
        val sovUser = SovereignUser(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            isAnonymous = false
        )
        _currentUser.value = sovUser
        localStore.saveUserProfile(uid, displayName, email, photoUrl, false)
        syncAllToCloud(uid)
        attachFirestoreListeners(uid)
    }

    /**
     * Links or signs in using Google Auth credentials.
     */
    fun signInWithGoogle(credential: AuthCredential, onComplete: (Boolean, String?) -> Unit) {
        val authInstance = auth ?: run {
            onComplete(false, "Auth service not initialized")
            return
        }

        val current = authInstance.currentUser
        if (current != null && current.isAnonymous) {
            current.linkWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val sovUser = SovereignUser(
                        uid = user?.uid ?: localStore.getOrCreateUserId(),
                        displayName = user?.displayName ?: "Google Listener",
                        email = user?.email,
                        photoUrl = user?.photoUrl?.toString(),
                        isAnonymous = false
                    )
                    _currentUser.value = sovUser
                    localStore.saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                    syncAllToCloud(sovUser.uid)
                    attachFirestoreListeners(sovUser.uid)
                    onComplete(true, null)
                } else {
                    authInstance.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = signInTask.result?.user
                            val sovUser = SovereignUser(
                                uid = user?.uid ?: localStore.getOrCreateUserId(),
                                displayName = user?.displayName ?: "Google Listener",
                                email = user?.email,
                                photoUrl = user?.photoUrl?.toString(),
                                isAnonymous = false
                            )
                            _currentUser.value = sovUser
                            localStore.saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                            syncAllToCloud(sovUser.uid)
                            attachFirestoreListeners(sovUser.uid)
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
                    val sovUser = SovereignUser(
                        uid = user?.uid ?: localStore.getOrCreateUserId(),
                        displayName = user?.displayName ?: "Google Listener",
                        email = user?.email,
                        photoUrl = user?.photoUrl?.toString(),
                        isAnonymous = false
                    )
                    _currentUser.value = sovUser
                    localStore.saveUserProfile(sovUser.uid, sovUser.displayName, sovUser.email, sovUser.photoUrl, false)
                    syncAllToCloud(sovUser.uid)
                    attachFirestoreListeners(sovUser.uid)
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage)
                }
            }
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        likedListener?.remove()
        historyListener?.remove()
        localStore.clearUserProfile()
        val guestUid = localStore.getOrCreateUserId()
        _currentUser.value = SovereignUser(guestUid, "Sovereign Guest", null, null, true)
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

    private fun parseSongItem(data: Map<*, *>?): SongItem? {
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
