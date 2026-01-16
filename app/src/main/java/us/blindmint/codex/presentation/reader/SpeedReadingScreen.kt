/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.ReaderText

@Composable
fun SpeedReadingScreen(
    text: List<ReaderText>,
    currentProgress: Float,
    backgroundColor: Color,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    sentencePauseMs: Int,
    onExitSpeedReading: () -> Unit
) {
    // Extract words from text starting from current position
    val words = remember(text, currentProgress) {
        val startIndex = (currentProgress * text.size).toInt()
        text.drop(startIndex)
            .filterIsInstance<ReaderText.Text>()
            .flatMap { it.line.text.split("\\s+".toRegex()) }
            .filter { it.isNotBlank() }
    }

    val currentWordIndex = remember { mutableIntStateOf(0) }
    val currentWord = remember { mutableStateOf("") }
    val isPlaying = remember { mutableStateOf(false) }
    val wpm = remember { mutableIntStateOf(300) } // Default 300 WPM
    val showCountdown = remember { mutableStateOf(false) }
    val countdownValue = remember { mutableIntStateOf(3) }
    val alwaysShowPlayPause = remember { mutableStateOf(false) }

    // Calculate delay between words (milliseconds)
    val wordDelay = remember(wpm.intValue) { (60.0 / wpm.intValue * 1000).toLong() }

    // Update current word
    LaunchedEffect(currentWordIndex.intValue, words) {
        if (currentWordIndex.intValue < words.size) {
            currentWord.value = words[currentWordIndex.intValue]
        } else {
            currentWord.value = ""
            isPlaying.value = false
        }
    }

    // Countdown animation
    LaunchedEffect(showCountdown.value) {
        if (showCountdown.value) {
            countdownValue.intValue = 3
            while (countdownValue.intValue > 0) {
                delay(1000)
                countdownValue.intValue--
            }
            showCountdown.value = false
            isPlaying.value = true
        }
    }

    // Auto-advance words when playing
    LaunchedEffect(isPlaying.value, currentWordIndex.intValue, wordDelay) {
        if (isPlaying.value && currentWordIndex.intValue < words.size) {
            val currentWordText = words.getOrNull(currentWordIndex.intValue) ?: ""
            val isSentenceEnd = currentWordText.endsWith(".") ||
                               currentWordText.endsWith("!") ||
                               currentWordText.endsWith("?") ||
                               currentWordText.endsWith(":") ||
                               currentWordText.endsWith(";")

            val delayTime = if (isSentenceEnd) sentencePauseMs.toLong() else wordDelay
            delay(delayTime)
            currentWordIndex.intValue++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main word display area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal bars (gray frames)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    val barHeight = 2.dp.toPx()
                    val barColor = Color.Gray.copy(alpha = 0.5f)
                    drawRect(
                        color = barColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height / 2 - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight)
                    )
                }

                // Word display
                Text(
                    text = currentWord.value,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = fontFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Accent character highlighting (simple version - highlight first character)
                if (currentWord.value.isNotEmpty()) {
                    val accentChar = currentWord.value.first()
                    val accentIndex = currentWord.value.indexOf(accentChar)
                    if (accentIndex >= 0) {
                        // Vertical indicator at accent position
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            val indicatorColor = Color.Red // Accent color
                            val indicatorWidth = 2f
                            val textWidth = currentWord.value.length * 24f // Rough estimate in pixels
                            val accentPosition = (accentIndex.toFloat() / currentWord.value.length) * textWidth

                            drawRect(
                                color = indicatorColor,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    x = size.width / 2 - textWidth / 2 + accentPosition,
                                    y = 0f
                                ),
                                size = androidx.compose.ui.geometry.Size(indicatorWidth, 32f)
                            )
                        }
                    }
                }
            }
        }

        // Top bar with controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${wpm.intValue} WPM",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            androidx.compose.material3.IconButton(
                onClick = { alwaysShowPlayPause.value = !alwaysShowPlayPause.value }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (alwaysShowPlayPause.value)
                        androidx.compose.material.icons.Icons.Default.Visibility
                    else
                        androidx.compose.material.icons.Icons.Default.VisibilityOff,
                    contentDescription = "Toggle always show play/pause"
                )
            }
        }

        // Bottom bar with controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = onExitSpeedReading
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Exit"
                )
            }

            androidx.compose.material3.IconButton(
                onClick = {
                    if (!isPlaying.value) {
                        showCountdown.value = true
                    } else {
                        isPlaying.value = false
                    }
                }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (isPlaying.value)
                        androidx.compose.material.icons.Icons.Default.Pause
                    else
                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying.value) "Pause" else "Play"
                )
            }

            androidx.compose.material3.Slider(
                value = wpm.intValue.toFloat(),
                onValueChange = { wpm.intValue = it.toInt() },
                valueRange = 100f..1000f,
                steps = 18, // 50 WPM increments
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Floating play/pause button
    if (alwaysShowPlayPause.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (!isPlaying.value) {
                        showCountdown.value = true
                    } else {
                        isPlaying.value = false
                    }
                }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (isPlaying.value)
                        androidx.compose.material.icons.Icons.Default.Pause
                    else
                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying.value) "Pause" else "Play"
                )
            }
        }
    }

    // Countdown overlay
    if (showCountdown.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = countdownValue.intValue.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    color = Color.White
                )
            )
        }
    }
}