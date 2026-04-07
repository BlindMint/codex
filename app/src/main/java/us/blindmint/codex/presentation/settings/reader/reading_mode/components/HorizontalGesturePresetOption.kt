/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.reading_mode.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

enum class HorizontalGesturePreset {
    OFF,
    BASIC,
    ADVANCED
}

@Composable
fun HorizontalGesturePresetOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Track the user's selected preset (not auto-detected from settings)
    val selectedPreset = remember {
        mutableStateOf(
            when {
                state.value.horizontalGesture == ReaderHorizontalGesture.OFF -> HorizontalGesturePreset.OFF
                state.value.horizontalGestureScroll == 50.0f &&
                state.value.horizontalGestureSensitivity == 50.0f &&
                state.value.horizontalGesturePullAnim &&
                state.value.horizontalGestureAlphaAnim -> HorizontalGesturePreset.BASIC
                else -> HorizontalGesturePreset.ADVANCED
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(title = stringResource(id = R.string.horizontal_gesture_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            HorizontalGesturePreset.entries.forEach { preset ->
                FilterChip(
                    modifier = Modifier.height(36.dp),
                    selected = preset == selectedPreset.value,
                    label = {
                        StyledText(
                            text = when (preset) {
                                HorizontalGesturePreset.OFF -> stringResource(R.string.horizontal_gesture_off)
                                HorizontalGesturePreset.BASIC -> stringResource(R.string.horizontal_gesture_basic)
                                HorizontalGesturePreset.ADVANCED -> stringResource(R.string.horizontal_gesture_advanced)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    onClick = {
                        selectedPreset.value = preset
                        when (preset) {
                            HorizontalGesturePreset.OFF -> {
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGesture(ReaderHorizontalGesture.OFF.toString()))
                            }
                            HorizontalGesturePreset.BASIC -> {
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGesture(ReaderHorizontalGesture.ON.toString()))
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGestureScroll(50.0f))
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGestureSensitivity(50.0f))
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGesturePullAnim(true))
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGestureAlphaAnim(true))
                            }
                            HorizontalGesturePreset.ADVANCED -> {
                                mainModel.onEvent(MainEvent.OnChangeHorizontalGesture(ReaderHorizontalGesture.ON.toString()))
                            }
                        }
                    }
                )
            }
        }
    }

        // Show advanced controls only when Advanced is selected
        AnimatedVisibility(
            visible = selectedPreset.value == HorizontalGesturePreset.ADVANCED,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                SliderWithTitle(
                    value = state.value.horizontalGestureScroll to "%",
                    toValue = 100,
                    title = stringResource(id = R.string.horizontal_gesture_scroll_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeHorizontalGestureScroll(it))
                    }
                )

                SliderWithTitle(
                    value = state.value.horizontalGestureSensitivity to "%",
                    toValue = 100,
                    title = stringResource(id = R.string.horizontal_gesture_sensitivity_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeHorizontalGestureSensitivity(it))
                    }
                )

                SwitchWithTitle(
                    selected = state.value.horizontalGesturePullAnim,
                    title = stringResource(id = R.string.horizontal_gesture_pull_anim_option),
                    description = stringResource(id = R.string.horizontal_gesture_pull_anim_option_desc),
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeHorizontalGesturePullAnim(!state.value.horizontalGesturePullAnim))
                    }
                )

                SwitchWithTitle(
                    selected = state.value.horizontalGestureAlphaAnim,
                    title = stringResource(id = R.string.horizontal_gesture_alpha_anim_option),
                    description = stringResource(id = R.string.horizontal_gesture_alpha_anim_option_desc),
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeHorizontalGestureAlphaAnim(!state.value.horizontalGestureAlphaAnim))
                    }
                )
            }
        }
    }
}