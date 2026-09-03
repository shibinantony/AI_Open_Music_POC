package com.brave.jsabmusic.equalizer

import android.content.Context
import android.content.SharedPreferences
import com.brave.jsabmusic.bridge.WebInterfaceBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages 5-band equalizer gains, bass boost, and preamp attenuation.
 * Automatically synchronizes changes to WebAudio DSP via WebInterfaceBridge.
 */
class EqualizerManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jsab_equalizer_prefs", Context.MODE_PRIVATE)

    private var bridge: WebInterfaceBridge? = null

    private val _bandGains = MutableStateFlow(FloatArray(5) { 0f })
    val bandGains: StateFlow<FloatArray> = _bandGains.asStateFlow()

    private val _bassBoost = MutableStateFlow(0f)
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _preampGain = MutableStateFlow(1.0f)
    val preampGain: StateFlow<Float> = _preampGain.asStateFlow()

    private val _currentPreset = MutableStateFlow("Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    init {
        loadPersistedState()
    }

    fun setBridge(bridge: WebInterfaceBridge) {
        this.bridge = bridge
        applyToWebAudio()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0..4) {
            val clamped = gainDb.coerceIn(-12f, 12f)
            val updated = _bandGains.value.clone()
            updated[bandIndex] = clamped
            _bandGains.value = updated
            _currentPreset.value = "Custom"
            saveState()
            applyToWebAudio()
        }
    }

    fun setBassBoost(gainDb: Float) {
        val clamped = gainDb.coerceIn(0f, 10f)
        _bassBoost.value = clamped
        saveState()
        applyToWebAudio()
    }

    fun setPreampGain(gainMultiplier: Float) {
        val clamped = gainMultiplier.coerceIn(0.5f, 1.5f)
        _preampGain.value = clamped
        saveState()
        applyToWebAudio()
    }

    fun applyPreset(presetName: String) {
        val preset = EqualizerDefaults.PRESETS.find { it.name == presetName } ?: return
        _bandGains.value = preset.gains.clone()
        _bassBoost.value = preset.bassBoost
        _currentPreset.value = preset.name
        saveState()
        applyToWebAudio()
    }

    private fun applyToWebAudio() {
        bridge?.setEqualizer(_bandGains.value, _bassBoost.value, _preampGain.value)
    }

    private fun saveState() {
        prefs.edit().apply {
            for (i in 0..4) {
                putFloat("band_$i", _bandGains.value[i])
            }
            putFloat("bass_boost", _bassBoost.value)
            putFloat("preamp_gain", _preampGain.value)
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
        _preampGain.value = prefs.getFloat("preamp_gain", 1.0f)
        _currentPreset.value = prefs.getString("preset_name", "Flat") ?: "Flat"
    }
}
