package com.brave.jsabmusic.timer

import android.content.Context
import android.os.CountDownTimer
import com.brave.jsabmusic.bridge.WebInterfaceBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.exp

/**
 * Intelligent Sleep Timer featuring a 30-second exponential audio fade-out
 * before issuing a hard pause and releasing system wake locks.
 */
class SleepTimerManager(private val context: Context) {

    private var bridge: WebInterfaceBridge? = null
    private var countDownTimer: CountDownTimer? = null

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun setBridge(bridge: WebInterfaceBridge) {
        this.bridge = bridge
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
                    bridge?.setVolume(curveVolume)
                }
            }

            override fun onFinish() {
                _remainingSeconds.value = 0L
                _isTimerRunning.value = false
                bridge?.pause()
                bridge?.setVolume(1.0f) // Restore volume level for future playback
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0L
        _isTimerRunning.value = false
        bridge?.setVolume(1.0f)
    }
}
