/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.util.Log
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
import androidx.compose.foundation.layout.width
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
import us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.presentation.core.util.noRippleClickable
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingContent(
    words: List<SpeedReaderWord>,
    currentWordIndex: Int,
    totalWords: Int,
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
    verticalIndicatorType: us.blindmint.codex.domain.reader.SpeedReadingVerticalIndicatorType,
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
    osdHeight: Float = 0.5f, // 0.0 = top, 1.0 = bottom
    osdSeparation: Float = 0.5f, // 0.0 = close, 1.0 = far
    centerWord: Boolean = false,
    wordPickerActive: Boolean = false,
    initialWordIndex: Int = 0,
    onShowWordPicker: () -> Unit = {},
    onProgressUpdate: (Float, Int) -> Unit = { _, _ -> }, // Callback for word-based progress updates
    onSaveProgress: (Float, Int) -> Unit = { _, _ -> }, // Callback for immediate progress saves (no throttling)
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Log initial composition
    Log.d("SPEED_READER_CONTENT", "[COMPOSITION START] words.size=${words.size}, currentWordIndex=$currentWordIndex, totalWords=$totalWords, initialWordIndex=$initialWordIndex, isPlaying=$isPlaying")

    // Speed reader always starts from beginning of book
    val startingWordIndex = 0

    var currentWordIndex by remember { mutableIntStateOf(initialWordIndex) }
    Log.d("SPEED_READER_CONTENT", "[INIT] currentWordIndex initialized from initialWordIndex=$initialWordIndex")
    var lastProgressSaveIndex by remember { mutableIntStateOf(startingWordIndex) }
    var lastNavigationDirection by remember { mutableIntStateOf(0) }
    var showQuickWpmMenu by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    val wordFontSize = wordSize.sp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Note: currentWordIndex is initialized with initialWordIndex and will be updated
    // by user navigation. We don't reset it here to avoid race conditions
    // with the parent SpeedReadingScaffold's selectedWordIndex management.

    // Sync local state with initialWordIndex when it changes (e.g., from word picker)
    LaunchedEffect(initialWordIndex) {
        if (currentWordIndex != initialWordIndex) {
            currentWordIndex = initialWordIndex
            Log.d("SPEED_READER_CONTENT", "[SYNC] Synced currentWordIndex to initialWordIndex=$initialWordIndex")
        }
    }

    // Handle word navigation
    val handleNavigateWord: (Int) -> Unit = { direction ->
        val newIndex = (currentWordIndex + direction).coerceIn(0, words.size - 1)
        if (newIndex != currentWordIndex) {
            currentWordIndex = newIndex
            lastNavigationDirection = direction
            // Pause auto-play when manually navigating
            if (isPlaying) {
                Log.d("SPEED_READER", "Navigation pause: currentWordIndex=$currentWordIndex, totalWords=$totalWords")
                onPlayPause()
                // Save progress immediately when manually pausing
                val globalWordIndex = startingWordIndex + currentWordIndex
                val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                Log.d("SPEED_READER", "Navigation saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                onSaveProgress(newProgress, globalWordIndex)
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
            val currentWordText = words.getOrNull(currentWordIndex)?.text ?: ""

            // Check for sentence-ending punctuation (period, exclamation, question, colon)
            val isSentenceEnd = currentWordText.endsWith(".") ||
                               currentWordText.endsWith("!") ||
                               currentWordText.endsWith("?") ||
                               currentWordText.endsWith(":")

            // Check for comma/semicolon pause (medium length pause)
            val isCommaPause = currentWordText.endsWith(",") ||
                              currentWordText.endsWith(";")

            val wordDelay = (60.0 / wpm * 1000).toLong()
            val delayTime = when {
                isSentenceEnd -> sentencePauseMs.toLong()
                isCommaPause -> (wordDelay * 1.5).toLong() // 50% longer than normal word
                else -> wordDelay
            }
            delay(delayTime)
            currentWordIndex = if (currentWordIndex < words.size - 1) currentWordIndex + 1 else 0
        }
    }

    // Periodic progress tracking - save every 50 words during playback only (for crash recovery)
    LaunchedEffect(isPlaying, currentWordIndex) {
        val globalWordIndex = startingWordIndex + currentWordIndex
        val wordsSinceLastSave = globalWordIndex - lastProgressSaveIndex

        // Only auto-save during playback (every 50 words) for crash recovery
        // Manual pauses are handled separately with immediate saves (no throttling)
        val shouldSave = isPlaying && wordsSinceLastSave >= 50

        if (shouldSave && totalWords > 0 && wordsSinceLastSave > 0) {
            // Store precise word-based progress - normal reader will convert when loading
            val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
            Log.d("SPEED_READER", "Auto-saving progress: globalWordIndex=$globalWordIndex, totalWords=$totalWords, newProgress=$newProgress")
            onProgressUpdate(newProgress, globalWordIndex)
            lastProgressSaveIndex = globalWordIndex
        }
    }

    // Save progress when pausing via OSD or other means
    var wasPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (wasPlaying && !isPlaying && words.isNotEmpty()) {
            val globalWordIndex = startingWordIndex + currentWordIndex
            val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
            onSaveProgress(newProgress, globalWordIndex)
        }
        wasPlaying = isPlaying
    }

    // Focal point position - configurable via settings
    // This creates a consistent focal point that words align to
    val accentOffsetRatio = if (centerWord) 0.5f else focalPointPosition

    var boxSize by remember { mutableStateOf(0 to 0) }

    // Get screen dimensions for calculating exclusion zones
    val windowInfo = LocalWindowInfo.current
    val screenHeightDp = with(LocalDensity.current) { windowInfo.containerSize.height.toDp() }

    // Calculate OSD exclusion zone bounds in dp
    // OSD is positioned at (screenHeight * (1 - osdHeight)) from top
    // OSD height is approximately 60dp (play button) + 16dp vertical padding = ~80dp
    val osdTopDp = (screenHeightDp.value * (1f - osdHeight)).dp - 8.dp // Account for padding
    val osdHeightDp = 80.dp // Approximate height of OSD controls with padding
    val osdBottomDp = osdTopDp + osdHeightDp

    // Bottom bar exclusion zone: approximately 60dp from bottom
    val bottomBarHeightDp = 92.dp // 60.dp bar height + 32.dp bottom padding

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it.width to it.height }
            .pointerInput(isPlaying, osdEnabled, osdHeight) {
                // Convert dp to pixels for tap detection
                val osdTopPx = with(density) { osdTopDp.toPx() }
                val osdBottomPx = with(density) { osdBottomDp.toPx() }
                val bottomBarTopPx = boxSize.second - with(density) { bottomBarHeightDp.toPx() }

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            val position = event.changes.first().position
                            val width = boxSize.first.toFloat()
                            val height = boxSize.second.toFloat()
                            val tapZoneWidth = width * 0.2f // 20% edge zones

                            // Check if tap is in OSD exclusion zone (only if OSD is enabled)
                            val inOsdZone = osdEnabled &&
                                position.y >= osdTopPx &&
                                position.y <= osdBottomPx

                            // Check if tap is in bottom bar exclusion zone
                            val inBottomBarZone = position.y >= bottomBarTopPx

                            // Skip processing if tap is in an exclusion zone
                            if (inOsdZone || inBottomBarZone) {
                                continue
                            }

                              when {
                                   position.x < tapZoneWidth -> {
                                       // Left tap zone - navigate back (or pause if playing)
                                       if (isPlaying) {
                                           Log.d("SPEED_READER", "Left tap pause: currentWordIndex=$currentWordIndex, totalWords=$totalWords")
                                           onPlayPause()
                                           // Save progress immediately when manually pausing
                                           val globalWordIndex = startingWordIndex + currentWordIndex
                                           val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                                           Log.d("SPEED_READER", "Left tap saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                                           onSaveProgress(newProgress, globalWordIndex)
                                       } else {
                                           navigateWord(-1)
                                       }
                                   }
                                   position.x > width - tapZoneWidth -> {
                                       // Right tap zone - navigate forward (or pause if playing)
                                       if (isPlaying) {
                                           Log.d("SPEED_READER", "Right tap pause: currentWordIndex=$currentWordIndex, totalWords=$totalWords")
                                           onPlayPause()
                                           // Save progress immediately when manually pausing
                                           val globalWordIndex = startingWordIndex + currentWordIndex
                                           val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                                           Log.d("SPEED_READER", "Right tap saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                                           onSaveProgress(newProgress, globalWordIndex)
                                       } else {
                                           navigateWord(1)
                                       }
                                   }
                                   else -> {
                                       // Middle tap zone - always toggle play/pause
                                       val wasPlaying = isPlaying
                                       Log.d("SPEED_READER", "Middle tap: wasPlaying=$wasPlaying, currentWordIndex=$currentWordIndex")
                                       onPlayPause()
                                       // Save progress immediately when manually pausing
                                       if (wasPlaying && !isPlaying) {
                                           val globalWordIndex = startingWordIndex + currentWordIndex
                                           val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                                           Log.d("SPEED_READER", "Middle tap saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                                           onSaveProgress(newProgress, globalWordIndex)
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
            val currentWord = words[currentWordIndex].text
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

                // Measure full word dimensions
                val fullWordSize = with(density) {
                    textMeasurer.measure(
                        text = currentWord,
                        style = textStyle
                    ).size
                }
                val fullWordWidth = fullWordSize.width.toFloat()
                val fullWordHeight = fullWordSize.height.toFloat()

                // Calculate offset to position the word at the focal point
                val wordOffsetX = if (centerWord) {
                    // Center the entire word horizontally
                    focalPointX - (fullWordWidth / 2f)
                } else {
                    // Position so accent character is at focal point
                    val accentCenterOffset = beforeAccentWidth + (accentCharWidth / 2f)
                    focalPointX - accentCenterOffset
                }

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

        // OSD controls positioned based on settings
        // The parent tap detector has an exclusion zone for this area
        if (osdEnabled) {
            // osdHeight: 0.0 = top, 1.0 = bottom, 0.5 = middle
            val verticalPosition = (screenHeightDp.value * (1f - osdHeight)).dp

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = verticalPosition)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val separationDp = (12f + (osdSeparation * 36f)).dp // 12dp to 48dp
                Row(
                    horizontalArrangement = Arrangement.spacedBy(separationDp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left arrow (<) - navigate to previous word
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = fontColor.copy(alpha = 0.7f),
                            fontSize = 28.sp
                        ),
                        modifier = Modifier
                            .padding(12.dp)
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
                            .size(60.dp)
                            .padding(8.dp)
                              .noRippleClickable {
                                  val wasPlaying = isPlaying
                                  Log.d("SPEED_READER", "OSD play/pause: wasPlaying=$wasPlaying, currentWordIndex=$currentWordIndex")
                                  onPlayPause()
                                  // Save progress immediately when manually pausing
                                  if (wasPlaying && !isPlaying) {
                                      val globalWordIndex = startingWordIndex + currentWordIndex
                                      val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                                      Log.d("SPEED_READER", "OSD saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                                      onSaveProgress(newProgress, globalWordIndex)
                                  }
                              }
                    )

                    // Right arrow (>) - navigate to next word
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = fontColor.copy(alpha = 0.7f),
                            fontSize = 28.sp
                        ),
                        modifier = Modifier
                            .padding(12.dp)
                            .noRippleClickable {
                                navigateWord(1) // Navigate to next word
                            }
                    )
                }
            }
        }

        // Bottom bar with progress bar and controls
        // The parent tap detector has an exclusion zone for this area
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp)
                    .background(
                        color = backgroundColor.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress bar (takes available space)
                LinearProgressIndicator(
                    progress = { if (totalWords > 0) currentWordIndex.toFloat() / totalWords else 0f },
                    modifier = Modifier.weight(1f),
                    color = fontColor.copy(alpha = 0.7f),
                    trackColor = fontColor.copy(alpha = 0.2f)
                )

                // Spacer for consistent spacing
                Spacer(modifier = Modifier.width(24.dp))

                 // WPM indicator (tappable for quick menu)
                 Text(
                     text = "$wpm wpm",
                     style = MaterialTheme.typography.bodyMedium.copy(
                         color = fontColor.copy(alpha = 0.8f),
                         fontWeight = FontWeight.Medium
                     ),
                      modifier = Modifier.noRippleClickable {
                          if (isPlaying) {
                              Log.d("SPEED_READER", "WPM tap pause: currentWordIndex=$currentWordIndex, totalWords=$totalWords")
                              onPlayPause()
                              // Save progress immediately when manually pausing
                              val globalWordIndex = startingWordIndex + currentWordIndex
                              val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                              Log.d("SPEED_READER", "WPM tap saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                              onSaveProgress(newProgress, globalWordIndex)
                          }
                          showQuickWpmMenu = true
                      }
                 )

                // Spacer for consistent spacing
                Spacer(modifier = Modifier.width(24.dp))

                 // Book icon - opens word picker
                 Icon(
                     imageVector = Icons.AutoMirrored.Filled.MenuBook,
                     contentDescription = "Select starting word",
                     tint = fontColor.copy(alpha = 0.7f),
                     modifier = Modifier
                         .size(20.dp)
                          .noRippleClickable {
                              if (isPlaying) {
                                  Log.d("SPEED_READER", "Book icon tap pause: currentWordIndex=$currentWordIndex, totalWords=$totalWords")
                                  onPlayPause()
                                  // Save progress immediately when manually pausing
                                  val globalWordIndex = startingWordIndex + currentWordIndex
                                  val newProgress = (globalWordIndex.toFloat() / totalWords).coerceIn(0f, 1f)
                                  Log.d("SPEED_READER", "Book icon saving: globalWordIndex=$globalWordIndex, newProgress=$newProgress")
                                  onSaveProgress(newProgress, globalWordIndex)
                              }
                              onShowWordPicker()
                          }
                 )
            }
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
            SpeedReadingWpmMenuSheet(
                show = showQuickWpmMenu,
                onDismiss = { showQuickWpmMenu = false },
                currentWpm = wpm,
                onWpmChange = onWpmChange
            )
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

    // Rule: 1-letter words - accent the entire word
    if (len == 1) {
        return 0
    }

    // Rule: 2-3 letter words - always 2nd character
    if (len <= 3) {
        return 1
    }

    // Rule: 4-6 letter words (medium) - scan after first letter for first vowel
    if (len < 7) {
        for (i in 1..<len) {
            if (cleanWord[i] in vowels) {
                return i
            }
        }
        // Fallback: 2nd character if no vowel found
        return 1
    }

    // Rule: 7+ letter words (long) - left-biased center scan
    // Calculate center and scan LEFT from center toward the beginning
    val center = len / 2
    // Scan from index 1 up to center (left-bias: find earliest vowel)
    for (i in 1..center) {
        if (cleanWord[i] in vowels) {
            return i
        }
    }

    // Fallback: if no vowel found in left-center region, return center position
    // (prioritizing left-half balance as per guidelines)
    return center.coerceAtLeast(2).coerceAtMost(len - 1)
}