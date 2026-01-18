/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance.colors.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ColorPreset
import us.blindmint.codex.domain.util.Selected
import us.blindmint.codex.presentation.core.components.common.AnimatedVisibility
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.ColorPickerWithTitle
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.settings.SettingsEvent
import us.blindmint.codex.ui.settings.SettingsModel
import us.blindmint.codex.ui.theme.FadeTransitionPreservingSpace
import us.blindmint.codex.ui.theme.Transitions

@Composable
fun ColorPresetOption() {
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
            items(
                items = state.value.colorPresets,
                key = { colorPreset -> colorPreset.id }
            ) { colorPreset ->
                ReorderableItem(
                    state = reorderableListState,
                    animateItemModifier = Modifier.animateItem(
                        fadeInSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        fadeOutSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    key = colorPreset.id
                ) {
                    ColorPresetOptionRowItem(
                        colorPreset = colorPreset,
                        colorPresets = state.value.colorPresets,
                        isSelected = remember(
                            state.value.selectedColorPreset?.id,
                            colorPreset.id,
                            state.value.colorPresets.size
                        ) {
                            if (state.value.colorPresets.size > 1) {
                                state.value.selectedColorPreset?.id == colorPreset.id
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
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = androidx.compose.material3.SegmentedButtonDefaults.colors().activeBorderColor,
                            shape = CircleShape
                        )
                        .background(Color.Transparent, CircleShape)
                        .clickable {
                            settingsModel.onEvent(
                                SettingsEvent.OnAddColorPreset(
                                    backgroundColor = defaultBackgroundColor,
                                    fontColor = defaultFontColor
                                )
                            )
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
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
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            state.value.selectedColorPreset?.let { selectedPreset ->
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
                      },
                      onToggleLock = {
                          settingsModel.onEvent(
                              SettingsEvent.OnToggleColorPresetLock(
                                  id = selectedPreset.id
                              )
                          )
                      },
                      onResetToInitial = {
                          settingsModel.onEvent(
                              SettingsEvent.OnResetColorPresetToInitial(
                                  id = selectedPreset.id
                              )
                          )
                      }
                )

                Spacer(modifier = Modifier.height(8.dp))

                  ColorPickerWithTitle(
                      value = selectedPreset.backgroundColor,
                      presetId = selectedPreset.id,
                      title = stringResource(id = R.string.background_color_option),
                      horizontalPadding = 0.dp,
                      isLocked = selectedPreset.isLocked,
                      showRgbInputs = true,
                      opacity = selectedPreset.backgroundColor.alpha,
                      onOpacityChange = { alpha ->
                         val updatedColor = selectedPreset.backgroundColor.copy(alpha = alpha)
                         settingsModel.onEvent(
                             SettingsEvent.OnUpdateColorPresetColor(
                                 id = selectedPreset.id,
                                 backgroundColor = updatedColor,
                                 fontColor = null
                             )
                         )
                      },
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

                 Spacer(modifier = Modifier.height(16.dp))
                 HorizontalDivider()
                 Spacer(modifier = Modifier.height(16.dp))

                    ColorPickerWithTitle(
                        value = selectedPreset.fontColor,
                        presetId = selectedPreset.id,
                        title = stringResource(id = R.string.font_color_option),
                        horizontalPadding = 0.dp,
                        isLocked = selectedPreset.isLocked,
                        showRgbInputs = true,
                        opacity = selectedPreset.fontColor.alpha,
                        onOpacityChange = { alpha ->
                           val updatedColor = selectedPreset.fontColor.copy(alpha = alpha)
                           settingsModel.onEvent(
                               SettingsEvent.OnUpdateColorPresetColor(
                                   id = selectedPreset.id,
                                   backgroundColor = null,
                                   fontColor = updatedColor
                               )
                           )
                       },
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
    val title = remember(colorPreset.id, colorPreset.name) {
        if ((colorPreset.name ?: "").isBlank()) {
            // Use the preset ID for consistent numbering
            context.getString(R.string.color_preset_query, colorPreset.id.toString())
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
    onDelete: () -> Unit,
    onToggleLock: () -> Unit,
    onResetToInitial: () -> Unit
) {
    val showDeleteDialog = remember { mutableStateOf(false) }
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
        Modifier.fillMaxWidth(),
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
        } else {
            // Reset to initial button (top-right)
            IconButton(
                modifier = Modifier.size(24.dp),
                icon = Icons.Default.Restore,
                contentDescription = R.string.reset_color_preset_content_desc,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                onResetToInitial()
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Lock/Unlock button (left of delete)
            IconButton(
                modifier = Modifier.size(24.dp),
                icon = if (selectedColorPreset.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (selectedColorPreset.isLocked)
                    R.string.unlock_color_preset_content_desc
                else
                    R.string.lock_color_preset_content_desc,
                disableOnClick = false,
                color = if (selectedColorPreset.isLocked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                onToggleLock()
            }

            // Delete button (always shown for custom presets, rightmost, but disabled when locked)
            if (canDelete) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    icon = Icons.Default.DeleteOutline,
                    contentDescription = R.string.delete_color_preset_content_desc,
                    disableOnClick = selectedColorPreset.isLocked,
                    color = if (selectedColorPreset.isLocked)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    if (!selectedColorPreset.isLocked) {
                        showDeleteDialog.value = true
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                title = {
                    Text(text = stringResource(id = R.string.delete_color_preset_title))
                },
                text = {
                    Text(text = stringResource(id = R.string.delete_color_preset_message, selectedColorPreset.name ?: "Color Preset"))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog.value = false
                            onDelete()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        }
    }
}