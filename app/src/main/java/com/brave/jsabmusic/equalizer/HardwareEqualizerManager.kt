package com.brave.jsabmusic.equalizer

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interfaces directly with Samsung Galaxy S24 FE's native hardware Audio HAL DSP.
 * Configures hardware 5-band parametric equalizer and sub-bass booster via audioSessionId.
 */
class HardwareEqualizerManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jsab_hw_equalizer_prefs", Context.MODE_PRIVATE)

    private var nativeEqualizer: Equalizer? = null
    private var nativeBassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    private val _bandGains = MutableStateFlow(FloatArray(5) { 0f })
    val bandGains: StateFlow<FloatArray> = _bandGains.asStateFlow()

    private val _bassBoost = MutableStateFlow(0f)
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _currentPreset = MutableStateFlow("Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    init {
        loadPersistedState()
    }

    /**
     * Re-attaches native audio effects to the active ExoPlayer audioSessionId.
     */
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId

        release()

        try {
            nativeEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            nativeBassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }

            applyCurrentSettingsToHardware()
        } catch (t: Throwable) {
            // Audio effect initialization notice
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0..4) {
            val clamped = gainDb.coerceIn(-12f, 12f)
            val updated = _bandGains.value.clone()
            updated[bandIndex] = clamped
            _bandGains.value = updated
            _currentPreset.value = "Custom"
            saveState()
            applyBandGainToHardware(bandIndex, clamped)
        }
    }

    fun setBassBoost(gainDb: Float) {
        val clamped = gainDb.coerceIn(0f, 10f)
        _bassBoost.value = clamped
        saveState()
        applyBassBoostToHardware(clamped)
    }

    fun applyPreset(presetName: String) {
        val preset = EqualizerDefaults.PRESETS.find { it.name == presetName } ?: return
        _bandGains.value = preset.gains.clone()
        _bassBoost.value = preset.bassBoost
        _currentPreset.value = preset.name
        saveState()
        applyCurrentSettingsToHardware()
    }

    private fun applyCurrentSettingsToHardware() {
        for (i in 0..4) {
            applyBandGainToHardware(i, _bandGains.value[i])
        }
        applyBassBoostToHardware(_bassBoost.value)
    }

    private fun applyBandGainToHardware(bandIndex: Int, gainDb: Float) {
        nativeEqualizer?.let { eq ->
            try {
                if (bandIndex < eq.numberOfBands) {
                    // Convert dB to millibels (1 dB = 100 mB)
                    val millibels = (gainDb * 100).toInt().toShort()
                    eq.setBandLevel(bandIndex.toShort(), millibels)
                }
            } catch (t: Throwable) {}
        }
    }

    private fun applyBassBoostToHardware(gainDb: Float) {
        nativeBassBoost?.let { bb ->
            try {
                // Strength scale: 0 to 1000
                val strength = ((gainDb / 10.0f) * 1000).toInt().coerceIn(0, 1000).toShort()
                bb.setStrength(strength)
            } catch (t: Throwable) {}
        }
    }

    private fun saveState() {
        prefs.edit().apply {
            for (i in 0..4) {
                putFloat("band_$i", _bandGains.value[i])
            }
            putFloat("bass_boost", _bassBoost.value)
            putString("preset_name", _currentPreset.value)
            apply()
        }
    }

    private fun loadPersistedState() {
        val gains = FloatArray(5)
        for (i in 0..4) {
            gains[i] = prefs.getFloat("band_$i", 0f)
        }
        _bandGains.value = gains
        _bassBoost.value = prefs.getFloat("bass_boost", 0f)
        _currentPreset.value = prefs.getString("preset_name", "Flat") ?: "Flat"
    }

    fun release() {
        try {
            nativeEqualizer?.release()
            nativeBassBoost?.release()
        } catch (t: Throwable) {}
        nativeEqualizer = null
        nativeBassBoost = null
    }
}
