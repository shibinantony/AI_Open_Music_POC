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
 * Orchestrates 320 kbps CDN playback, gapless playlist transitions, hardware DSP effects,
 * shuffle, and repeat modes.
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

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    /** Callback invoked whenever a song begins playing to sync with Firebase History & Cloud Session */
    var onSongStarted: ((SongItem, List<SongItem>) -> Unit)? = null

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

            override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleEnabled
            }

            override fun onRepeatModeChanged(repeatModeVal: Int) {
                _repeatMode.value = repeatModeVal
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                if (mediaId != null) {
                    val song = _queue.value.find { it.id == mediaId }
                    if (song != null) {
                        _currentSong.value = song
                        currentIndex = _queue.value.indexOf(song)
                        onSongStarted?.invoke(song, _queue.value)
                    }
                }
                equalizerManager.attachToAudioSession(exoPlayer.audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    when (_repeatMode.value) {
                        Player.REPEAT_MODE_ONE -> {
                            exoPlayer.seekTo(0L)
                            exoPlayer.play()
                        }
                        Player.REPEAT_MODE_ALL -> {
                            if (_queue.value.isNotEmpty()) {
                                exoPlayer.seekToDefaultPosition(0)
                                exoPlayer.play()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun createMediaItem(item: SongItem): MediaItem? {
        if (item.directStreamUrl.isEmpty()) return null
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

        return MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(item.directStreamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playSong(song: SongItem, playlist: List<SongItem> = listOf(song)) {
        try {
            _queue.value = playlist
            currentIndex = playlist.indexOf(song).coerceAtLeast(0)
            _currentSong.value = song

            exoPlayer.clearMediaItems()

            val mediaItems = playlist.mapNotNull { createMediaItem(it) }

            if (mediaItems.isNotEmpty()) {
                val safeIndex = currentIndex.coerceIn(0, mediaItems.size - 1)
                exoPlayer.setMediaItems(mediaItems, safeIndex, 0L)
                exoPlayer.repeatMode = _repeatMode.value
                exoPlayer.shuffleModeEnabled = _shuffleModeEnabled.value
                exoPlayer.prepare()
                exoPlayer.play()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerController", "Playback failed", e)
        }
    }

    /** Plays an entire playlist, truly shuffling the queue if shuffle is enabled */
    fun playAll(playlist: List<SongItem>, shuffle: Boolean = false) {
        if (playlist.isEmpty()) return
        _shuffleModeEnabled.value = shuffle
        exoPlayer.shuffleModeEnabled = shuffle

        val targetQueue = if (shuffle) playlist.shuffled() else playlist
        playSong(targetQueue.first(), targetQueue)
    }

    fun toggleShuffle() {
        val nextState = !_shuffleModeEnabled.value
        _shuffleModeEnabled.value = nextState
        exoPlayer.shuffleModeEnabled = nextState

        val current = _currentSong.value
        val currentQ = _queue.value
        if (nextState && current != null && currentQ.size > 1) {
            val remaining = currentQ.filter { it.id != current.id }.shuffled()
            val newQueue = listOf(current) + remaining
            _queue.value = newQueue

            val currentPos = exoPlayer.currentPosition
            val mediaItems = newQueue.mapNotNull { createMediaItem(it) }
            exoPlayer.setMediaItems(mediaItems, 0, currentPos)
        }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer.repeatMode = nextMode
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun skipNext() {
        if (_queue.value.isEmpty()) return

        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            val nextIndex = (currentIndex + 1) % _queue.value.size
            playSong(_queue.value[nextIndex], _queue.value)
            return
        }

        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if ((_repeatMode.value == Player.REPEAT_MODE_ALL || _shuffleModeEnabled.value) && _queue.value.isNotEmpty()) {
            exoPlayer.seekToDefaultPosition(0)
            exoPlayer.play()
        }
    }

    fun skipPrevious() {
        if (_queue.value.isEmpty()) return

        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
            return
        }

        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && _queue.value.isNotEmpty()) {
            val lastIndex = _queue.value.size - 1
            exoPlayer.seekToDefaultPosition(lastIndex)
            exoPlayer.play()
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
