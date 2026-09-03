package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraDeckViewModel
import com.example.ui.components.AudiophileRotaryDial
import com.example.ui.components.EqBandSlider
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen

@Composable
fun DspSuiteScreen(
    viewModel: AuraDeckViewModel,
    modifier: Modifier = Modifier
) {
    val dspState by viewModel.dspState.collectAsState()
    val presets by viewModel.dspPresets.collectAsState()

    val frequencyLabels = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080B11))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // DSP Header & Master Power Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101522)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "DSP Suite",
                        tint = if (dspState.isEnabled) DspSurroundViolet else Color(0xFF64748B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AUDIOPHILE DSP SUITE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (dspState.isEnabled) "ACTIVE • 24-bit 96kHz Processing" else "BYPASS (Direct Pure Path)",
                            color = if (dspState.isEnabled) MatrixGreen else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = dspState.isEnabled,
                    onCheckedChange = { viewModel.toggleDspMaster(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DspSurroundViolet
                    ),
                    modifier = Modifier.testTag("dsp_master_switch")
                )
            }
        }

        // DSP Presets Selector Bar
        Column {
            Text(
                text = "SOUNDSTAGE PRESETS",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = dspState.activePresetName == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.applyPreset(preset) },
                        label = {
                            Text(
                                text = preset.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DspSurroundViolet.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF141A29),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) DspSurroundViolet else Color(0xFF243048),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // 10-Band Graphic Equalizer Section
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101624)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10-BAND PARAMETRIC / GRAPHIC EQ (±12 dB)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "BANDPASS",
                        color = DeckACyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Equalizer Faders Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    frequencyLabels.forEachIndexed { index, label ->
                        val level = dspState.eqBandLevels.getOrElse(index) { 0 }
                        EqBandSlider(
                            label = label,
                            levelMilliBels = level,
                            onLevelChanged = { newMb ->
                                viewModel.setEqBandLevel(index, newMb)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp).testTag("eq_band_$index")
                        )
                    }
                }
            }
        }

        // Spatial Audio & Harmonic Enhancements: 3D Virtualizer + Bass Exciter + Dynamics
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101624)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ACOUSTIC ENHANCEMENT SUITE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3D Headset Surround Spatializer
                    AudiophileRotaryDial(
                        label = "3D HRTF SURROUND",
                        valueFraction = dspState.virtualizerPercent,
                        onValueChanged = { viewModel.setVirtualizer(it) },
                        activeColor = DspSurroundViolet
                    )

                    // Harmonic Bass Exciter
                    AudiophileRotaryDial(
                        label = "BASS EXCITER",
                        valueFraction = dspState.bassBoostPercent,
                        onValueChanged = { viewModel.setBassBoost(it) },
                        activeColor = DeckBAmber
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamics Processing Multiband Compressor & Peak Limiter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B101C))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DYNAMICS MULTIBAND COMPRESSOR & LIMITER",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Normalizes dynamic range and eliminates digital clipping distortion",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = dspState.dynamicsEnabled,
                        onCheckedChange = { viewModel.setDynamicsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MatrixGreen
                        ),
                        modifier = Modifier.testTag("dynamics_switch")
                    )
                }
            }
        }
    }
}
