/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingContent(
    text: List<ReaderText>,
    currentProgress: Float,
    totalProgress: Float, // Overall book progress
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
    verticalIndicatorType: us.blindmint.codex.ui.reader.SpeedReadingVerticalIndicatorType,
    showHorizontalBars: Boolean,
    horizontalBarsThickness: Int,
    horizontalBarsLength: Float,
    horizontalBarsDistance: Int,
    horizontalBarsColor: Color,
    horizontalBarsOpacity: Float,
    focalPointPosition: Float,
    wpm: Int,
    isPlaying: Boolean,
    onWpmChange: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNavigateWord: (Int) -> Unit = {}, // -1 for back, +1 for forward
    onToggleMenu: () -> Unit = {},
    navigateWord: (Int) -> Unit = {},
    onRegisterNavigationCallback: ((Int) -> Unit) -> Unit = {},
    alwaysShowPlayPause: Boolean,
    showWpmIndicator: Boolean = true,
    osdEnabled: Boolean = true,
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
    var lastNavigationDirection by remember { mutableIntStateOf(0) }
    var showQuickWpmMenu by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    val wordFontSize = wordSize.sp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Reset word index when words change
    LaunchedEffect(words) {
        currentWordIndex = 0
        lastNavigationDirection = 0
    }

    // Handle word navigation
    val handleNavigateWord: (Int) -> Unit = { direction ->
        val newIndex = (currentWordIndex + direction).coerceIn(0, words.size - 1)
        if (newIndex != currentWordIndex) {
            currentWordIndex = newIndex
            lastNavigationDirection = direction
            // Pause auto-play when manually navigating
            if (isPlaying) {
                onPlayPause()
            }
        }
    }

    // Register navigation callback
    LaunchedEffect(Unit) {
        onRegisterNavigationCallback { direction ->
            handleNavigateWord(direction)
        }
    }

    // Register the navigation callback
    LaunchedEffect(Unit) {
        onRegisterNavigationCallback { direction ->
            handleNavigateWord(direction)
        }
    }


    // Call the parent's onNavigateWord callback
    LaunchedEffect(currentWordIndex) {
        onNavigateWord(lastNavigationDirection)
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

    var boxSize by remember { mutableStateOf(0 to 0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it.width to it.height }
            .pointerInput(isPlaying) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            val position = event.changes.first().position
                            val width = boxSize.first.toFloat()
                            val tapZoneWidth = width * 0.2f // 20% edge zones

                            when {
                                position.x < tapZoneWidth -> {
                                    // Left tap zone - navigate back
                                    navigateWord(-1)
                                }
                                position.x > width - tapZoneWidth -> {
                                    // Right tap zone - navigate forward
                                    navigateWord(1)
                                }
                                else -> {
                                    // Middle tap zone - pause if playing, otherwise toggle menu
                                    if (isPlaying) {
                                        onPlayPause()
                                    } else {
                                        onToggleMenu()
                                    }
                                }
                            }
                        }
                    }
                }
            },
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

                // Measure full word height for bar positioning when distance is 0
                val fullWordHeight = with(density) {
                    textMeasurer.measure(
                        text = currentWord,
                        style = textStyle
                    ).size.height.toFloat()
                }

                // Calculate offset to position the accent character at the focal point
                val accentCenterOffset = beforeAccentWidth + (accentCharWidth / 2f)
                val wordOffsetX = focalPointX - accentCenterOffset

                // Frame dimensions
                val frameHeight = 120.dp
                val wordAreaHeight = 60.dp // Height reserved for word display
                val barToWordGap = horizontalBarsDistance.dp // Configurable gap between horizontal bar and word area

                val barColorWithOpacity = horizontalBarsColor.copy(alpha = horizontalBarsOpacity)

                // Calculate bar positions
                val barPositions = remember(frameHeight, wordAreaHeight, barToWordGap, density) {
                    with(density) {
                        val centerY = frameHeight.toPx() / 2f
                        val wordAreaTop = centerY - (wordAreaHeight.toPx() / 2f)
                        val wordAreaBottom = centerY + (wordAreaHeight.toPx() / 2f)
                        val topBarY = wordAreaTop - barToWordGap.toPx()
                        val bottomBarY = wordAreaBottom + barToWordGap.toPx()
                        Triple(topBarY, bottomBarY, centerY)
                    }
                }
                val (topBarY, bottomBarY, centerY) = barPositions

                // Draw the RSVP frame - horizontal bars above and below the word
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(frameHeight)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(frameHeight)
                    ) {
                        // Horizontal bars (only if enabled) - TOP and BOTTOM borders
                        if (showHorizontalBars) {
                            val lineThickness = horizontalBarsThickness.dp.toPx()
                            val barWidth = size.width * horizontalBarsLength
                            val barStartX = (size.width - barWidth) / 2f
                            val barEndX = barStartX + barWidth

                            // Top horizontal bar - variable width, centered
                            drawLine(
                                color = barColorWithOpacity,
                                start = Offset(barStartX, topBarY),
                                end = Offset(barEndX, topBarY),
                                strokeWidth = lineThickness
                            )

                            // Bottom horizontal bar - variable width, centered
                            drawLine(
                                color = barColorWithOpacity,
                                start = Offset(barStartX, bottomBarY),
                                end = Offset(barEndX, bottomBarY),
                                strokeWidth = lineThickness
                            )
                        }

                        // For LINE type, draw vertical indicators in canvas
                        if (showVerticalIndicators && verticalIndicatorType == SpeedReadingVerticalIndicatorType.LINE) {
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

                    // Vertical indicators as icons (for ARROWS and ARROWS_FILLED types)
                    if (showVerticalIndicators && verticalIndicatorType != SpeedReadingVerticalIndicatorType.LINE) {
                        val verticalIndicatorHeight = verticalIndicatorsSize.dp
                        val iconSize = verticalIndicatorHeight * 3.5f // Much larger than the indicator height (3-4x)

                        // Top arrow (pointing down from top bar)
                        val topIcon = when (verticalIndicatorType) {
                            SpeedReadingVerticalIndicatorType.ARROWS -> Icons.Filled.KeyboardArrowDown
                            SpeedReadingVerticalIndicatorType.ARROWS_FILLED -> Icons.Filled.ArrowDropDown
                            else -> Icons.Filled.KeyboardArrowDown
                        }

                        Icon(
                            imageVector = topIcon,
                            contentDescription = null,
                            tint = barColorWithOpacity,
                            modifier = Modifier
                                .size(iconSize)
                                .offset(
                                    x = with(density) { (focalPointX - iconSize.toPx() / 2).toDp() },
                                    y = with(density) { topBarY.toDp() }
                                )
                        )

                        // Bottom arrow (pointing up from bottom bar)
                        val bottomIcon = when (verticalIndicatorType) {
                            SpeedReadingVerticalIndicatorType.ARROWS -> Icons.Filled.KeyboardArrowUp
                            SpeedReadingVerticalIndicatorType.ARROWS_FILLED -> Icons.Filled.ArrowDropUp
                            else -> Icons.Filled.KeyboardArrowUp
                        }

                        Icon(
                            imageVector = bottomIcon,
                            contentDescription = null,
                            tint = barColorWithOpacity,
                            modifier = Modifier
                                .size(iconSize)
                                .offset(
                                    x = with(density) { (focalPointX - iconSize.toPx() / 2).toDp() },
                                    y = with(density) { (bottomBarY - iconSize.toPx()).toDp() }
                                )
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

        // OSD controls centered at bottom
        if (osdEnabled) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val bottomPadding = screenHeight * 0.2f // 20% from bottom

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp), // Increased spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left arrow (<) - navigate to previous word
                     Text(
                         text = "<",
                         style = MaterialTheme.typography.headlineMedium.copy( // Increased size
                             color = fontColor.copy(alpha = 0.7f),
                             fontSize = 28.sp // Larger size
                         ),
                         modifier = Modifier
                             .padding(12.dp) // Increased clickable area
                             .noRippleClickable {
                                 navigateWord(-1) // Navigate to previous word
                             }
                     )

                    // Play/Pause button - centered and larger, minimal icon
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = fontColor,
                        modifier = Modifier
                            .size(60.dp) // 2.5x larger than default 24.dp
                            .padding(8.dp)
                            .noRippleClickable {
                                onPlayPause()
                            }
                    )

                    // Right arrow (>) - navigate to next word
                     Text(
                         text = ">",
                         style = MaterialTheme.typography.headlineMedium.copy( // Increased size
                             color = fontColor.copy(alpha = 0.7f),
                             fontSize = 28.sp // Larger size
                         ),
                         modifier = Modifier
                             .padding(12.dp) // Increased clickable area
                             .noRippleClickable {
                                 navigateWord(1) // Navigate to next word
                             }
                     )
                }
            }
        }

        // Bottom bar with progress bar and controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = backgroundColor.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress bar (50% width)
            LinearProgressIndicator(
                progress = { totalProgress },
                modifier = Modifier.weight(0.5f),
                color = fontColor.copy(alpha = 0.7f),
                trackColor = fontColor.copy(alpha = 0.2f)
            )

            // Right side controls (50% width, evenly spaced)
            Row(
                modifier = Modifier.weight(0.5f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WPM indicator (tappable for quick menu)
                Text(
                    text = "$wpm",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = fontColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.noRippleClickable {
                        showQuickWpmMenu = true
                    }
                )

                // Book icon (placeholder for now)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Book info",
                    tint = fontColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
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

        // Quick WPM Menu Bottom Sheet
        if (showQuickWpmMenu) {
            val sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = { showQuickWpmMenu = false },
                sheetState = sheetState,
                containerColor = backgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "Words per minute",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = fontColor
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Top row: -100, -50, current WPM, +50, +100
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "-100",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .noRippleClickable {
                                    onWpmChange((wpm - 100).coerceAtLeast(200))
                                }
                        )

                        Text(
                            text = "-50",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .noRippleClickable {
                                    onWpmChange((wpm - 50).coerceAtLeast(200))
                                }
                        )

                        Text(
                            text = "$wpm",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = fontColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Text(
                            text = "+50",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .noRippleClickable {
                                    onWpmChange((wpm + 50).coerceAtMost(1200))
                                }
                        )

                        Text(
                            text = "+100",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .noRippleClickable {
                                    onWpmChange((wpm + 100).coerceAtMost(1200))
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom row: slider with - and + symbols
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.noRippleClickable {
                                onWpmChange((wpm - 10).coerceAtLeast(200))
                            }
                        )

                        Slider(
                            value = wpm.toFloat(),
                            onValueChange = { onWpmChange(it.toInt()) },
                            valueRange = 200f..1200f,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "+",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = fontColor.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.noRippleClickable {
                                onWpmChange((wpm + 10).coerceAtMost(1200))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Find the Optimal Viewing Position (OVP) for RSVP accent highlight.
 *
 * Hybrid strategy: Post-1st vowel priority with center-leftward scan for long words.
 * Balances saccade minimization (Rayner: 2nd/3rd position, 28% faster) with phonics
 * comprehension (vowel boost: +20%, Spreeder research).
 *
 * Rules:
 * - Short (1-3 chars): 2nd character (position 1)
 * - Medium (4-6 chars): 1st available vowel after position 0
 * - Long (7+ chars): Scan from center leftward (descending) for vowel
 * - Fallback: 2nd character (position 1)
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

    // Rule: Short words (1-3 chars) - always 2nd character
    if (len <= 3) {
        return if (len >= 2) 1 else 0
    }

    // Rule: Long words (7+ chars) - center leftward scan
    if (len >= 7) {
        val center = len / 2
        // Scan from center leftward (descending) for first vowel
        for (i in center downTo 1) {
            if (cleanWord[i] in vowels) {
                return i
            }
        }
        // Fallback: 2nd character
        return 1
    }

    // Rule: Medium words (4-6 chars) - post-1st vowel priority
    for (i in 1..<len) {
        if (cleanWord[i] in vowels) {
            return i
        }
    }

    // Fallback: 2nd character (position 1)
    return 1
}