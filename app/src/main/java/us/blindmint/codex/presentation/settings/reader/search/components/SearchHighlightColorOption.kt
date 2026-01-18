/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@OptIn(FlowPreview::class)
@Composable
fun SearchHighlightColorOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val defaultColor = 0x80FFEB3BL
    val initialValue = rememberSaveable { state.value.searchHighlightColor }
    var color by remember(state.value.searchHighlightColor) {
        mutableStateOf(Color(state.value.searchHighlightColor))
    }

    // State for showing/hiding RGBA sliders
    var showSliders by remember { mutableStateOf(false) }

    // Hex value state
    var hexValue by remember {
        mutableStateOf(
            String.format("%08X",
                ((color.alpha * 255).toInt() shl 24) or
                ((color.red * 255).toInt() shl 16) or
                ((color.green * 255).toInt() shl 8) or
                (color.blue * 255).toInt()
            )
        )
    }

    // Update hexValue when color changes
    LaunchedEffect(color) {
        hexValue = String.format("%08X",
            ((color.alpha * 255).toInt() shl 24) or
            ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt()
        )
    }

    LaunchedEffect(color) {
        snapshotFlow { color }
            .debounce(50)
            .collectLatest {
                val argb = (
                    ((it.alpha * 255).toLong() shl 24) or
                    ((it.red * 255).toLong() shl 16) or
                    ((it.green * 255).toLong() shl 8) or
                    (it.blue * 255).toLong()
                )
                mainModel.onEvent(MainEvent.OnChangeSearchHighlightColor(argb))
            }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 18.dp)
    ) {
        // Row 1: Title on left, Hex input on right (matching ColorPickerWithTitle layout)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.search_highlight_color),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = hexValue,
                onValueChange = { newHex ->
                    val cleanedHex = newHex.uppercase().removePrefix("#").take(8)
                    hexValue = cleanedHex
                    if (cleanedHex.length == 8) {
                        try {
                            val colorValue = cleanedHex.toLong(16)
                            color = Color(colorValue)
                        } catch (e: Exception) {
                            // Invalid hex, keep current value
                        }
                    }
                },
                label = { Text(stringResource(id = R.string.hex_color)) },
                prefix = {
                    Text(
                        text = "#",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                suffix = {
                    Text(
                        text = "#",
                        color = Color.Transparent,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Tappable area with RGBA text, color preview, and reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSliders = !showSliders }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RGBA values text (now first, with larger font)
            Text(
                text = "RGBA( ${(color.red * 255).toInt()} | ${(color.green * 255).toInt()} | ${(color.blue * 255).toInt()} | ${(color.alpha * 100).toInt()} )",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )

            // Color preview box (moved to middle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color, RoundedCornerShape(8.dp))
                )
            }

            // Reset button (stays on right)
            IconButton(
                modifier = Modifier.size(32.dp),
                icon = Icons.Default.History,
                contentDescription = R.string.revert_content_desc,
                disableOnClick = false,
                enabled = state.value.searchHighlightColor != defaultColor
            ) {
                color = Color(defaultColor)
            }
        }

        // Collapsible RGBA sliders
        AnimatedVisibility(
            visible = showSliders,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Red slider with manual input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SliderWithTitle(
                        modifier = Modifier.weight(1f),
                        value = color.red to "",
                        title = stringResource(id = R.string.red_color),
                        toValue = 255,
                        horizontalPadding = 0.dp,
                        onValueChange = { color = color.copy(red = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = (color.red * 255).toInt().toString(),
                        onValueChange = { newValue ->
                            val intValue = newValue.toIntOrNull() ?: (color.red * 255).toInt()
                            val coercedValue = intValue.coerceIn(0, 255)
                            color = color.copy(red = coercedValue / 255f)
                        },

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(60.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Green slider with manual input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SliderWithTitle(
                        modifier = Modifier.weight(1f),
                        value = color.green to "",
                        title = stringResource(id = R.string.green_color),
                        toValue = 255,
                        horizontalPadding = 0.dp,
                        onValueChange = { color = color.copy(green = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = (color.green * 255).toInt().toString(),
                        onValueChange = { newValue ->
                            val intValue = newValue.toIntOrNull() ?: (color.green * 255).toInt()
                            val coercedValue = intValue.coerceIn(0, 255)
                            color = color.copy(green = coercedValue / 255f)
                        },

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(60.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Blue slider with manual input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SliderWithTitle(
                        modifier = Modifier.weight(1f),
                        value = color.blue to "",
                        title = stringResource(id = R.string.blue_color),
                        toValue = 255,
                        horizontalPadding = 0.dp,
                        onValueChange = { color = color.copy(blue = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = (color.blue * 255).toInt().toString(),
                        onValueChange = { newValue ->
                            val intValue = newValue.toIntOrNull() ?: (color.blue * 255).toInt()
                            val coercedValue = intValue.coerceIn(0, 255)
                            color = color.copy(blue = coercedValue / 255f)
                        },

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(60.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Alpha slider with manual input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SliderWithTitle(
                        modifier = Modifier.weight(1f),
                        value = (color.alpha * 100).toInt() to "${(color.alpha * 100).toInt()}%",
                        title = stringResource(id = R.string.alpha_opacity),
                        fromValue = 0,
                        toValue = 100,
                        horizontalPadding = 0.dp,
                        onValueChange = { intValue ->
                            color = color.copy(alpha = intValue / 100f)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = (color.alpha * 100).toInt().toString(),
                        onValueChange = { newValue ->
                            val intValue = newValue.toIntOrNull() ?: (color.alpha * 100).toInt()
                            val coercedValue = intValue.coerceIn(0, 100)
                            color = color.copy(alpha = coercedValue / 100f)
                        },

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(60.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }
            }
        }
    }
}


