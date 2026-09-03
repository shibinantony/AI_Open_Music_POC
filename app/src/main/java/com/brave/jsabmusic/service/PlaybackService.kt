package com.brave.jsabmusic.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.brave.jsabmusic.R
import com.brave.jsabmusic.bridge.PlaybackStateData
import com.brave.jsabmusic.bridge.WebInterfaceBridge
import com.brave.jsabmusic.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Android 14+ Compliant Foreground Service for JioSaavn background playback.
 * Manages MediaSessionCompat, Lock-Screen Controls, WakeLock, and High-Performance WifiLock.
 */
class PlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private var webBridge: WebInterfaceBridge? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var currentArtworkUrl = ""
    private var currentArtworkBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "jsab_music_playback"
        const val NOTIFICATION_ID = 2002
        const val ACTION_PLAY = "com.brave.jsabmusic.ACTION_PLAY"
        const val ACTION_PAUSE = "com.brave.jsabmusic.ACTION_PAUSE"
        const val ACTION_NEXT = "com.brave.jsabmusic.ACTION_NEXT"
        const val ACTION_PREV = "com.brave.jsabmusic.ACTION_PREV"
        const val ACTION_STOP = "com.brave.jsabmusic.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initializeMediaSession()
        acquireLocks()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setBridge(bridge: WebInterfaceBridge) {
        this.webBridge = bridge
        serviceScope.launch {
            bridge.playbackState.collect { state ->
                updatePlaybackState(state)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "JSABMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    webBridge?.play()
                }

                override fun onPause() {
                    webBridge?.pause()
                }

                override fun onSkipToNext() {
                    webBridge?.next()
                }

                override fun onSkipToPrevious() {
                    webBridge?.previous()
                }

                override fun onSeekTo(pos: Long) {
                    webBridge?.seekTo(pos / 1000)
                }

                override fun onStop() {
                    webBridge?.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun updatePlaybackState(state: PlaybackStateData) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                state.positionSeconds * 1000,
                1.0f
            )
            .build()

        mediaSession.setPlaybackState(playbackState)

        // Load Album Art asynchronously if changed
        if (state.artUrl.isNotEmpty() && state.artUrl != currentArtworkUrl) {
            currentArtworkUrl = state.artUrl
            serviceScope.launch {
                val bitmap = fetchBitmap(state.artUrl)
                currentArtworkBitmap = bitmap
                updateMetadata(state, bitmap)
                val notification = buildNotification(state, bitmap)
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            updateMetadata(state, currentArtworkBitmap)
            val notification = buildNotification(state, currentArtworkBitmap)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMetadata(state: PlaybackStateData, artBitmap: Bitmap?) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state.title.ifEmpty { "JioSaavn Audio" })
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, state.artist.ifEmpty { "JSAB Music" })
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, state.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationSeconds * 1000)

        artBitmap?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun buildNotification(state: PlaybackStateData, artBitmap: Bitmap?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (state.isPlaying) {
            val pauseIntent = PendingIntent.getService(
                this, 2,
                Intent(this, PlaybackService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_pause),
                pauseIntent
            )
        } else {
            val playIntent = PendingIntent.getService(
                this, 2,
                Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY },
                PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.action_play),
                playIntent
            )
        }

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(state.title.ifEmpty { "JioSaavn Music" })
            .setContentText(state.artist.ifEmpty { "JSAB Shielded Audio" })
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.action_previous), prevIntent)
            .addAction(playPauseAction)
            .addAction(android.R.drawable.ic_media_next, getString(R.string.action_next), nextIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        artBitmap?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private suspend fun fetchBitmap(urlStr: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            BitmapFactory.decodeStream(url.openStream())
        } catch (e: Exception) {
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> webBridge?.play()
            ACTION_PAUSE -> webBridge?.pause()
            ACTION_NEXT -> webBridge?.next()
            ACTION_PREV -> webBridge?.previous()
            ACTION_STOP -> {
                webBridge?.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JSABMusic::MediaWakeLock"
        ).apply {
            acquire()
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "JSABMusic::MediaWifiLock"
        ).apply {
            acquire()
        }
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {}
        mediaSession.release()
        super.onDestroy()
    }
}
