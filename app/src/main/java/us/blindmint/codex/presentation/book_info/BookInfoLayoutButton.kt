/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.book_info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.util.calculateProgress

@Composable
fun BookInfoLayoutButton(
    book: Book,
    navigateToReader: () -> Unit,
    navigateToSpeedReading: (() -> Unit)? = null
) {
    val buttonText = if (book.progress == 0f) stringResource(id = R.string.start_reading)
    else stringResource(
        id = R.string.continue_reading_query,
        "${book.progress.calculateProgress(1)}%"
    )

    // Material 3 Split Button - Custom implementation
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading button area (main action) - takes most of the space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = if (!book.isComic && navigateToSpeedReading != null) {
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    } else {
                        CircleShape
                    }
                )
                .clickable(
                    onClick = {
                        if (book.id != -1) {
                            navigateToReader()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = buttonText,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Trailing button (speed reading) - only for books
        if (!book.isComic && navigateToSpeedReading != null) {
            // Visual separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.6f)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
            )

            // Trailing button area
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 24.dp,
                            bottomEnd = 24.dp
                        )
                    )
                    .clickable(
                        onClick = {
                            if (book.id != -1) {
                                navigateToSpeedReading()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = stringResource(id = R.string.speed_reading_content_desc),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}