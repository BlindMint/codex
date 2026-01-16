/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.domain.library.LibraryTabWithBooks
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.ui.library.LibraryEvent
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.theme.DefaultTransition

@Composable
fun LibraryPager(
    pagerState: PagerState,
    categories: List<LibraryTabWithBooks>,
    hasSelectedItems: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    selectBook: (LibraryEvent.OnSelectBook) -> Unit,
    navigateToBrowse: () -> Unit,
    navigateToBookInfo: (id: Int) -> Unit,
    navigateToReader: (id: Int) -> Unit,
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
        val category = remember(categories, index) {
            derivedStateOf {
                categories[index]
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            DefaultTransition(visible = !isLoading) {
                val mainModel = hiltViewModel<MainModel>()
                val mainState = mainModel.state.collectAsStateWithLifecycle()

                if (mainState.value.libraryLayout == LibraryLayout.GRID) {
                    LibraryLayout(
                        books = category.value.books,
                        hasSelectedItems = hasSelectedItems,
                        selectBook = selectBook,
                        navigateToBrowse = navigateToBrowse,
                        navigateToBookInfo = navigateToBookInfo,
                        navigateToReader = navigateToReader
                    ) {
                        items(
                            category.value.books.size,
                            key = { index -> category.value.books[index].data.id }
                        ) { index ->
                            val book = category.value.books[index]
                            LibraryItem(
                                book = book,
                                hasSelectedItems = hasSelectedItems,
                                selectBook = { select ->
                                    selectBook(
                                        LibraryEvent.OnSelectBook(
                                            id = book.data.id,
                                            select = select
                                        )
                                    )
                                },
                                navigateToBookInfo = { navigateToBookInfo(book.data.id) },
                                navigateToReader = { navigateToReader(book.data.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else {
                    // List layout - use LazyColumn directly
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(
                            category.value.books.size,
                            key = { index -> category.value.books[index].data.id }
                        ) { index ->
                            val book = category.value.books[index]
                            LibraryListItem(
                                book = book,
                                hasSelectedItems = hasSelectedItems,
                                selectBook = { select ->
                                    selectBook(
                                        LibraryEvent.OnSelectBook(
                                            id = book.data.id,
                                            select = select
                                        )
                                    )
                                },
                                navigateToBookInfo = { navigateToBookInfo(book.data.id) },
                                navigateToReader = { navigateToReader(book.data.id) },
                                modifier = Modifier.animateItem(),
                                listSize = mainState.value.libraryListSize
                            )
                        }
                    }
                }
            }

            LibraryEmptyPlaceholder(
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                isBooksEmpty = category.value.books.isEmpty(),
                emptyIcon = category.value.emptyIcon,
                emptyMessage = category.value.emptyMessage,
                navigateToBrowse = navigateToBrowse
            )
        }
    }
}