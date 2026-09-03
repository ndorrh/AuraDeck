package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.DeckACyan
import com.example.ui.theme.DeckBAmber
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun TurntablePlatter(
    deckId: String, // "A" or "B"
    isPlaying: Boolean,
    playbackSpeed: Float,
    thumbnailUrl: String?,
    bpm: Int,
    keySignature: String,
    modifier: Modifier = Modifier,
    accentColor: Color = if (deckId == "A") DeckACyan else DeckBAmber,
    onScratchScrub: (deltaAngle: Float) -> Unit = {}
) {
    // Rotation state
    var manualRotation by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "platter_spin")
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1800 / playbackSpeed.coerceAtLeast(0.2f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val currentRotation = if (isPlaying) (autoRotation + manualRotation) % 360f else manualRotation

    Box(
        modifier = modifier
            .testTag("turntable_platter_$deckId")
            .aspectRatio(1f)
            .padding(6.dp)
            .pointerInput(isPlaying) {
                var prevAngle = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        prevAngle = (atan2(offset.y - center.y, offset.x - center.x) * 180f / PI.toFloat())
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val newAngle = (atan2(change.position.y - center.y, change.position.x - center.x) * 180f / PI.toFloat())
                        val diff = newAngle - prevAngle
                        manualRotation = (manualRotation + diff) % 360f
                        onScratchScrub(diff)
                        prevAngle = newAngle
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vinyl Record Turntable Body
        Canvas(modifier = Modifier.fillMaxSize().rotate(currentRotation)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 4f

            // Outer metallic rim
            drawCircle(
                color = Color(0xFF1E2638),
                radius = outerRadius + 2f,
                center = center
            )

            // Neon Deck Accent Ring
            drawCircle(
                color = accentColor.copy(alpha = if (isPlaying) 0.9f else 0.4f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Dark Vinyl Record Surface
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF141926), Color(0xFF0A0D14)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius - 4f,
                center = center
            )

            // Concentric Vinyl Grooves
            val grooves = listOf(0.90f, 0.82f, 0.74f, 0.66f, 0.58f, 0.50f)
            grooves.forEach { factor ->
                drawCircle(
                    color = Color(0xFF222B40),
                    radius = (outerRadius - 4f) * factor,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Lead-out groove
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = (outerRadius - 4f) * 0.42f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        // Center Spindle Label & Artwork
        Box(
            modifier = Modifier
                .fillMaxSize(0.42f)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .rotate(currentRotation),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Deck $deckId Artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "Vinyl Spindle",
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Center Brass Spindle Pin
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F))
            )
        }

        // Top Left Deck Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "DECK $deckId",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Top Right BPM Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0xFF1E293B).copy(alpha = 0.85f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$bpm BPM • $keySignature",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
