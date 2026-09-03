package com.brave.jsabmusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.brave.jsabmusic.api.JioSaavnApiClient
import com.brave.jsabmusic.api.model.SongItem
import com.brave.jsabmusic.player.PlayerController
import com.brave.jsabmusic.service.PlaybackService
import com.brave.jsabmusic.timer.SleepTimerManager
import com.brave.jsabmusic.ui.components.EqualizerSheet
import com.brave.jsabmusic.ui.components.NowPlayingSheet
import com.brave.jsabmusic.ui.components.SleepTimerSheet
import com.brave.jsabmusic.ui.theme.AmoledBlack
import com.brave.jsabmusic.ui.theme.AmoledCard
import com.brave.jsabmusic.ui.theme.JSABMusicTheme
import com.brave.jsabmusic.ui.theme.SaavnTeal
import com.brave.jsabmusic.ui.theme.SaavnTealAccent
import com.brave.jsabmusic.ui.theme.TextPrimary
import com.brave.jsabmusic.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Pure Native AndroidX Media3 Audio Player for JioSaavn.
 * 100% Ad-Free, 320 kbps Uncompressed CDN Audio, Hardware Audio DSP, Zero WebViews.
 */
class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    private val sleepTimerManager = SleepTimerManager()
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            // Initialize PlayerController and bind to MediaSessionService
            playerController = PlaybackService.playerControllerInstance ?: PlayerController(applicationContext).also {
                PlaybackService.playerControllerInstance = it
            }
            sleepTimerManager.setPlayerController(playerController)

            checkNotificationPermission()
            bindPlaybackService()
        } catch (e: Exception) {
            // Handled safely
        }

        setContent {
            JSABMusicTheme {
                MainPlayerScreen(
                    playerController = playerController,
                    sleepTimerManager = sleepTimerManager
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun bindPlaybackService() {
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {}
            isServiceBound = false
        }
        super.onDestroy()
    }
}

@Composable
fun MainPlayerScreen(
    playerController: PlayerController,
    sleepTimerManager: SleepTimerManager
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var trendingSongs by remember { mutableStateOf(JioSaavnApiClient.getCuratedDefaultSongs()) }
    var isLoading by remember { mutableStateOf(false) }

    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }

    val currentSong by playerController.currentSong.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val isTimerRunning by sleepTimerManager.isTimerRunning.collectAsState()

    // Fetch fresh live trending songs on startup asynchronously
    LaunchedEffect(Unit) {
        try {
            val freshSongs = JioSaavnApiClient.getTrendingSongs()
            if (freshSongs.isNotEmpty()) {
                trendingSongs = freshSongs
            }
        } catch (e: Exception) {}
    }

    Scaffold(
        containerColor = AmoledBlack,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Logo",
                            tint = SaavnTeal,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "JSAB Music",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Pure 320 Kbps Direct CDN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SaavnTealAccent
                            )
                        }
                    }

                    // Action Controls Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmoledCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF222222))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            IconButton(
                                onClick = { showEqualizer = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = SaavnTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showSleepTimer = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (isTimerRunning) SaavnTeal else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.length >= 2) {
                            scope.launch {
                                isLoading = true
                                try {
                                    searchResults = JioSaavnApiClient.searchSongs(query)
                                } catch (e: Exception) {}
                                isLoading = false
                            }
                        } else if (query.isEmpty()) {
                            searchResults = emptyList()
                        }
                    },
                    placeholder = { Text("Search songs, artists, albums...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledCard,
                        unfocusedContainerColor = AmoledCard,
                        focusedBorderColor = SaavnTeal,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF222222),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section Title
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results" else "Trending Today (320 Kbps)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SaavnTeal)
                    }
                } else {
                    val displayedSongs = if (searchQuery.isNotEmpty()) searchResults else trendingSongs

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedSongs) { song ->
                            SongRowItem(
                                song = song,
                                isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                                onClick = {
                                    playerController.playSong(song, displayedSongs)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom mini-player
                        }
                    }
                }
            }

            // Bottom Mini-Player Bar
            if (currentSong != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showNowPlaying = true },
                    color = AmoledCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF222222)),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentSong?.highResArtworkUrl,
                            contentDescription = currentSong?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong?.title ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong?.artist ?: "",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { playerController.togglePlay() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = SaavnTeal,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(onClick = { playerController.skipNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets
    if (showNowPlaying) {
        NowPlayingSheet(
            playerController = playerController,
            onOpenEqualizer = {
                showNowPlaying = false
                showEqualizer = true
            },
            onDismissRequest = { showNowPlaying = false }
        )
    }

    if (showEqualizer) {
        EqualizerSheet(
            equalizerManager = playerController.equalizerManager,
            onDismissRequest = { showEqualizer = false }
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            timerManager = sleepTimerManager,
            onDismissRequest = { showSleepTimer = false }
        )
    }
}

@Composable
fun SongRowItem(
    song: SongItem,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isCurrentlyPlaying) androidx.compose.ui.graphics.Color(0xFF161618) else AmoledBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.highResArtworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentlyPlaying) SaavnTeal else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledCard)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "320K",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaavnTeal
                )
            }
        }
    }
}
