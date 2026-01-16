/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.theme.readerBarsColor

@Composable
fun SpeedReadingTopBar(
    onExitSpeedReading: () -> Unit,
    alwaysShowPlayPause: Boolean,
    onToggleAlwaysShowPlayPause: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onExitSpeedReading
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Exit Speed Reading",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Toggle always show play/pause
            IconButton(
                onClick = onToggleAlwaysShowPlayPause
            ) {
                Icon(
                    imageVector = if (alwaysShowPlayPause)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff,
                    contentDescription = "Toggle always show play/pause",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SpeedReadingBottomBar(
    progress: String,
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onShowSettings: () -> Unit,
    bottomBarPadding: Dp
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // Progress text
        StyledText(
            text = progress,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(Modifier.height(12.dp))

        // WPM indicator and slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${wpm} WPM",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Slider(
                value = wpm.toFloat(),
                onValueChange = { onWpmChange(it.toInt()) },
                valueRange = 100f..1000f,
                steps = 9, // 100 WPM increments (fewer pips)
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Play/Pause button
            IconButton(
                onClick = onPlayPause
            ) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Default.Pause
                    else
                        Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Settings button
            IconButton(
                onClick = onShowSettings
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(8.dp + bottomBarPadding))
    }
}