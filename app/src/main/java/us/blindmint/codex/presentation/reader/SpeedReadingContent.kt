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
    horizontalBarsDistance: Int,
    horizontalBarsColor: Color,
    horizontalBarsOpacity: Float,
    focalPointPosition: Float,
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

    // Focal point position - configurable via settings
    // This creates a consistent focal point that words align to
    val accentOffsetRatio = focalPointPosition

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
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val containerWidth = constraints.maxWidth.toFloat()
                // Fixed focal point - offset to the left of center
                val focalPointX = containerWidth * accentOffsetRatio

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

                // Calculate offset to position the accent character at the focal point
                val accentCenterOffset = beforeAccentWidth + (accentCharWidth / 2f)
                val wordOffsetX = focalPointX - accentCenterOffset

                // Frame dimensions
                val frameHeight = 120.dp
                val wordAreaHeight = 60.dp // Height reserved for word display
                val barToWordGap = horizontalBarsDistance.dp // Configurable gap between horizontal bar and word area

                // Draw the RSVP frame - horizontal bars above and below the word
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(frameHeight)
                ) {
                    val centerY = size.height / 2f
                    val wordAreaTop = centerY - (wordAreaHeight.toPx() / 2f)
                    val wordAreaBottom = centerY + (wordAreaHeight.toPx() / 2f)

                    // Position for horizontal bars (above and below word area)
                    val topBarY = wordAreaTop - barToWordGap.toPx()
                    val bottomBarY = wordAreaBottom + barToWordGap.toPx()

                    // Apply opacity to horizontal bars color
                    val barColorWithOpacity = horizontalBarsColor.copy(alpha = horizontalBarsOpacity)

                    // Horizontal bars (only if enabled) - TOP and BOTTOM borders
                    if (showHorizontalBars) {
                        val lineThickness = horizontalBarsThickness.dp.toPx()

                        // Top horizontal bar - full width
                        drawLine(
                            color = barColorWithOpacity,
                            start = Offset(0f, topBarY),
                            end = Offset(size.width, topBarY),
                            strokeWidth = lineThickness
                        )

                        // Bottom horizontal bar - full width
                        drawLine(
                            color = barColorWithOpacity,
                            start = Offset(0f, bottomBarY),
                            end = Offset(size.width, bottomBarY),
                            strokeWidth = lineThickness
                        )
                    }

                    // Vertical indicators (only if enabled) - form T shapes with horizontal bars
                    if (showVerticalIndicators) {
                        val verticalIndicatorHeight = verticalIndicatorsSize.dp.toPx()
                        val verticalIndicatorWidth = 1.5.dp.toPx()

                        // Top vertical indicator - starts at top bar, points DOWN toward word
                        drawLine(
                            color = barColorWithOpacity,
                            start = Offset(focalPointX, topBarY),
                            end = Offset(focalPointX, topBarY + verticalIndicatorHeight),
                            strokeWidth = verticalIndicatorWidth
                        )

                        // Bottom vertical indicator - starts at bottom bar, points UP toward word
                        drawLine(
                            color = barColorWithOpacity,
                            start = Offset(focalPointX, bottomBarY),
                            end = Offset(focalPointX, bottomBarY - verticalIndicatorHeight),
                            strokeWidth = verticalIndicatorWidth
                        )
                    }
                }

                // Word display with accent character highlighted and properly offset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(frameHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = fontColor)) {
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
                            }
                        },
                        style = textStyle.copy(color = fontColor),
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
 * Find the Optimal Viewing Position (OVP) for RSVP accent highlight.
 *
 * Rules based on eye-tracking research (Rayner saccades, ~14px fovea):
 * - Primary: 2nd letter if vowel (3-6 char words) - OVP center, 28% faster reading
 * - Fallback: 3rd letter for 7+ char words - word center (~50% length)
 * - Short: 2nd char for 1-3 char words - avoid predictable first char
 * - Long: Center (len/2) for 10+ chars with vowel scan
 * - No vowel: 2nd/3rd consonant as fallback
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
        'ı', 'İ', // Turkish dotless i
        'ø', 'Ø', // Danish/Norwegian
        'å', 'Å', 'ä', 'Ä', 'ö', 'Ö' // Swedish/German
    )

    val len = cleanWord.length

    // Rule: Short words (1-3 chars) - use 2nd char, prefer consonant if no vowel
    if (len <= 3) {
        if (len == 1) return 0
        // Check 2nd position (index 1) for vowel first
        if (len >= 2 && cleanWord[1] in vowels) return 1
        // Fallback to 2nd char even if consonant
        return if (len >= 2) 1 else 0
    }

    // Rule: Medium words (3-6 chars) - Primary: 2nd letter if vowel
    if (len in 3..6) {
        // Scan after 1st letter for 1st/2nd vowel, prefer 2nd position
        if (cleanWord[1] in vowels) return 1
        if (len > 2 && cleanWord[2] in vowels) return 2
        // Fallback: 2nd or 3rd char
        return 1
    }

    // Rule: Medium-long words (7-9 chars) - 3rd letter preferred
    if (len in 7..9) {
        // Prefer 3rd position if vowel
        if (cleanWord[2] in vowels) return 2
        if (cleanWord[1] in vowels) return 1
        if (len > 3 && cleanWord[3] in vowels) return 3
        // Fallback to 3rd char
        return 2
    }

    // Rule: Long words (10+ chars) - Center (len/2) with vowel scan
    val centerPos = len / 2

    // Scan for first available vowel near center
    val scanPositions = listOf(
        centerPos,
        centerPos - 1,
        centerPos + 1,
        centerPos - 2,
        centerPos + 2
    ).filter { it in cleanWord.indices }

    for (pos in scanPositions) {
        if (cleanWord[pos] in vowels) {
            return pos
        }
    }

    // Rule: No vowel found - use 2nd/3rd consonant
    // For longer words, prefer position closer to center
    return when {
        len >= 10 -> centerPos.coerceIn(0, len - 1)
        len >= 7 -> 2.coerceIn(0, len - 1)
        else -> 1.coerceIn(0, len - 1)
    }
}