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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
    progressValue: Float, // Add progress value for the bar
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNavigateWord: (Int) -> Unit, // Add navigation callback
    onCloseMenu: () -> Unit, // Add close menu callback
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

        // OSD Controls Bar (back, forward, play)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = R.string.previous_word,
                disableOnClick = false
            ) {
                onNavigateWord(-1)
            }

            // Play/Pause button (larger, centered)
            IconButton(
                modifier = Modifier.size(48.dp),
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) R.string.pause else R.string.play,
                disableOnClick = false
            ) {
                onPlayPause()
                onCloseMenu() // Close menu when play is pressed
            }

            // Forward button
            IconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = R.string.next_word,
                disableOnClick = false
            ) {
                onNavigateWord(1)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Progress text
        StyledText(
            text = progress,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(Modifier.height(8.dp))

        // Progress bar (same as speed reader)
        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )

        Spacer(Modifier.height(8.dp + bottomBarPadding))
    }
}