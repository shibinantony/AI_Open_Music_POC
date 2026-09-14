package com.brave.jsabmusic.api.model

/**
 * Represents an artist result from JioSaavn search.
 */
data class ArtistItem(
    val id: String,
    val name: String,
    val artworkUrl: String,
    val followerCount: String = ""
)
