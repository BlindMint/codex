/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.R
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.presentation.library.StatusChipsRow
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.book_info.BookInfoEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookInfoDetailsBottomSheet(
    book: Book,
    isEditingMetadata: Boolean,
    editedBook: Book?,
    showConfirmSaveDialog: Boolean,
    showConfirmCancelDialog: Boolean,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    showTagsDialog: (BookInfoEvent.OnShowTagsDialog) -> Unit,
    showSeriesDialog: (BookInfoEvent.OnShowSeriesDialog) -> Unit,
    showLanguagesDialog: (BookInfoEvent.OnShowLanguagesDialog) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    clearProgressHistory: (BookInfoEvent.OnClearProgressHistory) -> Unit,
    refreshMetadataFromOpds: (BookInfoEvent.OnRefreshMetadataFromOpds) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit,
    onEnterEditMode: () -> Unit,
    onConfirmEditMetadata: () -> Unit,
    onCancelEditMetadata: () -> Unit,
    onSilentCancelEditMetadata: () -> Unit,
    onConfirmSaveChanges: (Context: android.content.Context) -> Unit,
    onDismissSaveDialog: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onUpdateEditedBook: (Book) -> Unit,
    onCategoryChange: (Category) -> Unit
) {
    val pattern = remember { SimpleDateFormat("HH:mm dd MMM yyyy", Locale.getDefault()) }
    val lastOpened = remember(book.lastOpened) { pattern.format(Date(book.lastOpened ?: 0)) }

    val context = LocalContext.current
    val displayBook = if (isEditingMetadata) editedBook ?: book else book


    val cachedFile = remember(displayBook.filePath) {
        val uri = Uri.parse(displayBook.filePath)
        if (!uri.scheme.isNullOrBlank()) {
            CachedFileCompat.fromUri(context, uri)
        } else {
            CachedFileCompat.fromFullPath(context, displayBook.filePath)
        }
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

    // Confirmation dialogs
    if (showConfirmSaveDialog) {
        AlertDialog(
            onDismissRequest = onDismissSaveDialog,
            title = { Text("Save Changes") },
            text = { Text("Are you sure you want to save these metadata changes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmSaveChanges(context)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSaveDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmCancelDialog) {
        AlertDialog(
            onDismissRequest = { onDismissCancelDialog() },
            title = { Text("Discard Changes") },
            text = { Text("Are you sure you want to discard all changes?") },
            confirmButton = {
                TextButton(
                    onClick = onDismissCancelDialog
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissCancelDialog() }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = {
            if (isEditingMetadata) {
                // Auto-revert to read-only mode without saving, then dismiss
                onSilentCancelEditMetadata()
                dismissBottomSheet(BookInfoEvent.OnDismissBottomSheet)
            } else {
                dismissBottomSheet(BookInfoEvent.OnDismissBottomSheet)
            }
        },
        sheetGesturesEnabled = true
    ) {
        LazyColumnWithScrollbar(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSubcategoryTitle(
                        title = stringResource(id = R.string.file_details),
                        padding = 0.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isEditingMetadata) {
                            IconButton(
                                icon = Icons.Outlined.Edit,
                                contentDescription = R.string.edit_metadata,
                                disableOnClick = false,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                onEnterEditMode()
                            }
                            IconButton(
                                icon = Icons.Default.Restore,
                                contentDescription = R.string.reset_content_desc,
                                disableOnClick = false,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                resetTitle(BookInfoEvent.OnResetTitle(context))
                                resetAuthor(BookInfoEvent.OnResetAuthor(context))
                                resetDescription(BookInfoEvent.OnResetDescription(context))
                            }
                            if (book.opdsSourceUrl != null) {
                                IconButton(
                                    icon = Icons.Default.Refresh,
                                    contentDescription = R.string.refresh_from_opds,
                                    disableOnClick = false,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    refreshMetadataFromOpds(BookInfoEvent.OnRefreshMetadataFromOpds(context))
                                }
                            }
                        } else {
                            IconButton(
                                icon = Icons.Default.Check,
                                contentDescription = R.string.confirm_changes,
                                disableOnClick = false,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                onConfirmEditMetadata()
                            }
                            IconButton(
                                icon = Icons.Default.Close,
                                contentDescription = R.string.cancel_editing,
                                disableOnClick = false,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                onCancelEditMetadata()
                            }
                        }
                    }
                }
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.title),
                    text = displayBook.title,
                    editable = isEditingMetadata,
                    onTextChange = { newTitle ->
                        if (isEditingMetadata && editedBook != null) {
                            onUpdateEditedBook(editedBook.copy(title = newTitle))
                        }
                    },
                    onEditClick = { showTitleDialog(BookInfoEvent.OnShowTitleDialog) }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.author),
                    text = displayBook.authors.firstOrNull() ?: stringResource(id = R.string.unknown_author),
                    editable = isEditingMetadata,
                    onTextChange = { newAuthor ->
                        if (isEditingMetadata && editedBook != null) {
                            onUpdateEditedBook(editedBook.copy(authors = if (newAuthor.isNotEmpty()) listOf(newAuthor) else emptyList()))
                        }
                    },
                    onEditClick = { showAuthorDialog(BookInfoEvent.OnShowAuthorDialog) }
                )
            }

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
                    label = stringResource(id = R.string.description),
                    text = displayBook.description ?: "",
                    editable = isEditingMetadata,
                    onTextChange = { newDescription ->
                        if (isEditingMetadata && editedBook != null) {
                            onUpdateEditedBook(editedBook.copy(description = newDescription.ifEmpty { null }))
                        }
                    },
                    maxLines = 4,
                    onEditClick = { showDescriptionDialog(BookInfoEvent.OnShowDescriptionDialog) }
                )
            }

            // Tags row (always show)
            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.tags),
                    items = displayBook.tags,
                    isEditable = isEditingMetadata,
                    forceShowEmpty = true,
                    onEditClick = {
                        if (isEditingMetadata) {
                            showTagsDialog(BookInfoEvent.OnShowTagsDialog)
                        }
                    }
                )
            }

            // Series row (always show)
            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.series),
                    items = displayBook.series,
                    isEditable = isEditingMetadata,
                    forceShowEmpty = true,
                    onEditClick = {
                        if (isEditingMetadata) {
                            showSeriesDialog(BookInfoEvent.OnShowSeriesDialog)
                        }
                    }
                )
            }

            // Languages row (always show)
            item {
                BookInfoMetadataRow(
                    title = stringResource(id = R.string.languages),
                    items = displayBook.languages,
                    isEditable = isEditingMetadata,
                    forceShowEmpty = true,
                    onEditClick = {
                        if (isEditingMetadata) {
                            showLanguagesDialog(BookInfoEvent.OnShowLanguagesDialog)
                        }
                    }
                )
            }

            item {
                BookInfoDetailsBottomSheetItem(
                    label = stringResource(id = R.string.file_path),
                    text = cachedFile?.path ?: displayBook.filePath,
                    editable = false,
                    showError = !fileExists,
                    errorMessage = stringResource(id = R.string.error_no_file)
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
