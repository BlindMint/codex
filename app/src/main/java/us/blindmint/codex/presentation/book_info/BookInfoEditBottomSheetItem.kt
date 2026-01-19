/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun BookInfoEditBottomSheetItem(
    label: String,
    text: String,
    onEdit: () -> Unit,
    showError: Boolean = false,
    errorMessage: String = "",
    maxLines: Int = 2
) {
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
                .clickable { onEdit() },
            value = text,
            onValueChange = {},
            readOnly = true,
            maxLines = maxLines,
            label = {
                StyledText(label)
            },
            isError = showError,
            supportingText = if (showError && errorMessage.isNotEmpty()) {
                { StyledText(errorMessage) }
            } else null
        )
    }
}
