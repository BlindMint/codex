/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.common.IconButton
import us.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun BookInfoDetailsBottomSheetItem(
    label: String,
    text: String,
    editable: Boolean,
    showError: Boolean = false,
    errorMessage: String? = null,
    maxLines: Int = 1,
    onEdit: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onEditClick: (() -> Unit)? = null
) {
    // Use local TextFieldValue state to preserve cursor position across recompositions.
    // Only sync from external `text` when not editable or when text changes externally.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }

    // Sync external text changes (e.g., after cancel/confirm resets editedBook)
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
    ) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .focusable(!editable),
            value = textFieldValue,
            onValueChange = { newValue ->
                if (editable) {
                    textFieldValue = newValue
                    onTextChange(newValue.text)
                }
            },
            readOnly = !editable,
            isError = showError,
            maxLines = maxLines,
            supportingText = if (!showError || errorMessage.isNullOrBlank()) null else {
                {
                    StyledText(
                        text = errorMessage,
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            },
            label = {
                StyledText(label)
            }
        )

        if (!editable && onEditClick != null) {
            IconButton(
                icon = Icons.Default.EditNote,
                contentDescription = R.string.edit_content_desc,
                disableOnClick = false,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            ) {
                onEditClick()
            }
        }
    }
}