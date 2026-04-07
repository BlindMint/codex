/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.comic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

// Reading direction options
enum class ComicReadingDirection(val displayName: Int) {
    LTR(R.string.reading_direction_ltr),
    RTL(R.string.reading_direction_rtl),
    VERTICAL(R.string.reading_direction_vertical)
}

// Reader mode options
enum class ComicReaderModeType(val displayName: Int) {
    PAGED(R.string.reader_mode_paged),
    WEBTOON(R.string.reader_mode_webtoon)
}

// Tap zone inversion options
enum class ComicTapInversion(val displayName: Int) {
    NONE(R.string.tap_invert_none),
    HORIZONTAL(R.string.tap_invert_horizontal),
    VERTICAL(R.string.tap_invert_vertical),
    BOTH(R.string.tap_invert_both)
}

// Image scale type options
enum class ComicImageScale(val displayName: Int, val value: Int) {
    FIT_SCREEN(R.string.scale_fit_screen, 1),
    STRETCH(R.string.scale_stretch, 2),
    FIT_WIDTH(R.string.scale_fit_width, 3),
    FIT_HEIGHT(R.string.scale_fit_height, 4),
    ORIGINAL(R.string.scale_original, 5),
    SMART_FIT(R.string.scale_smart_fit, 6)
}

// Background color options for comics
enum class ComicBackgroundColor(val displayName: Int, val value: String) {
    DEFAULT(R.string.background_color_default, "DEFAULT"),
    GRAY(R.string.background_color_gray, "GRAY"),
    WHITE(R.string.background_color_white, "WHITE"),
    BLACK(R.string.background_color_black, "BLACK"),
    CUSTOM(R.string.background_color_custom, "CUSTOM")
}

@Composable
fun ComicReadingDirectionOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.reading_direction),
        chips = ComicReadingDirection.entries.map { direction ->
            ButtonItem(
                id = direction.name,
                title = stringResource(direction.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = direction.name == state.value.comicReadingDirection
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicReadingDirection(item.id))
        }
    )
}

@Composable
fun ComicReaderModeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.reader_mode),
        chips = ComicReaderModeType.entries.map { mode ->
            ButtonItem(
                id = mode.name,
                title = stringResource(mode.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = mode.name == state.value.comicReaderMode
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicReaderMode(item.id))
        }
    )
}

@Composable
fun ComicTapZoneOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Simple Enable/Disable for tap navigation
    // Enabled = zone 0 (default), Disabled = zone 5
    val isEnabled = state.value.comicTapZone != 5

    val tapZoneOptions = listOf(
        ButtonItem(
            id = "enabled",
            title = stringResource(R.string.enabled),
            textStyle = MaterialTheme.typography.labelLarge,
            selected = isEnabled
        ),
        ButtonItem(
            id = "disabled",
            title = stringResource(R.string.disabled),
            textStyle = MaterialTheme.typography.labelLarge,
            selected = !isEnabled
        )
    )

    ChipsWithTitle(
        title = stringResource(R.string.tap_zones),
        chips = tapZoneOptions,
        onClick = { item ->
            // Set tap zone to 0 (default with all zones) or 5 (disabled)
            val tapZone = if (item.id == "enabled") 0 else 5
            mainModel.onEvent(MainEvent.OnChangeComicTapZone(tapZone))
        }
    )
}

@Composable
fun ComicInvertTapsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.invert_taps),
        chips = ComicTapInversion.entries.map { invert ->
            ButtonItem(
                id = invert.name,
                title = stringResource(invert.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = invert.name == state.value.comicInvertTaps
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicInvertTaps(item.id))
        }
    )
}

@Composable
fun ComicImageScaleOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.image_scale),
        chips = ComicImageScale.entries.map { scale ->
            ButtonItem(
                id = scale.value.toString(),
                title = stringResource(scale.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = scale.value == state.value.comicScaleType
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicScaleType(item.id.toIntOrNull() ?: 1))
        }
    )
}

@Composable
fun ComicBackgroundColorOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    var showCustomColorDialog by remember { mutableStateOf(false) }

    ChipsWithTitle(
        title = stringResource(R.string.background_color),
        chips = ComicBackgroundColor.entries.map { bgColor ->
            ButtonItem(
                id = bgColor.value,
                title = stringResource(bgColor.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = bgColor.value == state.value.comicBackgroundColor
            )
        },
        onClick = { item ->
            if (item.id == "CUSTOM") {
                showCustomColorDialog = true
            } else {
                mainModel.onEvent(MainEvent.OnChangeComicBackgroundColor(item.id))
            }
        }
    )

    if (showCustomColorDialog) {
        CustomBackgroundColorDialog(
            currentColor = try {
                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${state.value.comicCustomBackgroundColor}"))
            } catch (_: Exception) {
                androidx.compose.ui.graphics.Color.White
            },
            onColorSelected = { color ->
                val hexColor = String.format("%06X",
                    ((color.red * 255).toInt() shl 16) or
                    ((color.green * 255).toInt() shl 8) or
                    (color.blue * 255).toInt()
                )
                mainModel.onEvent(MainEvent.OnChangeComicCustomBackgroundColor(hexColor))
                mainModel.onEvent(MainEvent.OnChangeComicBackgroundColor("CUSTOM"))
                showCustomColorDialog = false
            },
            onDismiss = { showCustomColorDialog = false }
        )
    }
}

@Composable
fun ComicVolumeKeysOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        title = stringResource(R.string.volume_keys),
        description = stringResource(R.string.volume_keys_description),
        selected = state.value.comicVolumeKeysEnabled,
        onClick = {
            mainModel.onEvent(MainEvent.OnChangeComicVolumeKeysEnabled(!state.value.comicVolumeKeysEnabled))
        }
    )
}

@Composable
fun ComicVolumeKeysInvertedOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        title = stringResource(R.string.volume_keys_inverted),
        description = stringResource(R.string.volume_keys_inverted_description),
        selected = state.value.comicVolumeKeysInverted,
        onClick = {
            mainModel.onEvent(MainEvent.OnChangeComicVolumeKeysInverted(!state.value.comicVolumeKeysInverted))
        }
    )
}

@Composable
fun CustomBackgroundColorDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var color by remember { mutableStateOf(currentColor) }
    var hexValue by remember {
        mutableStateOf(
            String.format("%06X",
                ((color.red * 255).toInt() shl 16) or
                ((color.green * 255).toInt() shl 8) or
                (color.blue * 255).toInt()
            )
        )
    }

    LaunchedEffect(color) {
        hexValue = String.format("%06X",
            ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_color_picker_title)) },
        text = {
            Column {
                // Color preview box at the top right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HEX input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hexValue,
                        onValueChange = { newHex ->
                            val cleanedHex = newHex.uppercase().removePrefix("#").take(6)
                            hexValue = cleanedHex
                            if (cleanedHex.length == 6) {
                                try {
                                    val colorValue = cleanedHex.toLong(16)
                                    color = Color(colorValue or 0xFF000000L)
                                } catch (_: Exception) {
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.hex_color)) },
                        prefix = { Text("#") },
                        modifier = Modifier.width(120.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Red slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.red_color),
                        modifier = Modifier.width(48.dp)
                    )
                    Slider(
                        value = color.red,
                        onValueChange = { color = color.copy(red = it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Green slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.green),
                        modifier = Modifier.width(48.dp)
                    )
                    Slider(
                        value = color.green,
                        onValueChange = { color = color.copy(green = it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Blue slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.blue),
                        modifier = Modifier.width(48.dp)
                    )
                    Slider(
                        value = color.blue,
                        onValueChange = { color = color.copy(blue = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(color) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

