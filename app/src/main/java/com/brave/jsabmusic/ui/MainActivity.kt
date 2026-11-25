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
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
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
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.brave.jsabmusic.ui.components.CollectionDetailSheet
import com.brave.jsabmusic.ui.components.CollectionDetailState
import com.brave.jsabmusic.ui.theme.SpotifyBase
import com.brave.jsabmusic.ui.theme.SpotifyCard
import com.brave.jsabmusic.ui.theme.SpotifyElevated
import com.brave.jsabmusic.ui.theme.SpotifyNavBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.brave.jsabmusic.R
import com.brave.jsabmusic.api.JioSaavnApiClient
import com.brave.jsabmusic.api.LanguageHomeFeed
import com.brave.jsabmusic.api.MusicLanguage
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
            startAndBindPlaybackService()
        } catch (_: Exception) {}

        setContent {
            JSABMusicTheme {
                // Evict stale feed cache so trending data refreshes daily
                JioSaavnApiClient.evictStaleCacheIfNeeded()
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

    private fun startAndBindPlaybackService() {
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        JioSaavnApiClient.evictStaleCacheIfNeeded()
    }

    override fun onDestroy() {
        if (isServiceBound) {
            try { unbindService(serviceConnection) } catch (_: Exception) {}
            isServiceBound = false
        }
        super.onDestroy()
    }
}

private val SEARCH_TABS = listOf("Songs", "Albums", "Artists", "Playlists")
private enum class MainNavSection(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIKED_MUSIC("Liked"),
    RECENT("Recent")
}

private data class BrowseCategory(
    val title: String,
    val query: String,
    val color: Color
)

private val BROWSE_CATEGORIES = listOf(
    BrowseCategory("Malayalam Hits", "Malayalam latest hits", Color(0xFF0EA5E9)),
    BrowseCategory("Bollywood Top", "Hindi latest hits", Color(0xFFE11D48)),
    BrowseCategory("Tamil Bangers", "Tamil latest hits", Color(0xFF7C3AED)),
    BrowseCategory("Global Pop", "English pop hits", Color(0xFF059669)),
    BrowseCategory("Arijit Singh", "Arijit Singh", Color(0xFFD97706)),
    BrowseCategory("Sushin Shyam", "Sushin Shyam", Color(0xFF2563EB)),
    BrowseCategory("Anirudh Mix", "Anirudh Ravichander", Color(0xFF9333EA)),
    BrowseCategory("Chill Acoustic", "Acoustic chill hits", Color(0xFF0D9488))
)

private fun getTimeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Main Screen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun MainPlayerScreen(
    playerController: PlayerController,
    sleepTimerManager: SleepTimerManager,
    firebaseSyncManager: FirebaseSyncManager
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // ── Navigation State ─────────────────────────────────────────────────────
    var currentSection  by remember { mutableStateOf(MainNavSection.HOME) }
    var searchQuery     by remember { mutableStateOf("") }
    var selectedTab     by remember { mutableIntStateOf(0) }
    var shouldAutoFocusSearch by remember { mutableStateOf(false) }

    // ── Data States ──────────────────────────────────────────────────────────
    var songResults      by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var albumResults     by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var artistResults    by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var playlistResults  by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }

    // ── Search Pagination State ───────────────────────────────────────────────
    var songPage         by remember { mutableIntStateOf(1) }
    var albumPage        by remember { mutableIntStateOf(1) }
    var artistPage       by remember { mutableIntStateOf(1) }
    var playlistPage     by remember { mutableIntStateOf(1) }

    var hasMoreSongs     by remember { mutableStateOf(true) }
    var hasMoreAlbums    by remember { mutableStateOf(true) }
    var hasMoreArtists   by remember { mutableStateOf(true) }
    var hasMorePlaylists by remember { mutableStateOf(true) }

    var isLoading        by remember { mutableStateOf(false) }
    var isLoadingMore    by remember { mutableStateOf(false) }

    // ── Home Feed State (Top 5 Trends) ───────────────────────────────────────
    var selectedLanguage by remember { mutableStateOf(MusicLanguage.ALL) }
    var homeFeed         by remember { mutableStateOf(JioSaavnApiClient.getInstantInitialFeed(MusicLanguage.ALL)) }
    var trendingSongs    by remember { mutableStateOf(homeFeed.trendingSongs.take(5)) }
    var trendingPage     by remember { mutableIntStateOf(1) }
    var hasMoreTrending  by remember { mutableStateOf(true) }
    var isTrendingLoadingMore by remember { mutableStateOf(false) }
    var isFeedLoading    by remember { mutableStateOf(false) }

    // ── Sheets State ─────────────────────────────────────────────────────────
    var showEqualizer   by remember { mutableStateOf(false) }
    var showSleepTimer  by remember { mutableStateOf(false) }
    var showNowPlaying  by remember { mutableStateOf(false) }
    var showUserProfile by remember { mutableStateOf(false) }
    var collectionDetail by remember { mutableStateOf<CollectionDetailState?>(null) }

    // ── Reactive Flows ───────────────────────────────────────────────────────
    val currentSong     by playerController.currentSong.collectAsState()
    val isPlaying       by playerController.isPlaying.collectAsState()
    val positionMs      by playerController.currentPositionMs.collectAsState()
    val durationMs      by playerController.durationMs.collectAsState()
    val isTimerRunning  by sleepTimerManager.isTimerRunning.collectAsState()
    val currentUser     by firebaseSyncManager.currentUser.collectAsState()
    val likedSongs      by firebaseSyncManager.likedSongs.collectAsState()
    val recentlyListened by firebaseSyncManager.recentlyListened.collectAsState()
    val dislikedIds     by firebaseSyncManager.dislikedIds.collectAsState()

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

    LaunchedEffect(selectedLanguage) {
        trendingPage = 1
        hasMoreTrending = true
        val instant = JioSaavnApiClient.getInstantInitialFeed(selectedLanguage)
        if (homeFeed.language != selectedLanguage) {
            homeFeed = instant
            if (instant.trendingSongs.isNotEmpty()) {
                trendingSongs = instant.trendingSongs.take(5)
            }
        }
        try {
            isFeedLoading = true
            val feed = JioSaavnApiClient.getHomeFeedForLanguage(selectedLanguage)
            homeFeed = feed
            if (feed.trendingSongs.isNotEmpty()) {
                trendingSongs = feed.trendingSongs.take(5)
            }
        } catch (_: Exception) {
        } finally {
            isFeedLoading = false
        }
    }

    LaunchedEffect(Unit) {
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

    // ── Search Trigger & Lazy Pagination Handlers ────────────────────────────
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun triggerSearch(query: String) {
        searchJob?.cancel()
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            songResults = emptyList()
            albumResults = emptyList()
            artistResults = emptyList()
            playlistResults = emptyList()
            songPage = 1; albumPage = 1; artistPage = 1; playlistPage = 1
            hasMoreSongs = true; hasMoreAlbums = true; hasMoreArtists = true; hasMorePlaylists = true
            isLoading = false
            return
        }
        searchJob = scope.launch {
            delay(280) // Debounce keystrokes to ensure smooth, responsive typing
            isLoading = true
            songPage = 1; albumPage = 1; artistPage = 1; playlistPage = 1
            try {
                val results = JioSaavnApiClient.searchAll(cleanQuery)
                songResults      = results.songs
                albumResults     = results.albums
                artistResults    = results.artists
                playlistResults  = results.playlists
                hasMoreSongs     = results.songs.size >= 10
                hasMoreAlbums    = results.albums.size >= 5
                hasMoreArtists   = results.artists.size >= 5
                hasMorePlaylists = results.playlists.size >= 5
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreSongs() {
        val cleanQuery = searchQuery.trim()
        if (isLoadingMore || !hasMoreSongs || cleanQuery.length < 2) return
        scope.launch {
            isLoadingMore = true
            val nextPage = songPage + 1
            val more = JioSaavnApiClient.searchSongsPage(cleanQuery, page = nextPage, count = 10)
            if (more.isEmpty()) {
                hasMoreSongs = false
            } else {
                val currentList = songResults.toMutableList()
                val newItems = more.filter { m -> currentList.none { it.id == m.id } }
                if (newItems.isEmpty() || more.size < 10) {
                    hasMoreSongs = false
                }
                songResults = (currentList + newItems)
                songPage = nextPage
            }
            isLoadingMore = false
        }
    }

    fun loadMoreAlbums() {
        val cleanQuery = searchQuery.trim()
        if (isLoadingMore || !hasMoreAlbums || cleanQuery.length < 2) return
        scope.launch {
            isLoadingMore = true
            val nextPage = albumPage + 1
            val more = JioSaavnApiClient.searchAlbumsPage(cleanQuery, page = nextPage, count = 10)
            if (more.isEmpty()) {
                hasMoreAlbums = false
            } else {
                val currentList = albumResults.toMutableList()
                val newItems = more.filter { m -> currentList.none { it.id == m.id } }
                if (newItems.isEmpty() || more.size < 10) {
                    hasMoreAlbums = false
                }
                albumResults = (currentList + newItems)
                albumPage = nextPage
            }
            isLoadingMore = false
        }
    }

    fun loadMoreArtists() {
        val cleanQuery = searchQuery.trim()
        if (isLoadingMore || !hasMoreArtists || cleanQuery.length < 2) return
        scope.launch {
            isLoadingMore = true
            val nextPage = artistPage + 1
            val more = JioSaavnApiClient.searchArtistsPage(cleanQuery, page = nextPage, count = 10)
            if (more.isEmpty()) {
                hasMoreArtists = false
            } else {
                val currentList = artistResults.toMutableList()
                val newItems = more.filter { m -> currentList.none { it.id == m.id } }
                if (newItems.isEmpty() || more.size < 10) {
                    hasMoreArtists = false
                }
                artistResults = (currentList + newItems)
                artistPage = nextPage
            }
            isLoadingMore = false
        }
    }

    fun loadMorePlaylists() {
        val cleanQuery = searchQuery.trim()
        if (isLoadingMore || !hasMorePlaylists || cleanQuery.length < 2) return
        scope.launch {
            isLoadingMore = true
            val nextPage = playlistPage + 1
            val more = JioSaavnApiClient.searchPlaylistsPage(cleanQuery, page = nextPage, count = 10)
            if (more.isEmpty()) {
                hasMorePlaylists = false
            } else {
                val currentList = playlistResults.toMutableList()
                val newItems = more.filter { m -> currentList.none { it.id == m.id } }
                if (newItems.isEmpty() || more.size < 10) {
                    hasMorePlaylists = false
                }
                playlistResults = (currentList + newItems)
                playlistPage = nextPage
            }
            isLoadingMore = false
        }
    }

    fun loadMoreTrending() {
        if (isTrendingLoadingMore || !hasMoreTrending) return
        scope.launch {
            isTrendingLoadingMore = true
            val nextPage = trendingPage + 1
            val more = JioSaavnApiClient.fetchMoreTrendingSongs(selectedLanguage, page = nextPage, count = 5)
            if (more.isEmpty()) {
                hasMoreTrending = false
            } else {
                val currentList = trendingSongs.toMutableList()
                val newItems = more.filter { m -> currentList.none { it.id == m.id } }
                if (newItems.isEmpty() || more.size < 5) {
                    hasMoreTrending = false
                }
                trendingSongs = (currentList + newItems)
                trendingPage = nextPage
            }
            isTrendingLoadingMore = false
        }
    }

    fun refreshCurrentFeed() {
        scope.launch {
            isFeedLoading = true
            trendingPage = 1
            hasMoreTrending = true
            JioSaavnApiClient.clearHomeFeedCache()
            val feed = JioSaavnApiClient.getHomeFeedForLanguage(selectedLanguage, forceRefresh = true)
            homeFeed = feed
            if (feed.trendingSongs.isNotEmpty()) {
                trendingSongs = feed.trendingSongs.take(5)
            }
            isFeedLoading = false
        }
    }

    fun openAlbumDetail(album: AlbumItem) {
        collectionDetail = CollectionDetailState(
            title = album.name,
            subtitle = buildString {
                if (album.artist.isNotEmpty()) append(album.artist)
                if (album.year.isNotEmpty()) {
                    if (album.artist.isNotEmpty()) append(" · ")
                    append(album.year)
                }
            }.ifEmpty { "Album" },
            badge = "ALBUM",
            artworkUrl = album.artworkUrl,
            isArtist = false,
            isLoading = true
        )
        scope.launch {
            val songs = JioSaavnApiClient.getAlbumSongs(album.id, album.name)
            collectionDetail = collectionDetail?.copy(isLoading = false, songs = songs)
        }
    }

    fun openArtistDetail(artist: ArtistItem) {
        collectionDetail = CollectionDetailState(
            title = artist.name,
            subtitle = if (artist.followerCount.isNotEmpty())
                "${formatFollowers(artist.followerCount)} followers" else "Artist",
            badge = "ARTIST TOP TRACKS",
            artworkUrl = artist.artworkUrl,
            isArtist = true,
            isLoading = true
        )
        scope.launch {
            val songs = JioSaavnApiClient.getArtistSongs(artist.id, artist.name)
            collectionDetail = collectionDetail?.copy(isLoading = false, songs = songs)
        }
    }

    fun openPlaylistDetail(playlist: PlaylistItem) {
        collectionDetail = CollectionDetailState(
            title = playlist.name,
            subtitle = if (playlist.songCount > 0) "${playlist.songCount} tracks" else "Playlist",
            badge = "PLAYLIST",
            artworkUrl = playlist.artworkUrl,
            isArtist = false,
            isLoading = true
        )
        scope.launch {
            val songs = JioSaavnApiClient.getPlaylistSongs(playlist.id, playlist.name)
            collectionDetail = collectionDetail?.copy(isLoading = false, songs = songs)
        }
    }

    // Auto-focus Search input & show keyboard cleanly when entering Search tab
    LaunchedEffect(currentSection, shouldAutoFocusSearch) {
        if (currentSection == MainNavSection.SEARCH && shouldAutoFocusSearch) {
            delay(60)
            try {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
            shouldAutoFocusSearch = false
        }
    }

    // ── Spotify-Grade Layout ─────────────────────────────────────────────────
    Scaffold(
        containerColor = AmoledBlack,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, AmoledBlack.copy(alpha = 0.96f), AmoledBlack)
                        )
                    )
            ) {
                // Persistent Spotify-style Mini-Player with live 2dp progress bar
                if (currentSong != null) {
                    val isSongLiked = likedSongs.any { it.id == currentSong?.id }
                    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showNowPlaying = true },
                        color = SpotifyCard,
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSong?.title ?: "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentSong?.artist ?: "",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

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
                                        modifier = Modifier.size(26.dp)
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
                            // Thin Spotify-style live progress bar at the bottom of Mini-Player
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = SovereignBlue,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                // Spotify-Style Bottom Navigation Bar
                Surface(
                    color = SpotifyNavBar,
                    border = BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainNavSection.values().forEach { section ->
                            val isSelected = currentSection == section
                            val icon = when (section) {
                                MainNavSection.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                MainNavSection.SEARCH -> if (isSelected) Icons.Filled.Search else Icons.Outlined.Search
                                MainNavSection.LIKED_MUSIC -> if (isSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                                MainNavSection.RECENT -> if (isSelected) Icons.Filled.History else Icons.Outlined.History
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (section == MainNavSection.SEARCH) {
                                            shouldAutoFocusSearch = true
                                        } else {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                        currentSection = section
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = section.label,
                                    tint = if (isSelected) SovereignBlue else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = section.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SovereignBlue else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ── Spotify-Style Top Greeting Header ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = getTimeGreeting(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "JSAB Music · 320K Sovereign",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SovereignBlueAccent
                        )
                    }
                }

                // Action Controls Pill + Refresh + User Profile
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SpotifyCard,
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { refreshCurrentFeed() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Trends",
                                tint = if (isFeedLoading) SovereignBlue else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = { showEqualizer = true },
                            modifier = Modifier.size(32.dp)
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
                            modifier = Modifier.size(32.dp)
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
                            modifier = Modifier.size(32.dp)
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

            // ── Section Content ──────────────────────────────────────────────
            when (currentSection) {
                MainNavSection.HOME -> {
                    // Quick Search Pill (Spotify-style: tap to jump straight into Search + Keyboard)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SpotifyCard,
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                shouldAutoFocusSearch = true
                                currentSection = MainNavSection.SEARCH
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SovereignBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "What do you want to listen to?",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LanguageFilterChips(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { lang ->
                            if (selectedLanguage != lang) {
                                selectedLanguage = lang
                                val instant = JioSaavnApiClient.getInstantInitialFeed(lang)
                                homeFeed = instant
                                trendingSongs = instant.trendingSongs.take(5)
                                trendingPage = 1
                                hasMoreTrending = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val visibleTrendingHeader = trendingSongs.filter { it.id !in dislikedIds }
                    PlayAllHeaderRow(
                        title = "Today's Top ${visibleTrendingHeader.size} · ${selectedLanguage.displayName}",
                        onPlayAll = { playerController.playAll(visibleTrendingHeader, shuffle = false) },
                        onShuffleAll = { playerController.playAll(visibleTrendingHeader, shuffle = true) },
                        isEnabled = visibleTrendingHeader.isNotEmpty()
                    )

                    ExploreLanguageHub(
                        homeFeed = homeFeed,
                        trendingSongs = trendingSongs,
                        recentlyListened = recentlyListened.filter { it.id !in dislikedIds },
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        likedSongs = likedSongs,
                        dislikedIds = dislikedIds,
                        firebaseSyncManager = firebaseSyncManager,
                        playerController = playerController,
                        isFeedLoading = isFeedLoading,
                        hasMoreTrending = hasMoreTrending,
                        isTrendingLoadingMore = isTrendingLoadingMore,
                        onLoadMoreTrending = { loadMoreTrending() },
                        onAlbumClick = { album -> openAlbumDetail(album) },
                        onArtistClick = { artist -> openArtistDetail(artist) },
                        onPlaylistClick = { playlist -> openPlaylistDetail(playlist) },
                        onRadioPlay = { song ->
                            scope.launch {
                                isLoading = true
                                val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                                isLoading = false
                                val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                                if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                MainNavSection.SEARCH -> {
                    // Instant-Focus Spotify Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            triggerSearch(query)
                        },
                        placeholder = {
                            Text("What do you want to listen to?", color = TextSecondary, fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = SovereignBlue
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    songResults = emptyList(); albumResults = emptyList()
                                    artistResults = emptyList(); playlistResults = emptyList()
                                    selectedTab = 0
                                    searchFocusRequester.requestFocus()
                                    keyboardController?.show()
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
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = SpotifyElevated,
                            unfocusedContainerColor = SpotifyCard,
                            focusedBorderColor      = SovereignBlue,
                            unfocusedBorderColor    = Color(0xFF1E293B),
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = SovereignBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    keyboardController?.show()
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (searchQuery.length >= 2) {
                        SearchTabBar(
                            selectedTab = selectedTab,
                            tabs = SEARCH_TABS,
                            onTabSelected = { selectedTab = it }
                        )
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp)
                                    .height(2.dp),
                                color = SovereignBlue,
                                trackColor = AmoledCard
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val isInitialSearchEmpty = isLoading && songResults.isEmpty() && albumResults.isEmpty() && artistResults.isEmpty() && playlistResults.isEmpty()
                        if (isInitialSearchEmpty) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = SovereignBlue)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Searching JioSaavn...", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            when (selectedTab) {
                                0 -> SongList(
                                    songs = songResults.filter { it.id !in dislikedIds },
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    likedSongs = likedSongs,
                                    dislikedIds = dislikedIds,
                                    firebaseSyncManager = firebaseSyncManager,
                                    playerController = playerController,
                                    hasMore = hasMoreSongs,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = { loadMoreSongs() },
                                    modifier = Modifier.weight(1f),
                                    onSongClick = { song -> playerController.playSong(song, songResults.filter { it.id !in dislikedIds }) },
                                    onRadioPlay = { song ->
                                        scope.launch {
                                            isLoading = true
                                            val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                                            isLoading = false
                                            val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                                            if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                                        }
                                    }
                                )
                                1 -> AlbumList(
                                    albums = albumResults,
                                    hasMore = hasMoreAlbums,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = { loadMoreAlbums() },
                                    modifier = Modifier.weight(1f),
                                    onAlbumClick = { album -> openAlbumDetail(album) },
                                    onRadioClick = { album ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getAlbumSongs(album.id, album.name).filter { it.id !in dislikedIds }
                                            if (songs.isNotEmpty()) {
                                                val radioQueue = JioSaavnApiClient.getRadioSongs(songs.first()).filter { it.id !in dislikedIds }
                                                val fullQueue = (songs + radioQueue).distinctBy { it.id }
                                                playerController.playSong(fullQueue.first(), fullQueue)
                                            }
                                            isLoading = false
                                        }
                                    }
                                )
                                2 -> ArtistList(
                                    artists = artistResults,
                                    hasMore = hasMoreArtists,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = { loadMoreArtists() },
                                    modifier = Modifier.weight(1f),
                                    onArtistClick = { artist -> openArtistDetail(artist) },
                                    onRadioClick = { artist ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getArtistSongs(artist.id, artist.name).filter { it.id !in dislikedIds }
                                            if (songs.isNotEmpty()) {
                                                val radioQueue = JioSaavnApiClient.getRadioSongs(songs.first()).filter { it.id !in dislikedIds }
                                                val fullQueue = (songs + radioQueue).distinctBy { it.id }
                                                playerController.playSong(fullQueue.first(), fullQueue)
                                            }
                                            isLoading = false
                                        }
                                    }
                                )
                                3 -> PlaylistList(
                                    playlists = playlistResults,
                                    hasMore = hasMorePlaylists,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = { loadMorePlaylists() },
                                    modifier = Modifier.weight(1f),
                                    onPlaylistClick = { playlist -> openPlaylistDetail(playlist) },
                                    onRadioClick = { playlist ->
                                        scope.launch {
                                            isLoading = true
                                            val songs = JioSaavnApiClient.getPlaylistSongs(playlist.id, playlist.name).filter { it.id !in dislikedIds }
                                            if (songs.isNotEmpty()) {
                                                val radioQueue = JioSaavnApiClient.getRadioSongs(songs.first()).filter { it.id !in dislikedIds }
                                                val fullQueue = (songs + radioQueue).distinctBy { it.id }
                                                playerController.playSong(fullQueue.first(), fullQueue)
                                            }
                                            isLoading = false
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Spotify-style Browse All Moods & Languages Grid
                        Text(
                            text = "Browse All",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(BROWSE_CATEGORIES) { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cat.color.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, cat.color.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .height(78.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            searchQuery = cat.query
                                            triggerSearch(cat.query)
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = cat.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MainNavSection.LIKED_MUSIC -> {
                    val visibleLiked = likedSongs.filter { it.id !in dislikedIds }
                    PlayAllHeaderRow(
                        title = "Liked Songs (${visibleLiked.size})",
                        onPlayAll = { playerController.playAll(visibleLiked, shuffle = false) },
                        onShuffleAll = { playerController.playAll(visibleLiked, shuffle = true) },
                        isEnabled = visibleLiked.isNotEmpty()
                    )
                    SongList(
                        songs = visibleLiked,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        likedSongs = likedSongs,
                        dislikedIds = dislikedIds,
                        firebaseSyncManager = firebaseSyncManager,
                        playerController = playerController,
                        emptyMessage = "No liked songs yet. Tap the heart on any song to add it here!",
                        modifier = Modifier.weight(1f),
                        onSongClick = { song -> playerController.playSong(song, visibleLiked) },
                        onRadioPlay = { song ->
                            scope.launch {
                                isLoading = true
                                val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                                isLoading = false
                                val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                                if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                            }
                        }
                    )
                }

                MainNavSection.RECENT -> {
                    val visibleRecent = recentlyListened.filter { it.id !in dislikedIds }
                    PlayAllHeaderRow(
                        title = "Recently Played (${visibleRecent.size})",
                        onPlayAll = { playerController.playAll(visibleRecent, shuffle = false) },
                        onShuffleAll = { playerController.playAll(visibleRecent, shuffle = true) },
                        isEnabled = visibleRecent.isNotEmpty()
                    )
                    SongList(
                        songs = visibleRecent,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        likedSongs = likedSongs,
                        dislikedIds = dislikedIds,
                        firebaseSyncManager = firebaseSyncManager,
                        playerController = playerController,
                        emptyMessage = "No songs listened in the last 7 days. Play music to track history!",
                        modifier = Modifier.weight(1f),
                        onSongClick = { song -> playerController.playSong(song, visibleRecent) },
                        onRadioPlay = { song ->
                            scope.launch {
                                isLoading = true
                                val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                                isLoading = false
                                val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                                if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Modal Sheets ─────────────────────────────────────────────────────────
    collectionDetail?.let { detail ->
        CollectionDetailSheet(
            state = detail,
            currentSong = currentSong,
            isPlaying = isPlaying,
            likedSongs = likedSongs,
            dislikedIds = dislikedIds,
            firebaseSyncManager = firebaseSyncManager,
            playerController = playerController,
            onPlayAll = { songs -> playerController.playAll(songs, shuffle = false) },
            onShuffleAll = { songs -> playerController.playAll(songs, shuffle = true) },
            onRadioPlay = { song ->
                scope.launch {
                    isLoading = true
                    val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                    isLoading = false
                    val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                    if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                }
            },
            onDismissRequest = { collectionDetail = null }
        )
    }
    if (showNowPlaying) {
        NowPlayingSheet(
            playerController = playerController,
            firebaseSyncManager = firebaseSyncManager,
            onOpenEqualizer = { showNowPlaying = false; showEqualizer = true },
            onRadioPlay = { song ->
                scope.launch {
                    isLoading = true
                    val radioQueue = JioSaavnApiClient.getRadioSongs(song)
                    isLoading = false
                    val filtered = (listOf(song) + radioQueue).filter { it.id !in dislikedIds }
                    if (filtered.isNotEmpty()) playerController.playSong(filtered.first(), filtered)
                }
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
                        "547731593917-hmf6f180abaurt5rsf6ktelqthukrvn5.apps.googleusercontent.com"
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Play All & Shuffle All Header Row
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// User Profile & Google Account Sheet
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSheet(
    firebaseSyncManager: FirebaseSyncManager,
    onSignInClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Tab bar
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// ─────────────────────────────────────────────────────────────────────────
// Load More Pagination Button
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadMoreButton(
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AmoledCard,
            border = BorderStroke(1.dp, SovereignBlue.copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !isLoading, onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = SovereignBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading more...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SovereignBlueAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SovereignBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Song list
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun SongList(
    songs: List<SongItem>,
    currentSong: SongItem?,
    isPlaying: Boolean,
    likedSongs: List<SongItem>,
    firebaseSyncManager: FirebaseSyncManager,
    playerController: PlayerController? = null,
    dislikedIds: Set<String> = emptySet(),
    emptyMessage: String = "No songs found",
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSongClick: (SongItem) -> Unit,
    onRadioPlay: ((SongItem) -> Unit)? = null
) {
    val likedIds = remember(likedSongs) { likedSongs.map { it.id }.toSet() }
    val visibleSongs = remember(songs, dislikedIds) {
        songs.filter { it.id !in dislikedIds }
    }

    if (visibleSongs.isEmpty()) {
        EmptyState(emptyMessage, modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    playerController?.removeSongFromQueue(song.id)
                },
                onRadioPlay = onRadioPlay?.let { cb -> { cb(song) } },
                onClick = { onSongClick(song) }
            )
        }
        if (hasMore && onLoadMore != null) {
            item {
                LoadMoreButton(
                    label = "Load More Songs (10+)",
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Album list
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun AlbumList(
    albums: List<AlbumItem>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onAlbumClick: (AlbumItem) -> Unit,
    onRadioClick: ((AlbumItem) -> Unit)? = null
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
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onRadioClick = onRadioClick?.let { cb -> { cb(album) } }
            )
        }
        if (hasMore && onLoadMore != null) {
            item {
                LoadMoreButton(
                    label = "Load More Albums (10+)",
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumItem,
    onClick: () -> Unit,
    onRadioClick: (() -> Unit)? = null
) {
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
            if (onRadioClick != null) {
                IconButton(
                    onClick = onRadioClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Play Album Radio",
                        tint = SovereignBlueAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
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

// ─────────────────────────────────────────────────────────────────────────
// Artist list
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistList(
    artists: List<ArtistItem>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onArtistClick: (ArtistItem) -> Unit,
    onRadioClick: ((ArtistItem) -> Unit)? = null
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
            ArtistCard(
                artist = artist,
                onClick = { onArtistClick(artist) },
                onRadioClick = onRadioClick?.let { cb -> { cb(artist) } }
            )
        }
        if (hasMore && onLoadMore != null) {
            item {
                LoadMoreButton(
                    label = "Load More Artists (10+)",
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ArtistCard(
    artist: ArtistItem,
    onClick: () -> Unit,
    onRadioClick: (() -> Unit)? = null
) {
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
            if (onRadioClick != null) {
                IconButton(
                    onClick = onRadioClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Play Artist Radio",
                        tint = SovereignBlueAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
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

// ─────────────────────────────────────────────────────────────────────────
// Playlist list
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistList(
    playlists: List<PlaylistItem>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onRadioClick: ((PlaylistItem) -> Unit)? = null
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
            PlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onRadioClick = onRadioClick?.let { cb -> { cb(playlist) } }
            )
        }
        if (hasMore && onLoadMore != null) {
            item {
                LoadMoreButton(
                    label = "Load More Playlists (10+)",
                    isLoading = isLoadingMore,
                    onClick = onLoadMore
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    onRadioClick: (() -> Unit)? = null
) {
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
            if (onRadioClick != null) {
                IconButton(
                    onClick = onRadioClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Play Playlist Radio",
                        tint = SovereignBlueAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Shared: SongRowItem with Like Button
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun SongRowItem(
    song: SongItem,
    isCurrentlyPlaying: Boolean,
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    onLikeToggle: () -> Unit = {},
    onDislikeToggle: () -> Unit = {},
    onRadioPlay: (() -> Unit)? = null,
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

            // Radio Button — only shown when the context supports Radio
            if (onRadioPlay != null) {
                IconButton(
                    onClick = onRadioPlay,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Play as Radio",
                        tint = SovereignBlueAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
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

            // Dislike Button — marks song as "never play again"
            IconButton(
                onClick = onDislikeToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = if (isDisliked) "Remove Dislike" else "Dislike (Never play again)",
                    tint = if (isDisliked) Color(0xFFEF4444) else TextSecondary,
                    modifier = Modifier.size(17.dp)
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Helpers
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Multi-Language Home Hub Components
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun LanguageFilterChips(
    selectedLanguage: MusicLanguage,
    onLanguageSelected: (MusicLanguage) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MusicLanguage.values(), key = { it.code }) { lang ->
            val isSelected = lang == selectedLanguage
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) SovereignBlue else AmoledCard,
                border = BorderStroke(1.dp, if (isSelected) SovereignBlue else Color(0xFF1E293B)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onLanguageSelected(lang) }
            ) {
                Text(
                    text = lang.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AmoledBlack else TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun HorizontalAlbumRow(
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAlbumClick(album) }
            ) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = album.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist.ifEmpty { "Album" },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HorizontalArtistRow(
    artists: List<ArtistItem>,
    onArtistClick: (ArtistItem) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onArtistClick(artist) }
            ) {
                if (artist.artworkUrl.isNotEmpty()) {
                    AsyncImage(
                        model = artist.artworkUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(AmoledCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SovereignBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = artist.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HorizontalPlaylistRow(
    playlists: List<PlaylistItem>,
    onPlaylistClick: (PlaylistItem) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick(playlist) }
            ) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = playlist.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} tracks",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ExploreLanguageHub(
    homeFeed: LanguageHomeFeed?,
    trendingSongs: List<SongItem>,
    recentlyListened: List<SongItem> = emptyList(),
    currentSong: SongItem?,
    isPlaying: Boolean,
    likedSongs: List<SongItem>,
    dislikedIds: Set<String> = emptySet(),
    firebaseSyncManager: FirebaseSyncManager,
    playerController: PlayerController,
    isFeedLoading: Boolean,
    hasMoreTrending: Boolean = false,
    isTrendingLoadingMore: Boolean = false,
    onLoadMoreTrending: (() -> Unit)? = null,
    onAlbumClick: (AlbumItem) -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onRadioPlay: ((SongItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val likedIds = remember(likedSongs) { likedSongs.map { it.id }.toSet() }
    val visibleTrending = remember(trendingSongs, dislikedIds) {
        trendingSongs.filter { it.id !in dislikedIds }
    }

    if (isFeedLoading && trendingSongs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SovereignBlue)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Spotify-style 2-column "Jump back in" Quick Picks Grid
        val quickPicks = recentlyListened.take(4)
        if (quickPicks.size >= 2) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickPicks.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { song ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SpotifyCard,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { playerController.playSong(song, recentlyListened) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = song.highResArtworkUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = song.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (currentSong?.id == song.id) SovereignBlue else TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        // Trending songs (starts with Top 5, grows with on-demand pagination)
        items(visibleTrending, key = { it.id }) { song ->
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
                onRadioPlay = onRadioPlay?.let { cb -> { cb(song) } },
                onClick = { playerController.playSong(song, visibleTrending) }
            )
        }

        // On-demand pagination for trending songs
        if (hasMoreTrending && onLoadMoreTrending != null) {
            item {
                LoadMoreButton(
                    label = "Load More Trending (Top 5)",
                    isLoading = isTrendingLoadingMore,
                    onClick = onLoadMoreTrending
                )
            }
        }

        // Top 5 Albums Section
        if (homeFeed?.topAlbums?.isNotEmpty() == true) {
            item {
                SectionHeader("Top 5 Albums")
                HorizontalAlbumRow(
                    albums = homeFeed.topAlbums.take(5),
                    onAlbumClick = onAlbumClick
                )
            }
        }

        // Popular Artists Section (Top 5)
        if (homeFeed?.topArtists?.isNotEmpty() == true) {
            item {
                SectionHeader("Popular Artists")
                HorizontalArtistRow(
                    artists = homeFeed.topArtists.take(5),
                    onArtistClick = onArtistClick
                )
            }
        }

        // Featured Playlists Section (Top 5)
        if (homeFeed?.topPlaylists?.isNotEmpty() == true) {
            item {
                SectionHeader("Featured Playlists")
                HorizontalPlaylistRow(
                    playlists = homeFeed.topPlaylists.take(5),
                    onPlaylistClick = onPlaylistClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}
