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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.ReaderText

@Composable
fun SpeedReadingContent(
    text: List<ReaderText>,
    currentProgress: Float,
    backgroundColor: Color,
    fontFamily: FontFamily,
    sentencePauseMs: Int,
    wpm: Int,
    isPlaying: Boolean,
    onWpmChange: (Int) -> Unit,
    onPlayPause: () -> Unit,
    alwaysShowPlayPause: Boolean
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
    val showCountdown = remember { mutableStateOf(false) }
    val countdownValue = remember { mutableIntStateOf(3) }
    val wordFontSize = remember { mutableStateOf(48.sp) }

    // Update current word
    LaunchedEffect(currentWordIndex.intValue, words) {
        if (currentWordIndex.intValue < words.size) {
            // currentWord is handled in the display logic below
        } else {
            // Reset to beginning when finished
            currentWordIndex.intValue = 0
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
        }
    }

    // Auto-advance words when playing
    LaunchedEffect(isPlaying, currentWordIndex.intValue, wpm) {
        if (isPlaying && currentWordIndex.intValue < words.size) {
            val currentWordText = words.getOrNull(currentWordIndex.intValue) ?: ""
            val isSentenceEnd = currentWordText.endsWith(".") ||
                               currentWordText.endsWith("!") ||
                               currentWordText.endsWith("?") ||
                               currentWordText.endsWith(":") ||
                               currentWordText.endsWith(";")

            val wordDelay = (60.0 / wpm * 1000).toLong()
            val delayTime = if (isSentenceEnd) sentencePauseMs.toLong() else wordDelay
            delay(delayTime)
            currentWordIndex.intValue++
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main word display area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal bars (gray frames) - now positioned around the word area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    val barHeight = 2f
                    val barColor = Color.Gray.copy(alpha = 0.5f)
                    val centerY = size.height / 2
                    drawRect(
                        color = barColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight)
                    )
                }

                // Word display with accent character centering
                if (currentWordIndex.intValue < words.size) {
                    val currentWord = words[currentWordIndex.intValue]
                    SpeedReadingWordDisplay(
                        word = currentWord,
                        fontSize = wordFontSize.value,
                        fontFamily = fontFamily,
                        onFontSizeChange = { wordFontSize.value = it }
                    )
                }

                // Vertical indicators aligned with accent character
                if (currentWordIndex.intValue < words.size) {
                    val currentWord = words[currentWordIndex.intValue]
                    val accentCharIndex = findAccentCharIndex(currentWord)

                    if (accentCharIndex >= 0) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            val indicatorColor = Color.Red // Accent color
                            val indicatorWidth = 2f
                            val indicatorHeight = 32f

                            // Calculate position based on accent character
                            // This is approximate - would need more sophisticated text measurement
                            val approxCharWidth = wordFontSize.value.toPx() * 0.6f // Rough estimate
                            val accentPosition = accentCharIndex * approxCharWidth
                            val centerX = size.width / 2
                            val indicatorX = centerX - accentPosition

                            // Draw vertical indicator at accent character position
                            drawRect(
                                color = indicatorColor,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    x = indicatorX - indicatorWidth / 2,
                                    y = size.height / 2 - indicatorHeight / 2
                                ),
                                size = androidx.compose.ui.geometry.Size(indicatorWidth, indicatorHeight)
                            )
                        }
                    }
                }
            }
        }

        // Floating play/pause button
        if (alwaysShowPlayPause) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isPlaying) {
                            showCountdown.value = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
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
}

@Composable
private fun SpeedReadingWordDisplay(
    word: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily,
    onFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit
) {
    val accentCharIndex = findAccentCharIndex(word)

    if (accentCharIndex >= 0) {
        // Split word around accent character for proper centering
        val beforeAccent = word.substring(0, accentCharIndex)
        val accentChar = word[accentCharIndex]
        val afterAccent = word.substring(accentCharIndex + 1)

        // Create annotated string with accent character highlighted
        val annotatedWord = buildAnnotatedString {
            append(beforeAccent)
            withStyle(style = SpanStyle(color = Color.Red)) {
                append(accentChar)
            }
            append(afterAccent)
        }

        Text(
            text = annotatedWord,
            style = TextStyle(
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                fontFamily = fontFamily
            ),
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        // No accent character found, display normally
        Text(
            text = word,
            style = TextStyle(
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                fontFamily = fontFamily
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun findAccentCharIndex(word: String): Int {
    // Find the first vowel (a, e, i, o, u) as accent character
    // Prioritize 2nd or 3rd character if it's a vowel
    val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')

    // Check positions 1, 2, then 0, then 3, etc.
    val preferredPositions = listOf(1, 2, 0, 3, 4)

    for (pos in preferredPositions) {
        if (pos < word.length && word[pos] in vowels) {
            return pos
        }
    }

    // If no vowel found in preferred positions, find first vowel
    for (i in word.indices) {
        if (word[i] in vowels) {
            return i
        }
    }

    return -1 // No accent character found
}