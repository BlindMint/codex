/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.presentation.library.StatusChipsRow
import us.blindmint.codex.presentation.navigator.NavigatorBackIconButton
import us.blindmint.codex.ui.book_info.BookInfoEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoDetailsPanel(
    book: Book,
    editedBook: Book?,
    showTagsDialog: (BookInfoEvent.OnShowTagsDialog) -> Unit,
    showSeriesDialog: (BookInfoEvent.OnShowSeriesDialog) -> Unit,
    showLanguagesDialog: (BookInfoEvent.OnShowLanguagesDialog) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    onConfirmSaveChanges: (android.content.Context) -> Unit,
    onCancelChanges: () -> Unit,
    onUpdateEditedBook: (Book) -> Unit,
    onCategoryChange: (Category) -> Unit
) {
    val pattern = remember { SimpleDateFormat("HH:mm dd MMM yyyy", Locale.getDefault()) }
    val lastOpened = remember(book.lastOpened) { pattern.format(Date(book.lastOpened ?: 0)) }

    val context = LocalContext.current
    val displayBook = editedBook ?: book

    val hasChanges = editedBook != null && (
        editedBook.title != book.title ||
        editedBook.authors != book.authors ||
        editedBook.description != book.description ||
        editedBook.tags != book.tags ||
        editedBook.series != book.series ||
        editedBook.languages != book.languages
    )

    val cachedFile = remember(displayBook.filePath) {
        CachedFileFactory.fromBook(context, displayBook)
    }

    val fileSize = remember(cachedFile) {
        if (cachedFile != null && cachedFile.canAccess()) {
            val sizeBytes = cachedFile.size
            val sizeKB = sizeBytes / 1024f
            val sizeMB = sizeKB / 1024f
            when {
                sizeMB >= 1f -> "%.2f MB".format(sizeMB)
                sizeKB > 0f -> "%.2f KB".format(sizeKB)
                else -> ""
            }
        } else {
            ""
        }
    }

    val fileExists = remember(cachedFile) {
        cachedFile.let {
            it != null && it.canAccess() && !it.isDirectory && provideExtensions()
                .any { ext ->
                    it.name.endsWith(ext, ignoreCase = true)
                }
        }
    }

    val fileExtension = remember(cachedFile) {
        cachedFile?.name?.substringAfterLast('.', "")?.uppercase() ?: ""
    }

    val dismissAction = {
        if (editedBook != null) {
            onCancelChanges()
        }
        dismissBottomSheet(BookInfoEvent.OnDismissBottomSheet)
    }

    BackHandler { dismissAction() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    StyledText(stringResource(id = R.string.file_details))
                },
                navigationIcon = {
                    NavigatorBackIconButton(
                        navigateBack = dismissAction
                    )
                },
                actions = {
                    if (book.opdsSourceUrl != null) {
                        IconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = R.string.refresh_from_opds,
                            disableOnClick = false
                        ) {
                            refreshMetadataFromOpds(BookInfoEvent.OnRefreshMetadataFromOpds(context))
                        }
                    }
                    if (hasChanges) {
                        IconButton(
                            icon = Icons.Default.Check,
                            contentDescription = R.string.confirm_changes,
                            disableOnClick = false
                        ) {
                            onConfirmSaveChanges(context)
                        }
                        IconButton(
                            icon = Icons.Default.Close,
                            contentDescription = R.string.cancel_editing,
                            disableOnClick = false
                        ) {
                            onCancelChanges()
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                StatusChipsRow(
                    selectedStatuses = setOf(displayBook.category),
                    onStatusToggle = { status, selected ->
                        if (selected) {
                            onCategoryChange(status)
                        }
                    }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.title),
                    text = displayBook.title,
                    editable = true,
                    onTextChange = { newTitle ->
                        val base = editedBook ?: book
                        onUpdateEditedBook(base.copy(title = newTitle))
                    }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.author),
                    text = displayBook.authors.firstOrNull() ?: stringResource(id = R.string.unknown_author),
                    editable = true,
                    onTextChange = { newAuthor ->
                        val base = editedBook ?: book
                        onUpdateEditedBook(base.copy(authors = if (newAuthor.isNotEmpty()) listOf(newAuthor) else emptyList()))
                    }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.description),
                    text = displayBook.description ?: "",
                    editable = true,
                    onTextChange = { newDescription ->
                        val base = editedBook ?: book
                        onUpdateEditedBook(base.copy(description = newDescription.ifEmpty { null }))
                    },
                    maxLines = 4
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.tags),
                    items = displayBook.tags,
                    isEditable = true,
                    forceShowEmpty = true,
                    onEditClick = {
                        showTagsDialog(BookInfoEvent.OnShowTagsDialog)
                    }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.series),
                    items = displayBook.series,
                    isEditable = true,
                    forceShowEmpty = true,
                    onEditClick = {
                        showSeriesDialog(BookInfoEvent.OnShowSeriesDialog)
                    }
                )
            }

            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.languages),
                    items = displayBook.languages,
                    isEditable = true,
                    forceShowEmpty = true,
                    onEditClick = {
                        showLanguagesDialog(BookInfoEvent.OnShowLanguagesDialog)
                    }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.file_path),
                    text = cachedFile?.path ?: displayBook.filePath,
                    editable = false,
                    showError = !fileExists,
                    errorMessage = stringResource(id = R.string.error_no_file),
                    maxLines = Int.MAX_VALUE
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.file_last_opened),
                    text = if (book.lastOpened != null) lastOpened
                    else stringResource(id = R.string.never),
                    editable = false
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusable(false),
                        value = fileSize.ifBlank { stringResource(id = R.string.unknown) },
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            StyledText(stringResource(id = R.string.file_size))
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusable(false),
                        value = fileExtension.ifBlank { stringResource(id = R.string.unknown) },
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            StyledText(stringResource(id = R.string.file_extension))
                        }
                    )
                }
            }
        }
    }
}
