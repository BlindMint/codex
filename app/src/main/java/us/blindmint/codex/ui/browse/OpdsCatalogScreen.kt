/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse

import android.os.Parcelable
import androidx.compose.foundation.lazy.rememberLazyListState
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

// Base class for OPDS catalog screens
abstract class BaseOpdsCatalogScreen(
    open val source: OpdsSourceEntity,
    open val url: String? = null,
    open val title: String? = null
) : Screen {

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

@Parcelize
data class OpdsRootScreen(override val source: OpdsSourceEntity) : BaseOpdsCatalogScreen(source, null, null), Parcelable

@Parcelize
data class OpdsCategoryScreen(
    override val source: OpdsSourceEntity,
    override val url: String,
    override val title: String
) : BaseOpdsCatalogScreen(source, url, title), Parcelable