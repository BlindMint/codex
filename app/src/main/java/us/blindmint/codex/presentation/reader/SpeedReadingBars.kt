/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.theme.readerBarsColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SpeedReadingTopBar(
    bookTitle: String,
    chapterTitle: String?,
    currentProgress: Float,
    onExitSpeedReading: () -> Unit,
    onShowSettings: () -> Unit
) {
    val animatedProgress = animateFloatAsState(
        targetValue = currentProgress,
        label = "progress"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = R.string.go_back_content_desc,
                    disableOnClick = true
                ) {
                    onExitSpeedReading()
                }
            },
            title = {
                StyledText(
                    text = bookTitle,
                    modifier = Modifier.padding(end = 8.dp),
                    style = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
            },
            subtitle = {
                StyledText(
                    text = chapterTitle ?: "Speed Reading",
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            },
            actions = {
                IconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = R.string.open_reader_settings_content_desc,
                    disableOnClick = false
                ) {
                    onShowSettings()
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SpeedReadingBottomBar(
    progress: String,
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    bottomBarPadding: Dp
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .noRippleClickable(onClick = {})
            .navigationBarsPadding()
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

        Spacer(Modifier.height(6.dp))

        // WPM slider with play/pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button on the left
            IconButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) R.string.pause else R.string.play,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.onSurface
            ) {
                onPlayPause()
            }

            // WPM slider
            Slider(
                value = wpm.toFloat(),
                onValueChange = { onWpmChange((it / 5).toInt() * 5) }, // Snap to 5 increments
                valueRange = 100f..1500f,
                steps = 280, // (1500-100)/5 - 1 = 280 steps
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // WPM indicator
            Text(
                text = "$wpm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp + bottomBarPadding))
    }
}