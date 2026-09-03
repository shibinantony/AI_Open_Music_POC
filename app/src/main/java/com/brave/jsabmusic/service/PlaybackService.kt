package com.brave.jsabmusic.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.brave.jsabmusic.player.PlayerController

/**
 * Android 14+ / API 34+ Compliant AndroidX MediaSessionService.
 * Exposes system lock-screen notification controls, Bluetooth hardware triggers,
 * and maintains continuous background audio via CPU and Wi-Fi keep-alives.
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
        acquireHardwareLocks()

        val controller = playerControllerInstance ?: PlayerController(applicationContext).also {
            playerControllerInstance = it
        }

        mediaSession = MediaSession.Builder(this, controller.exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireHardwareLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JSABMusic::Media3WakeLock"
        ).apply {
            acquire()
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "JSABMusic::Media3WifiLock"
        ).apply {
            acquire()
        }
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {}

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
