package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeckBAmber
import com.example.ui.theme.MatrixGreen

@Composable
fun StereoVuMeter(
    levelL: Float,
    levelR: Float,
    modifier: Modifier = Modifier,
    label: String = "CH"
) {
    Column(
        modifier = modifier
            .width(28.dp)
            .background(Color(0xFF0C101A), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleVuBar(level = levelL, modifier = Modifier.weight(1f).padding(horizontal = 1.dp))
            SingleVuBar(level = levelR, modifier = Modifier.weight(1f).padding(horizontal = 1.dp))
        }
    }
}

@Composable
private fun SingleVuBar(
    level: Float,
    modifier: Modifier = Modifier
) {
    val totalSegments = 16
    val clamped = level.coerceIn(0f, 1f)
    val activeSegments = (clamped * totalSegments).toInt()

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(1.5.dp, Alignment.Bottom)
    ) {
        for (i in (totalSegments - 1) downTo 0) {
            val isActive = i < activeSegments
            val color = when {
                i >= 14 -> DangerRed
                i >= 11 -> DeckBAmber
                else -> MatrixGreen
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) color else color.copy(alpha = 0.15f))
            )
        }
    }
}
