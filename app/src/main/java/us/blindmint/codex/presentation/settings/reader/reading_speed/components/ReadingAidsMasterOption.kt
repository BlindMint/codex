/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.reading_speed.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ReadingAidsMasterOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Check if any reading aids are currently enabled
    val anyAidEnabled = state.value.highlightedReading || state.value.perceptionExpander

    Column {
        SwitchWithTitle(
            selected = anyAidEnabled,
            title = stringResource(id = R.string.reading_aids_option),
            description = stringResource(id = R.string.reading_aids_option_desc),
            onClick = {
                val newState = !anyAidEnabled
                mainModel.onEvent(MainEvent.OnChangeHighlightedReading(newState))
                mainModel.onEvent(MainEvent.OnChangePerceptionExpander(newState))
            }
        )

        // Show detailed controls when aids are enabled
        AnimatedVisibility(
            visible = anyAidEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                // Individual toggles for each aid
                SwitchWithTitle(
                    selected = state.value.highlightedReading,
                    title = stringResource(id = R.string.highlighted_reading_option),
                    description = stringResource(id = R.string.highlighted_reading_option_desc),
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeHighlightedReading(!state.value.highlightedReading))
                    }
                )

                if (state.value.highlightedReading) {
                    HighlightedReadingThicknessOption()
                }

                SwitchWithTitle(
                    selected = state.value.perceptionExpander,
                    title = stringResource(id = R.string.perception_expander_option),
                    description = stringResource(id = R.string.perception_expander_option_desc),
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangePerceptionExpander(!state.value.perceptionExpander))
                    }
                )

                if (state.value.perceptionExpander) {
                    PerceptionExpanderPaddingOption()
                    PerceptionExpanderThicknessOption()
                }
            }
        }
    }
}