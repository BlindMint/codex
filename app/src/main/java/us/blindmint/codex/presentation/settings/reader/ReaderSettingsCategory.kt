/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.reader.chapters.components.ChapterTitleAlignmentOption
import us.blindmint.codex.presentation.settings.reader.dictionary.components.DictionarySourceOption
import us.blindmint.codex.presentation.settings.reader.dictionary.components.DoubleTapDictionaryOption
import us.blindmint.codex.presentation.settings.reader.dictionary.components.OpenLookupsInAppOption
import us.blindmint.codex.presentation.settings.reader.font.components.CustomFontsOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontFamilyOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontSizeOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontStyleOption
import us.blindmint.codex.presentation.settings.reader.font.components.FontThicknessOption
import us.blindmint.codex.presentation.settings.reader.font.components.LetterSpacingOption
import us.blindmint.codex.presentation.settings.reader.images.components.ImagesAlignmentOption
import us.blindmint.codex.presentation.settings.reader.images.components.ImagesOption
import us.blindmint.codex.presentation.settings.reader.misc.components.FullscreenOption
import us.blindmint.codex.presentation.settings.reader.misc.components.HideBarsOnFastScrollOption
import us.blindmint.codex.presentation.settings.reader.misc.components.KeepScreenOnOption
import us.blindmint.codex.presentation.settings.reader.padding.components.BottomBarPaddingOption
import us.blindmint.codex.presentation.settings.reader.padding.components.CutoutPaddingOption
import us.blindmint.codex.presentation.settings.reader.padding.components.SidePaddingOption
import us.blindmint.codex.presentation.settings.reader.padding.components.VerticalPaddingOption
import us.blindmint.codex.presentation.settings.reader.progress.components.ProgressBarAlignmentOption
import us.blindmint.codex.presentation.settings.reader.progress.components.ProgressBarFontSizeOption
import us.blindmint.codex.presentation.settings.reader.progress.components.ProgressBarOption
import us.blindmint.codex.presentation.settings.reader.progress.components.ProgressBarPaddingOption
import us.blindmint.codex.presentation.settings.reader.progress.components.ProgressCountOption
import us.blindmint.codex.presentation.settings.reader.reading_mode.components.HorizontalGestureAlphaAnimOption
import us.blindmint.codex.presentation.settings.reader.reading_mode.components.HorizontalGestureOption
import us.blindmint.codex.presentation.settings.reader.reading_mode.components.HorizontalGesturePullAnimOption
import us.blindmint.codex.presentation.settings.reader.reading_mode.components.HorizontalGestureScrollOption
import us.blindmint.codex.presentation.settings.reader.reading_mode.components.HorizontalGestureSensitivityOption
import us.blindmint.codex.presentation.settings.reader.reading_speed.components.HighlightedReadingOption
import us.blindmint.codex.presentation.settings.reader.reading_speed.components.HighlightedReadingThicknessOption
import us.blindmint.codex.presentation.settings.reader.reading_speed.components.PerceptionExpanderOption
import us.blindmint.codex.presentation.settings.reader.reading_speed.components.PerceptionExpanderPaddingOption
import us.blindmint.codex.presentation.settings.reader.reading_speed.components.PerceptionExpanderThicknessOption
import us.blindmint.codex.presentation.settings.reader.search.components.SearchHighlightColorOption
import us.blindmint.codex.presentation.settings.reader.search.components.SearchScrollbarOpacityOption
import us.blindmint.codex.presentation.settings.reader.system.components.CustomScreenBrightnessOption
import us.blindmint.codex.presentation.settings.reader.system.components.ScreenBrightnessOption
import us.blindmint.codex.presentation.settings.reader.system.components.ScreenOrientationOption
import us.blindmint.codex.presentation.settings.reader.text.components.LineHeightOption
import us.blindmint.codex.presentation.settings.reader.text.components.ParagraphHeightOption
import us.blindmint.codex.presentation.settings.reader.text.components.ParagraphIndentationOption
import us.blindmint.codex.presentation.settings.reader.text.components.TextAlignmentOption

fun LazyListScope.ReaderSettingsCategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    // Font section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FontFamilyOption()
                CustomFontsOption()
                FontThicknessOption()
                FontStyleOption()
                FontSizeOption()
                LetterSpacingOption()
            }
        }
    }

    // Text section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextAlignmentOption()
                LineHeightOption()
                ParagraphHeightOption()
                ParagraphIndentationOption()
            }
        }
    }

    // Images section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.images_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ImagesOption()
                ImagesAlignmentOption()
            }
        }
    }

    // Chapters section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.chapters_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ChapterTitleAlignmentOption()
            }
        }
    }

    // Reading Mode section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reading_mode_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalGestureOption()
                HorizontalGestureSensitivityOption()
                HorizontalGestureScrollOption()
                HorizontalGesturePullAnimOption()
                HorizontalGestureAlphaAnimOption()
            }
        }
    }

    // Padding section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.padding_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                VerticalPaddingOption()
                SidePaddingOption()
                BottomBarPaddingOption()
                CutoutPaddingOption()
            }
        }
    }

    // System section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.system_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ScreenOrientationOption()
                ScreenBrightnessOption()
                CustomScreenBrightnessOption()
            }
        }
    }

    // Reading Speed section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reading_speed_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PerceptionExpanderOption()
                PerceptionExpanderThicknessOption()
                PerceptionExpanderPaddingOption()
                HighlightedReadingOption()
                HighlightedReadingThicknessOption()
            }
        }
    }

    // Progress section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ProgressBarOption()
                ProgressCountOption()
                ProgressBarAlignmentOption()
                ProgressBarFontSizeOption()
                ProgressBarPaddingOption()
            }
        }
    }

    // Search section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SearchHighlightColorOption()
                SearchScrollbarOpacityOption()
            }
        }
    }

    // Dictionary section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dictionary_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OpenLookupsInAppOption()
                DoubleTapDictionaryOption()
                DictionarySourceOption()
            }
        }
    }

    // Misc section - grouped in a card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.misc_reader_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FullscreenOption()
                KeepScreenOnOption()
                HideBarsOnFastScrollOption()
            }
        }
    }
}