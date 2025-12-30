/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.book_info

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.blindmint.codex.R
import ua.blindmint.codex.presentation.core.components.common.IconButton
import ua.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun BookInfoEditBottomSheetItem(
    label: String,
    text: String,
    onEdit: () -> Unit,
    onReset: () -> Unit
) {
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
            value = text,
            onValueChange = {},
            readOnly = true,
            maxLines = 2,
            label = {
                StyledText(label)
            }
        )

        IconButton(
            icon = Icons.Default.EditNote,
            contentDescription = R.string.edit_content_desc,
            disableOnClick = false,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            onEdit()
        }

        IconButton(
            icon = Icons.Default.Restore,
            contentDescription = R.string.reset_content_desc,
            disableOnClick = false,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        ) {
            onReset()
        }
    }
}
