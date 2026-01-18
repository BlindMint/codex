/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.util.noRippleClickable
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

@OptIn(FlowPreview::class)
@Composable
fun ColorPickerWithTitle(
    modifier: Modifier = Modifier,
    value: Color,
    presetId: Int,
    title: String,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 8.dp,
    isLocked: Boolean = false,
    showRgbInputs: Boolean = false,
    opacity: Float? = null,
    onValueChange: (Color) -> Unit,
    onOpacityChange: ((Float) -> Unit)? = null
) {
    val initialValue = rememberSaveable(presetId) { value.value.toString() }
    var color by remember(value) { mutableStateOf(value) }
    var hexValue by remember {
        mutableStateOf(
            if (opacity != null) {
                String.format("%08X",
                    ((opacity * 255).toInt() shl 24) or
                    ((value.red * 255).toInt() shl 16) or
                    ((value.green * 255).toInt() shl 8) or
                    (value.blue * 255).toInt()
                )
            } else {
                String.format("%06X",
                    ((value.red * 255).toInt() shl 16) or
                    ((value.green * 255).toInt() shl 8) or
                    (value.blue * 255).toInt()
                )
            }
        )
    }

    // Update hexValue when color changes
    LaunchedEffect(color) {
        hexValue = if (opacity != null) {
            String.format("%08X",
                ((opacity * 255).toInt() shl 24) or
                ((color.red * 255).toInt() shl 16) or
                ((color.green * 255).toInt() shl 8) or
                (color.blue * 255).toInt()
            )
        } else {
            String.format("%06X",
                ((color.red * 255).toInt() shl 16) or
                ((color.green * 255).toInt() shl 8) or
                (color.blue * 255).toInt()
            )
        }
    }

    LaunchedEffect(color) {
        snapshotFlow {
            color
        }.debounce(50).collectLatest {
            onValueChange(it)
        }
    }

    // Update hexValue when opacity parameter changes
    if (opacity != null) {
        LaunchedEffect(opacity) {
            // Update hexValue to include current color RGB + new opacity alpha
            hexValue = String.format("%08X",
                ((opacity * 255).toInt() shl 24) or
                ((color.red * 255).toInt() shl 16) or
                ((color.green * 255).toInt() shl 8) or
                (color.blue * 255).toInt()
            )
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding, horizontal = horizontalPadding)
    ) {
        // HEX Color Input - inline with title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = hexValue,
                onValueChange = { newHex ->
                    // Remove leading "#" if present
                    val cleanedHex = newHex.uppercase().removePrefix("#")
                    val maxDigits = if (opacity != null) 8 else 6
                    val truncatedHex = cleanedHex.take(maxDigits)
                    hexValue = truncatedHex
                    if (truncatedHex.length == maxDigits) {
                        try {
                            val colorValue = truncatedHex.toLong(16)
                            if (opacity != null && maxDigits == 8) {
                                // 8-digit hex: extract alpha and RGB
                                val alpha = ((colorValue shr 24) and 0xFF).toInt() / 255f
                                val rgb = colorValue and 0x00FFFFFFL
                                color = Color(rgb or 0xFF000000L) // RGB with full alpha for color
                                // Update opacity
                                onOpacityChange?.invoke(alpha)
                            } else {
                                // 6-digit hex: RGB only, keep current opacity
                                color = Color(colorValue or 0xFF000000L)
                            }
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
                        color = Color.Transparent, // Invisible "#" on the right for visual balance
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f),
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Red slider with optional input
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithTitle(
                modifier = Modifier.weight(1f),
                value = color.red to "",
                title = stringResource(id = R.string.red_color),
                toValue = 255,
                enabled = !isLocked,
                horizontalPadding = 0.dp,
                onValueChange = {
                    color = color.copy(red = it)
                }
            )
            if (showRgbInputs) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = (color.red * 255).toInt().toString(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull() ?: (color.red * 255).toInt()
                        val coercedValue = intValue.coerceIn(0, 255)
                        color = color.copy(red = coercedValue / 255f)
                    },
                    label = { androidx.compose.material3.Text("R") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    enabled = !isLocked,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
            }
        }

        // Green slider with optional input
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithTitle(
                modifier = Modifier.weight(1f),
                value = color.green to "",
                title = stringResource(id = R.string.green_color),
                toValue = 255,
                enabled = !isLocked,
                horizontalPadding = 0.dp,
                onValueChange = {
                    color = color.copy(green = it)
                }
            )
            if (showRgbInputs) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = (color.green * 255).toInt().toString(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull() ?: (color.green * 255).toInt()
                        val coercedValue = intValue.coerceIn(0, 255)
                        color = color.copy(green = coercedValue / 255f)
                    },
                    label = { androidx.compose.material3.Text("G") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    enabled = !isLocked,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
            }
        }

        // Blue slider with optional input
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SliderWithTitle(
                modifier = Modifier.weight(1f),
                value = color.blue to "",
                title = stringResource(id = R.string.blue_color),
                toValue = 255,
                enabled = !isLocked,
                horizontalPadding = 0.dp,
                onValueChange = {
                    color = color.copy(blue = it)
                }
            )
            if (showRgbInputs) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = (color.blue * 255).toInt().toString(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toIntOrNull() ?: (color.blue * 255).toInt()
                        val coercedValue = intValue.coerceIn(0, 255)
                        color = color.copy(blue = coercedValue / 255f)
                    },
                    label = { androidx.compose.material3.Text("B") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    enabled = !isLocked,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
            }
        }

        // Opacity slider (if provided)
        opacity?.let { currentOpacity ->
            onOpacityChange?.let { opacityCallback ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SliderWithTitle(
                        modifier = Modifier.weight(1f),
                        value = (currentOpacity * 100).toInt() to "${(currentOpacity * 100).toInt()}%",
                        title = "Opacity",
                        fromValue = 0,
                        toValue = 100,
                        enabled = !isLocked,
                        horizontalPadding = 0.dp,
                        steps = 0,
                        onValueChange = { intValue ->
                            opacityCallback(intValue / 100f)
                        }
                    )
                    if (showRgbInputs) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = (currentOpacity * 100).toInt().toString(),
                            onValueChange = { newValue ->
                                val intValue = newValue.toIntOrNull() ?: (currentOpacity * 100).toInt()
                                val coercedValue = intValue.coerceIn(0, 100)
                                opacityCallback(coercedValue / 100f)
                            },
                            label = { androidx.compose.material3.Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(60.dp),
                            enabled = !isLocked,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevertibleSlider(
    value: Pair<Float, String>,
    initialValue: Float,
    title: String,
    horizontalPadding: Dp = 0.dp,
    verticalPadding: Dp = 8.dp,
    isLocked: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    var editValue by remember(value.second) { mutableStateOf(value.second) }
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(
            horizontal = horizontalPadding,
            vertical = verticalPadding
        )
    ) {
        SliderWithTitle(
            modifier = Modifier.weight(1f),
            value = value,
            title = title,
            toValue = 255,
            enabled = !isLocked,
            onValueChange = {
                onValueChange(it)
            },
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Editable RGB value field
        OutlinedTextField(
            value = editValue,
            onValueChange = { newValue ->
                editValue = newValue.take(3) // Max 3 digits (0-255)
                val intValue = newValue.toIntOrNull()
                if (intValue != null && intValue in 0..255) {
                    onValueChange(intValue / 255f)
                }
            },
            modifier = Modifier
                .width(60.dp)
                .focusRequester(focusRequester)
                .clickable(enabled = !isLocked) {
                    focusRequester.requestFocus()
                },
            enabled = !isLocked,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
            modifier = Modifier.size(28.dp),
            icon = Icons.Default.History,
            contentDescription = R.string.revert_content_desc,
            disableOnClick = false,
            enabled = !isLocked && initialValue != value.first,
            color = if (initialValue == value.first) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        ) {
            onValueChange(initialValue)
            editValue = ((initialValue * 255).toInt()).toString()
        }
    }
}