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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.constants.provideExtensions
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.book_info.BookInfoEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookInfoDetailsBottomSheet(
    book: Book,
    showPathDialog: (BookInfoEvent.OnShowPathDialog) -> Unit,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    showEditBottomSheet: (BookInfoEvent.OnShowEditBottomSheet) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    clearProgressHistory: (BookInfoEvent.OnClearProgressHistory) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit
) {
    val pattern = remember { SimpleDateFormat("HH:mm dd MMM yyyy", Locale.getDefault()) }
    val lastOpened = remember(book.lastOpened) { pattern.format(Date(book.lastOpened ?: 0)) }

    val context = LocalContext.current
    val cachedFile = remember(book.filePath) {
        val uri = Uri.parse(book.filePath)
        if (!uri.scheme.isNullOrBlank()) {
            CachedFileCompat.fromUri(context, uri)
        } else {
            CachedFileCompat.fromFullPath(context, book.filePath)
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
                         title = stringResource(id = R.string.file_details),
                         padding = 0.dp,
                         color = MaterialTheme.colorScheme.primary
                     )
                     Row(
                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         IconButton(
                             icon = Icons.Outlined.Edit,
                             contentDescription = R.string.edit_metadata,
                             disableOnClick = false,
                             color = MaterialTheme.colorScheme.secondary,
                             modifier = Modifier.size(24.dp)
                         ) {
                             showEditBottomSheet(BookInfoEvent.OnShowEditBottomSheet)
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
                     }
                 }
            }

             item {
                 BookInfoDetailsBottomSheetItem(
                     label = stringResource(id = R.string.title),
                     text = book.title,
                     editable = false
                 )
             }

             item {
                 BookInfoDetailsBottomSheetItem(
                     label = stringResource(id = R.string.author),
                     text = book.author.asString(),
                     editable = false
                 )
             }

             item {
                 BookInfoDetailsBottomSheetItem(
                     label = stringResource(id = R.string.description),
                     text = book.description ?: "",
                     editable = false
                 )
             }

             item {
                 BookInfoDetailsBottomSheetItem(
                     label = stringResource(id = R.string.file_path),
                     text = cachedFile?.path ?: book.filePath,
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