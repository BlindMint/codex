/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.start

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.browse.opds.BrowseOpdsManagementContent
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

fun LazyListScope.StartSettingsLayoutOpds() {
    item {
        Spacer(modifier = Modifier.height(16.dp))
    }

    item {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.start_opds_setup),
            color = MaterialTheme.colorScheme.secondary
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
    }

    item {
        BrowseOpdsManagementContent()
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
    }
}