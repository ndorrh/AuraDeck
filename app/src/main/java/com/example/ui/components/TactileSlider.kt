package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import kotlin.math.*

@Composable
fun AudiophileCrossfader(
    position: Float, // 0.0 (Deck A) to 1.0 (Deck B)
    onPositionChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1522), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onPositionChanged(0.0f) },
                colors = ButtonDefaults.buttonColors(containerColor = DeckACyan.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp).testTag("cut_deck_a")
            ) {
                Text("CUT A", color = DeckACyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "CROSSFADER",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = { onPositionChanged(1.0f) },
                colors = ButtonDefaults.buttonColors(containerColor = DeckBAmber.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp).testTag("cut_deck_b")
            ) {
                Text("CUT B", color = DeckBAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fader Slider Track
        Slider(
            value = position,
            onValueChange = onPositionChanged,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFF1F5F9),
                activeTrackColor = DeckBAmber,
                inactiveTrackColor = DeckACyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("crossfader_slider")
        )

        // Center Detent & Scale Markings
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("CH A 100%", color = DeckACyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("MID (50/50)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("100% CH B", color = DeckBAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun EqBandSlider(
    label: String,
    levelMilliBels: Int, // -1200 to +1200
    onLevelChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dbValue = (levelMilliBels / 100f)
    val fraction = ((levelMilliBels + 1200f) / 2400f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .width(36.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%+.1f", dbValue),
            color = if (dbValue > 0) DeckBAmber else if (dbValue < 0) DeckACyan else Color.White,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Custom Vertical Touch Slider
        Box(
            modifier = Modifier
                .weight(1f)
                .width(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0D121D))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val y = change.position.y
                        val totalH = size.height
                        if (totalH > 0) {
                            val newFraction = (1f - (y / totalH)).coerceIn(0f, 1f)
                            val newMb = ((newFraction * 2400f) - 1200f).toInt()
                            onLevelChanged(newMb)
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Center 0dB Line
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(2f, midY),
                    end = Offset(w - 2f, midY),
                    strokeWidth = 1.5f
                )

                // Track Slot
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(w / 2f, 4f),
                    end = Offset(w / 2f, h - 4f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Slider Thumb Knob
                val thumbY = (1f - fraction) * (h - 16f) + 8f
                drawCircle(
                    color = if (dbValue > 0) DeckBAmber else DeckACyan,
                    radius = 8f,
                    center = Offset(w / 2f, thumbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(w / 2f, thumbY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AudiophileRotaryDial(
    label: String,
    valueFraction: Float, // 0.0 to 1.0
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = DeckACyan
) {
    val percentage = (valueFraction * 100).toInt()

    Column(
        modifier = modifier.width(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(Unit) {
                    var startY = 0f
                    detectDragGestures(
                        onDragStart = { offset -> startY = offset.y },
                        onDrag = { change, _ ->
                            val deltaY = startY - change.position.y
                            val changeFraction = deltaY / 150f
                            onValueChanged((valueFraction + changeFraction).coerceIn(0f, 1f))
                            startY = change.position.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 4f

                // Dial Base Arc (270 degrees sweep, starting from 135 deg)
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Active Glow Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(activeColor.copy(alpha = 0.5f), activeColor)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * valueFraction,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Dial Inner Disc
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = radius - 8f,
                    center = center
                )

                // Indicator Needle
                val needleAngle = (135f + 270f * valueFraction) * (PI.toFloat() / 180f)
                val needleLength = radius - 10f
                val needleEnd = Offset(
                    center.x + cos(needleAngle) * needleLength,
                    center.y + sin(needleAngle) * needleLength
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "$percentage%",
                color = activeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
