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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.brave.jsabmusic.R
import com.brave.jsabmusic.api.JioSaavnApiClient
import com.brave.jsabmusic.api.model.AlbumItem
import com.brave.jsabmusic.api.model.ArtistItem
import com.brave.jsabmusic.api.model.PlaylistItem
import com.brave.jsabmusic.api.model.SongItem
import com.brave.jsabmusic.firebase.FirebaseSyncManager
import com.brave.jsabmusic.player.PlayerController
import com.brave.jsabmusic.service.PlaybackService
import com.brave.jsabmusic.timer.SleepTimerManager
import com.brave.jsabmusic.ui.components.EqualizerSheet
import com.brave.jsabmusic.ui.components.NowPlayingSheet
import com.brave.jsabmusic.ui.components.SleepTimerSheet
import com.brave.jsabmusic.ui.theme.AmoledBlack
import com.brave.jsabmusic.ui.theme.AmoledCard
import com.brave.jsabmusic.ui.theme.HeartRed
import com.brave.jsabmusic.ui.theme.JSABMusicTheme
import com.brave.jsabmusic.ui.theme.SovereignBlue
import com.brave.jsabmusic.ui.theme.SovereignBlueAccent
import com.brave.jsabmusic.ui.theme.TextPrimary
import com.brave.jsabmusic.ui.theme.TextSecondary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * Pure Native AndroidX Media3 Audio Player for JioSaavn.
 * 100% Ad-Free, 320 kbps Uncompressed CDN Audio, Hardware Audio DSP.
 *
 * v2.2.0: Sovereign Blue & Doll Mascot branding, Play All / Shuffle All / Repeat modes,
 *         Firebase Cloud Persistence (Google Sign-In, Liked Music, 7-Day History, and Session Restoration).
 */
