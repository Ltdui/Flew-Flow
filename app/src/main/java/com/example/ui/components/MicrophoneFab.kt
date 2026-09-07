package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MicrophoneFab(
    isRecording: Boolean,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_1"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFF2B8B5) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "btn_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFF601410) else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(300),
        label = "content_color"
    )

    val dynamicAudioScale = if (isRecording) {
        1f + (audioLevel * 0.18f).coerceIn(0f, 0.22f)
    } else 1f

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer halo glow from design HTML
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
        )

        // Pulsing active rings when recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale1)
                    .background(buttonColor.copy(alpha = pulseAlpha1), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(1f + (audioLevel * 0.35f))
                    .background(buttonColor.copy(alpha = 0.25f), CircleShape)
            )
        }

        // Main 92dp action button
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(dynamicAudioScale)
                .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(buttonColor)
                .testTag("microphone_toggle_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 45.dp),
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "Stop live transcription" else "Start live transcription",
                tint = contentColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

