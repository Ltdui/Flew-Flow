package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    audioLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32
) {
    val animatedLevel = remember { Animatable(0f) }

    LaunchedEffect(audioLevel, isRecording) {
        if (isRecording) {
            animatedLevel.animateTo(
                targetValue = audioLevel,
                animationSpec = tween(durationMillis = 80, easing = LinearEasing)
            )
        } else {
            animatedLevel.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "idle_wave")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val totalSpacing = width / barCount
        val barWidth = (totalSpacing * 0.55f).coerceIn(2.5f, 10f)

        val gradient = Brush.verticalGradient(
            colors = listOf(
                primaryColor,
                secondaryColor,
                primaryColor
            ),
            startY = 0f,
            endY = height
        )

        val idleColor = primaryColor.copy(alpha = 0.25f)

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            val x = i * totalSpacing + (totalSpacing - barWidth) / 2f

            val barHeight = if (isRecording) {
                // Bell curve envelope centered around the middle
                val envelope = sin(fraction * Math.PI).toFloat()
                // Mix in random-looking harmonic variation
                val harmonic = (sin(fraction * 12.0 + idlePhase * 2.0).toFloat() * 0.25f + 0.75f)
                val dynamicHeight = height * (0.12f + (animatedLevel.value * 0.85f * envelope * harmonic))
                dynamicHeight.coerceIn(6f, height * 0.95f)
            } else {
                val wave = (sin(fraction * 6.0 + idlePhase).toFloat() * 0.5f + 0.5f)
                (height * 0.12f + wave * 8f).coerceIn(4f, height * 0.35f)
            }

            val top = centerY - barHeight / 2f

            drawRoundRect(
                brush = if (isRecording) gradient else Brush.linearGradient(listOf(idleColor, idleColor)),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
