package com.brave.jsabmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brave.jsabmusic.equalizer.EqualizerDefaults
import com.brave.jsabmusic.equalizer.EqualizerManager
import com.brave.jsabmusic.ui.theme.AmoledBlack
import com.brave.jsabmusic.ui.theme.AmoledCard
import com.brave.jsabmusic.ui.theme.SaavnTeal
import com.brave.jsabmusic.ui.theme.SaavnTealAccent
import com.brave.jsabmusic.ui.theme.TextPrimary
import com.brave.jsabmusic.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    equalizerManager: EqualizerManager,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bandGains by equalizerManager.bandGains.collectAsState()
    val bassBoost by equalizerManager.bassBoost.collectAsState()
    val preampGain by equalizerManager.preampGain.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AmoledBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Studio Equalizer",
                    tint = SaavnTeal,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Studio Equalizer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = currentPreset,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SaavnTealAccent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerDefaults.PRESETS.forEach { preset ->
                    val isSelected = currentPreset == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { equalizerManager.applyPreset(preset.name) },
                        label = { Text(preset.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SaavnTeal,
                            selectedLabelColor = AmoledBlack,
                            containerColor = AmoledCard,
                            labelColor = TextPrimary
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5-Band Vertical-style sliders rendered in clean rows
            EqualizerDefaults.BANDS.forEach { band ->
                val gain = bandGains.getOrElse(band.index) { 0f }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = band.frequencyLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = String.format(Locale.US, "%+.1f dB", gain),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gain != 0f) SaavnTeal else TextSecondary
                        )
                    }
                    Slider(
                        value = gain,
                        onValueChange = { equalizerManager.setBandGain(band.index, it) },
                        valueRange = -12f..12f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = SaavnTeal,
                            activeTrackColor = SaavnTeal,
                            inactiveTrackColor = AmoledCard
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bass Booster Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledCard, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sub-Bass Booster (+10 dB)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "+%.1f dB", bassBoost),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaavnTealAccent
                    )
                }
                Slider(
                    value = bassBoost,
                    onValueChange = { equalizerManager.setBassBoost(it) },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = SaavnTealAccent,
                        activeTrackColor = SaavnTealAccent,
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}
