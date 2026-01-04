/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.ui.reader.ReaderEvent
import us.blindmint.codex.ui.theme.readerBarsColor

@OptIn(ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun ReaderSearchBar(
    searchQuery: String,
    searchResultsCount: Int,
    currentResultIndex: Int,
    onQueryChange: (ReaderEvent.OnSearchQueryChange) -> Unit,
    onNextResult: (ReaderEvent.OnNextSearchResult) -> Unit,
    onPrevResult: (ReaderEvent.OnPrevSearchResult) -> Unit,
    onHideSearch: (ReaderEvent.OnHideSearch) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // Local state for debounced input
    var localQuery by remember(searchQuery) { mutableStateOf(searchQuery) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounce search query changes
    LaunchedEffect(localQuery) {
        snapshotFlow { localQuery }
            .debounce(300) // 300ms delay after user stops typing
            .collectLatest { query ->
                onQueryChange(ReaderEvent.OnSearchQueryChange(query))
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.readerBarsColor)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            icon = Icons.Rounded.ArrowUpward,
            contentDescription = R.string.go_back_content_desc,
            disableOnClick = false
        ) {
            keyboardController?.hide()
            onHideSearch(ReaderEvent.OnHideSearch)
        }

        OutlinedTextField(
            value = localQuery,
            onValueChange = { localQuery = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.search_in_book)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            ),
            trailingIcon = {
                if (localQuery.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchResultsCount > 0) {
                                "${currentResultIndex + 1}/$searchResultsCount"
                            } else if (localQuery.isNotBlank()) {
                                "0/0"
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = R.string.clear_search_content_desc,
                            disableOnClick = false
                        ) {
                            localQuery = ""
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.medium
        )

        IconButton(
            icon = Icons.Rounded.KeyboardArrowUp,
            contentDescription = R.string.previous_result_content_desc,
            disableOnClick = false,
            enabled = searchResultsCount > 1
        ) {
            onPrevResult(ReaderEvent.OnPrevSearchResult)
        }

        IconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = R.string.next_result_content_desc,
            disableOnClick = false,
            enabled = searchResultsCount > 1
        ) {
            onNextResult(ReaderEvent.OnNextSearchResult)
        }
    }
}
