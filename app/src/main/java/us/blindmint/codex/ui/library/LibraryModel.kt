/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.book.SelectableBook
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.use_case.book.DeleteBooks
import us.blindmint.codex.domain.use_case.book.DeleteProgressHistoryUseCase
import us.blindmint.codex.domain.use_case.book.GetBooks
import us.blindmint.codex.domain.use_case.book.UpdateBook
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.history.HistoryScreen
import javax.inject.Inject

@HiltViewModel
class LibraryModel @Inject constructor(
    private val getBooks: GetBooks,
    private val bookRepository: BookRepository,
    private val deleteBooks: DeleteBooks,
    private val moveBooks: UpdateBook,
    private val deleteProgressHistory: DeleteProgressHistoryUseCase
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    val allSelectedBooksAreFavorites: Boolean
        get() = _state.value.books.filter { it.selected }.all { it.data.isFavorite }

    fun toggleSelectedBooksFavorite() {
        onEvent(LibraryEvent.OnToggleSelectedBooksFavorite)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            onEvent(
                LibraryEvent.OnRefreshList(
                    loading = true,
                    hideSearch = true
                )
            )
        }

        /* Observe channel - - - - - - - - - - - */
        viewModelScope.launch(Dispatchers.IO) {
            LibraryScreen.refreshListChannel.receiveAsFlow().collectLatest {
                delay(it)
                yield()

                onEvent(
                    LibraryEvent.OnRefreshList(
                        loading = false,
                        hideSearch = false
                    )
                )
            }
        }
        /* - - - - - - - - - - - - - - - - - - - */
    }

    private var refreshJob: Job? = null
    private var searchQueryChange: Job? = null

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.OnRefreshList -> {
                refreshJob?.cancel()
                refreshJob = viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            isRefreshing = true,
                            isLoading = event.loading,
                            showSearch = if (event.hideSearch) false else it.showSearch
                        )
                    }

                    yield()
                    getBooksFromDatabase()

                    delay(500)
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false
                        )
                    }
                }
            }

            is LibraryEvent.OnSearchVisibility -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (!event.show) {
                        onEvent(
                            LibraryEvent.OnRefreshList(
                                loading = false,
                                hideSearch = true
                            )
                        )
                    } else {
                        _state.update {
                            it.copy(
                                searchQuery = "",
                                hasFocused = false
                            )
                        }
                    }

                    _state.update {
                        it.copy(
                            showSearch = event.show
                        )
                    }
                }
            }

            is LibraryEvent.OnSearchQueryChange -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            searchQuery = event.query
                        )
                    }
                    searchQueryChange?.cancel()
                    searchQueryChange = launch(Dispatchers.IO) {
                        delay(500)
                        yield()
                        onEvent(LibraryEvent.OnSearch)
                    }
                }
            }

            is LibraryEvent.OnSearch -> {
                viewModelScope.launch(Dispatchers.IO) {
                    onEvent(
                        LibraryEvent.OnRefreshList(
                            loading = false,
                            hideSearch = false
                        )
                    )
                }
            }

            is LibraryEvent.OnRequestFocus -> {
                viewModelScope.launch(Dispatchers.Main) {
                    if (!_state.value.hasFocused) {
                        event.focusRequester.requestFocus()
                        _state.update {
                            it.copy(
                                hasFocused = true
                            )
                        }
                    }
                }
            }

            is LibraryEvent.OnClearSelectedBooks -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            books = it.books.map { book -> book.copy(selected = false) },
                            hasSelectedItems = false
                        )
                    }
                }
            }

            is LibraryEvent.OnSelectBook -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val editedList = _state.value.books.map {
                        if (it.data.id == event.id) it.copy(selected = event.select ?: !it.selected)
                        else it
                    }

                    _state.update {
                        it.copy(
                            books = editedList,
                            selectedItemsCount = editedList.filter { book -> book.selected }.size,
                            hasSelectedItems = editedList.any { book -> book.selected }
                        )
                    }
                }
            }

            is LibraryEvent.OnShowMoveDialog -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            dialog = LibraryScreen.MOVE_DIALOG
                        )
                    }
                }
            }

            is LibraryEvent.OnActionMoveDialog -> {
                viewModelScope.launch {
                    _state.value.books.forEach { book ->
                        if (!book.selected) return@forEach
                        moveBooks.execute(
                            book.data.copy(
                                category = event.selectedCategory
                            )
                        )
                    }

                    _state.update {
                        it.copy(
                            books = it.books.map { book ->
                                if (!book.selected) return@map book
                                book.copy(
                                    data = book.data.copy(
                                        category = event.selectedCategory
                                    ),
                                    selected = false
                                )
                            },
                            hasSelectedItems = false,
                            dialog = null
                        )
                    }

                    HistoryScreen.refreshListChannel.trySend(0)
                    LibraryScreen.scrollToPageCompositionChannel.trySend(
                        event.categories.dropLastWhile {
                            it.category != event.selectedCategory
                        }.lastIndex
                    )

                    withContext(Dispatchers.Main) {
                        event.context
                            .getString(R.string.books_moved)
                            .showToast(context = event.context)
                    }
                }
            }

            is LibraryEvent.OnShowDeleteDialog -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            dialog = LibraryScreen.DELETE_DIALOG
                        )
                    }
                }
            }

            is LibraryEvent.OnActionDeleteDialog -> {
                viewModelScope.launch {
                    deleteBooks.execute(
                        _state.value.books.mapNotNull {
                            if (!it.selected) return@mapNotNull null
                            it.data
                        }
                    )

                    _state.update {
                        it.copy(
                            books = it.books.filter { book -> !book.selected },
                            hasSelectedItems = false,
                            dialog = null
                        )
                    }

                    HistoryScreen.refreshListChannel.trySend(0)
                    BrowseScreen.refreshListChannel.trySend(Unit)

                    withContext(Dispatchers.Main) {
                        event.context
                            .getString(R.string.books_deleted)
                            .showToast(context = event.context)
                    }
                }
            }

        is LibraryEvent.OnShowClearProgressHistoryDialog -> {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        dialog = "clear_progress_history_dialog"
                    )
                }
            }
        }

        is LibraryEvent.OnActionClearProgressHistoryDialog -> {
            viewModelScope.launch(Dispatchers.IO) {
                _state.value.books.forEach { book ->
                    if (book.selected) {
                        deleteProgressHistory.execute(book.data)
                    }
                }

                _state.update {
                    it.copy(
                        dialog = null
                    )
                }

                withContext(Dispatchers.Main) {
                    event.context.getString(R.string.progress_history_cleared_bulk)
                        .showToast(context = event.context)
                }
            }
        }

        is LibraryEvent.OnDismissDialog -> {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        dialog = null
                    )
                }
            }
        }

        is LibraryEvent.OnShowSortMenu -> {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        showSortMenu = true
                    )
                }
            }
        }

        is LibraryEvent.OnDismissSortMenu -> {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        showSortMenu = false
                    )
                }
            }
        }

        is LibraryEvent.OnUpdateFilterState -> {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        filterState = event.filterState
                    )
                }
                // Refresh the list to apply filters
                onEvent(LibraryEvent.OnRefreshList(loading = false, hideSearch = false))
            }
        }

        is LibraryEvent.OnToggleSelectedBooksFavorite -> {
            viewModelScope.launch(Dispatchers.IO) {
                val selectedBooks = _state.value.books.filter { it.selected }
                val allAreFavorites = selectedBooks.all { it.data.isFavorite }
                val newFavoriteState = !allAreFavorites // If all are favorites, remove; if some aren't, add

                selectedBooks.forEach { book ->
                    if (book.data.isFavorite != newFavoriteState) {
                        moveBooks.execute(book.data.copy(isFavorite = newFavoriteState))
                    }
                }

                // Update the local state
                _state.update {
                    it.copy(
                        books = it.books.map { book ->
                            if (book.selected) {
                                book.copy(data = book.data.copy(isFavorite = newFavoriteState))
                            } else {
                                book
                            }
                        }
                    )
                }
            }
        }
        }
    }

    private suspend fun getBooksFromDatabase(
        query: String = if (_state.value.showSearch) _state.value.searchQuery else ""
    ) {
        val allBooks = getBooks.execute(query)
        val filteredBooks = applyFilters(allBooks, _state.value.filterState)
        val sortedBooks = filteredBooks
            .sortedWith(compareByDescending<Book> { it.lastOpened }.thenBy { it.title })
            .map { book -> SelectableBook(book, false) }

        _state.update {
            it.copy(
                books = sortedBooks,
                hasSelectedItems = false,
                isLoading = false
            )
        }
    }

    fun getSortedBooks(books: List<Book>, sortOrder: LibrarySortOrder?, descending: Boolean): List<Book> {
        if (sortOrder == null) return books

        val comparator = when (sortOrder) {
            LibrarySortOrder.NAME -> compareBy<Book> { it.title.toString() }
            LibrarySortOrder.LAST_READ -> compareBy<Book> { it.lastOpened ?: 0L }
            LibrarySortOrder.PROGRESS -> compareBy<Book> { it.progress }
            LibrarySortOrder.AUTHOR -> compareBy<Book> { it.author.toString() }
        }

        return if (descending) {
            books.sortedWith(comparator.reversed())
        } else {
            books.sortedWith(comparator)
        }
    }

    fun loadMetadata(
        onTagsLoaded: (List<String>) -> Unit,
        onAuthorsLoaded: (List<String>) -> Unit,
        onSeriesLoaded: (List<String>) -> Unit,
        onLanguagesLoaded: (List<String>) -> Unit,
        onYearRangeLoaded: (Int, Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tags = bookRepository.getAllTags()
                val authors = bookRepository.getAllAuthors()
                val series = bookRepository.getAllSeries()
                val languages = bookRepository.getAllLanguages()
                val (minYear, maxYear) = bookRepository.getPublicationYearRange()

                withContext(Dispatchers.Main) {
                    onTagsLoaded(tags)
                    onAuthorsLoaded(authors)
                    onSeriesLoaded(series)
                    onLanguagesLoaded(languages)
                    onYearRangeLoaded(minYear, maxYear)
                }
            } catch (e: Exception) {
                Log.e("LibraryModel", "Error loading metadata", e)
            }
        }
    }

    private fun applyFilters(books: List<Book>, filterState: FilterState): List<Book> {
        return books.filter { book ->
            // Filter by selected statuses (Reading, Planning, Already Read)
            val statusMatch = filterState.selectedStatuses.isEmpty() ||
                filterState.selectedStatuses.any { selectedStatus ->
                    when (selectedStatus) {
                        "Reading" -> book.category.name == "READING"
                        "Planning" -> book.category.name == "PLANNING"
                        "Already Read" -> book.category.name == "ALREADY_READ"
                        else -> false
                    }
                }

            // Filter by selected tags (if any selected, book must have at least one)
            val tagsMatch = filterState.selectedTags.isEmpty() ||
                filterState.selectedTags.any { selectedTag ->
                    book.tags.any { bookTag -> bookTag.equals(selectedTag, ignoreCase = true) }
                }

            // Filter by selected authors
            val authorsMatch = filterState.selectedAuthors.isEmpty() ||
                filterState.selectedAuthors.any { selectedAuthor ->
                    book.author.getAsString()?.equals(selectedAuthor, ignoreCase = true) == true
                }

            // Filter by selected series
            val seriesMatch = filterState.selectedSeries.isEmpty() ||
                filterState.selectedSeries.any { selectedSeries ->
                    book.seriesName?.equals(selectedSeries, ignoreCase = true) == true
                }

            // Filter by selected languages
            val languagesMatch = filterState.selectedLanguages.isEmpty() ||
                filterState.selectedLanguages.any { selectedLanguage ->
                    book.language?.equals(selectedLanguage, ignoreCase = true) == true
                }

            // Filter by publication year range
            val yearMatch = if (book.publicationDate != null) {
                val bookYear = java.time.Instant.ofEpochMilli(book.publicationDate).atZone(java.time.ZoneId.systemDefault()).year
                bookYear in filterState.publicationYearRange
            } else {
                true // Include books without publication date
            }

            statusMatch && tagsMatch && authorsMatch && seriesMatch && languagesMatch && yearMatch
        }
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}