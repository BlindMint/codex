/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.NullableBook
import us.blindmint.codex.domain.library.book.SelectableNullableBook
import us.blindmint.codex.presentation.core.components.dialog.Dialog
import us.blindmint.codex.presentation.core.components.progress_indicator.CircularProgressIndicator
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.browse.BrowseEvent
import java.util.UUID

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun BrowseAddDialog(
    loadingAddDialog: Boolean,
    selectedBooksAddDialog: List<SelectableNullableBook>,
    dismissAddDialog: (BrowseEvent.OnDismissAddDialog) -> Unit,
    actionAddDialog: (BrowseEvent.OnActionAddDialog) -> Unit,
    selectAddDialog: (BrowseEvent.OnSelectAddDialog) -> Unit,
    navigateToLibrary: () -> Unit
) {
    val context = LocalContext.current

    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = { dismissAddDialog(BrowseEvent.OnDismissAddDialog) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.AddChart,
                    contentDescription = stringResource(id = R.string.add_books),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.add_books),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.add_books_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (loadingAddDialog) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            selectedBooksAddDialog,
                            key = { it.data.fileName ?: UUID.randomUUID() }
                        ) { book ->
                            BrowseAddDialogItem(
                                result = book
                            ) {
                                if (it) {
                                    selectAddDialog(
                                        BrowseEvent.OnSelectAddDialog(
                                            book = book
                                        )
                                    )
                                } else {
                                    book.data.message?.asString(context)
                                        ?.showToast(context = context)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fixed buttons at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { dismissAddDialog(BrowseEvent.OnDismissAddDialog) }
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(id = R.string.cancel),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        onClick = {
                            actionAddDialog(
                                BrowseEvent.OnActionAddDialog(
                                    context = context,
                                    navigateToLibrary = navigateToLibrary
                                )
                            )
                        },
                        enabled = !loadingAddDialog && selectedBooksAddDialog.any { it.data is NullableBook.NotNull }
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(id = R.string.ok),
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}