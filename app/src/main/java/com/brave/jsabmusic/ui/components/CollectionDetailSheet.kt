package com.brave.jsabmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.brave.jsabmusic.api.model.SongItem
import com.brave.jsabmusic.firebase.FirebaseSyncManager
import com.brave.jsabmusic.player.PlayerController
import com.brave.jsabmusic.ui.SongRowItem
import com.brave.jsabmusic.ui.theme.AmoledBlack
import com.brave.jsabmusic.ui.theme.SovereignBlue
import com.brave.jsabmusic.ui.theme.SovereignBlueAccent
import com.brave.jsabmusic.ui.theme.SpotifyBase
import com.brave.jsabmusic.ui.theme.SpotifyCard
import com.brave.jsabmusic.ui.theme.SpotifyElevated
import com.brave.jsabmusic.ui.theme.TextPrimary
import com.brave.jsabmusic.ui.theme.TextSecondary

data class CollectionDetailState(
    val title: String,
    val subtitle: String,
    val badge: String,
    val artworkUrl: String,
    val isArtist: Boolean = false,
    val isLoading: Boolean = true,
    val songs: List<SongItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailSheet(
    state: CollectionDetailState,
    currentSong: SongItem?,
    isPlaying: Boolean,
    likedSongs: List<SongItem>,
    dislikedIds: Set<String>,
    firebaseSyncManager: FirebaseSyncManager,
    playerController: PlayerController,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
    onRadioPlay: (SongItem) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val likedIds = remember(likedSongs) { likedSongs.map { it.id }.toSet() }
    val visibleSongs = remember(state.songs, dislikedIds) {
        state.songs.filter { it.id !in dislikedIds }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SpotifyBase,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C2238), SpotifyBase, AmoledBlack)
                    )
                )
                .padding(horizontal = 16.dp)
        ) {
            // Header Close & Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpotifyCard,
                    border = BorderStroke(1.dp, SovereignBlue.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = state.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SovereignBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }
            }

            // Artwork + Title + Action Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = state.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(148.dp)
                        .clip(if (state.isArtist) CircleShape else RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (state.subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.subtitle,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spotify-style Action Bar: Shuffle + Big Play + Radio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SpotifyCard,
                        border = BorderStroke(1.dp, SpotifyElevated),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = visibleSongs.isNotEmpty()) {
                                onShuffleAll(visibleSongs)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = SovereignBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shuffle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (visibleSongs.isNotEmpty()) SovereignBlue else SpotifyElevated,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable(enabled = visibleSongs.isNotEmpty()) {
                                onPlayAll(visibleSongs)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play All",
                                tint = AmoledBlack,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SpotifyCard,
                        border = BorderStroke(1.dp, SpotifyElevated),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = visibleSongs.isNotEmpty()) {
                                visibleSongs.firstOrNull()?.let { onRadioPlay(it) }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = "Radio",
                                tint = SovereignBlueAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Radio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SovereignBlue)
                }
            } else if (visibleSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playable tracks found.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(visibleSongs, key = { it.id }) { song ->
                        SongRowItem(
                            song = song,
                            isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                            isLiked = likedIds.contains(song.id),
                            isDisliked = dislikedIds.contains(song.id),
                            onLikeToggle = { firebaseSyncManager.toggleLike(song) },
                            onDislikeToggle = {
                                firebaseSyncManager.toggleDislike(song)
                                playerController.removeSongFromQueue(song.id)
                            },
                            onRadioPlay = { onRadioPlay(song) },
                            onClick = { playerController.playSong(song, visibleSongs) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
