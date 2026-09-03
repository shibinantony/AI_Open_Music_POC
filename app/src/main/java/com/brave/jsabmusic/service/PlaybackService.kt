package com.brave.jsabmusic.service

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.brave.jsabmusic.player.PlayerController

/**
 * Android 14+ / API 34+ Compliant AndroidX MediaSessionService.
 * Exposes system lock-screen notification controls, Bluetooth hardware triggers,
 * and maintains continuous background audio via CPU and Wi-Fi keep-alives during active playback.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        var playerControllerInstance: PlayerController? = null
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val controller = playerControllerInstance ?: PlayerController(applicationContext).also {
                playerControllerInstance = it
            }

            mediaSession = MediaSession.Builder(this, controller.exoPlayer).build()

            // Only acquire hardware keep-alives when playback is actively running
            controller.exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        acquireHardwareLocks()
                    } else {
                        releaseHardwareLocks()
                    }
                }
            })
        } catch (e: Exception) {
            // Initialization handled safely
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
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
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // 2 hours max safety timeout
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
        } catch (e: Exception) {}
    }

    private fun releaseHardwareLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        releaseHardwareLocks()
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
