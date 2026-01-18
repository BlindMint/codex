/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.blindmint.codex.domain.reader.ReaderSettingsTab

@Composable
fun ReaderSettingsLayout(
    paddingValues: PaddingValues
) {
    val pagerState = rememberPagerState(pageCount = { ReaderSettingsTab.entries.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        ReaderSettingsTabs(pagerState = pagerState)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                when (ReaderSettingsTab.entries[page]) {
                    ReaderSettingsTab.BOOKS -> BooksReaderSettingsCategory()
                    ReaderSettingsTab.SPEED_READING -> SpeedReadingReaderSettingsCategory()
                    ReaderSettingsTab.COMICS -> ComicsReaderSettingsCategory()
                }
            }
        }
    }
}