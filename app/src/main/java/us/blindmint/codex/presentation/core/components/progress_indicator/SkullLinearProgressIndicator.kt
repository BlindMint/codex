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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A clean linear progress indicator.
 *
 * @param progress Progress value from 0 to 1
 * @param modifier Modifier for the component
 * @param trackColor Color of the background track
 * @param progressColor Color of the progress bar
 * @param height Height of the progress bar
 * @param cornerRadius Corner radius for rounded ends
 */
@Composable
fun SkullLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cornerRadiusPx = cornerRadius.toPx()

            // Background track
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // Progress bar
            val progressWidth = size.width * animatedProgress
            if (progressWidth > 0) {
                drawRoundRect(
                    color = progressColor,
                    size = Size(progressWidth, size.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }
    }
}

/**
 * An indeterminate linear progress indicator with clean animation.
 *
 * @param modifier Modifier for the component
 * @param trackColor Color of the background track
 * @param progressColor Color of the progress bar
 * @param height Height of the progress bar
 * @param cornerRadius Corner radius for rounded ends
 */
@Composable
fun SkullLinearProgressIndicator(
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "linear_progress")

    // Animate position of progress segment
    val position by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "position"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cornerRadiusPx = cornerRadius.toPx()
            val segmentWidth = size.width * 0.3f

            // Background track
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // Moving progress segment
            val startX = (position * size.width) - segmentWidth / 2
            val clampedStart = startX.coerceIn(0f, size.width - segmentWidth)
            val endX = (startX + segmentWidth).coerceIn(0f, size.width)
            val visibleWidth = endX - clampedStart.coerceAtLeast(0f)

            if (visibleWidth > 0 && clampedStart < size.width) {
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(clampedStart.coerceAtLeast(0f), 0f),
                    size = Size(visibleWidth, size.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }
    }
}
