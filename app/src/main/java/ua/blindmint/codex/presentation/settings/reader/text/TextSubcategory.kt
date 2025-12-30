/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.blindmint.codex.presentation.settings.reader.text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.reader.ReaderTextAlignment
import ua.blindmint.codex.presentation.settings.components.SettingsSubcategory
import ua.blindmint.codex.presentation.settings.reader.text.components.LineHeightOption
import ua.blindmint.codex.presentation.settings.reader.text.components.ParagraphHeightOption
import ua.blindmint.codex.presentation.settings.reader.text.components.ParagraphIndentationOption
import ua.blindmint.codex.presentation.settings.reader.text.components.TextAlignmentOption
import ua.blindmint.codex.ui.main.MainModel

fun LazyListScope.TextSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.text_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item {
            TextAlignmentOption()
        }

        item {
            TextFormattingOptions()
        }
    }
}

@Composable
private fun TextFormattingOptions() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    val isOriginal = state.value.textAlignment == ReaderTextAlignment.ORIGINAL

    AnimatedVisibility(
        visible = !isOriginal,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        androidx.compose.foundation.layout.Column {
            LineHeightOption()
            ParagraphHeightOption()
            ParagraphIndentationOption()
        }
    }
}