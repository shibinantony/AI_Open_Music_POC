package com.brave.jsabmusic.api.model

/**
 * Clean data model representing a song from JioSaavn.
 */
data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Long,
    val highResArtworkUrl: String,
    val encryptedMediaUrl: String = "",
    val mediaPreviewUrl: String = "",
    val directStreamUrl: String = ""
)
