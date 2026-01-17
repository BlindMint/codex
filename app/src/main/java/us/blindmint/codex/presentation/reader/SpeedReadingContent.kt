/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.ReaderText
import kotlin.math.roundToInt

@Composable
fun SpeedReadingContent(
    text: List<ReaderText>,
    currentProgress: Float,
    backgroundColor: Color,
    fontColor: Color,
    fontFamily: FontFamily,
    sentencePauseMs: Int,
    wordSize: Int,
    accentCharacterEnabled: Boolean,
    accentColor: Color,
    accentOpacity: Float,
    showVerticalIndicators: Boolean,
    verticalIndicatorsSize: Int,
    showHorizontalBars: Boolean,
    horizontalBarsThickness: Int,
    horizontalBarsColor: Color,
    wpm: Int,
    isPlaying: Boolean,
    onWpmChange: (Int) -> Unit,
    onPlayPause: () -> Unit,
    alwaysShowPlayPause: Boolean,
    showWpmIndicator: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Extract words from text starting from current position
    val words = remember(text, currentProgress) {
        val startIndex = (currentProgress * text.size).toInt()
        text.drop(startIndex)
            .filterIsInstance<ReaderText.Text>()
            .flatMap { it.line.text.split("\\s+".toRegex()) }
            .filter { it.isNotBlank() }
    }

    var currentWordIndex by remember { mutableIntStateOf(0) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    val wordFontSize = wordSize.sp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Reset word index when words change
    LaunchedEffect(words) {
        currentWordIndex = 0
    }

    // Countdown animation
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            countdownValue = 3
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
            }
            showCountdown = false
        }
    }

    // Auto-advance words when playing
    LaunchedEffect(isPlaying, currentWordIndex, wpm, words) {
        if (isPlaying && words.isNotEmpty() && currentWordIndex < words.size) {
            val currentWordText = words.getOrNull(currentWordIndex) ?: ""
            val isSentenceEnd = currentWordText.endsWith(".") ||
                               currentWordText.endsWith("!") ||
                               currentWordText.endsWith("?") ||
                               currentWordText.endsWith(":") ||
                               currentWordText.endsWith(";")

            val wordDelay = (60.0 / wpm * 1000).toLong()
            val delayTime = if (isSentenceEnd) sentencePauseMs.toLong() else wordDelay
            delay(delayTime)
            currentWordIndex = if (currentWordIndex < words.size - 1) currentWordIndex + 1 else 0
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (words.isNotEmpty() && currentWordIndex < words.size) {
            val currentWord = words[currentWordIndex]
            val accentIndex = findAccentCharIndex(currentWord)

            // Measure text parts for proper positioning
            val textStyle = TextStyle(
                fontSize = wordFontSize,
                fontFamily = fontFamily,
                color = fontColor
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val containerWidth = constraints.maxWidth.toFloat()
                val centerX = containerWidth / 2f

                // Measure text before accent to calculate offset
                val beforeAccent = if (accentIndex > 0) currentWord.substring(0, accentIndex) else ""
                val accentChar = if (accentIndex >= 0 && accentIndex < currentWord.length)
                    currentWord[accentIndex].toString() else ""

                val beforeAccentWidth = if (beforeAccent.isNotEmpty()) {
                    with(density) {
                        textMeasurer.measure(
                            text = beforeAccent,
                            style = textStyle
                        ).size.width.toFloat()
                    }
                } else 0f

                val accentCharWidth = if (accentChar.isNotEmpty()) {
                    with(density) {
                        textMeasurer.measure(
                            text = accentChar,
                            style = textStyle
                        ).size.width.toFloat()
                    }
                } else 0f

                // Calculate offset to center the accent character
                // The accent char center should be at the screen center
                val accentCenterOffset = beforeAccentWidth + (accentCharWidth / 2f)
                val wordOffsetX = centerX - accentCenterOffset

                // Draw the RSVP frame
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val horizontalLineY = size.height / 2f
                    val gapHalfWidth = 120.dp.toPx() // Gap around the word

                    // Horizontal bars (only if enabled)
                    if (showHorizontalBars) {
                        val lineThickness = horizontalBarsThickness.dp.toPx()

                        // Left horizontal line
                        drawLine(
                            color = horizontalBarsColor,
                            start = Offset(0f, horizontalLineY),
                            end = Offset(centerX - gapHalfWidth, horizontalLineY),
                            strokeWidth = lineThickness
                        )

                        // Right horizontal line
                        drawLine(
                            color = horizontalBarsColor,
                            start = Offset(centerX + gapHalfWidth, horizontalLineY),
                            end = Offset(size.width, horizontalLineY),
                            strokeWidth = lineThickness
                        )
                    }

                    // Vertical indicators (only if enabled)
                    if (showVerticalIndicators) {
                        val verticalIndicatorHeight = verticalIndicatorsSize.dp.toPx()
                        val verticalIndicatorWidth = 1.5.dp.toPx()

                        // Top vertical indicator (points down to accent position)
                        drawLine(
                            color = horizontalBarsColor,
                            start = Offset(centerX, horizontalLineY - 40.dp.toPx()),
                            end = Offset(centerX, horizontalLineY - 40.dp.toPx() + verticalIndicatorHeight),
                            strokeWidth = verticalIndicatorWidth
                        )

                        // Bottom vertical indicator (points up to accent position)
                        drawLine(
                            color = horizontalBarsColor,
                            start = Offset(centerX, horizontalLineY + 40.dp.toPx() - verticalIndicatorHeight),
                            end = Offset(centerX, horizontalLineY + 40.dp.toPx()),
                            strokeWidth = verticalIndicatorWidth
                        )
                    }
                }

                // Word display with accent character highlighted and properly offset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (accentCharacterEnabled && accentIndex >= 0) {
                                if (accentIndex > 0) {
                                    append(currentWord.substring(0, accentIndex))
                                }
                                if (accentIndex < currentWord.length) {
                                    withStyle(style = SpanStyle(color = accentColor.copy(alpha = accentOpacity))) {
                                        append(currentWord[accentIndex])
                                    }
                                }
                                if (accentIndex < currentWord.length - 1) {
                                    append(currentWord.substring(accentIndex + 1))
                                }
                            } else {
                                append(currentWord)
                            }
                        },
                        style = textStyle,
                        modifier = Modifier
                            .offset {
                                IntOffset(wordOffsetX.roundToInt(), 0)
                            }
                            .semantics {
                                contentDescription = "Speed reading word: $currentWord at $wpm words per minute"
                            }
                    )
                }
            }
        }

        // WPM indicator in bottom right
        if (showWpmIndicator) {
            Text(
                text = "$wpm wpm",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = fontColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        // Countdown overlay
        if (showCountdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownValue.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Find the Optimal Recognition Point (ORP) for RSVP.
 *
 * The ORP is typically around 25-35% into the word, often on a vowel.
 * This follows standard RSVP guidelines where the eye fixation point
 * should be slightly left of center for most words.
 *
 * Enhanced to handle:
 * - Accented vowels for multiple languages
 * - Better ORP calculation for very long words
 * - Numbers and special characters
 */
fun findAccentCharIndex(word: String): Int {
    if (word.isEmpty()) return -1

    // Strip punctuation for calculation
    val cleanWord = word.trimEnd { it in ".,!?;:'\"-" }
    if (cleanWord.isEmpty()) return 0

    // Extended vowel set for multiple languages including accented characters
    val vowels = setOf(
        'a', 'e', 'i', 'o', 'u', 'y', // Basic English
        'A', 'E', 'I', 'O', 'U', 'Y', // Uppercase
        'à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā', // Accented a
        'À', 'Á', 'Â', 'Ä', 'Æ', 'Ã', 'Å', 'Ā',
        'è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę', // Accented e
        'È', 'É', 'Ê', 'Ë', 'Ē', 'Ė', 'Ę',
        'ì', 'í', 'î', 'ï', 'ī', 'į', // Accented i
        'Ì', 'Í', 'Î', 'Ï', 'Ī', 'Į',
        'ò', 'ó', 'ô', 'ö', 'õ', 'ø', 'ō', 'œ', // Accented o
        'Ò', 'Ó', 'Ô', 'Ö', 'Õ', 'Ø', 'Ō', 'Œ',
        'ù', 'ú', 'û', 'ü', 'ū', // Accented u
        'Ù', 'Ú', 'Û', 'Ü', 'Ū',
        'ÿ', 'Ÿ', // Accented y
        // Additional language vowels
        'ı', 'İ', // Turkish dotless i
        'ø', 'Ø', // Danish/Norwegian
        'å', 'Å', 'ä', 'Ä', 'ö', 'Ö' // Swedish/German
    )

    // ORP position based on word length (refined for better long word handling)
    val optimalPosition = when {
        cleanWord.length <= 1 -> 0
        cleanWord.length <= 3 -> 0
        cleanWord.length <= 5 -> 1
        cleanWord.length <= 9 -> 2
        cleanWord.length <= 13 -> 3
        cleanWord.length <= 17 -> 4
        else -> (cleanWord.length * 0.3).toInt().coerceAtMost(6) // Allow further for very long words
    }

    // Check optimal position first, then nearby positions
    val positionsToCheck = listOf(
        optimalPosition,
        optimalPosition + 1,
        optimalPosition - 1,
        optimalPosition + 2,
        optimalPosition - 2
    ).filter { it in cleanWord.indices }

    // Prefer vowels at or near the optimal position
    for (pos in positionsToCheck) {
        if (cleanWord[pos] in vowels) {
            return pos
        }
    }

    // For words with numbers or symbols, find first letter-like character
    val letterIndices = cleanWord.indices.filter { cleanWord[it].isLetter() }
    if (letterIndices.isNotEmpty()) {
        // Use ORP among letters
        val letterOptimal = when {
            letterIndices.size <= 1 -> 0
            letterIndices.size <= 3 -> 0
            letterIndices.size <= 5 -> 1
            else -> (letterIndices.size * 0.3).toInt()
        }
        return letterIndices[letterOptimal.coerceIn(0, letterIndices.size - 1)]
    }

    // If no suitable position found, use the optimal position anyway
    return optimalPosition.coerceIn(0, cleanWord.length - 1)
}