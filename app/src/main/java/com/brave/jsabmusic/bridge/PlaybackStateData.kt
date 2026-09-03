package com.brave.jsabmusic.bridge

/**
 * Immutable data snapshot representing current JioSaavn playback state.
 */
data class PlaybackStateData(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artUrl: String = "",
    val durationSeconds: Long = 0L,
    val positionSeconds: Long = 0L
)
