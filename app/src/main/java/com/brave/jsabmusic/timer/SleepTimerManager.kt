package com.brave.jsabmusic.timer

import android.os.CountDownTimer
import com.brave.jsabmusic.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.exp

/**
 * Intelligent Sleep Timer with a smooth 30-second exponential acoustic fade-out
 * directly interacting with the native ExoPlayer audio pipeline.
 */
class SleepTimerManager {

    private var playerController: PlayerController? = null
    private var countDownTimer: CountDownTimer? = null

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun setPlayerController(controller: PlayerController) {
        this.playerController = controller
    }

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalMillis = minutes * 60 * 1000L
        _isTimerRunning.value = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000L
                _remainingSeconds.value = secondsLeft

                // Smooth exponential audio fade-out during the final 30 seconds
                if (secondsLeft in 1..30) {
                    val fadeFactor = (secondsLeft / 30.0f)
                    val curveVolume = exp(3.0 * (fadeFactor - 1.0)).toFloat().coerceIn(0.01f, 1.0f)
                    playerController?.setVolume(curveVolume)
                }
            }

            override fun onFinish() {
                _remainingSeconds.value = 0L
                _isTimerRunning.value = false
                playerController?.exoPlayer?.pause()
                playerController?.setVolume(1.0f)
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0L
        _isTimerRunning.value = false
        playerController?.setVolume(1.0f)
    }
}
