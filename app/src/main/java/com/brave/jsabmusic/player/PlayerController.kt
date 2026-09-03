package com.brave.jsabmusic.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.brave.jsabmusic.api.model.SongItem
import com.brave.jsabmusic.equalizer.HardwareEqualizerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-performance centralized audio playback controller powered by AndroidX Media3 / ExoPlayer.
 * Orchestrates 320 kbps CDN playback, gapless playlist transitions, and hardware DSP effects.
 */
class PlayerController(private val context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    val equalizerManager = HardwareEqualizerManager(context)

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<SongItem>>(emptyList())
    val queue: StateFlow<List<SongItem>> = _queue.asStateFlow()

    private var currentIndex = 0

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                    equalizerManager.attachToAudioSession(exoPlayer.audioSessionId)
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                if (mediaId != null) {
                    val song = _queue.value.find { it.id == mediaId }
                    if (song != null) {
                        _currentSong.value = song
                        currentIndex = _queue.value.indexOf(song)
                    }
                }
                equalizerManager.attachToAudioSession(exoPlayer.audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        })
    }

    fun playSong(song: SongItem, playlist: List<SongItem> = listOf(song)) {
        try {
            _queue.value = playlist
            currentIndex = playlist.indexOf(song).coerceAtLeast(0)
            _currentSong.value = song

            exoPlayer.clearMediaItems()

            // Enqueue media items for continuous gapless playlist streaming
            val mediaItems = playlist.mapNotNull { item ->
                if (item.directStreamUrl.isEmpty()) return@mapNotNull null
                val metadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .apply {
                        if (item.highResArtworkUrl.isNotEmpty()) {
                            setArtworkUri(Uri.parse(item.highResArtworkUrl))
                        }
                    }
                    .build()

                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(item.directStreamUrl)
                    .setMediaMetadata(metadata)
                    .build()
            }

            if (mediaItems.isNotEmpty()) {
                val safeIndex = currentIndex.coerceIn(0, mediaItems.size - 1)
                exoPlayer.setMediaItems(mediaItems, safeIndex, 0L)
                exoPlayer.prepare()
                exoPlayer.play()
            }
        } catch (e: Exception) {}
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun skipNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0.0f, 1.0f)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        equalizerManager.release()
        exoPlayer.release()
    }
}
