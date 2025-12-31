/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.search.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsSubcategoryTitle(
                title = stringResource(id = R.string.search_highlight_color),
                padding = 0.dp
            )

            // Color preview box
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        RevertibleSlider(
            value = color.red to "",
            initialValue = Color(initialValue).red,
            title = stringResource(id = R.string.red_color),
            onValueChange = { color = color.copy(red = it) }
        )

        RevertibleSlider(
            value = color.green to "",
            initialValue = Color(initialValue).green,
            title = stringResource(id = R.string.green_color),
            onValueChange = { color = color.copy(green = it) }
        )

        RevertibleSlider(
            value = color.blue to "",
            initialValue = Color(initialValue).blue,
            title = stringResource(id = R.string.blue_color),
            onValueChange = { color = color.copy(blue = it) }
        )

        RevertibleSlider(
            value = color.alpha to "",
            initialValue = Color(initialValue).alpha,
            title = stringResource(id = R.string.alpha_opacity),
            onValueChange = { color = color.copy(alpha = it) }
        )
    }
}

@Composable
private fun RevertibleSlider(
    value: Pair<Float, String>,
    initialValue: Float,
    title: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        SliderWithTitle(
            modifier = Modifier.weight(1f),
            value = value,
            title = title,
            toValue = 255,
            onValueChange = { onValueChange(it) },
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp
        )

        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            modifier = Modifier.size(28.dp),
            icon = Icons.Default.History,
            contentDescription = R.string.revert_content_desc,
            disableOnClick = false,
            enabled = initialValue != value.first,
            color = if (initialValue == value.first) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        ) {
            onValueChange(initialValue)
        }
    }
}
