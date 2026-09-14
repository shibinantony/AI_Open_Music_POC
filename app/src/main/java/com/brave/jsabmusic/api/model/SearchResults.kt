package com.brave.jsabmusic.api.model

/**
 * Aggregate container for all four search result categories returned in parallel.
 */
data class SearchResults(
    val songs: List<SongItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList()
)
