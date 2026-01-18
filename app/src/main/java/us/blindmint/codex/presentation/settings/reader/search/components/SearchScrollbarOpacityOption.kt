/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SearchScrollbarOpacityOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Track the initial value from when this settings screen opened (session default)
    val sessionInitialValue = remember { state.value.searchScrollbarOpacity }
    var opacity by remember { mutableStateOf(state.value.searchScrollbarOpacity) }

    // Update local state when external state changes (but not from our own debounced updates)
    LaunchedEffect(state.value.searchScrollbarOpacity) {
        if (opacity != state.value.searchScrollbarOpacity) {
            opacity = state.value.searchScrollbarOpacity
        }
    }

    // Debounce opacity changes before sending to model
    LaunchedEffect(opacity) {
        snapshotFlow { opacity }
            .debounce(50)
            .collectLatest {
                mainModel.onEvent(MainEvent.OnChangeSearchScrollbarOpacity(it))
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
            // Use second SliderWithTitle overload with Float range 0-1
            // toValue parameter is used for display calculation: (value.first * toValue).roundToInt()
            SliderWithTitle(
                modifier = Modifier.weight(1f),
                value = opacity.toFloat() to "%",
                title = stringResource(id = R.string.search_scrollbar_opacity),
                toValue = 100,
                onValueChange = { opacity = it.toDouble() },
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp
            )

            IconButton(
                modifier = Modifier.size(28.dp),
                icon = Icons.Default.History,
                contentDescription = R.string.revert_content_desc,
                disableOnClick = false,
                enabled = sessionInitialValue != opacity,
                color = if (sessionInitialValue == opacity) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            ) {
                opacity = sessionInitialValue
            }
        }
    }
}