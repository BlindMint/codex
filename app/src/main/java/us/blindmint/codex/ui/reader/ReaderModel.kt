/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.app.SearchManager
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.R
import us.blindmint.codex.domain.dictionary.DictionarySource
import us.blindmint.codex.domain.reader.Checkpoint
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.reader.SearchResult
import us.blindmint.codex.domain.ui.UIText
import us.blindmint.codex.domain.use_case.book.GetBookById
import us.blindmint.codex.domain.use_case.book.GetText
import us.blindmint.codex.domain.use_case.book.UpdateBook
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.use_case.bookmark.GetBookmarksByBookId
import us.blindmint.codex.domain.use_case.bookmark.InsertBookmark
import us.blindmint.codex.domain.use_case.bookmark.DeleteBookmark
import us.blindmint.codex.domain.use_case.bookmark.DeleteBookmarksByBookId
import us.blindmint.codex.domain.use_case.history.GetLatestHistory
import us.blindmint.codex.presentation.core.util.coerceAndPreventNaN
import us.blindmint.codex.presentation.core.util.launchActivity
import us.blindmint.codex.presentation.core.util.setBrightness
import us.blindmint.codex.presentation.core.util.showToast
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryScreen
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.roundToInt

private const val READER = "READER, MODEL"

@HiltViewModel
class ReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val updateBook: UpdateBook,
    private val getText: GetText,
    private val getLatestHistory: GetLatestHistory,
    private val getBookmarksByBookId: GetBookmarksByBookId,
    private val insertBookmark: InsertBookmark,
    private val deleteBookmark: DeleteBookmark,
    private val deleteBookmarksByBookId: DeleteBookmarksByBookId,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private var eventJob = SupervisorJob()
    private var resetJob: Job? = null

    // Flag to prevent resetScreen() from resetting state when init() is about to run.
    // When transitioning between ReaderScreens, DisposableEffect.onDispose calls resetScreen()
    // on the old screen and LaunchedEffect calls init() on the new screen. These race on
    // different threads (Main vs IO) and resetScreen can corrupt state mid-init.
    @Volatile
    private var isInitializing = false

    private var scrollJob: Job? = null

    fun onEvent(event: ReaderEvent) {
        viewModelScope.launch(eventJob + Dispatchers.Main) {
            when (event) {
                is ReaderEvent.OnLoadText -> {
                    // Capture book id on Main before dispatching to IO to avoid
                    // race with resetScreen() which can reset state between dispatch
                    val currentBookId = _state.value.book.id
                    launch(Dispatchers.IO) {
                        val text = getText.execute(currentBookId)
                        yield()

                        if (text.isEmpty()) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UIText.StringResource(R.string.error_could_not_get_text)
                                )
                            }
                            systemBarsVisibility(show = true, activity = event.activity)
                            return@launch
                        }

                        systemBarsVisibility(
                            show = !event.fullscreenMode,
                            activity = event.activity
                        )

                        val lastOpened = getLatestHistory.execute(currentBookId)?.time
                        yield()

                        // Invalidate word cache when new text is loaded
                        invalidateWordCache()

                        _state.update {
                            it.copy(
                                showMenu = false,
                                book = it.book.copy(
                                    lastOpened = lastOpened
                                ),
                                text = text
                            )
                        }

                        yield()

                        // Read the book from state immediately after the update above,
                        // before any yield that could allow resetScreen to corrupt it
                        val bookForUpdate = _state.value.book
                        updateBook.execute(bookForUpdate)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        launch {
                            snapshotFlow {
                                _state.value.listState.layoutInfo.totalItemsCount
                            }.collectLatest { itemsCount ->
                                if (itemsCount == 0) return@collectLatest // Wait for list to start loading

                                _state.value.book.apply {
                                    // Debug logging for progress loading
                                    Log.d("READER", "Loading book: progress=$progress, scrollIndex=$scrollIndex, scrollOffset=$scrollOffset")
                                    Log.d("READER", "Text has ${_state.value.text.size} items (lastIndex: ${_state.value.text.lastIndex})")

                                    val finalScrollIndex: Int
                                    val finalScrollOffset: Int

                                  if (scrollOffset > 0) {
                                      // Normal reader saved precise position - use it
                                      finalScrollIndex = scrollIndex
                                      finalScrollOffset = scrollOffset
                                      Log.d("READER", "Using saved position: scrollIndex=$finalScrollIndex, scrollOffset=$finalScrollOffset")
                                  } else {
                                      // Use progress as fraction of total text items
                                      finalScrollIndex = (progress * _state.value.text.lastIndex).toInt()
                                          .coerceIn(0, _state.value.text.lastIndex)
                                      finalScrollOffset = 0
                                      Log.d("READER", "Converting progress to position: progress=$progress * text.lastIndex=${_state.value.text.lastIndex} = $finalScrollIndex")
                                  }

                                    Log.d("READER", "Final scroll position: index=$finalScrollIndex, offset=$finalScrollOffset")

                                  _state.value.listState.requestScrollToItem(
                                      finalScrollIndex,
                                      finalScrollOffset
                                  )
                                  updateChapter(index = finalScrollIndex)

                                  // Wait for scroll animation to actually complete before hiding loading
                                  // This ensures smooth transition without visible jump
                                  snapshotFlow {
                                      val layoutInfo = _state.value.listState.layoutInfo
                                      val isAtTarget = layoutInfo.visibleItemsInfo.any { it.index == finalScrollIndex } ||
                                                       finalScrollIndex >= layoutInfo.totalItemsCount - 1
                                      val isNotScrolling = !_state.value.listState.isScrollInProgress
                                      isAtTarget && isNotScrolling
                                  }.first { complete -> complete }
                              }

                              _state.update {
                                  it.copy(
                                      isLoading = false,
                                      isScrollRestorationComplete = true,
                                      errorMessage = null
                                  )
                              }

                                // Load bookmarks for the current book
                                launch(Dispatchers.IO) {
                                    val bookmarks = getBookmarksByBookId.execute(currentBookId)
                                    _state.update {
                                        it.copy(bookmarks = bookmarks)
                                    }
                                }

                                return@collectLatest
                            }
                        }
                    }
                }

                is ReaderEvent.OnMenuVisibility -> {
                    launch {
                        if (_state.value.lockMenu) return@launch

                        yield()

                        systemBarsVisibility(
                            show = event.show || !event.fullscreenMode,
                            activity = event.activity
                        )
                        _state.update {
                            it.copy(
                                showMenu = event.show,
                                showSearch = event.show && it.searchBarPersistent,
                                checkpoint = _state.value.listState.run {
                                    if (!event.show || !event.saveCheckpoint) return@run it.checkpoint

                                    Checkpoint(firstVisibleItemIndex, firstVisibleItemScrollOffset)
                                }
                            )
                        }
                    }
                }

                is ReaderEvent.OnChangeProgress -> {
                    launch(Dispatchers.IO) {
                        val currentBook = _state.value.book
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    progress = event.progress,
                                    scrollIndex = event.firstVisibleItemIndex,
                                    scrollOffset = event.firstVisibleItemOffset
                                )
                            )
                        }

                        bookRepository.updateNormalReaderProgress(
                            bookId = currentBook.id,
                            scrollIndex = event.firstVisibleItemIndex,
                            scrollOffset = event.firstVisibleItemOffset,
                            progress = event.progress
                        )

                        LibraryScreen.refreshListChannel.trySend(300)
                        HistoryScreen.refreshListChannel.trySend(300)
                    }
                }

                is ReaderEvent.OnScrollToChapter -> {
                    launch {
                        _state.value.apply {
                            val chapterIndex = text.indexOf(event.chapter).takeIf { it != -1 }
                            if (chapterIndex == null) {
                                return@launch
                            }

                            listState.requestScrollToItem(chapterIndex)
                            updateChapter(index = chapterIndex)
                            onEvent(
                                ReaderEvent.OnChangeProgress(
                                    progress = calculateProgress(chapterIndex),
                                    firstVisibleItemIndex = chapterIndex,
                                    firstVisibleItemOffset = 0
                                )
                            )
                        }
                    }
                }

                is ReaderEvent.OnScrollToBookmark -> {
                    launch {
                        _state.value.apply {
                            listState.requestScrollToItem(
                                event.scrollIndex,
                                event.scrollOffset
                            )
                            updateChapter(index = event.scrollIndex)
                            onEvent(
                                ReaderEvent.OnChangeProgress(
                                    progress = calculateProgress(event.scrollIndex),
                                    firstVisibleItemIndex = event.scrollIndex,
                                    firstVisibleItemOffset = event.scrollOffset
                                )
                            )
                        }
                    }
                }

                is ReaderEvent.OnScroll -> {
                    scrollJob?.cancel()
                    scrollJob = launch {
                        delay(300)
                        yield()

                        val scrollTo = (_state.value.text.lastIndex * event.progress).roundToInt()
                        _state.value.listState.requestScrollToItem(scrollTo)
                        updateChapter(scrollTo)
                    }
                }

                is ReaderEvent.OnRestoreCheckpoint -> {
                    launch {
                        _state.value.apply {
                            listState.requestScrollToItem(
                                checkpoint.index,
                                checkpoint.offset
                            )

                            updateChapter(checkpoint.index)
                            onEvent(
                                ReaderEvent.OnChangeProgress(
                                    progress = calculateProgress(checkpoint.index),
                                    firstVisibleItemIndex = checkpoint.index,
                                    firstVisibleItemOffset = checkpoint.offset,
                                )
                            )
                        }
                    }
                }

                is ReaderEvent.OnLeave -> {
                    launch {
                        yield()

                        _state.update {
                            it.copy(
                                lockMenu = true
                            )
                        }

                        // Save position for text books
                        _state.value.listState.apply {
                            if (
                                !_state.value.book.isPageBased &&
                                !_state.value.isLoading &&
                                layoutInfo.totalItemsCount >= 1 &&
                                _state.value.text.isNotEmpty() &&
                                _state.value.errorMessage == null
                            ) {
                                    val currentBook = _state.value.book
                                    val firstVisibleItemIndex = _state.value.listState.firstVisibleItemIndex
                                    val firstVisibleItemScrollOffset = _state.value.listState.firstVisibleItemScrollOffset
                                    val textLastIndex = _state.value.text.lastIndex.coerceAtLeast(1)
                                    val newProgress = (firstVisibleItemIndex.toFloat() / textLastIndex).coerceIn(0f, 1f)

                                    _state.update {
                                        it.copy(
                                            book = it.book.copy(
                                                progress = newProgress,
                                                scrollIndex = firstVisibleItemIndex,
                                                scrollOffset = firstVisibleItemScrollOffset
                                            )
                                        )
                                    }

                        bookRepository.updateNormalReaderProgress(
                            bookId = currentBook.id,
                            scrollIndex = firstVisibleItemIndex,
                            scrollOffset = firstVisibleItemScrollOffset,
                            progress = newProgress
                        )
                            }
                        }

                        // Save position for page-based books (comics and PDFs)
                                if (_state.value.book.isPageBased && !_state.value.isLoading) {
                                    val currentBook = _state.value.book
                                    val totalComicPages = _state.value.totalComicPages
                                    val currentComicPage = _state.value.currentComicPage
                                    val newProgress = if (totalComicPages > 0) {
                                        (currentComicPage + 1).toFloat() / totalComicPages
                                    } else 0f

                                    _state.update {
                                        it.copy(
                                            book = it.book.copy(
                                                currentPage = currentComicPage,
                                                lastPageRead = currentComicPage,
                                                 progress = newProgress
                                            )
                                        )
                                    }

                        bookRepository.updateNormalReaderProgress(
                            bookId = currentBook.id,
                            scrollIndex = currentBook.scrollIndex,
                            scrollOffset = currentBook.scrollOffset,
                            progress = newProgress
                        )
                        }

                        WindowCompat.getInsetsController(
                            event.activity.window,
                            event.activity.window.decorView
                        ).show(WindowInsetsCompat.Type.systemBars())
                        event.activity.setBrightness(brightness = null)

                        event.navigate()
                    }
                }

                is ReaderEvent.OnOpenTranslator -> {
                    launch {
                        val translatorIntent = Intent()
                        val browserIntent = Intent()

                        translatorIntent.type = "text/plain"
                        translatorIntent.action = Intent.ACTION_PROCESS_TEXT
                        browserIntent.action = Intent.ACTION_WEB_SEARCH

                        translatorIntent.putExtra(
                            Intent.EXTRA_PROCESS_TEXT,
                            event.textToTranslate
                        )
                        translatorIntent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                        browserIntent.putExtra(
                            SearchManager.QUERY,
                            "translate: ${event.textToTranslate.trim()}"
                        )

                        yield()

                        translatorIntent.launchActivity(
                            activity = event.activity,
                            createChooser = !event.translateWholeParagraph,
                            success = {
                                return@launch
                            }
                        )
                        browserIntent.launchActivity(
                            activity = event.activity,
                            success = {
                                return@launch
                            }
                        )

                        withContext(Dispatchers.Main) {
                            event.activity.getString(R.string.error_no_translator)
                                .showToast(context = event.activity, longToast = false)
                        }
                    }
                }

                is ReaderEvent.OnOpenShareApp -> {
                    launch {
                        val shareIntent = Intent()

                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(
                            Intent.EXTRA_SUBJECT,
                            event.activity.getString(R.string.app_name)
                        )
                        shareIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            event.textToShare.trim()
                        )

                        yield()

                        shareIntent.launchActivity(
                            activity = event.activity,
                            createChooser = true,
                            success = {
                                return@launch
                            }
                        )

                        withContext(Dispatchers.Main) {
                            event.activity.getString(R.string.error_no_share_app)
                                .showToast(context = event.activity, longToast = false)
                        }
                    }
                }

                is ReaderEvent.OnOpenWebBrowser -> {
                    launch {
                        val browserIntent = Intent()

                        browserIntent.action = Intent.ACTION_WEB_SEARCH
                        browserIntent.putExtra(
                            SearchManager.QUERY,
                            event.textToSearch
                        )

                        yield()

                        browserIntent.launchActivity(
                            activity = event.activity,
                            success = {
                                return@launch
                            }
                        )

                        withContext(Dispatchers.Main) {
                            event.activity.getString(R.string.error_no_browser)
                                .showToast(context = event.activity, longToast = false)
                        }
                    }
                }

                is ReaderEvent.OnOpenDictionary -> {
                    launch {
                        val word = event.textToDefine.trim()
                        val encodedWord = URLEncoder.encode(word, "UTF-8")

                        // Try system dictionary app first if SYSTEM_DEFAULT is selected
                        if (event.dictionarySource == DictionarySource.SYSTEM_DEFAULT) {
                            val dictionaryIntent = Intent().apply {
                                type = "text/plain"
                                action = Intent.ACTION_PROCESS_TEXT
                                putExtra(Intent.EXTRA_PROCESS_TEXT, word)
                                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                            }

                            yield()

                            dictionaryIntent.launchActivity(
                                activity = event.activity,
                                createChooser = true,
                                success = {
                                    return@launch
                                }
                            )

                            // Fallback to OneLook if no dictionary app found
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = "https://www.onelook.com/?w=$encodedWord".toUri()
                            }
                            fallbackIntent.launchActivity(
                                activity = event.activity,
                                success = {
                                    return@launch
                                }
                            )
                        } else {
                            // Use the selected web dictionary source
                            val urlTemplate = when (event.dictionarySource) {
                                DictionarySource.ONELOOK -> "https://www.onelook.com/?w=%s"
                                DictionarySource.WIKTIONARY -> "https://en.wiktionary.org/wiki/%s"
                                DictionarySource.GOOGLE_DEFINE -> "https://www.google.com/search?q=define+%s"
                                DictionarySource.MERRIAM_WEBSTER -> "https://www.merriam-webster.com/dictionary/%s"
                                DictionarySource.CUSTOM -> event.customDictionaryUrl.ifBlank { "https://www.onelook.com/?w=%s" }
                                else -> "https://www.onelook.com/?w=%s"
                            }

                            val url = urlTemplate.replace("%s", encodedWord)
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = url.toUri()
                            }

                            yield()

                            browserIntent.launchActivity(
                                activity = event.activity,
                                success = {
                                    return@launch
                                }
                            )
                        }

                        withContext(Dispatchers.Main) {
                            event.activity.getString(R.string.error_no_dictionary)
                                .showToast(context = event.activity, longToast = false)
                        }
                    }
                }

                is ReaderEvent.OnShowSettingsBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = ReaderScreen.SETTINGS_BOTTOM_SHEET,
                            drawer = null,
                            showComicSettingsInMenu = event.showComicSettings
                        )
                    }
                }

                is ReaderEvent.OnDismissBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = null
                        )
                    }
                }

                // Text Selection Bottom Sheet Events
                is ReaderEvent.OnTextSelected -> {
                    val context = us.blindmint.codex.domain.reader.TextSelectionContext.fromSelection(
                        selectedText = event.selectedText,
                        paragraphText = event.paragraphText.ifEmpty { event.selectedText }
                    )
                    _state.update {
                        it.copy(
                            textSelectionContext = context,
                            bottomSheet = null,
                            drawer = null
                        )
                    }
                }

                is ReaderEvent.OnDismissTextSelection -> {
                    _state.update {
                        it.copy(
                            textSelectionContext = null
                        )
                    }
                }

                is ReaderEvent.OnExpandSelection -> {
                    _state.value.textSelectionContext?.let { currentContext ->
                        val expandedContext = currentContext.expandSelection(event.expandLeading)
                        _state.update {
                            it.copy(textSelectionContext = expandedContext)
                        }
                    }
                }

                is ReaderEvent.OnCopySelection -> {
                    val clipboardManager = android.content.ClipboardManager::class.java
                    // Copy is handled directly in the UI since we need context
                }

                is ReaderEvent.OnBookmarkSelection -> {
                    launch(Dispatchers.IO) {
                        // Use the first visible item index as a proxy for page position
                        val pageNumber = _state.value.listState.firstVisibleItemIndex

                        val selectedText = _state.value.textSelectionContext?.selectedText.orEmpty()
                        val customName = event.customName

                        val currentBookmark = us.blindmint.codex.domain.bookmark.Bookmark(
                            bookId = _state.value.book.id,
                            scrollIndex = _state.value.listState.firstVisibleItemIndex,
                            scrollOffset = _state.value.listState.firstVisibleItemScrollOffset,
                            timestamp = System.currentTimeMillis(),
                            selectedText = selectedText,
                            customName = customName,
                            pageNumber = pageNumber
                        )
                        insertBookmark.execute(currentBookmark)

                        // Reload bookmarks to update the list
                        val bookmarks = getBookmarksByBookId.execute(_state.value.book.id)
                        _state.update {
                            it.copy(
                                bookmarks = bookmarks,
                                textSelectionContext = null,  // Clear text selection after creating bookmark
                                drawer = "bookmarks_drawer"  // Auto-open bookmarks panel
                            )
                        }
                    }
                }

                is ReaderEvent.OnWebSearch -> {
                    launch {
                        val url = event.engine.buildUrl(event.query)
                        if (event.openInApp) {
                            _state.update {
                                it.copy(
                                    webViewUrl = url,
                                    textSelectionContext = null
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(textSelectionContext = null)
                            }
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = url.toUri()
                            }
                            browserIntent.launchActivity(
                                activity = event.activity,
                                success = { return@launch }
                            )
                            withContext(Dispatchers.Main) {
                                event.activity.getString(R.string.error_no_browser)
                                    .showToast(context = event.activity, longToast = false)
                            }
                        }
                    }
                }

                is ReaderEvent.OnDictionaryLookup -> {
                    launch {
                        val url = event.dictionary.buildUrl(event.word)
                        if (event.openInApp) {
                            _state.update {
                                it.copy(
                                    webViewUrl = url,
                                    textSelectionContext = null
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(textSelectionContext = null)
                            }
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = url.toUri()
                            }
                            browserIntent.launchActivity(
                                activity = event.activity,
                                success = { return@launch }
                            )
                            withContext(Dispatchers.Main) {
                                event.activity.getString(R.string.error_no_browser)
                                    .showToast(context = event.activity, longToast = false)
                            }
                        }
                    }
                }

                is ReaderEvent.OnOpenInAppWebView -> {
                    _state.update {
                        it.copy(
                            webViewUrl = event.url,
                            textSelectionContext = null
                        )
                    }
                }

                is ReaderEvent.OnDismissWebView -> {
                    _state.update {
                        it.copy(webViewUrl = null)
                    }
                }

                is ReaderEvent.OnShowChaptersDrawer -> {
                    _state.update {
                        it.copy(
                            drawer = ReaderScreen.CHAPTERS_DRAWER,
                            bottomSheet = null
                        )
                    }
                }

                is ReaderEvent.OnShowBookmarksDrawer -> {
                    _state.update {
                        it.copy(
                            drawer = ReaderScreen.CHAPTERS_DRAWER, // Now combined with chapters
                            bottomSheet = null
                        )
                    }
                }



                is ReaderEvent.OnDismissDrawer -> {
                    _state.update {
                        it.copy(
                            drawer = null
                        )
                    }
                }

                is ReaderEvent.OnShowSearch -> {
                    _state.update {
                        it.copy(
                            showSearch = true,
                            showMenu = true,
                            searchBarPersistent = true
                        )
                    }
                }

                is ReaderEvent.OnHideSearch -> {
                    _state.update {
                        it.copy(
                            showSearch = false,
                            searchBarPersistent = false,
                            searchQuery = "",
                            searchResults = emptyList(),
                            currentSearchResultIndex = -1
                        )
                    }
                }

                is ReaderEvent.OnSearchQueryChange -> {
                    // Update query immediately on main thread to keep text field responsive
                    _state.update {
                        it.copy(searchQuery = event.query)
                    }

                    // Then search in background
                    launch(Dispatchers.Default) {
                        val results = if (event.query.isBlank()) {
                            emptyList()
                        } else {
                            searchInText(event.query)
                        }

                        _state.update {
                            it.copy(
                                searchResults = results,
                                currentSearchResultIndex = if (results.isNotEmpty()) 0 else -1
                            )
                        }

                        if (results.isNotEmpty()) {
                            scrollToSearchResult(0)
                        }
                    }
                }

                is ReaderEvent.OnNextSearchResult -> {
                    launch {
                        val results = _state.value.searchResults
                        val currentIndex = _state.value.currentSearchResultIndex
                        if (results.isEmpty()) return@launch

                        val nextIndex = (currentIndex + 1) % results.size
                        _state.update {
                            it.copy(currentSearchResultIndex = nextIndex)
                        }
                        scrollToSearchResult(nextIndex)
                    }
                }

                is ReaderEvent.OnPrevSearchResult -> {
                    launch {
                        val results = _state.value.searchResults
                        val currentIndex = _state.value.currentSearchResultIndex
                        if (results.isEmpty()) return@launch

                        val prevIndex = if (currentIndex <= 0) results.lastIndex else currentIndex - 1
                        _state.update {
                            it.copy(currentSearchResultIndex = prevIndex)
                        }
                        scrollToSearchResult(prevIndex)
                    }
                }

                is ReaderEvent.OnScrollToSearchResult -> {
                    launch {
                        val results = _state.value.searchResults
                        if (event.resultIndex in results.indices) {
                            _state.update {
                                it.copy(currentSearchResultIndex = event.resultIndex)
                            }
                            scrollToSearchResult(event.resultIndex)
                        }
                    }
                }

                is ReaderEvent.OnShowSearchPersistent -> {
                    _state.update {
                        it.copy(
                            showSearch = true,
                            searchBarPersistent = true,
                            showMenu = true
                        )
                    }
                }

                is ReaderEvent.OnHideSearchPersistent -> {
                    _state.update {
                        it.copy(
                            showSearch = false,
                            searchBarPersistent = false
                        )
                    }
                }

                is ReaderEvent.OnComicLoadingComplete -> {
                    // Comic/PDF content loaded, but wait for scroll restoration to complete
                    // before hiding loading animation - handled by OnComicScrollRestorationComplete
                }

                is ReaderEvent.OnComicScrollRestorationComplete -> {
                    // Only hide loading after scroll to saved position is complete
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isScrollRestorationComplete = true,
                            errorMessage = null
                        )
                    }
                }

                is ReaderEvent.OnComicTotalPagesLoaded -> {
                    _state.update {
                        it.copy(
                            totalComicPages = event.totalPages
                        )
                    }
                }

                is ReaderEvent.OnComicPageChanged -> {
                    launch(Dispatchers.IO) {
                        val currentBook = _state.value.book
                        val totalComicPages = _state.value.totalComicPages
                        _state.update {
                            it.copy(
                                currentComicPage = event.currentPage,
                                book = it.book.copy(
                                    currentPage = event.currentPage,
                                    lastPageRead = event.currentPage,
                                    progress = if (totalComicPages > 0) {
                                        (event.currentPage + 1).toFloat() / totalComicPages
                                    } else 0f
                                )
                            )
                        }

                        val newProgress = if (totalComicPages > 0) {
                            (event.currentPage + 1).toFloat() / totalComicPages
                        } else 0f

                        bookRepository.updateNormalReaderProgress(
                            bookId = currentBook.id,
                            scrollIndex = currentBook.scrollIndex,
                            scrollOffset = currentBook.scrollOffset,
                            progress = newProgress
                        )
                        LibraryScreen.refreshListChannel.trySend(300)
                        HistoryScreen.refreshListChannel.trySend(300)
                    }
                }

                is ReaderEvent.OnComicPageSelected -> {
                    launch(Dispatchers.IO) {
                        val currentBook = _state.value.book
                        val totalComicPages = _state.value.totalComicPages
                        _state.update {
                            it.copy(
                                currentComicPage = event.page,
                                book = it.book.copy(
                                    currentPage = event.page,
                                    lastPageRead = event.page,
                                    progress = if (totalComicPages > 0) {
                                        (event.page + 1).toFloat() / totalComicPages
                                    } else 0f
                                )
                            )
                        }

                        val newProgress = if (totalComicPages > 0) {
                            (event.page + 1).toFloat() / totalComicPages
                        } else 0f

                        bookRepository.updateNormalReaderProgress(
                            bookId = currentBook.id,
                            scrollIndex = currentBook.scrollIndex,
                            scrollOffset = currentBook.scrollOffset,
                            progress = newProgress
                        )
                        LibraryScreen.refreshListChannel.trySend(300)
                        HistoryScreen.refreshListChannel.trySend(300)
                    }
                }
            }
        }
    }

    fun init(
        bookId: Int,
        fullscreenMode: Boolean,
        activity: ComponentActivity,
        navigateBack: () -> Unit,
        skipTextLoading: Boolean = false,
        reuseExistingText: Boolean = false
    ) {
        // Set flag before launching to prevent resetScreen() from corrupting state.
        // This is set on Main (from LaunchedEffect) before the IO coroutine dispatches,
        // and resetScreen checks it on Main after yield(), so ordering is guaranteed.
        isInitializing = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
            // Cancel any pending reset immediately, before the async getBookById call.
            resetJob?.cancel()

            val book = getBookById.execute(bookId)

            if (book == null) {
                navigateBack()
                return@launch
            }

            // If reusing existing text, don't reset the ViewModel state
            if (!reuseExistingText) {
                eventJob.cancel()
                resetJob?.cancel()
                eventJob.join()
                resetJob?.join()
                eventJob = SupervisorJob()
            }

            // Skip text loading for page-based formats (comics and PDFs)
            if (!book.isPageBased) {
                val hasExistingText = _state.value.text.isNotEmpty() && _state.value.book.id == bookId

                if (hasExistingText) {
                    // Reuse existing text but update book (for fresh progress from database)
                    _state.update {
                        it.copy(
                            book = book,  // Always use fresh book from database (includes latest progress)
                            isLoading = false // Already loaded
                        )
                    }
                } else {
                    // Fresh initialization - calculate initial scroll position from saved progress
                    val (initialScrollIndex, initialScrollOffset) = if (book.scrollOffset > 0) {
                        // Normal reader saved precise position - use it
                        Log.d("READER", "Using saved position: scrollIndex=${book.scrollIndex}, scrollOffset=${book.scrollOffset}")
                        Pair(book.scrollIndex, book.scrollOffset)
                    } else {
                        // Speed reader saved progress or no precise position - will convert after text loads
                        Log.d("READER", "Will convert progress to position after text loads: progress=${book.progress}")
                        Pair(-1, 0) // -1 indicates we need to calculate from progress after text is loaded
                    }

                    _state.update {
                        ReaderState(
                            book = book,
                            initialScrollIndex = initialScrollIndex,
                            initialScrollOffset = initialScrollOffset
                        )
                    }

                    if (skipTextLoading) {
                        onEvent(
                            ReaderEvent.OnLoadText(
                                activity = activity,
                                fullscreenMode = fullscreenMode
                            )
                        )
                    } else {
                        // Load text directly and complete loading immediately.
                        // Use captured bookId instead of _state.value.book.id to avoid
                        // a race with resetScreen() which can reset state between dispatch.
                        launch(Dispatchers.IO) {
                            val text = try {
                                getText.execute(bookId)
                            } catch (e: Exception) {
                                Log.e("READER", "Failed to load text for book $bookId", e)
                                emptyList<us.blindmint.codex.domain.reader.ReaderText>()
                            }

                            yield()

                            if (text.isEmpty()) {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = UIText.StringResource(R.string.error_could_not_get_text)
                                    )
                                }
                                systemBarsVisibility(show = true, activity = activity)
                                return@launch
                            }

                            systemBarsVisibility(
                                show = false,
                                activity = activity
                            )

                            val lastOpened = getLatestHistory.execute(bookId)?.time
                            yield()

                            // Calculate initial scroll position from progress if needed
                            val finalInitialScrollIndex = if (_state.value.initialScrollIndex == -1 && text.isNotEmpty()) {
                                // Convert progress to position
                                val calculatedIndex = (book.progress * (text.size - 1)).toInt()
                                    .coerceIn(0, text.size - 1)
                                Log.d("READER", "Converting progress to position: progress=${book.progress} * text.lastIndex=${text.size - 1} = $calculatedIndex")
                                calculatedIndex
                            } else {
                                _state.value.initialScrollIndex.coerceAtLeast(0)
                            }

                            _state.update {
                                it.copy(
                                    showMenu = false,
                                    book = it.book.copy(
                                        lastOpened = lastOpened
                                    ),
                                    text = text,
                                    isLoading = false,
                                    errorMessage = null,
                                    initialScrollIndex = finalInitialScrollIndex
                                )
                            }

                            yield()

                            // Use captured book with lastOpened instead of _state.value.book,
                            // which can be corrupted by a concurrent resetScreen()
                            updateBook.execute(book.copy(lastOpened = lastOpened))

                            LibraryScreen.refreshListChannel.trySend(0)
                            HistoryScreen.refreshListChannel.trySend(0)

                            // Load bookmarks for the current book
                            launch(Dispatchers.IO) {
                                val bookmarks = getBookmarksByBookId.execute(bookId)
                                _state.update {
                                    it.copy(bookmarks = bookmarks)
                                }
                            }
                        }
                    }
                }
            } else {
                // For comics, keep loading state until comic pages are loaded
                systemBarsVisibility(
                    show = !fullscreenMode,
                    activity = activity
                )

                val lastOpened = getLatestHistory.execute(book.id)

                // Single state update to prevent triggering LaunchedEffect multiple times
                _state.update {
                    ReaderState(
                        book = book.copy(
                            lastOpened = lastOpened?.time
                        ),
                        currentComicPage = book.lastPageRead.coerceIn(0, book.pageCount ?: 0),
                        showMenu = false,
                        isLoading = true
                    )
                }
 
                updateBook.execute(book)
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
            }
            } finally {
                isInitializing = false
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun updateProgress(listState: LazyListState) {
        viewModelScope.launch(Dispatchers.Main) {
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.distinctUntilChanged().debounce(300).collectLatest { (index, offset) ->
                // Snap to nearest paragraph start and calculate word-based progress for speed reader compatibility
                val snappedIndex = findNearestParagraphStart(index)
                val wordBasedProgress = calculateWordBasedProgress(snappedIndex)
                if (wordBasedProgress == _state.value.book.progress) return@collectLatest
                val (currentChapter, currentChapterProgress) = calculateCurrentChapter(snappedIndex)

                Log.i(
                    READER,
                    "Changed progress|currentChapter: $wordBasedProgress; ${currentChapter?.title}"
                )
                val currentBook = _state.value.book

                _state.update {
                    it.copy(
                        book = it.book.copy(
                            progress = wordBasedProgress,
                            scrollIndex = snappedIndex,
                            scrollOffset = offset
                        ),
                        currentChapter = currentChapter,
                        currentChapterProgress = currentChapterProgress
                    )
                }

                bookRepository.updateNormalReaderProgress(
                    bookId = currentBook.id,
                    scrollIndex = snappedIndex,
                    scrollOffset = offset,
                    progress = wordBasedProgress
                )
 
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
            }
        }
    }

    fun findChapterIndexAndLength(index: Int): Pair<Int, Int> {
        return findCurrentChapter(index)?.let { chapter ->
            _state.value.text.run {
                val startIndex = indexOf(chapter).coerceIn(0, lastIndex)
                val endIndex = (indexOfFirst {
                    it is Chapter && indexOf(it) > startIndex
                }.takeIf { it != -1 }) ?: (lastIndex + 1)

                val currentIndexInChapter = (index - startIndex).coerceAtLeast(1)
                val chapterLength = endIndex - (startIndex + 1)
                currentIndexInChapter to chapterLength
            }
        } ?: (-1 to -1)
    }

    fun updateChapter(index: Int) {
        viewModelScope.launch {
            val (currentChapter, currentChapterProgress) = calculateCurrentChapter(index)
            _state.update {
                Log.i(
                    READER,
                    "Changed currentChapter|currentChapterProgress:" +
                            " ${currentChapter?.title}($currentChapterProgress)"
                )
                it.copy(
                    currentChapter = currentChapter,
                    currentChapterProgress = currentChapterProgress
                )
            }
        }
    }

    private fun calculateCurrentChapter(index: Int): Pair<Chapter?, Float> {
        val currentChapter = findCurrentChapter(index)
        val currentChapterProgress = currentChapter?.let { chapter ->
            _state.value.text.run {
                val startIndex = indexOf(chapter).coerceIn(0, lastIndex)
                val endIndex = (indexOfFirst {
                    it is Chapter && indexOf(it) > startIndex
                }.takeIf { it != -1 }) ?: (lastIndex + 1)

                val currentIndexInChapter = (index - startIndex).coerceAtLeast(1)
                val chapterLength = endIndex - (startIndex + 1)
                (currentIndexInChapter / chapterLength.toFloat())
            }
        }.coerceAndPreventNaN()

        return currentChapter to currentChapterProgress
    }

    private fun findCurrentChapter(index: Int): Chapter? {
        return try {
            for (textIndex in index downTo 0) {
                val readerText = _state.value.text.getOrNull(textIndex) ?: break
                if (readerText is Chapter) {
                    return readerText
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateProgress(firstVisibleItemIndex: Int? = null): Float {
        return _state.value.run {
            if (
                isLoading ||
                listState.layoutInfo.totalItemsCount == 0 ||
                text.isEmpty() ||
                errorMessage != null
            ) {
                return book.progress
            }

            if ((firstVisibleItemIndex ?: listState.firstVisibleItemIndex) == 0) {
                return 0f
            }

            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.last().index
            if (lastVisibleItemIndex >= text.lastIndex) {
                return 1f
            }

            return@run (firstVisibleItemIndex ?: listState.firstVisibleItemIndex)
                .div(text.lastIndex.toFloat())
                .coerceAndPreventNaN()
        }
    }

    // Cache for word-based progress calculation - computed once when text loads
    private var cachedTotalWords: Int? = null
    private var cachedWordCounts: List<Int>? = null

    private fun invalidateWordCache() {
        cachedTotalWords = null
        cachedWordCounts = null
    }

    // Calculate word-based progress for speed reader compatibility
    private fun calculateWordBasedProgress(textItemIndex: Int): Float {
        val text = _state.value.text
        if (text.isEmpty()) return 0f

        // Build cache if not available
        if (cachedTotalWords == null || cachedWordCounts == null) {
            val wordCounts = mutableListOf<Int>()
            var runningTotal = 0
            for (readerText in text) {
                val wordCount = when (readerText) {
                    is ReaderText.Text -> readerText.line.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    else -> 0
                }
                runningTotal += wordCount
                wordCounts.add(runningTotal)
            }
            cachedWordCounts = wordCounts
            cachedTotalWords = runningTotal
        }

        val totalWords = cachedTotalWords ?: return 0f
        if (totalWords == 0) return 0f

        // Use cached cumulative word counts for O(1) lookup
        val safeIndex = textItemIndex.coerceIn(0, text.lastIndex)
        val wordCount = cachedWordCounts?.getOrNull(safeIndex) ?: 0

        return wordCount.toFloat() / totalWords.toFloat()
    }

    // Find the nearest paragraph start before the given index
    private fun findNearestParagraphStart(targetIndex: Int): Int {
        if (targetIndex <= 0) return targetIndex

        // Look backwards through text items to find a paragraph boundary
        // A paragraph boundary is typically a transition from one text item to another
        // We'll go back up to 3 text items to find a suitable paragraph start
        val searchStartItem = maxOf(0, targetIndex - 3)

        for (itemIndex in targetIndex downTo searchStartItem) {
            val textItem = _state.value.text.getOrNull(itemIndex)
            if (textItem is ReaderText.Text) {
                // Return the index of this text item (paragraph start)
                return itemIndex
            }
        }

        // If no paragraph boundary found, return the original target index
        return targetIndex
    }

    private suspend fun systemBarsVisibility(
        show: Boolean,
        activity: ComponentActivity
    ) {
        withContext(Dispatchers.Main) {
            WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView
            ).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (show) show(WindowInsetsCompat.Type.systemBars())
                else hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun searchInText(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val queryLower = query.lowercase()

        _state.value.text.forEachIndexed { index, readerText ->
            val text = when (readerText) {
                is ReaderText.Text -> readerText.line.text
                is Chapter -> readerText.title
                else -> null
            }

            if (text != null && text.lowercase().contains(queryLower)) {
                // Find the matched text with context
                val (matchedText, beforeContext, afterContext) = extractContext(text, queryLower)
                results.add(SearchResult(
                    textIndex = index,
                    fullText = text,
                    matchedText = matchedText,
                    beforeContext = beforeContext,
                    afterContext = afterContext
                ))
            }
        }

        return results
    }

    private fun extractContext(text: String, queryLower: String): Triple<String, String, String> {
        val textLower = text.lowercase()
        val queryIndex = textLower.indexOf(queryLower)
        if (queryIndex == -1) return Triple(text, "", "")

        // Extract matched text (the actual query match)
        val matchedText = text.substring(
            queryIndex,
            (queryIndex + queryLower.length).coerceAtMost(text.length)
        )

        // Calculate context - aim for about 20-30 characters before and after
        val beforeStart = (queryIndex - 25).coerceAtLeast(0)
        val beforeContext = text.substring(beforeStart, queryIndex).trimStart()

        val afterEnd = (queryIndex + queryLower.length + 25).coerceAtMost(text.length)
        val afterContext = text.substring(queryIndex + queryLower.length, afterEnd).trimEnd()

        return Triple(matchedText, beforeContext, afterContext)
    }

    private suspend fun scrollToSearchResult(resultIndex: Int) {
        val results = _state.value.searchResults
        if (resultIndex !in results.indices) return

        val result = results[resultIndex]
        _state.value.listState.requestScrollToItem(result.textIndex)
        updateChapter(result.textIndex)
    }

    fun resetScreen() {
        resetJob = viewModelScope.launch(Dispatchers.Main) {
            eventJob.cancel()
            eventJob = SupervisorJob()

            yield()
            // Skip the state reset if init() is in progress or about to run.
            // This prevents corrupting state during reader-to-reader transitions.
            if (isInitializing) return@launch
            invalidateWordCache()
            _state.update { ReaderState() }
        }
    }

    fun deleteBookmarkItem(bookmark: us.blindmint.codex.domain.bookmark.Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteBookmark.execute(bookmark)

            // Reload bookmarks to update the list
            val bookmarks = getBookmarksByBookId.execute(_state.value.book.id)
            _state.update {
                it.copy(bookmarks = bookmarks)
            }
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            deleteBookmarksByBookId.execute(_state.value.book.id)

            // Clear bookmarks list in UI
            _state.update {
                it.copy(bookmarks = emptyList())
            }
        }
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}