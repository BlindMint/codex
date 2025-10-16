/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.blindmint.codex.presentation.settings.library.sort.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.library.sort.LibrarySortOrder
import ua.blindmint.codex.presentation.core.components.common.StyledText
import ua.blindmint.codex.ui.main.MainEvent
import ua.blindmint.codex.ui.main.MainModel

fun LazyListScope.LibrarySortOption() {
    items(LibrarySortOrder.entries, key = { it.name }) {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        LibrarySortOptionItem(
            item = it,
            isSelected = state.value.librarySortOrder == it,
            isDescending = state.value.librarySortOrderDescending
        ) {
            if (state.value.librarySortOrder == it) {
                mainModel.onEvent(
                    MainEvent.OnChangeLibrarySortOrderDescending(
                        !state.value.librarySortOrderDescending
                    )
                )
            } else {
                mainModel.onEvent(MainEvent.OnChangeLibrarySortOrderDescending(true))
                mainModel.onEvent(MainEvent.OnChangeLibrarySortOrder(it.name))
            }
        }
    }
}

@Composable
private fun LibrarySortOptionItem(
    item: LibrarySortOrder,
    isSelected: Boolean,
    isDescending: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDescending) Icons.Default.ArrowDownward
            else Icons.Default.ArrowUpward,
            contentDescription = stringResource(id = R.string.sort_order_content_desc),
            modifier = Modifier
                .size(28.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.secondary
            else Color.Transparent
        )
        Spacer(modifier = Modifier.width(24.dp))

        StyledText(
            text = stringResource(
                when (item) {
                    LibrarySortOrder.NAME -> R.string.library_sort_order_name
                    LibrarySortOrder.LAST_READ -> R.string.library_sort_order_last_read
                    LibrarySortOrder.PROGRESS -> R.string.library_sort_order_progress
                    LibrarySortOrder.AUTHOR -> R.string.library_sort_order_author
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
    }
}