/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawer
import us.blindmint.codex.presentation.core.components.modal_drawer.ModalDrawerTitleItem

@Composable
fun LibraryFilterDrawer(
    show: Boolean,
    onDismiss: () -> Unit
) {
    ModalDrawer(
        show = show,
        side = us.blindmint.codex.presentation.core.components.modal_drawer.DrawerSide.LEFT,
        onDismissRequest = onDismiss
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModalDrawerTitleItem(
                    title = stringResource(R.string.filter_title)
                )

                // TODO: Add filter options: Status presets, Tags, Authors, Series, Publication year, Language

                Text("Status Presets", style = MaterialTheme.typography.titleSmall)
                Text("Reading", modifier = Modifier.padding(start = 16.dp))
                Text("Planning", modifier = Modifier.padding(start = 16.dp))
                Text("Already Read", modifier = Modifier.padding(start = 16.dp))
                Text("Favorites", modifier = Modifier.padding(start = 16.dp))

                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Text("Placeholder for tags", modifier = Modifier.padding(start = 16.dp))

                Text("Authors", style = MaterialTheme.typography.titleSmall)
                Text("Placeholder for authors", modifier = Modifier.padding(start = 16.dp))

                Text("Series", style = MaterialTheme.typography.titleSmall)
                Text("Placeholder for series", modifier = Modifier.padding(start = 16.dp))

                Text("Publication Year", style = MaterialTheme.typography.titleSmall)
                Text("Placeholder for year range", modifier = Modifier.padding(start = 16.dp))

                Text("Language", style = MaterialTheme.typography.titleSmall)
                Text("Placeholder for languages", modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}