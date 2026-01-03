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
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
    onValueChange: (Color) -> Unit
) {
    val initialValue = rememberSaveable(presetId) { value.value.toString() }
    var color by remember(value) { mutableStateOf(value) }
    var hexValue by remember {
        mutableStateOf(
            String.format("%06X",
                ((value.red * 255).toInt() shl 16) or
                ((value.green * 255).toInt() shl 8) or
                (value.blue * 255).toInt()
            )
        )
    }

    // Update hexValue when color changes
    LaunchedEffect(color) {
        hexValue = String.format("%06X",
            ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt()
        )
    }

    LaunchedEffect(color) {
        snapshotFlow {
            color
        }.debounce(50).collectLatest {
            onValueChange(it)
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
                    hexValue = newHex.uppercase().take(6)
                    if (hexValue.length == 6) {
                        try {
                            val colorValue = newHex.toLong(16) or 0xFF000000L // Add alpha = 255
                            color = Color(colorValue)
                        } catch (e: Exception) {
                            // Invalid hex, keep current value
                        }
                    }
                },
                label = { Text(stringResource(id = R.string.hex_color)) },
                visualTransformation = VisualTransformation { text ->
                    TransformedText(
                        text = androidx.compose.ui.text.AnnotatedString("#" + text.text),
                        offsetMapping = androidx.compose.ui.text.input.OffsetMapping.Identity
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

        RevertibleSlider(
            value = color.red to ((color.red * 255).toInt().toString()),
            initialValue = Color(initialValue.toULong()).red,
            title = stringResource(id = R.string.red_color),
            isLocked = isLocked,
            onValueChange = {
                color = color.copy(red = it)
            }
        )
        RevertibleSlider(
            value = color.green to ((color.green * 255).toInt().toString()),
            initialValue = Color(initialValue.toULong()).green,
            title = stringResource(id = R.string.green_color),
            isLocked = isLocked,
            onValueChange = {
                color = color.copy(green = it)
            }
        )
        RevertibleSlider(
            value = color.blue to ((color.blue * 255).toInt().toString()),
            initialValue = Color(initialValue.toULong()).blue,
            title = stringResource(id = R.string.blue_color),
            isLocked = isLocked,
            onValueChange = {
                color = color.copy(blue = it)
            }
        )
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