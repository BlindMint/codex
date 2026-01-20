/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.book_info

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.R
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.use_case.book.CanResetCover
import us.blindmint.codex.domain.use_case.book.DeleteBooks
import us.blindmint.codex.domain.use_case.book.DeleteProgressHistoryUseCase
import us.blindmint.codex.domain.use_case.book.GetBookById
import us.blindmint.codex.domain.use_case.book.ResetCoverImage
import us.blindmint.codex.domain.use_case.book.UpdateBook
import us.blindmint.codex.domain.use_case.opds.RefreshBookMetadataFromOpds
import us.blindmint.codex.domain.use_case.book.UpdateCoverImageOfBook
import us.blindmint.codex.domain.repository.OpdsRepository
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryScreen
import javax.inject.Inject

@HiltViewModel
class BookInfoModel @Inject constructor(
    private val getBookById: GetBookById,
    private val updateBook: UpdateBook,
    private val canResetCover: CanResetCover,
    private val updateCoverImageOfBook: UpdateCoverImageOfBook,
    private val resetCoverImage: ResetCoverImage,
    private val deleteBooks: DeleteBooks,
    private val deleteProgressHistory: DeleteProgressHistoryUseCase,
    private val refreshBookMetadataFromOpds: RefreshBookMetadataFromOpds,
    private val opdsRepository: OpdsRepository
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(BookInfoState())
    val state = _state.asStateFlow()

    private var eventJob = SupervisorJob()
    private var resetJob: Job? = null

    fun onEvent(event: BookInfoEvent) {
        viewModelScope.launch(eventJob + Dispatchers.Main) {
            when (event) {
                is BookInfoEvent.OnShowDetailsBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = BookInfoScreen.DETAILS_BOTTOM_SHEET
                        )
                    }
                }

                is BookInfoEvent.OnShowEditBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = BookInfoScreen.EDIT_BOTTOM_SHEET
                        )
                    }
                }

                is BookInfoEvent.OnShowChangeCoverBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = BookInfoScreen.CHANGE_COVER_BOTTOM_SHEET
                        )
                    }
                }

                is BookInfoEvent.OnChangeCover -> {
                    launch {
                        val image = event.context.contentResolver?.openInputStream(event.uri)?.use {
                            BitmapFactory.decodeStream(it)
                        } ?: return@launch

                        updateCoverImageOfBook.execute(
                            _state.value.book,
                            image
                        )

                        val newCoverImage = getBookById.execute(
                            _state.value.book.id
                        )?.coverImage ?: return@launch

                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    coverImage = newCoverImage
                                ),
                                bottomSheet = null,
                                canResetCover = canResetCover.execute(bookId = it.book.id)
                            )
                        }

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.cover_image_changed)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnResetCover -> {
                    launch {
                        val result = resetCoverImage.execute(_state.value.book.id)

                        if (!result) {
                            withContext(Dispatchers.Main) {
                                event.context.getString(R.string.error_could_not_reset_cover)
                                    .showToast(context = event.context)
                            }
                            return@launch
                        }

                        val book = getBookById.execute(_state.value.book.id)

                        if (book == null) {
                            withContext(Dispatchers.Main) {
                                event.context.getString(R.string.error_something_went_wrong)
                                    .showToast(context = event.context)
                            }
                            return@launch
                        }

                        _state.update {
                            it.copy(
                                book = book,
                                bottomSheet = null,
                                canResetCover = false
                            )
                        }

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.cover_reset)
                                .showToast(context = event.context)
                        }

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)
                    }
                }

                is BookInfoEvent.OnDeleteCover -> {
                    launch {
                        if (_state.value.book.coverImage == null) {
                            return@launch
                        }

                        updateCoverImageOfBook.execute(
                            bookWithOldCover = _state.value.book,
                            newCoverImage = null
                        )
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    coverImage = null
                                ),
                                bottomSheet = null,
                                canResetCover = canResetCover.execute(bookId = it.book.id)
                            )
                        }

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.cover_image_deleted)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnCheckCoverReset -> {
                    launch(Dispatchers.IO) {
                        if (_state.value.book.id == -1) return@launch
                        canResetCover.execute(_state.value.book.id).apply {
                            _state.update {
                                it.copy(
                                    canResetCover = this
                                )
                            }
                        }
                    }
                }

                is BookInfoEvent.OnDismissBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = null
                        )
                    }
                }

                is BookInfoEvent.OnShowTitleDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.TITLE_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionTitleDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    title = event.title
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.title_changed)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnShowAuthorDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.AUTHOR_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionAuthorDialog -> {
                    launch {
                        val authorString = (event.author as? us.blindmint.codex.domain.ui.UIText.StringValue)?.value
                            ?: if (event.author is us.blindmint.codex.domain.ui.UIText.StringResource) "" else ""
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    authors = if (authorString.isNotEmpty()) listOf(authorString) else emptyList()
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.author_changed)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnShowDescriptionDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.DESCRIPTION_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionDescriptionDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    description = event.description
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.description_changed)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnShowPathDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.PATH_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionPathDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    filePath = event.path
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.path_changed)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnShowDeleteDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.DELETE_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnShowResetProgressDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.RESET_PROGRESS_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionDeleteDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                dialog = null,
                                bottomSheet = null
                            )
                        }

                        deleteBooks.execute(listOf(_state.value.book))

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)
                        BrowseScreen.refreshListChannel.trySend(Unit)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.book_deleted)
                                .showToast(context = event.context)
                        }

                        event.navigateBack()
                    }
                }

                is BookInfoEvent.OnActionResetProgressDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                dialog = null
                            )
                        }
                        onEvent(BookInfoEvent.OnResetReadingProgress(event.context))
                    }
                }

                is BookInfoEvent.OnShowMoveDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.MOVE_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionMoveDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                dialog = null,
                                bottomSheet = null
                            )
                        }

                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    category = event.category
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        LibraryScreen.scrollToPageCompositionChannel.trySend(
                            Category.entries.dropLastWhile {
                                it != event.category
                            }.size - 1
                        )
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.book_moved)
                                .showToast(context = event.context)
                        }

                        event.navigateToLibrary()
                    }
                }

                is BookInfoEvent.OnClearProgressHistory -> {
                    launch(Dispatchers.IO) {
                        deleteProgressHistory.execute(_state.value.book)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.progress_history_cleared)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnResetReadingProgress -> {
                    launch {
                        val updatedBook = _state.value.book.copy(
                            scrollIndex = 0,
                            scrollOffset = 0,
                            progress = 0f,
                            currentPage = 0,
                            lastPageRead = 0
                        )
                        updateBook.execute(updatedBook)
                        _state.update {
                            it.copy(book = updatedBook)
                        }

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.reading_progress_reset)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnDismissDialog -> {
                    _state.update {
                        it.copy(
                            dialog = null
                        )
                    }
                }

                is BookInfoEvent.OnResetTitle -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnResetAuthor -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnResetDescription -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnShowTagsDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.TAGS_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionTagsDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    tags = event.tags
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            "Tags updated".showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnResetTags -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnShowSeriesDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.SERIES_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionSeriesDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    series = event.series
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            "Series updated".showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnResetSeries -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnShowLanguagesDialog -> {
                    _state.update {
                        it.copy(
                            dialog = BookInfoScreen.LANGUAGES_DIALOG
                        )
                    }
                }

                is BookInfoEvent.OnActionLanguagesDialog -> {
                    launch {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    languages = event.languages
                                )
                            )
                        }
                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        withContext(Dispatchers.Main) {
                            "Languages updated".showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnResetLanguages -> {
                    withContext(Dispatchers.Main) {
                        event.context.getString(R.string.reset_no_original)
                            .showToast(context = event.context)
                    }
                }

                is BookInfoEvent.OnRefreshMetadataFromOpds -> {
                    launch(Dispatchers.IO) {
                        val currentBook = _state.value.book
                        if (currentBook.opdsSourceUrl == null) {
                            withContext(Dispatchers.Main) {
                                "This book has no OPDS source".showToast(context = event.context)
                            }
                            return@launch
                        }

                        try {
                            val refreshedBook = refreshBookMetadataFromOpds.execute(currentBook) { uuid, isbn ->
                                // Find OPDS entry by UUID or ISBN from the book's OPDS source
                                try {
                                    val feed = opdsRepository.fetchFeed(currentBook.opdsSourceUrl!!)
                                    feed.entries.firstOrNull { entry ->
                                        (uuid != null && entry.id == uuid) ||
                                        (isbn != null && entry.identifiers.any { it == isbn })
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (refreshedBook != null) {
                                _state.update {
                                    it.copy(book = refreshedBook)
                                }
                                updateBook.execute(refreshedBook)
                            }

                            LibraryScreen.refreshListChannel.trySend(0)
                            HistoryScreen.refreshListChannel.trySend(0)

                            withContext(Dispatchers.Main) {
                                "Metadata refreshed from OPDS".showToast(context = event.context)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                "Failed to refresh metadata: ${e.message}".showToast(context = event.context)
                            }
                        }
                    }
                }

                is BookInfoEvent.OnToggleFavorite -> {
                    val currentBook = _state.value.book
                    val updatedBook = currentBook.copy(isFavorite = !currentBook.isFavorite)
                    updateBook.execute(updatedBook)
                    _state.update { it.copy(book = updatedBook) }
                    LibraryScreen.refreshListChannel.trySend(0)
                }

                is BookInfoEvent.OnEnterEditMode -> {
                    _state.update {
                        it.copy(
                            isEditingMetadata = true,
                            editedBook = it.book.copy()
                        )
                    }
                }

                is BookInfoEvent.OnUpdateEditedBook -> {
                    _state.update {
                        it.copy(
                            editedBook = event.updatedBook
                        )
                    }
                }

                is BookInfoEvent.OnConfirmEditMetadata -> {
                    _state.update {
                        it.copy(
                            showConfirmSaveDialog = true
                        )
                    }
                }

                is BookInfoEvent.OnCancelEditMetadata -> {
                    _state.update {
                        it.copy(
                            showConfirmCancelDialog = true
                        )
                    }
                }

                is BookInfoEvent.OnSilentCancelEditMetadata -> {
                    _state.update {
                        it.copy(
                            isEditingMetadata = false,
                            editedBook = null,
                            showConfirmCancelDialog = false
                        )
                    }
                }

                is BookInfoEvent.OnConfirmSaveChanges -> {
                    launch {
                        val bookToSave = _state.value.editedBook ?: return@launch
                        updateBook.execute(bookToSave)

                        _state.update {
                            it.copy(
                                book = bookToSave,
                                isEditingMetadata = false,
                                editedBook = null,
                                showConfirmSaveDialog = false
                            )
                        }

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)
                        BrowseScreen.refreshListChannel.trySend(Unit)

                        withContext(Dispatchers.Main) {
                            event.context.getString(R.string.metadata_saved)
                                .showToast(context = event.context)
                        }
                    }
                }

                is BookInfoEvent.OnDismissSaveDialog -> {
                    _state.update {
                        it.copy(
                            showConfirmSaveDialog = false
                        )
                    }
                }

                is BookInfoEvent.OnDismissCancelDialog -> {
                    _state.update {
                        it.copy(
                            showConfirmCancelDialog = false,
                            isEditingMetadata = false,
                            editedBook = null
                        )
                    }
                }

                is BookInfoEvent.OnChangeCategory -> {
                    launch {
                        val updatedBook = _state.value.book.copy(category = event.category)
                        updateBook.execute(updatedBook)

                        _state.update {
                            it.copy(
                                book = updatedBook
                            )
                        }

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)
                        BrowseScreen.refreshListChannel.trySend(Unit)
                    }
                }
            }
        }
    }

    fun init(
        bookId: Int,
        changePath: Boolean,
        navigateBack: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val book = getBookById.execute(bookId)

            if (book == null) {
                navigateBack()
                return@launch
            }

            eventJob.cancel()
            resetJob?.cancel()
            eventJob.join()
            resetJob?.join()
            eventJob = SupervisorJob()

            _state.update {
                BookInfoState(
                    book = book
                )
            }

            if (changePath) {
                onEvent(BookInfoEvent.OnShowPathDialog)
            }
            onEvent(BookInfoEvent.OnCheckCoverReset)
        }
    }

    fun resetScreen() {
        resetJob = viewModelScope.launch(Dispatchers.Main) {
            eventJob.cancel()
            eventJob = SupervisorJob()

            yield()
            _state.update { BookInfoState() }
        }
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}