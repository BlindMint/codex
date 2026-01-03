/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.components.progress_indicator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter

/**
 * A circular progress indicator with a subtle skull accent in the center.
 * The skull pulses gently while the progress arc rotates around it.
 *
 * @param modifier Modifier for the component
 * @param size Size of the indicator
 * @param trackColor Color of the background track
 * @param progressColor Color of the progress arc
 * @param strokeWidth Width of the progress arc stroke
 * @param skullAlpha Base alpha for the skull icon (0-1)
 */
@Composable
fun SkullProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    skullAlpha: Float = 0.6f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skull_progress")

    // Rotation animation for the progress arc
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Subtle pulse animation for the skull
    val skullPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skull_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Progress arc
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                width = this.size.width - strokeWidthPx,
                height = this.size.height - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Rotating progress arc
            rotate(rotation) {
                drawArc(
                    color = progressColor,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // Skull icon in center with pulse effect
        Image(
            painter = painterResource(id = R.drawable.skull_outline),
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.45f)
                .alpha(skullAlpha * skullPulse),
            colorFilter = ColorFilter.tint(progressColor)
        )
    }
}

/**
 * A circular progress indicator with skull accent showing determinate progress.
 *
 * @param progress Progress value from 0 to 1
 * @param modifier Modifier for the component
 * @param size Size of the indicator
 * @param trackColor Color of the background track
 * @param progressColor Color of the progress arc
 * @param strokeWidth Width of the progress arc stroke
 * @param skullAlpha Base alpha for the skull icon
 */
@Composable
fun SkullProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    skullAlpha: Float = 0.6f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val arcSize = Size(
                width = this.size.width - strokeWidthPx,
                height = this.size.height - strokeWidthPx
            )
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Skull icon - more visible as progress increases
        val skullVisibility = (skullAlpha * 0.5f) + (animatedProgress * skullAlpha * 0.5f)
        Image(
            painter = painterResource(id = R.drawable.skull_outline),
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.45f)
                .alpha(skullVisibility),
            colorFilter = ColorFilter.tint(progressColor)
        )
    }
}
