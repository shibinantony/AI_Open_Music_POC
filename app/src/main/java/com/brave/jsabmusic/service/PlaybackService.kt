package com.brave.jsabmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.brave.jsabmusic.R
import com.brave.jsabmusic.player.PlayerController
import com.brave.jsabmusic.ui.MainActivity

/**
 * Enterprise Samsung One UI & Android 14/15/16 Compliant MediaSessionService.
 * Exposes lock-screen media controls widget, AOD integration, hardware headset triggers,
 * and maintains resilient background playback like Spotify using START_STICKY, Foreground Service,
 * and CPU / Wi-Fi keep-alive locks.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val CHANNEL_ID = "jsab_music_playback_v4"
        const val NOTIFICATION_ID = 1001
        var playerControllerInstance: PlayerController? = null
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        try {
            val controller = playerControllerInstance ?: PlayerController(applicationContext).also {
                playerControllerInstance = it
            }

            // Notification provider with custom public lockscreen channel and monochrome vector icon
            val notificationProvider = DefaultMediaNotificationProvider.Builder(applicationContext)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
            setMediaNotificationProvider(notificationProvider)

            // PendingIntent to bring user back to MainActivity when tapping the lockscreen or notification widget
            val sessionActivityIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // MediaSession callback ensuring lockscreen next, prev, play/pause are always accepted
            val sessionCallback = object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_PREPARE)
                        .add(Player.COMMAND_STOP)
                        .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }

                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            playerControllerInstance?.skipNext()
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            playerControllerInstance?.skipPrevious()
                            return SessionResult.RESULT_SUCCESS
                        }
                    }
                    return super.onPlayerCommandRequest(session, controllerInfo, playerCommand)
                }
            }

            mediaSession = MediaSession.Builder(this, controller.exoPlayer)
                .setSessionActivity(sessionActivityIntent)
                .setCallback(sessionCallback)
                .build()

            // Manage hardware CPU and Wi-Fi keep-alives during active streaming
            controller.exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        acquireHardwareLocks()
                    } else {
                        releaseHardwareLocks()
                    }
                }
            })
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // START_STICKY instructs Android to recreate the playback service if terminated under extreme pressure
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // If music is actively playing when the user swipes away the app from recent tasks,
        // keep playing continuously in the background like Spotify.
        if (player != null && (player.playWhenReady && player.playbackState != Player.STATE_ENDED)) {
            // Do not terminate; retain background playback
        } else {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH   // HIGH required on Samsung One UI for lock screen widget
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)  // Don't bypass DnD to respect user preference
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireHardwareLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "JSABMusic::Media3WakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours maximum safety timeout
            }

            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "JSABMusic::Media3WifiLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseHardwareLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        releaseHardwareLocks()
        try {
            mediaSession?.run {
                release()
                mediaSession = null
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
