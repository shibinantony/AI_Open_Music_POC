package com.brave.jsabmusic.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Bi-directional JavaScript interface bridging JioSaavn Web DOM with Android OS.
 */
class WebInterfaceBridge(webView: WebView) {

    private val webViewRef = WeakReference(webView)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackStateData())
    val playbackState: StateFlow<PlaybackStateData> = _playbackState.asStateFlow()

    @JavascriptInterface
    fun onPlaybackStateChanged(
        isPlaying: Boolean,
        title: String,
        artist: String,
        album: String,
        artUrl: String,
        duration: Long,
        position: Long
    ) {
        _playbackState.value = PlaybackStateData(
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            album = album,
            artUrl = artUrl,
            durationSeconds = duration,
            positionSeconds = position
        )
    }

    fun play() {
        evaluateScript("window.bravePlayer && window.bravePlayer.play();")
    }

    fun pause() {
        evaluateScript("window.bravePlayer && window.bravePlayer.pause();")
    }

    fun togglePlay() {
        evaluateScript("window.bravePlayer && window.bravePlayer.togglePlay();")
    }

    fun next() {
        evaluateScript("window.bravePlayer && window.bravePlayer.next();")
    }

    fun previous() {
        evaluateScript("window.bravePlayer && window.bravePlayer.previous();")
    }

    fun seekTo(seconds: Long) {
        evaluateScript("window.bravePlayer && window.bravePlayer.seekTo($seconds);")
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        evaluateScript("window.bravePlayer && window.bravePlayer.setVolume($clamped);")
    }

    fun setEqualizer(bands: FloatArray, bassBoost: Float, preampGain: Float) {
        val bandsJson = bands.joinToString(prefix = "[", postfix = "]") { it.toString() }
        evaluateScript("window.bravePlayer && window.bravePlayer.setEqualizer($bandsJson, $bassBoost, $preampGain);")
    }

    fun injectShieldScript(scriptContent: String) {
        evaluateScript(scriptContent)
    }

    private fun evaluateScript(script: String) {
        mainHandler.post {
            webViewRef.get()?.evaluateJavascript(script, null)
        }
    }
}
