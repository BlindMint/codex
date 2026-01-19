/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

import android.os.Parcelable
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.LibraryTab
import us.blindmint.codex.domain.library.LibraryTabWithBooks
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.presentation.library.LibraryContent
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.book_info.BookInfoScreen
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.main.MainModel
import us.blindmint.codex.ui.reader.ReaderScreen
import us.blindmint.codex.ui.settings.BrowseSettingsScreen

@Parcelize
object LibraryScreen : Screen, Parcelable {

    @IgnoredOnParcel
    const val MOVE_DIALOG = "move_dialog"

    @IgnoredOnParcel
    const val DELETE_DIALOG = "delete_dialog"

    @IgnoredOnParcel
    val refreshListChannel: Channel<Long> = Channel(Channel.CONFLATED)

    @IgnoredOnParcel
    val scrollToPageCompositionChannel: Channel<Int> = Channel(Channel.CONFLATED)

    @IgnoredOnParcel
    private var initialPage = 0

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        val screenModel = hiltViewModel<LibraryModel>()
        val mainModel = hiltViewModel<MainModel>()

        val state = screenModel.state.collectAsStateWithLifecycle()
        val mainState = mainModel.state.collectAsStateWithLifecycle()

        val sortedBooks = remember(state.value.books, mainState.value.librarySortOrder, mainState.value.librarySortOrderDescending) {
            val sortOrder = mainState.value.librarySortOrder
            val isDescending = mainState.value.librarySortOrderDescending

            when (sortOrder) {
                LibrarySortOrder.NAME -> {
                    if (isDescending) {
                        state.value.books.sortedByDescending { it.data.title.toString() }
                    } else {
                        state.value.books.sortedBy { it.data.title.toString() }
                    }
                }
                LibrarySortOrder.LAST_READ -> {
                    if (isDescending) {
                        state.value.books.sortedByDescending { it.data.lastOpened }
                    } else {
                        state.value.books.sortedBy { it.data.lastOpened }
                    }
                }
                LibrarySortOrder.PROGRESS -> {
                    if (isDescending) {
                        state.value.books.sortedByDescending { it.data.progress }
                    } else {
                        state.value.books.sortedBy { it.data.progress }
                    }
                }
                LibrarySortOrder.AUTHOR -> {
                    if (isDescending) {
                        state.value.books.sortedByDescending { it.data.author.toString() }
                    } else {
                        state.value.books.sortedBy { it.data.author.toString() }
                    }
                }
            }
        }

        val skullPainter = painterResource(id = R.drawable.skull_medium)

        val tabs = remember(sortedBooks) {
            derivedStateOf {
                // Book file extensions for filtering
                val bookExtensions = setOf("epub", "pdf", "azw3", "mobi", "fb2", "txt", "html", "htm", "md")
                val comicExtensions = setOf("cbr", "cbz", "cb7")

                listOf(
                    LibraryTabWithBooks(
                        tab = LibraryTab.ALL,
                        title = UIText.StringResource(R.string.reading_tab), // "All"
                        books = sortedBooks,
                        emptyIcon = skullPainter,
                        emptyMessage = UIText.StringResource(R.string.library_reading_empty)
                    ),
                    LibraryTabWithBooks(
                        tab = LibraryTab.BOOKS,
                        title = UIText.StringResource(R.string.planning_tab), // "Books"
                        books = sortedBooks.filter { book ->
                            !book.data.isComic
                        },
                        emptyIcon = skullPainter,
                        emptyMessage = UIText.StringResource(R.string.library_planning_empty)
                    ),
                    LibraryTabWithBooks(
                        tab = LibraryTab.COMICS,
                        title = UIText.StringResource(R.string.already_read_tab), // "Comics"
                        books = sortedBooks.filter { book ->
                            book.data.isComic
                        },
                        emptyIcon = skullPainter,
                        emptyMessage = UIText.StringResource(R.string.library_already_read_empty)
                    ),
                    LibraryTabWithBooks(
                        tab = LibraryTab.FAVORITES,
                        title = UIText.StringResource(R.string.favorites_tab), // "Favorites"
                        books = sortedBooks.filter { it.data.isFavorite },
                        emptyIcon = skullPainter,
                        emptyMessage = UIText.StringResource(R.string.library_favorites_empty)
                    )
                )
            }
        }

        val focusRequester = remember { FocusRequester() }
        val refreshState = rememberPullRefreshState(
            refreshing = state.value.isRefreshing,
            onRefresh = {
                screenModel.onEvent(
                    LibraryEvent.OnRefreshList(
                        loading = false,
                        hideSearch = true
                    )
                )
            }
        )

        val pagerState = rememberPagerState(
            initialPage = initialPage
        ) { tabs.value.count() }
        DisposableEffect(Unit) { onDispose { initialPage = pagerState.currentPage } }

        LaunchedEffect(Unit) {
            scrollToPageCompositionChannel.receiveAsFlow().collectLatest {
                pagerState.animateScrollToPage(it)
            }
        }

        LibraryContent(
            books = sortedBooks,
            selectedItemsCount = state.value.selectedItemsCount,
            hasSelectedItems = state.value.hasSelectedItems,
            showSearch = state.value.showSearch,
            searchQuery = state.value.searchQuery,
            bookCount = state.value.books.count(),
            showSortMenu = state.value.showSortMenu,
            focusRequester = focusRequester,
            pagerState = pagerState,
            isLoading = state.value.isLoading,
            isRefreshing = state.value.isRefreshing,
            doublePressExit = mainState.value.doublePressExit,
            categories = tabs.value,
            refreshState = refreshState,
            dialog = state.value.dialog,
            libraryShowCategoryTabs = mainState.value.libraryShowCategoryTabs,
            libraryShowBookCount = mainState.value.libraryShowBookCount,
            selectBook = screenModel::onEvent,
            searchVisibility = screenModel::onEvent,
            requestFocus = screenModel::onEvent,
            searchQueryChange = screenModel::onEvent,
            search = screenModel::onEvent,
            clearSelectedBooks = screenModel::onEvent,
            showMoveDialog = screenModel::onEvent,
            actionMoveDialog = screenModel::onEvent,
            actionDeleteDialog = screenModel::onEvent,
            showDeleteDialog = screenModel::onEvent,
            showClearProgressHistoryDialog = screenModel::onEvent,
            dismissDialog = screenModel::onEvent,
            sortMenuVisibility = screenModel::onEvent,
            allSelectedBooksAreFavorites = screenModel.allSelectedBooksAreFavorites,
            toggleSelectedBooksFavorite = screenModel::toggleSelectedBooksFavorite,
            allBooksSelected = state.value.selectedItemsCount == state.value.books.count() && state.value.hasSelectedItems,
            selectAllBooks = screenModel::onEvent,
            navigateToBrowse = {
                navigator.push(BrowseScreen)
            },
            navigateToStorage = {
                navigator.push(BrowseSettingsScreen)
            },
            navigateToReader = {
                HistoryScreen.insertHistoryChannel.trySend(it)
                navigator.push(ReaderScreen(it))
            },
            navigateToBookInfo = {
                navigator.push(BookInfoScreen(bookId = it))
            }
        )
    }
}