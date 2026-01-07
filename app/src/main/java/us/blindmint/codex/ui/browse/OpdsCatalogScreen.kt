/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.browse.opds.OpdsCatalogContent

@Parcelize
data class OpdsCatalogScreen(val source: OpdsSourceEntity, val url: String? = null, val title: String? = null) : Screen, Parcelable {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        OpdsCatalogContent(
            source = source,
            url = url,
            title = title,
            listState = listState,
            scrollBehavior = scrollBehavior,
            navigateBack = { navigator.pop() }
        )
    }
}