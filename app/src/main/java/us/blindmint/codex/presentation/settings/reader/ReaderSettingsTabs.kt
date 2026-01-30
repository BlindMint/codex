/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderSettingsTab
import us.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun ReaderSettingsTabs(
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.5.dp),
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {},
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    val width by animateDpAsState(
                        targetValue = tabPositions[pagerState.currentPage].contentWidth,
                        label = ""
                    )

                    TabRowDefaults.PrimaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        width = width
                    )
                }
            }
        ) {
            ReaderSettingsTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        StyledText(
                            text = when (tab) {
                                ReaderSettingsTab.BOOKS -> stringResource(R.string.books_tab)
                                ReaderSettingsTab.SPEED_READING -> stringResource(R.string.speed_reading_tab)
                                ReaderSettingsTab.COMICS -> stringResource(R.string.comics_tab)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                        )
                    }
                )
            }
        }
    }
}