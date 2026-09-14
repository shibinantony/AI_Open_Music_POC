package com.brave.jsabmusic.api.model

/**
 * Represents an album result from JioSaavn search.
 */
data class AlbumItem(
    val id: String,
    val name: String,
    val year: String,
    val artist: String,
    val artworkUrl: String,
    val songCount: Int = 0
)
