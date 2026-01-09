/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.ui.library.FilterState
import us.blindmint.codex.ui.library.LibraryEvent
import us.blindmint.codex.ui.library.LibraryModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryFilterDrawer(
    show: Boolean,
    onDismiss: () -> Unit
) {
    val statuses = listOf("Reading", "Planning", "Already Read", "Favorites")

    // Get access to LibraryModel to read/write filter state
    val libraryModel = hiltViewModel<LibraryModel>()
    val libraryState by libraryModel.state.collectAsStateWithLifecycle()

    // Extract available values from all books (not just filtered ones)
    val allBooks = libraryState.books.map { it.data }

    val availableTags = mutableSetOf<String>()
    val availableAuthors = mutableSetOf<String>()
    val availableSeries = mutableSetOf<String>()
    val availableLanguages = mutableSetOf<String>()

    for (book in allBooks) {
        availableTags.addAll(book.tags)
        when (book.author) {
            is us.blindmint.codex.domain.ui.UIText.StringValue -> availableAuthors.add(book.author.value)
            is us.blindmint.codex.domain.ui.UIText.StringResource -> {} // Skip string resources for now
        }
        book.seriesName?.let { availableSeries.add(it) }
        book.language?.let { availableLanguages.add(it) }
    }

    val sortedTags = availableTags.sorted()
    val sortedAuthors = availableAuthors.sorted()
    val sortedSeries = availableSeries.sorted()
    val sortedLanguages = availableLanguages.sorted()

    // Local state for UI
    var selectedStatuses: Set<String> by remember(libraryState.filterState.selectedStatuses) {
        mutableStateOf(libraryState.filterState.selectedStatuses)
    }
    var selectedTags: Set<String> by remember(libraryState.filterState.selectedTags) {
        mutableStateOf(libraryState.filterState.selectedTags)
    }
    var selectedAuthors: Set<String> by remember(libraryState.filterState.selectedAuthors) {
        mutableStateOf(libraryState.filterState.selectedAuthors)
    }
    var selectedSeries: Set<String> by remember(libraryState.filterState.selectedSeries) {
        mutableStateOf(libraryState.filterState.selectedSeries)
    }
    var yearRange: ClosedFloatingPointRange<Float> by remember(libraryState.filterState.publicationYearRange) {
        mutableStateOf(
            libraryState.filterState.publicationYearRange.start.toFloat()..libraryState.filterState.publicationYearRange.endInclusive.toFloat()
        )
    }
    var selectedLanguages: Set<String> by remember(libraryState.filterState.selectedLanguages) {
        mutableStateOf(libraryState.filterState.selectedLanguages)
    }
    var useAndLogic: Boolean by remember(libraryState.filterState.useAndLogic) {
        mutableStateOf(libraryState.filterState.useAndLogic)
    }

    ModalDrawer(
        show = show,
        side = us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide.LEFT,
        onDismissRequest = onDismiss,
        header = {
            TopAppBar(
                title = { Text(stringResource(R.string.filter_title)) },
                actions = {
                    Button(onClick = {
                        selectedStatuses = emptySet()
                        selectedTags = emptySet()
                        selectedAuthors = emptySet()
                        selectedSeries = emptySet()
                        yearRange = 1900f..2026f
                        selectedLanguages = emptySet()
                        useAndLogic = true
                    }) {
                        Text("Clear All")
                    }
                }
            )
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val newFilterState = FilterState(
                            selectedStatuses = selectedStatuses,
                            selectedTags = selectedTags,
                            selectedAuthors = selectedAuthors,
                            selectedSeries = selectedSeries,
                            publicationYearRange = yearRange.start.toInt()..yearRange.endInclusive.toInt(),
                            selectedLanguages = selectedLanguages,
                            useAndLogic = useAndLogic
                        )
                        libraryModel.onEvent(LibraryEvent.OnUpdateFilterState(newFilterState))
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Status Presets", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEach { status ->
                        FilterChip(
                            selected = status in selectedStatuses,
                            onClick = {
                                selectedStatuses = if (status in selectedStatuses) {
                                    selectedStatuses - status
                                } else {
                                    selectedStatuses + status
                                }
                            },
                            label = { Text(status) }
                        )
                    }
                }

                // Tags menu item (simplified - just show count for now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to tags screen */ }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tags (${sortedTags.size})", style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open tags")
                }

                // Authors menu item (simplified - just show count for now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to authors screen */ }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Authors (${sortedAuthors.size})", style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open authors")
                }

                // Series menu item (simplified - just show count for now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to series screen */ }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Series (${sortedSeries.size})", style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open series")
                }

                Text("Publication Year", style = MaterialTheme.typography.titleSmall)
                RangeSlider(
                    value = yearRange,
                    onValueChange = { yearRange = it },
                    valueRange = 1900f..2026f,
                    steps = 126
                )
                Text("${yearRange.start.toInt()} - ${yearRange.endInclusive.toInt()}")

                Text("Language", style = MaterialTheme.typography.titleSmall)
                if (sortedLanguages.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortedLanguages.forEach { language ->
                            FilterChip(
                                selected = selectedLanguages.contains(language),
                                onClick = {
                                    selectedLanguages = if (selectedLanguages.contains(language)) {
                                        selectedLanguages - language
                                    } else {
                                        selectedLanguages + language
                                    }
                                },
                                label = { Text(language.uppercase()) }
                            )
                        }
                    }
                } else {
                    Text("No languages available", style = MaterialTheme.typography.bodySmall)
                }

                Text("Logic", style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = useAndLogic,
                        onClick = { useAndLogic = true },
                        label = { Text("AND") }
                    )
                    FilterChip(
                        selected = !useAndLogic,
                        onClick = { useAndLogic = false },
                        label = { Text("OR") }
                    )
                }
            }
        }
    }
}