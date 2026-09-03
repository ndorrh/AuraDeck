package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ScatterPlot
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
import com.example.ui.components.VisualizerSurface
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen
import com.example.visualizer.VisualizerMode

@Composable
fun VisualizerStudioScreen(
    viewModel: AuraDeckViewModel,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.visualizerMode.collectAsState()
    val frame by viewModel.visualizerFrame.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisualizerMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                val icon = when (mode) {
                    VisualizerMode.NEON_SPECTRUM -> Icons.Default.GraphicEq
                    VisualizerMode.CIRCULAR_WAVE -> Icons.Default.MotionPhotosOn
                    VisualizerMode.LISSAJOUS_PHASE -> Icons.Default.Radio
                    VisualizerMode.LIQUID_PARTICLES -> Icons.Default.ScatterPlot
                }

                Surface(
                    onClick = { viewModel.setVisualizerMode(mode) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) DspSurroundViolet.copy(alpha = 0.25f) else Color(0xFF121826),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) DspSurroundViolet else Color(0xFF1E283C)
                    ),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("vis_mode_${mode.name}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.displayName,
                            tint = if (isSelected) DspSurroundViolet else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mode.displayName.substringBefore(" "),
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Expanded Hardware/Canvas Visualizer Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        ) {
            VisualizerSurface(
                frame = frame,
                mode = currentMode,
                modifier = Modifier.fillMaxSize()
            )

            // Top Left Active Mode Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color(0xFF0C101A).copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MatrixGreen, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${currentMode.displayName} • 1024-PT FFT 60 FPS",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Real-Time Energy Telemetry Strip
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101524)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnergyTelemetryMeter(
                    label = "SUB-BASS",
                    energy = frame.bassEnergy,
                    color = DeckBAmber
                )
                EnergyTelemetryMeter(
                    label = "MIDRANGE",
                    energy = frame.midEnergy,
                    color = DeckACyan
                )
                EnergyTelemetryMeter(
                    label = "TREBLE",
                    energy = frame.highEnergy,
                    color = DspSurroundViolet
                )
            }
        }
    }
}

@Composable
private fun EnergyTelemetryMeter(
    label: String,
    energy: Float,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { energy.coerceIn(0f, 1f) },
            modifier = Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF1E283C)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${(energy * 100).toInt()}%",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
