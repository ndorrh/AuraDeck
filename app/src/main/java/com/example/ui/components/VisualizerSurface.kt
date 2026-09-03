package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.DspSurroundViolet
import com.example.ui.theme.MatrixGreen
import com.example.visualizer.VisualizerFrame
import com.example.visualizer.VisualizerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerSurface(
    frame: VisualizerFrame,
    mode: VisualizerMode,
    modifier: Modifier = Modifier,
    primaryColor: Color = DeckACyan,
    secondaryColor: Color = DeckBAmber
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        when (mode) {
            VisualizerMode.NEON_SPECTRUM -> {
                drawNeonSpectrum(frame, width, height, primaryColor, secondaryColor)
            }
            VisualizerMode.CIRCULAR_WAVE -> {
                drawCircularWave(frame, width, height, primaryColor, secondaryColor)
            }
            VisualizerMode.LISSAJOUS_PHASE -> {
                drawLissajousPhase(frame, width, height, primaryColor)
            }
            VisualizerMode.LIQUID_PARTICLES -> {
                drawLiquidParticles(frame, width, height, primaryColor, secondaryColor)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonSpectrum(
    frame: VisualizerFrame,
    width: Float,
    height: Float,
    primary: Color,
    secondary: Color
) {
    val bars = 36
    val barSpacing = 4f
    val totalSpacing = barSpacing * (bars + 1)
    val barWidth = ((width - totalSpacing) / bars).coerceAtLeast(2f)

    val step = (frame.fftMagnitudes.size / bars).coerceAtLeast(1)

    for (i in 0 until bars) {
        val magIndex = (i * step).coerceAtMost(frame.fftMagnitudes.size - 1)
        val mag = frame.fftMagnitudes[magIndex].coerceIn(0.04f, 1.0f)
        val peak = frame.peakMagnitudes[magIndex].coerceIn(0.04f, 1.0f)

        val barHeight = (mag * (height * 0.88f)).coerceAtLeast(4f)
        val peakY = height - (peak * (height * 0.88f))

        val x = barSpacing + i * (barWidth + barSpacing)
        val y = height - barHeight

        // Interpolate color from primary (bass/mid) to secondary (treble)
        val fraction = i.toFloat() / bars.toFloat()
        val barColor = if (fraction < 0.6f) primary else secondary

        // Draw glowing gradient bar
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(barColor, barColor.copy(alpha = 0.25f)),
                startY = y,
                endY = height
            ),
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight)
        )

        // Draw glowing peak cap
        drawRect(
            color = Color.White,
            topLeft = Offset(x, peakY - 3f),
            size = Size(barWidth, 3f)
        )
    }

    // Baseline accent glow line
    drawLine(
        color = primary.copy(alpha = 0.4f),
        start = Offset(0f, height - 1f),
        end = Offset(width, height - 1f),
        strokeWidth = 2f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircularWave(
    frame: VisualizerFrame,
    width: Float,
    height: Float,
    primary: Color,
    secondary: Color
) {
    val center = Offset(width / 2f, height / 2f)
    val baseRadius = (minOf(width, height) * 0.28f).coerceAtLeast(10f)
    val maxExpansion = (minOf(width, height) * 0.18f)

    // Center pulse ring
    val bassPulse = frame.bassEnergy * 20f
    drawCircle(
        color = primary.copy(alpha = 0.15f),
        radius = baseRadius + bassPulse,
        center = center
    )
    drawCircle(
        color = Color(0xFF101522),
        radius = baseRadius * 0.7f,
        center = center
    )
    drawCircle(
        color = secondary,
        radius = 4f,
        center = center
    )

    // 360-degree radial oscilloscope points
    val pointsCount = frame.waveformPoints.size
    val path = Path()

    for (i in 0 until pointsCount) {
        val angle = (i.toFloat() / pointsCount.toFloat()) * (2f * PI.toFloat()) - (PI.toFloat() / 2f)
        val waveVal = frame.waveformPoints[i]
        val r = baseRadius + (waveVal * maxExpansion) + (frame.bassEnergy * 15f)

        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = primary,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Secondary inner harmonic ring
    val innerPath = Path()
    for (i in 0 until pointsCount) {
        val angle = (i.toFloat() / pointsCount.toFloat()) * (2f * PI.toFloat())
        val waveVal = frame.waveformPoints[(i + 32) % pointsCount]
        val r = (baseRadius * 0.85f) + (waveVal * maxExpansion * 0.5f)

        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r

        if (i == 0) {
            innerPath.moveTo(x, y)
        } else {
            innerPath.lineTo(x, y)
        }
    }
    innerPath.close()

    drawPath(
        path = innerPath,
        color = secondary.copy(alpha = 0.6f),
        style = Stroke(width = 2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLissajousPhase(
    frame: VisualizerFrame,
    width: Float,
    height: Float,
    primary: Color
) {
    val center = Offset(width / 2f, height / 2f)
    val maxRadius = minOf(width, height) * 0.42f

    // Grid crosshairs for phase correlation
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(center.x - maxRadius, center.y),
        end = Offset(center.x + maxRadius, center.y),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(center.x, center.y - maxRadius),
        end = Offset(center.x, center.y + maxRadius),
        strokeWidth = 1f
    )
    drawCircle(
        color = Color(0xFF1E293B),
        radius = maxRadius,
        center = center,
        style = Stroke(width = 1f)
    )

    // Lissajous curve
    val pointsCount = frame.waveformPoints.size
    val path = Path()

    for (i in 0 until pointsCount) {
        val waveL = frame.waveformPoints[i]
        val waveR = frame.waveformPoints[(i + 16) % pointsCount]

        val x = center.x + (waveL * maxRadius * 0.9f)
        val y = center.y - (waveR * maxRadius * 0.9f)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = MatrixGreen,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiquidParticles(
    frame: VisualizerFrame,
    width: Float,
    height: Float,
    primary: Color,
    secondary: Color
) {
    val center = Offset(width / 2f, height / 2f)
    val particleCount = 48

    for (i in 0 until particleCount) {
        val angle = (i.toFloat() / particleCount.toFloat()) * 2f * PI.toFloat() + (frame.timestamp % 10000) * 0.001f
        val fftIdx = (i % frame.fftMagnitudes.size)
        val mag = frame.fftMagnitudes[fftIdx]
        val dist = (mag * minOf(width, height) * 0.45f) + 20f

        val x = center.x + cos(angle) * dist
        val y = center.y + sin(angle) * dist
        val radius = (mag * 9f).coerceIn(2f, 14f)

        val color = if (i % 2 == 0) primary else DspSurroundViolet

        drawCircle(
            color = color.copy(alpha = (mag * 0.9f).coerceIn(0.2f, 1.0f)),
            radius = radius,
            center = Offset(x, y)
        )
    }
}
