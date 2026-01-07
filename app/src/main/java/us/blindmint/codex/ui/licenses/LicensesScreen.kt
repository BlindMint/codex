/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.licenses

import android.os.Parcelable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.R
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.licenses.LicensesContent
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.license_info.LicenseInfoScreen

@Parcelize
data class LicensesScreen(val id: Int = 0) : Screen, Parcelable {



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val context = LocalContext.current

        val (scrollBehavior, listState) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()
        val licenses = remember {
            derivedStateOf {
                Libs.Builder().withJson(context, R.raw.aboutlibraries).build()
                    .libraries.sortedBy { it.openSource }
            }
        }



        LicensesContent(
            licenses = licenses.value,
            scrollBehavior = scrollBehavior,
            listState = listState,
            navigateToLicenseInfo = {
                navigator.push(LicenseInfoScreen(it.uniqueId))
            },
            navigateBack = {
                navigator.pop()
            }
        )
    }
}