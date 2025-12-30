/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.book_info

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.library.book.Book
import ua.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import ua.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import ua.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import ua.blindmint.codex.ui.book_info.BookInfoEvent

@Composable
fun BookInfoEditBottomSheet(
    book: Book,
    showTitleDialog: (BookInfoEvent.OnShowTitleDialog) -> Unit,
    showAuthorDialog: (BookInfoEvent.OnShowAuthorDialog) -> Unit,
    showDescriptionDialog: (BookInfoEvent.OnShowDescriptionDialog) -> Unit,
    resetTitle: (BookInfoEvent.OnResetTitle) -> Unit,
    resetAuthor: (BookInfoEvent.OnResetAuthor) -> Unit,
    resetDescription: (BookInfoEvent.OnResetDescription) -> Unit,
    dismissBottomSheet: (BookInfoEvent.OnDismissBottomSheet) -> Unit
) {
    val context = LocalContext.current

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
                SettingsSubcategoryTitle(
                    title = stringResource(id = R.string.edit_metadata),
                    padding = 16.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.title),
                    text = book.title,
                    onEdit = {
                        showTitleDialog(BookInfoEvent.OnShowTitleDialog)
                    },
                    onReset = {
                        resetTitle(BookInfoEvent.OnResetTitle(context))
                    }
                )
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.author),
                    text = book.author.asString(),
                    onEdit = {
                        showAuthorDialog(BookInfoEvent.OnShowAuthorDialog)
                    },
                    onReset = {
                        resetAuthor(BookInfoEvent.OnResetAuthor(context))
                    }
                )
            }

            item {
                BookInfoEditBottomSheetItem(
                    label = stringResource(id = R.string.description),
                    text = book.description ?: "",
                    onEdit = {
                        showDescriptionDialog(BookInfoEvent.OnShowDescriptionDialog)
                    },
                    onReset = {
                        resetDescription(BookInfoEvent.OnResetDescription(context))
                    }
                )
            }
        }
    }
}