class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    private lateinit var firebaseSyncManager: FirebaseSyncManager
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
            firebaseSyncManager = FirebaseSyncManager(applicationContext)

            playerController = PlaybackService.playerControllerInstance
                ?: PlayerController(applicationContext).also {
                    PlaybackService.playerControllerInstance = it
                }
            sleepTimerManager.setPlayerController(playerController)

            // Connect track playback events to Firebase for 7-day history & cloud session backup
            playerController.onSongStarted = { song, queue ->
                firebaseSyncManager.recordSongPlayed(song)
                firebaseSyncManager.saveLastSession(song, queue)
            }

            checkNotificationPermission()
            bindPlaybackService()
        } catch (_: Exception) {}

        setContent {
            JSABMusicTheme {
                MainPlayerScreen(
                    playerController = playerController,
                    sleepTimerManager = sleepTimerManager,
                    firebaseSyncManager = firebaseSyncManager
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
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
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        if (isServiceBound) {
            try { unbindService(serviceConnection) } catch (_: Exception) {}
            isServiceBound = false
        }
        super.onDestroy()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation & Tab definitions
// ─────────────────────────────────────────────────────────────────────────────

private val SEARCH_TABS = listOf("Songs", "Albums", "Artists", "Playlists")
private enum class MainNavSection(val label: String) {
    EXPLORE("Explore"),
    LIKED_MUSIC("Liked Music"),
    RECENT("Recent (7d)")
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainPlayerScreen(
    playerController: PlayerController,
    sleepTimerManager: SleepTimerManager,
    firebaseSyncManager: FirebaseSyncManager
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // ── Navigation State ─────────────────────────────────────────────────────
    var currentSection  by remember { mutableStateOf(MainNavSection.EXPLORE) }
    var searchQuery     by remember { mutableStateOf("") }
    var selectedTab     by remember { mutableIntStateOf(0) }

    // ── Data States ──────────────────────────────────────────────────────────
    var songResults     by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var albumResults    by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var artistResults   by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var trendingSongs   by remember { mutableStateOf(JioSaavnApiClient.getCuratedDefaultSongs()) }
    var isLoading       by remember { mutableStateOf(false) }

    // ── Sheets State ─────────────────────────────────────────────────────────
    var showEqualizer   by remember { mutableStateOf(false) }
    var showSleepTimer  by remember { mutableStateOf(false) }
    var showNowPlaying  by remember { mutableStateOf(false) }
    var showUserProfile by remember { mutableStateOf(false) }

    // ── Reactive Flows ───────────────────────────────────────────────────────
    val currentSong     by playerController.currentSong.collectAsState()
    val isPlaying       by playerController.isPlaying.collectAsState()
    val isTimerRunning  by sleepTimerManager.isTimerRunning.collectAsState()
    val currentUser     by firebaseSyncManager.currentUser.collectAsState()
    val likedSongs      by firebaseSyncManager.likedSongs.collectAsState()
    val recentlyListened by firebaseSyncManager.recentlyListened.collectAsState()

    // ── Google Sign In Activity Result Launcher ──────────────────────────────
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseSyncManager.signInWithGoogle(credential) { success, errMsg ->
                    if (success) {
                        android.widget.Toast.makeText(context, "Welcome back, ${account.displayName ?: "Listener"}!", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val restored = firebaseSyncManager.restoreLastSession()
                            if (restored != null && playerController.currentSong.value == null) {
                                playerController.playSong(restored.first, restored.second)
                                playerController.exoPlayer.pause()
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Firebase: ${errMsg ?: "Verification error"}. Sovereign session active.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val name = account.displayName ?: "Google User"
                val email = account.email ?: ""
                firebaseSyncManager.signInWithProfile(name, email, account.photoUrl?.toString())
                android.widget.Toast.makeText(context, "Signed in as $name", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val hint = when (e.statusCode) {
                10 -> "Google Auth Developer Error 10: SHA-1 certificate fingerprint needed in Firebase. Sovereign Cloud Profile remains active."
                12500 -> "Google Sign-In cancelled or unavailable on device."
                else -> "Sign-in (${e.statusCode}): ${e.localizedMessage ?: "Please try Sovereign Cloud Profile"}"
            }
            android.widget.Toast.makeText(context, hint, android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Sign-in error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ── Startup: load trending & restore cloud session on reinstall ──────────
    LaunchedEffect(Unit) {
        try {
            val fresh = JioSaavnApiClient.getTrendingSongs()
            if (fresh.isNotEmpty()) trendingSongs = fresh
        } catch (_: Exception) {}

        // Restore cloud session if app was reinstalled or fresh launched
        try {
            if (playerController.currentSong.value == null) {
                val restored = firebaseSyncManager.restoreLastSession()
                if (restored != null) {
                    playerController.playSong(restored.first, restored.second)
                    playerController.exoPlayer.pause()
                }
            }
        } catch (_: Exception) {}
    }

    // ── Search trigger ───────────────────────────────────────────────────────
    fun triggerSearch(query: String) {
        if (query.length < 2) {
            songResults = emptyList(); albumResults = emptyList()
            artistResults = emptyList(); playlistResults = emptyList()
            return
        }
        scope.launch {
            isLoading = true
            try {
                val results = JioSaavnApiClient.searchAll(query)
                songResults     = results.songs
                albumResults    = results.albums
                artistResults   = results.artists
                playlistResults = results.playlists
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────
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

                // ── Header Bar with Peaceful Doll Mascot ─────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Custom Peaceful Doll Mascot Icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_serene_doll_music),
                            contentDescription = "JSAB Mascot",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "JSAB Music",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enjoy the beauty of sovereign music",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SovereignBlueAccent
                            )
                        }
                    }

                    // Action Controls Pill + User Profile
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmoledCard,
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showEqualizer = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer",
                                    tint = SovereignBlue,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { showSleepTimer = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (isTimerRunning) SovereignBlue else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { showUserProfile = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                if (currentUser?.photoUrl != null) {
                                    AsyncImage(
                                        model = currentUser?.photoUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "User Account",
                                        tint = if (currentUser?.isAnonymous == false) SovereignBlue else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Main Section Switcher (Explore | Liked Music | Recent) ────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MainNavSection.values().forEach { section ->
                        val isSelected = currentSection == section
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SovereignBlue else AmoledCard,
                            border = BorderStroke(1.dp, if (isSelected) SovereignBlue else Color(0xFF1E293B)),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable {
                                    currentSection = section
                                    if (section != MainNavSection.EXPLORE) {
                                        focusManager.clearFocus()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (section) {
                                        MainNavSection.EXPLORE -> Icons.Default.Explore
                                        MainNavSection.LIKED_MUSIC -> Icons.Default.Favorite
                                        MainNavSection.RECENT -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) AmoledBlack else if (section == MainNavSection.LIKED_MUSIC) HeartRed else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = section.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AmoledBlack else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Section Content ──────────────────────────────────────────
                when (currentSection) {
                    MainNavSection.EXPLORE -> {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                triggerSearch(query)
                            },
                            placeholder = {
                                Text("Search songs, artists, albums, playlists...", color = TextSecondary, fontSize = 13.sp)
                            },
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
                                        songResults = emptyList(); albumResults = emptyList()
                                        artistResults = emptyList(); playlistResults = emptyList()
                                        selectedTab = 0
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
                                focusedContainerColor   = AmoledCard,
                                unfocusedContainerColor = AmoledCard,
                                focusedBorderColor      = SovereignBlue,
                                unfocusedBorderColor    = Color(0xFF1E293B),
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab bar or Action Bar
                        if (searchQuery.length >= 2) {
                            SearchTabBar(
                                selectedTab = selectedTab,
                                tabs = SEARCH_TABS,
                                onTabSelected = { selectedTab = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            PlayAllHeaderRow(
                                title = "Trending Today (320 Kbps)",
                                onPlayAll = { playerController.playAll(trendingSongs, shuffle = false) },
                                onShuffleAll = { playerController.playAll(trendingSongs, shuffle = true) },
                                isEnabled = trendingSongs.isNotEmpty()
                            )
                        }

                        // Content List
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SovereignBlue)
                            }
                        } else if (searchQuery.length >= 2) {
                            when (selectedTab) {
                                0 -> SongList(
                                    songs = songResults,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    likedSongs = likedSongs,
                                    firebaseSyncManager = firebaseSyncManager,
                                    modifier = Modifier.weight(1f),
                                    onSongClick = { song -> playerController.playSong(song, songResults) }
                                )
                                1 -> AlbumList(
                                    albums = albumResults,
                                    modifier = Modifier.weight(1f),
                                    onAlbumClick = { album ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getAlbumSongs(album.id)
                                            isLoading = false
                                            if (songs.isNotEmpty()) playerController.playSong(songs.first(), songs)
                                        }
                                    }
                                )
                                2 -> ArtistList(
                                    artists = artistResults,
                                    modifier = Modifier.weight(1f),
                                    onArtistClick = { artist ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getArtistSongs(artist.id)
                                            isLoading = false
                                            if (songs.isNotEmpty()) playerController.playSong(songs.first(), songs)
                                        }
                                    }
                                )
                                3 -> PlaylistList(
                                    playlists = playlistResults,
                                    modifier = Modifier.weight(1f),
                                    onPlaylistClick = { playlist ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getPlaylistSongs(playlist.id)
                                            isLoading = false
                                            if (songs.isNotEmpty()) playerController.playSong(songs.first(), songs)
                                        }
                                    }
                                )
                            }
                        } else {
                            SongList(
                                songs = trendingSongs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                likedSongs = likedSongs,
                                firebaseSyncManager = firebaseSyncManager,
                                modifier = Modifier.weight(1f),
                                onSongClick = { song -> playerController.playSong(song, trendingSongs) }
                            )
                        }
                    }

                    MainNavSection.LIKED_MUSIC -> {
                        PlayAllHeaderRow(
                            title = "Liked Music (${likedSongs.size})",
                            onPlayAll = { playerController.playAll(likedSongs, shuffle = false) },
                            onShuffleAll = { playerController.playAll(likedSongs, shuffle = true) },
                            isEnabled = likedSongs.isNotEmpty()
                        )
                        SongList(
                            songs = likedSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            likedSongs = likedSongs,
                            firebaseSyncManager = firebaseSyncManager,
                            emptyMessage = "No liked songs yet. Tap the heart on any song to add it here!",
                            modifier = Modifier.weight(1f),
                            onSongClick = { song -> playerController.playSong(song, likedSongs) }
                        )
                    }

                    MainNavSection.RECENT -> {
                        PlayAllHeaderRow(
                            title = "Recently Listened (${recentlyListened.size})",
                            onPlayAll = { playerController.playAll(recentlyListened, shuffle = false) },
                            onShuffleAll = { playerController.playAll(recentlyListened, shuffle = true) },
                            isEnabled = recentlyListened.isNotEmpty()
                        )
                        SongList(
                            songs = recentlyListened,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            likedSongs = likedSongs,
                            firebaseSyncManager = firebaseSyncManager,
                            emptyMessage = "No songs listened in the last 7 days. Play music to track history!",
                            modifier = Modifier.weight(1f),
                            onSongClick = { song -> playerController.playSong(song, recentlyListened) }
                        )
                    }
                }
            }

            // ── Persistent Mini-Player Bar ────────────────────────────────────
            if (currentSong != null) {
                val isSongLiked = likedSongs.any { it.id == currentSong?.id }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showNowPlaying = true },
                    color = AmoledCard,
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
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
                        Spacer(modifier = Modifier.width(10.dp))
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

                        // Mini-Player Like Button
                        IconButton(
                            onClick = { currentSong?.let { firebaseSyncManager.toggleLike(it) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSongLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like Song",
                                tint = if (isSongLiked) HeartRed else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerController.togglePlay() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = SovereignBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerController.skipNext() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Modal Sheets ──────────────────────────────────────────────────────────
    if (showNowPlaying) {
        NowPlayingSheet(
            playerController = playerController,
            firebaseSyncManager = firebaseSyncManager,
            onOpenEqualizer = { showNowPlaying = false; showEqualizer = true },
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
    if (showUserProfile) {
        UserProfileSheet(
            firebaseSyncManager = firebaseSyncManager,
            onSignInClick = {
                try {
                    val defaultClientId = try {
                        val idRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                        if (idRes != 0) context.getString(idRes) else ""
                    } catch (_: Exception) { "" }

                    val webClientId = if (defaultClientId.isNotEmpty()) {
                        defaultClientId
                    } else {
                        "1085293847291-webclientidforgooglesignin012345.apps.googleusercontent.com"
                    }

                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val signInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(signInClient.signInIntent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Sign-In Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismissRequest = { showUserProfile = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Play All & Shuffle All Header Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayAllHeaderRow(
    title: String,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    isEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Play All Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isEnabled) SovereignBlue else AmoledCard,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isEnabled, onClick = onPlayAll)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isEnabled) AmoledBlack else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Play All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) AmoledBlack else TextSecondary
                    )
                }
            }

            // Shuffle All Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmoledCard,
                border = BorderStroke(1.dp, if (isEnabled) SovereignBlue else Color(0xFF1E293B)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isEnabled, onClick = onShuffleAll)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = if (isEnabled) SovereignBlue else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Shuffle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) SovereignBlue else TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// User Profile & Google Account Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSheet(
    firebaseSyncManager: FirebaseSyncManager,
    onSignInClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val user by firebaseSyncManager.currentUser.collectAsState()
    val likedSongs by firebaseSyncManager.likedSongs.collectAsState()
    val recentSongs by firebaseSyncManager.recentlyListened.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AmoledCard,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            if (user?.photoUrl != null) {
                AsyncImage(
                    model = user?.photoUrl,
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_serene_doll_music),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Name & Email
            val displayName = user?.displayName?.ifEmpty { null }
                ?: if (user?.isAnonymous == true) "Sovereign Guest" else "Sovereign Listener"
            Text(
                text = displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (!user?.email.isNullOrEmpty()) {
                Text(
                    text = user?.email ?: "",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloud Sync Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmoledBlack,
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SovereignBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (user?.isAnonymous == false) "Synced with Google Cloud" else "Local Guest (Auto-Sync Active)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SovereignBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${likedSongs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Liked Songs", fontSize = 12.sp, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${recentSongs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "7d History", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Google Sign In, Cloud Profile Setup, or Sign Out
            var showProfileDialog by remember { mutableStateOf(false) }

            if (user == null || user?.isAnonymous == true) {
                Button(
                    onClick = {
                        onSignInClick()
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sign In with Google",
                        color = AmoledBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showProfileDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SovereignBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set Cloud Profile & Sync DB",
                        color = SovereignBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        firebaseSyncManager.signOut()
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sign Out",
                        color = HeartRed,
                        fontSize = 14.sp
                    )
                }
            }

            if (showProfileDialog) {
                var inputName by remember { mutableStateOf(if (user?.isAnonymous == false) (user?.displayName ?: "") else "") }
                var inputEmail by remember { mutableStateOf(user?.email ?: "") }

                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    containerColor = AmoledCard,
                    title = {
                        Text(text = "Sovereign Cloud Profile", color = TextPrimary, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Set your profile name and email to establish your database and synchronize liked music & listening history to Cloud Firestore.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Display Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SovereignBlue,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text("Email (Optional)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SovereignBlue,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = inputName.trim().ifEmpty { "Sovereign Listener" }
                                val email = inputEmail.trim()
                                firebaseSyncManager.signInWithProfile(name, email)
                                showProfileDialog = false
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignBlue)
                        ) {
                            Text("Create & Sync DB", color = AmoledBlack, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showProfileDialog = false },
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchTabBar(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = AmoledBlack,
        contentColor = SovereignBlue,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = SovereignBlue
            )
        }
    ) {
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) SovereignBlue else TextSecondary
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Song list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SongList(
    songs: List<SongItem>,
    currentSong: SongItem?,
    isPlaying: Boolean,
    likedSongs: List<SongItem>,
    firebaseSyncManager: FirebaseSyncManager,
    emptyMessage: String = "No songs found",
    modifier: Modifier = Modifier,
    onSongClick: (SongItem) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyState(emptyMessage, modifier)
        return
    }
    val likedIds = remember(likedSongs) { likedSongs.map { it.id }.toSet() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongRowItem(
                song = song,
                isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                isLiked = likedIds.contains(song.id),
                onLikeToggle = { firebaseSyncManager.toggleLike(song) },
                onClick = { onSongClick(song) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Album list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AlbumList(
    albums: List<AlbumItem>,
    modifier: Modifier = Modifier,
    onAlbumClick: (AlbumItem) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState("No albums found", modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album, onClick = { onAlbumClick(album) })
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AlbumCard(album: AlbumItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = AmoledBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.artworkUrl,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (album.artist.isNotEmpty()) append(album.artist)
                        if (album.year.isNotEmpty()) {
                            if (album.artist.isNotEmpty()) append(" · ")
                            append(album.year)
                        }
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (album.songCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmoledCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${album.songCount} tracks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SovereignBlue
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Artist list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistList(
    artists: List<ArtistItem>,
    modifier: Modifier = Modifier,
    onArtistClick: (ArtistItem) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyState("No artists found", modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistCard(artist = artist, onClick = { onArtistClick(artist) })
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ArtistCard(artist: ArtistItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = AmoledBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (artist.artworkUrl.isNotEmpty()) {
                AsyncImage(
                    model = artist.artworkUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AmoledCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SovereignBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (artist.followerCount.isNotEmpty())
                        "${formatFollowers(artist.followerCount)} followers"
                    else "Artist",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledCard)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Top Songs",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SovereignBlue
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlist list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistList(
    playlists: List<PlaylistItem>,
    modifier: Modifier = Modifier,
    onPlaylistClick: (PlaylistItem) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState("No playlists found", modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist) })
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = AmoledBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (playlist.artworkUrl.isNotEmpty()) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmoledCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = SovereignBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (playlist.songCount > 0) append("${playlist.songCount} songs")
                        if (playlist.followerCount.isNotEmpty() && playlist.followerCount != "0") {
                            if (playlist.songCount > 0) append(" · ")
                            append("${formatFollowers(playlist.followerCount)} followers")
                        }
                    }.ifEmpty { "Playlist" },
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
                    text = "Play All",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SovereignBlue
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared: SongRowItem with Like Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SongRowItem(
    song: SongItem,
    isCurrentlyPlaying: Boolean,
    isLiked: Boolean = false,
    onLikeToggle: () -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isCurrentlyPlaying) Color(0xFF0F172A) else AmoledBlack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.highResArtworkUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentlyPlaying) SovereignBlue else TextPrimary,
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

            // Quick Heart / Like Action
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) HeartRed else TextSecondary,
                    modifier = Modifier.size(20.dp)
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
                    color = SovereignBlue
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = TextSecondary, fontSize = 14.sp)
    }
}

private fun formatFollowers(raw: String): String {
    val count = raw.toLongOrNull() ?: return raw
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000     -> "${count / 1_000}K"
        else               -> count.toString()
    }
}
