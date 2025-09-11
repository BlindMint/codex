/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.settings.appearance.colors.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.reader.ColorPreset
import ua.blindmint.codex.domain.util.Selected
import ua.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import ua.blindmint.codex.presentation.core.components.common.IconButton
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.presentation.core.components.settings.ColorPickerWithTitle
import ua.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import ua.blindmint.codex.ui.settings.SettingsEvent
import ua.blindmint.codex.ui.settings.SettingsModel
import ua.blindmint.codex.ui.theme.FadeTransitionPreservingSpace
import ua.blindmint.codex.ui.theme.Transitions

@Composable
fun ColorPresetOption(backgroundColor: Color) {
    val settingsModel = hiltViewModel<SettingsModel>()
    val state = settingsModel.state.collectAsStateWithLifecycle()

    // Auto-selection is now handled in MainActivity when the app starts

    val reorderableListState = rememberReorderableLazyListState(
        lazyListState = state.value.colorPresetListState
    ) { from, to ->
        settingsModel.onEvent(
            SettingsEvent.OnReorderColorPresets(
                from = from.index,
                to = to.index
            )
        )
    }
    val defaultBackgroundColor = MaterialTheme.colorScheme.surface
    val defaultFontColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.color_preset_option),
            padding = 18.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = state.value.colorPresetListState,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            itemsIndexed(
                state.value.colorPresets,
                key = { index, colorPreset -> "${colorPreset.id}_${colorPreset.name}_${index}" }
            ) { index, colorPreset ->
                ReorderableItem(
                    state = reorderableListState,
                    animateItemModifier = Modifier,
                    key = "${colorPreset.id}_${colorPreset.name}_${index}"
                ) {
                    ColorPresetOptionRowItem(
                        colorPreset = colorPreset,
                        colorPresets = state.value.colorPresets,
                        isSelected = remember(
                            colorPreset.isSelected,
                            state.value.colorPresets.size
                        ) {
                            if (state.value.colorPresets.size > 1) {
                                colorPreset.isSelected
                            } else true
                        },
                        enableAnimation = state.value.animateColorPreset,
                        onClick = {
                            settingsModel.onEvent(
                                SettingsEvent.OnSelectColorPreset(
                                    id = colorPreset.id
                                )
                            )
                        }
                    )
                }
            }
            item {
                androidx.compose.material3.IconButton(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = androidx.compose.material3.SegmentedButtonDefaults.colors().activeBorderColor,
                            shape = CircleShape
                        )
                        .background(Color.Transparent, CircleShape)
                        .padding(horizontal = 12.dp),
                    onClick = {
                        settingsModel.onEvent(
                            SettingsEvent.OnAddColorPreset(
                                backgroundColor = defaultBackgroundColor,
                                fontColor = defaultFontColor
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.create_color_preset_content_desc),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            val selectedPreset = state.value.selectedColorPreset
            if (selectedPreset != null) {
                ColorPresetOptionConfigurationItem(
                    selectedColorPreset = selectedPreset,
                    canEditName = selectedPreset.name != "Light" && selectedPreset.name != "Dark",
                    canDelete = state.value.colorPresets.size > 1 && selectedPreset.name != "Light" && selectedPreset.name != "Dark",
                    onTitleChange = {
                        settingsModel.onEvent(
                            SettingsEvent.OnUpdateColorPresetTitle(
                                id = selectedPreset.id,
                                title = it
                            )
                        )
                    },
                    onRestore = {
                        settingsModel.onEvent(
                            SettingsEvent.OnRestoreDefaultColorPreset(
                                id = selectedPreset.id
                            )
                        )
                    },
                    onDelete = {
                        settingsModel.onEvent(
                            SettingsEvent.OnDeleteColorPreset(
                                id = selectedPreset.id
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedPreset != null) {
                ColorPickerWithTitle(
                    value = selectedPreset.backgroundColor,
                    presetId = selectedPreset.id,
                    title = stringResource(id = R.string.background_color_option),
                    onValueChange = {
                        settingsModel.onEvent(
                            SettingsEvent.OnUpdateColorPresetColor(
                                id = selectedPreset.id,
                                backgroundColor = it,
                                fontColor = null
                            )
                        )
                    }
                )
                ColorPickerWithTitle(
                    value = selectedPreset.fontColor,
                    presetId = selectedPreset.id,
                    title = stringResource(id = R.string.font_color_option),
                    onValueChange = {
                        settingsModel.onEvent(
                            SettingsEvent.OnUpdateColorPresetColor(
                                id = selectedPreset.id,
                                backgroundColor = null,
                                fontColor = it
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.ColorPresetOptionRowItem(
    colorPreset: ColorPreset,
    colorPresets: List<ColorPreset>,
    isSelected: Selected,
    enableAnimation: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val title = remember(colorPreset, colorPresets) {
        // Find the index of this preset in the current list
        val presetIndex = colorPresets.indexOf(colorPreset)
        val displayNumber = presetIndex + 1 // 1-based numbering

        if ((colorPreset.name ?: "").isBlank()) {
            context.getString(R.string.color_preset_query, displayNumber.toString())
        } else {
            colorPreset.name!!
        }
    }

    val borderColor = remember(isSelected, colorPreset.fontColor, colorPreset.backgroundColor) {
        if (!isSelected) {
            // For better contrast, use a darker border for light presets and lighter border for dark presets
            // Calculate luminance manually: 0.299*R + 0.587*G + 0.114*B
            val r = colorPreset.backgroundColor.red
            val g = colorPreset.backgroundColor.green
            val b = colorPreset.backgroundColor.blue
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b

            if (luminance > 0.5f) {
                // Light preset - use darker border for contrast
                Color(0xFF1C1B1F) // Dark color for light background
            } else {
                // Dark preset - use the font color with some transparency
                colorPreset.fontColor.copy(0.5f)
            }
        } else {
            colorPreset.fontColor
        }
    }
    val animatedBorderColor = animateColorAsState(
        borderColor,
        label = ""
    )

    val animatedBackgroundColor = animateColorAsState(
        colorPreset.backgroundColor,
        label = ""
    )
    val animatedFontColor = animateColorAsState(
        colorPreset.fontColor,
        label = ""
    )

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (enableAnimation) animatedBorderColor.value
                else borderColor,
                shape = CircleShape
            )
            .background(animatedBackgroundColor.value, CircleShape)
            .clickable(enabled = !isSelected) {
                onClick()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = if (enableAnimation) expandHorizontally() + fadeIn()
            else Transitions.NoEnterAnimation,
            exit = if (enableAnimation) shrinkHorizontally() + fadeOut()
            else Transitions.NoExitAnimation
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                tint = colorPreset.fontColor
            )
        }

        StyledText(
            text = title.trim(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = animatedFontColor.value
            ),
            maxLines = 1
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ColorPresetOptionConfigurationItem(
    selectedColorPreset: ColorPreset,
    canEditName: Boolean,
    canDelete: Boolean,
    onTitleChange: (String) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val title = remember(selectedColorPreset.id) {
        mutableStateOf(selectedColorPreset.name ?: "")
    }

    LaunchedEffect(title) {
        snapshotFlow {
            title.value
        }.debounce(50).collectLatest {
            onTitleChange(it)
        }
    }

    val isDefaultPreset = selectedColorPreset.name == "Light" || selectedColorPreset.name == "Dark"

    Row(
        Modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = title.value,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
            textStyle = TextStyle(
                color = if (canEditName) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily
            ),
            onValueChange = {
                if (canEditName && (it.length < 40 || it.length <= title.value.length)) {
                    title.value = it
                }
            },
            keyboardOptions = KeyboardOptions(
                KeyboardCapitalization.Sentences
            ),
            cursorBrush = if (canEditName) SolidColor(MaterialTheme.colorScheme.onSurfaceVariant) else SolidColor(Color.Transparent),
            readOnly = !canEditName,
            enabled = canEditName
        ) { innerText ->
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.value.isEmpty()) {
                    StyledText(
                        text = stringResource(id = R.string.color_preset_placeholder),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
                innerText()
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isDefaultPreset) {
            IconButton(
                modifier = Modifier.size(24.dp),
                icon = Icons.Default.Restore,
                contentDescription = R.string.restore_default_colors_content_desc,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                onRestore()
            }
        } else if (canDelete) {
            IconButton(
                modifier = Modifier.size(24.dp),
                icon = Icons.Default.DeleteOutline,
                contentDescription = R.string.delete_color_preset_content_desc,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                onDelete()
            }
        }
    }
}