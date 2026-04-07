/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.images.components

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
import us.blindmint.codex.domain.reader.ReaderColorEffects
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

enum class ImageStylePreset {
    HIDDEN,
    DEFAULT,
    ENHANCED,
    CUSTOM
}

@Composable
fun ImageStylePresetOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Track the user's selected preset (not auto-detected from settings)
    val selectedPreset = remember {
        mutableStateOf(
            when {
                !state.value.images -> ImageStylePreset.HIDDEN
                state.value.imagesColorEffects == ReaderColorEffects.OFF &&
                state.value.imagesAlignment == HorizontalAlignment.CENTER &&
                state.value.imagesCornersRoundness == 8 &&
                state.value.imagesWidth == 100.0f -> ImageStylePreset.DEFAULT
                state.value.imagesColorEffects == ReaderColorEffects.GRAYSCALE &&
                state.value.imagesAlignment == HorizontalAlignment.CENTER &&
                state.value.imagesCornersRoundness == 16 &&
                state.value.imagesWidth == 100.0f -> ImageStylePreset.ENHANCED
                else -> ImageStylePreset.CUSTOM
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(title = stringResource(id = R.string.images_style_option), padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            ImageStylePreset.entries.forEach { preset ->
                FilterChip(
                    modifier = Modifier.height(36.dp),
                    selected = preset == selectedPreset.value,
                    label = {
                        StyledText(
                            text = when (preset) {
                                ImageStylePreset.HIDDEN -> stringResource(R.string.images_style_hidden)
                                ImageStylePreset.DEFAULT -> stringResource(R.string.images_style_default)
                                ImageStylePreset.ENHANCED -> stringResource(R.string.images_style_enhanced)
                                ImageStylePreset.CUSTOM -> stringResource(R.string.images_style_custom)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    onClick = {
                        selectedPreset.value = preset
                        when (preset) {
                            ImageStylePreset.HIDDEN -> {
                                mainModel.onEvent(MainEvent.OnChangeImages(false))
                            }
                            ImageStylePreset.DEFAULT -> {
                                mainModel.onEvent(MainEvent.OnChangeImages(true))
                                mainModel.onEvent(MainEvent.OnChangeImagesColorEffects(ReaderColorEffects.OFF.toString()))
                                mainModel.onEvent(MainEvent.OnChangeImagesAlignment(HorizontalAlignment.CENTER.toString()))
                                mainModel.onEvent(MainEvent.OnChangeImagesCornersRoundness(8))
                                mainModel.onEvent(MainEvent.OnChangeImagesWidth(100.0f))
                            }
                            ImageStylePreset.ENHANCED -> {
                                mainModel.onEvent(MainEvent.OnChangeImages(true))
                                mainModel.onEvent(MainEvent.OnChangeImagesColorEffects(ReaderColorEffects.GRAYSCALE.toString()))
                                mainModel.onEvent(MainEvent.OnChangeImagesAlignment(HorizontalAlignment.CENTER.toString()))
                                mainModel.onEvent(MainEvent.OnChangeImagesCornersRoundness(16))
                                mainModel.onEvent(MainEvent.OnChangeImagesWidth(100.0f))
                            }
                            ImageStylePreset.CUSTOM -> {
                                mainModel.onEvent(MainEvent.OnChangeImages(true))
                            }
                        }
                    }
                )
            }
        }
    }

        // Show custom controls only when Custom is selected
        AnimatedVisibility(
            visible = selectedPreset.value == ImageStylePreset.CUSTOM,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                ChipsWithTitle(
                    title = stringResource(id = R.string.images_color_effects_option),
                    chips = ReaderColorEffects.entries.map {
                        ButtonItem(
                            id = it.toString(),
                            title = when (it) {
                                ReaderColorEffects.OFF -> stringResource(R.string.color_effects_off)
                                ReaderColorEffects.GRAYSCALE -> stringResource(R.string.color_effects_grayscale)
                                ReaderColorEffects.FONT -> stringResource(R.string.color_effects_font)
                                ReaderColorEffects.BACKGROUND -> stringResource(R.string.color_effects_background)
                            },
                            textStyle = MaterialTheme.typography.labelLarge,
                            selected = it == state.value.imagesColorEffects
                        )
                    },
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeImagesColorEffects(it.id))
                    }
                )

                SegmentedButtonWithTitle(
                    title = stringResource(id = R.string.images_alignment_option),
                    buttons = HorizontalAlignment.entries.map {
                        ButtonItem(
                            id = it.toString(),
                            title = when (it) {
                                HorizontalAlignment.START -> stringResource(id = R.string.alignment_start)
                                HorizontalAlignment.CENTER -> stringResource(id = R.string.alignment_center)
                                HorizontalAlignment.END -> stringResource(id = R.string.alignment_end)
                            },
                            textStyle = MaterialTheme.typography.labelLarge,
                            selected = it == state.value.imagesAlignment
                        )
                    },
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeImagesAlignment(it.id))
                    }
                )

                ImagesCornersRoundnessOption()
                ImagesWidthOption()
            }
        }
    }
}