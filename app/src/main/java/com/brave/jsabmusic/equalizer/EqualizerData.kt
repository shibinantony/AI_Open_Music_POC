package com.brave.jsabmusic.equalizer

/**
 * Frequency bands and acoustic presets for Studio 5-Band Equalizer.
 */
data class EqualizerBand(
    val index: Int,
    val frequencyLabel: String,
    val centerFrequencyHz: Int,
    val gainDb: Float
)

data class EqualizerPreset(
    val name: String,
    val gains: FloatArray,
    val bassBoost: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualizerPreset
        return name == other.name && gains.contentEquals(other.gains) && bassBoost == other.bassBoost
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + gains.contentHashCode()
        result = 31 * result + bassBoost.hashCode()
        return result
    }
}

object EqualizerDefaults {
    val BANDS = listOf(
        EqualizerBand(0, "60 Hz", 60, 0f),
        EqualizerBand(1, "230 Hz", 230, 0f),
        EqualizerBand(2, "910 Hz", 910, 0f),
        EqualizerBand(3, "3.6 kHz", 3600, 0f),
        EqualizerBand(4, "14 kHz", 14000, 0f)
    )

    val PRESETS = listOf(
        EqualizerPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f), 0f),
        EqualizerPreset("Bass Booster", floatArrayOf(6f, 4f, 1f, 0f, -1f), 8f),
        EqualizerPreset("Electronic / EDM", floatArrayOf(5f, 3f, -1f, 3f, 4f), 5f),
        EqualizerPreset("Rock", floatArrayOf(4f, 2f, -1f, 3f, 5f), 3f),
        EqualizerPreset("Pop", floatArrayOf(-1f, 2f, 4f, 3f, -1f), 2f),
        EqualizerPreset("Vocal Booster", floatArrayOf(-2f, 1f, 5f, 3f, 1f), 0f),
        EqualizerPreset("Hip-Hop", floatArrayOf(6f, 5f, 0f, 2f, 3f), 7f),
        EqualizerPreset("Classical", floatArrayOf(4f, 3f, 2f, 3f, -1f), 0f)
    )
}
