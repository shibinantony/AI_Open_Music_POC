package com.brave.jsabmusic.api.model

/**
 * Represents a playlist result from JioSaavn search.
 */
data class PlaylistItem(
    val id: String,
    val name: String,
    val artworkUrl: String,
    val songCount: Int = 0,
    val followerCount: String = ""
)
