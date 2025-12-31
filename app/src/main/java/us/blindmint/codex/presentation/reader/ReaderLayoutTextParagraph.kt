/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import us.blindmint.codex.domain.reader.FontWithName
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.reader.ReaderText.Text
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.ui.reader.ReaderEvent

@Composable
fun LazyItemScope.ReaderLayoutTextParagraph(
    paragraph: Text,
    activity: ComponentActivity,
    showMenu: Boolean,
    fontFamily: FontWithName,
    fontColor: Color,
    lineHeight: TextUnit,
    fontThickness: ReaderFontThickness,
    fontStyle: FontStyle,
    textAlignment: ReaderTextAlignment,
    horizontalAlignment: Alignment.Horizontal,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    sidePadding: Dp,
    paragraphIndentation: TextUnit,
    fullscreenMode: Boolean,
    doubleClickTranslation: Boolean,
    highlightedReading: Boolean,
    highlightedReadingThickness: FontWeight,
    toolbarHidden: Boolean,
    openTranslator: (ReaderEvent.OnOpenTranslator) -> Unit,
    menuVisibility: (ReaderEvent.OnMenuVisibility) -> Unit,
    searchQuery: String,
    searchHighlightColor: Color
) {
    // Apply search highlighting to the text if there's a search query
    val highlightedText = remember(paragraph.line, searchQuery, searchHighlightColor) {
        if (searchQuery.isBlank()) {
            paragraph.line
        } else {
            highlightSearchMatches(paragraph.line, searchQuery, searchHighlightColor)
        }
    }
    Column(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .fillMaxWidth()
            .padding(horizontal = sidePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment
    ) {
        StyledText(
            text = highlightedText,
            modifier = Modifier.then(
                if (doubleClickTranslation && toolbarHidden) {
                    Modifier.noRippleClickable(
                        onDoubleClick = {
                            openTranslator(
                                ReaderEvent.OnOpenTranslator(
                                    textToTranslate = paragraph.line.text,
                                    translateWholeParagraph = true,
                                    activity = activity
                                )
                            )
                        },
                        onClick = {
                            menuVisibility(
                                ReaderEvent.OnMenuVisibility(
                                    show = !showMenu,
                                    fullscreenMode = fullscreenMode,
                                    saveCheckpoint = true,
                                    activity = activity
                                )
                            )
                        }
                    )
                } else Modifier
            ),
            style = TextStyle(
                fontFamily = fontFamily.font,
                fontWeight = fontThickness.thickness,
                textAlign = textAlignment.textAlignment,
                textIndent = TextIndent(firstLine = paragraphIndentation),
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = fontColor,
                lineBreak = LineBreak.Paragraph
            ),
            highlightText = highlightedReading,
            highlightThickness = highlightedReadingThickness
        )
    }
}

/**
 * Highlights all occurrences of the search query in the text with the specified background color.
 */
private fun highlightSearchMatches(
    text: AnnotatedString,
    query: String,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) return text

    val originalText = text.text
    val queryLower = query.lowercase()
    val textLower = originalText.lowercase()

    // Find all match positions
    val matches = mutableListOf<IntRange>()
    var searchIndex = 0
    while (true) {
        val foundIndex = textLower.indexOf(queryLower, searchIndex)
        if (foundIndex == -1) break
        matches.add(foundIndex until (foundIndex + query.length))
        searchIndex = foundIndex + 1
    }

    if (matches.isEmpty()) return text

    return buildAnnotatedString {
        // First, copy all existing spans from the original text
        append(text)

        // Then add background highlight spans for each match
        matches.forEach { range ->
            addStyle(
                style = SpanStyle(background = highlightColor),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}