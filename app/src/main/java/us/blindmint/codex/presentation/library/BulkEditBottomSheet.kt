/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.presentation.book_info.BookInfoMetadataRow
import us.blindmint.codex.presentation.book_info.MetadataItemEditor
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.library.LibraryEvent

/**
 * Compute shared metadata items across selected books.
 * Only includes items that ALL selected books have in common.
 */
fun computeSharedTags(books: List<Book>): List<String> {
    if (books.isEmpty()) return emptyList()
    return books.map { it.tags.toSet() }
        .reduce { acc, set -> acc.intersect(set) }
        .toList()
        .sorted()
}

fun computeSharedSeries(books: List<Book>): List<String> {
    if (books.isEmpty()) return emptyList()
    return books.map { it.series.toSet() }
        .reduce { acc, set -> acc.intersect(set) }
        .toList()
        .sorted()
}

fun computeSharedLanguages(books: List<Book>): List<String> {
    if (books.isEmpty()) return emptyList()
    return books.map { it.languages.toSet() }
        .reduce { acc, set -> acc.intersect(set) }
        .toList()
        .sorted()
}

fun computeSharedAuthors(books: List<Book>): List<String> {
    if (books.isEmpty()) return emptyList()
    return books.map { it.authors.toSet() }
        .reduce { acc, set -> acc.intersect(set) }
        .toList()
        .sorted()
}

@Composable
fun BulkEditBottomSheet(
    selectedBooks: List<Book>,
    actionBulkEditTags: (LibraryEvent.OnActionBulkEditTags) -> Unit,
    actionBulkEditSeries: (LibraryEvent.OnActionBulkEditSeries) -> Unit,
    actionBulkEditLanguages: (LibraryEvent.OnActionBulkEditLanguages) -> Unit,
    actionBulkEditAuthors: (LibraryEvent.OnActionBulkEditAuthors) -> Unit,
    actionBulkEditCategory: (LibraryEvent.OnActionBulkEditCategory) -> Unit,
    dismissDialog: (LibraryEvent.OnDismissDialog) -> Unit
) {
    val context = LocalContext.current
    var showTagsEditor by remember { mutableStateOf(false) }
    var showSeriesEditor by remember { mutableStateOf(false) }
    var showLanguagesEditor by remember { mutableStateOf(false) }
    var showAuthorsEditor by remember { mutableStateOf(false) }

    val sharedTags = computeSharedTags(selectedBooks)
    val sharedSeries = computeSharedSeries(selectedBooks)
    val sharedLanguages = computeSharedLanguages(selectedBooks)
    val sharedAuthors = computeSharedAuthors(selectedBooks)

    // Determine shared category across selected books (only if ALL have the same category)
    val sharedCategory = if (selectedBooks.isNotEmpty()) {
        val allCategories = selectedBooks.map { it.category }.distinct()
        if (allCategories.size == 1) allCategories.first() else null
    } else {
        null
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = {
            dismissDialog(LibraryEvent.OnDismissDialog)
        },
        sheetGesturesEnabled = true
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SettingsSubcategoryTitle(
                    title = "Edit Metadata"
                )
            }

            item {
                Text(
                    text = stringResource(id = R.string.selected_items_count_query, selectedBooks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            item {
                StatusChipsRow(
                    selectedStatuses = if (sharedCategory != null) setOf(sharedCategory) else emptySet(),
                    onStatusToggle = { status, selected ->
                        val categoryToSet = if (selected) status else null
                        actionBulkEditCategory(LibraryEvent.OnActionBulkEditCategory(categoryToSet, context))
                    }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.tags),
                    items = sharedTags,
                    forceShowEmpty = true,
                    onEditClick = { showTagsEditor = true }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.series),
                    items = sharedSeries,
                    forceShowEmpty = true,
                    onEditClick = { showSeriesEditor = true }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.authors),
                    items = sharedAuthors,
                    forceShowEmpty = true,
                    onEditClick = { showAuthorsEditor = true }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.languages),
                    items = sharedLanguages,
                    forceShowEmpty = true,
                    onEditClick = { showLanguagesEditor = true }
                )
            }
        }
    }

    if (showTagsEditor) {
        MetadataItemEditor(
            title = stringResource(id = R.string.tags),
            items = sharedTags,
            onItemsChanged = { tags ->
                actionBulkEditTags(LibraryEvent.OnActionBulkEditTags(tags, context))
                showTagsEditor = false
            },
            onDismiss = { showTagsEditor = false }
        )
    }

    if (showSeriesEditor) {
        MetadataItemEditor(
            title = stringResource(id = R.string.series),
            items = sharedSeries,
            onItemsChanged = { series ->
                actionBulkEditSeries(LibraryEvent.OnActionBulkEditSeries(series, context))
                showSeriesEditor = false
            },
            onDismiss = { showSeriesEditor = false }
        )
    }

    if (showLanguagesEditor) {
        MetadataItemEditor(
            title = stringResource(id = R.string.languages),
            items = sharedLanguages,
            onItemsChanged = { languages ->
                actionBulkEditLanguages(LibraryEvent.OnActionBulkEditLanguages(languages, context))
                showLanguagesEditor = false
            },
            onDismiss = { showLanguagesEditor = false }
        )
    }

    if (showAuthorsEditor) {
        MetadataItemEditor(
            title = stringResource(id = R.string.authors),
            items = sharedAuthors,
            onItemsChanged = { authors ->
                actionBulkEditAuthors(LibraryEvent.OnActionBulkEditAuthors(authors, context))
                showAuthorsEditor = false
            },
            onDismiss = { showAuthorsEditor = false }
        )
    }
}
