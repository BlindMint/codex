/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ua.blindmint.codex.R
import ua.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import ua.blindmint.codex.presentation.core.constants.provideContributorsPage
import ua.blindmint.codex.presentation.core.constants.provideIssuesPage
import ua.blindmint.codex.presentation.core.constants.provideReleasesPage
import ua.blindmint.codex.ui.about.AboutEvent

@Composable
fun AboutLayout(
    paddingValues: PaddingValues,
    listState: LazyListState,
    navigateToBrowserPage: (AboutEvent.OnNavigateToBrowserPage) -> Unit,
    navigateToLicenses: () -> Unit,
    navigateToCredits: () -> Unit
) {
    val context = LocalContext.current

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    LazyColumnWithScrollbar(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        state = listState
    ) {
        item {
            AboutItem(
                title = stringResource(id = R.string.app_version_option),
                description = "Codex v$appVersion",
            ) {
                navigateToBrowserPage(
                    AboutEvent.OnNavigateToBrowserPage(
                        page = provideReleasesPage(),
                        context = context
                    )
                )
            }
        }

        item {
            AboutItem(
                title = stringResource(id = R.string.report_bug_option),
                description = null
            ) {
                navigateToBrowserPage(
                    AboutEvent.OnNavigateToBrowserPage(
                        page = provideIssuesPage(),
                        context = context
                    )
                )
            }
        }

        item {
            AboutItem(
                title = stringResource(id = R.string.contributors_option),
                description = null
            ) {
                navigateToBrowserPage(
                    AboutEvent.OnNavigateToBrowserPage(
                        page = provideContributorsPage(),
                        context = context
                    )
                )
            }
        }

        item {
            AboutItem(
                title = stringResource(id = R.string.licenses_option),
                description = null
            ) {
                navigateToLicenses()
            }
        }

        item {
            AboutItem(
                title = stringResource(id = R.string.credits_option),
                description = null
            ) {
                navigateToCredits()
            }
        }


        item {
            AboutBadges(
                navigateToBrowserPage = navigateToBrowserPage
            )
        }
    }
}