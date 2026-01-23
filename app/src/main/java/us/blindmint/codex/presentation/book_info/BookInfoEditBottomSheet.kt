/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.book_info.BookInfoEvent
import us.blindmint.codex.domain.ui.UIText

@Composable
fun BookInfoEditBottomSheet(
    book: Book,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    onTagsChanged: (BookInfoEvent.OnActionTagsDialog) -> Unit = {},
    onSeriesChanged: (BookInfoEvent.OnActionSeriesDialog) -> Unit = {},
    onLanguagesChanged: (BookInfoEvent.OnActionLanguagesDialog) -> Unit = {},
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit
) {
    val context = LocalContext.current
    val cachedFile = remember(book.filePath) {
        val uri = Uri.parse(book.filePath)
        if (!uri.scheme.isNullOrBlank()) {
            CachedFileCompat.fromUri(context, uri)
        } else {
            CachedFileCompat.fromFullPath(context, book.filePath)
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

    // Track if any changes have been made
    var hasChanges by remember { mutableStateOf(false) }

    // Metadata editor state
    var showTagsEditor by remember { mutableStateOf(false) }
    var showSeriesEditor by remember { mutableStateOf(false) }
    var showLanguagesEditor by remember { mutableStateOf(false) }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = {
            dismissBottomSheet(BookInfoEvent.OnDismissBottomSheet)
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
                        title = stringResource(id = R.string.edit_metadata),
                        padding = 0.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Reset button (top-right)
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
                        hasChanges = false
                    }
                }
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.title),
                    text = book.title,
                    onEdit = {
                        showTitleDialog(BookInfoEvent.OnShowTitleDialog)
                        hasChanges = true
                    }
                )
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.author),
                    text = book.authors.firstOrNull() ?: stringResource(id = R.string.unknown_author),
                    onEdit = {
                        showAuthorDialog(BookInfoEvent.OnShowAuthorDialog)
                        hasChanges = true
                    }
                )
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.description),
                    text = book.description ?: "",
                    onEdit = {
                        showDescriptionDialog(BookInfoEvent.OnShowDescriptionDialog)
                        hasChanges = true
                    },
                    maxLines = 3
                )
            }

            // Metadata rows for editing
            if (book.tags.isNotEmpty()) {
                item {
                    BookInfoMetadataRow(
                        title = stringResource(id = R.string.tags),
                        items = book.tags,
                        onEdit = { showTagsEditor = true }
                    )
                }
            }

            if (book.authors.size > 1) {
                item {
                    BookInfoMetadataRow(
                        title = stringResource(id = R.string.authors),
                        items = book.authors
                    )
                }
            }

            if (book.series.isNotEmpty()) {
                item {
                    BookInfoMetadataRow(
                        title = stringResource(id = R.string.series),
                        items = book.series,
                        onEdit = { showSeriesEditor = true }
                    )
                }
            }

            if (book.languages.isNotEmpty()) {
                item {
                    BookInfoMetadataRow(
                        title = stringResource(id = R.string.languages),
                        items = book.languages,
                        onEdit = { showLanguagesEditor = true }
                    )
                }
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.file_path),
                    text = cachedFile?.let { "${it.path}/${it.name}" } ?: book.filePath,
                    onEdit = {
                        showPathDialog(BookInfoEvent.OnShowPathDialog)
                        hasChanges = true
                    },
                    showError = !fileExists,
                    errorMessage = stringResource(id = R.string.error_no_file)
                )
            }

            // Save/Revert buttons - only show if changes have been made
            if (hasChanges) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Revert button (looks different from reset button)
                        IconButton(
                            icon = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = R.string.revert_changes,
                            disableOnClick = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        ) {
                            // Reset to original values
                            resetTitle(BookInfoEvent.OnResetTitle(context))
                            resetAuthor(BookInfoEvent.OnResetAuthor(context))
                            resetDescription(BookInfoEvent.OnResetDescription(context))
                            hasChanges = false
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Save button
                        IconButton(
                            icon = Icons.Default.Check,
                            contentDescription = R.string.save_changes,
                            disableOnClick = false,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            // Save changes - this is handled by the individual dialog actions
                            dismissBottomSheet(BookInfoEvent.OnDismissBottomSheet)
                        }
                    }
                }
            }
        }

        // Metadata item editors
        if (showTagsEditor) {
            MetadataItemEditor(
                title = stringResource(id = R.string.tags),
                items = book.tags,
                onItemsChanged = { tags ->
                    onTagsChanged(BookInfoEvent.OnActionTagsDialog(tags, context))
                    hasChanges = true
                },
                onDismiss = { showTagsEditor = false }
            )
        }

        if (showSeriesEditor) {
            MetadataItemEditor(
                title = stringResource(id = R.string.series),
                items = book.series,
                onItemsChanged = { series ->
                    onSeriesChanged(BookInfoEvent.OnActionSeriesDialog(series, context))
                    hasChanges = true
                },
                onDismiss = { showSeriesEditor = false }
            )
        }

        if (showLanguagesEditor) {
            MetadataItemEditor(
                title = stringResource(id = R.string.languages),
                items = book.languages,
                onItemsChanged = { languages ->
                    onLanguagesChanged(BookInfoEvent.OnActionLanguagesDialog(languages, context))
                    hasChanges = true
                },
                onDismiss = { showLanguagesEditor = false }
            )
        }
    }
}