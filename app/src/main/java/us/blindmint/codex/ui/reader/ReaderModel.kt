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
import us.blindmint.codex.domain.use_case.bookmark.GetBookmarksByBookId
import us.blindmint.codex.domain.use_case.bookmark.InsertBookmark
import us.blindmint.codex.domain.use_case.bookmark.DeleteBookmark
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
    private val deleteBookmark: DeleteBookmark
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private var eventJob = SupervisorJob()
    private var resetJob: Job? = null

    private var scrollJob: Job? = null

    fun onEvent(event: ReaderEvent) {
        viewModelScope.launch(eventJob + Dispatchers.Main) {
            when (event) {
                is ReaderEvent.OnLoadText -> {
                    launch(Dispatchers.IO) {
                        val text = getText.execute(_state.value.book.id)
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

                        val lastOpened = getLatestHistory.execute(_state.value.book.id)?.time
                        yield()

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

                        updateBook.execute(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        launch {
                            snapshotFlow {
                                _state.value.listState.layoutInfo.totalItemsCount
                            }.collectLatest { itemsCount ->
                                if (itemsCount < _state.value.text.size) return@collectLatest

                                _state.value.book.apply {
                                    _state.value.listState.requestScrollToItem(
                                        scrollIndex,
                                        scrollOffset
                                    )
                                    updateChapter(index = scrollIndex)
                                }

                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = null
                                    )
                                }

                                // Load bookmarks for the current book
                                launch(Dispatchers.IO) {
                                    val bookmarks = getBookmarksByBookId.execute(_state.value.book.id)
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
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    progress = event.progress,
                                    scrollIndex = event.firstVisibleItemIndex,
                                    scrollOffset = event.firstVisibleItemOffset
                                )
                            )
                        }

                        updateBook.execute(_state.value.book)

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

                        _state.value.listState.apply {
                            if (
                                _state.value.isLoading ||
                                layoutInfo.totalItemsCount < 1 ||
                                _state.value.text.isEmpty() ||
                                _state.value.errorMessage != null
                            ) return@apply

                            _state.update {
                                it.copy(
                                    book = it.book.copy(
                                        progress = calculateProgress(),
                                        scrollIndex = firstVisibleItemIndex,
                                        scrollOffset = firstVisibleItemScrollOffset
                                    )
                                )
                            }

                            updateBook.execute(_state.value.book)

                            LibraryScreen.refreshListChannel.trySend(0)
                            HistoryScreen.refreshListChannel.trySend(0)
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
                            drawer = null
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
                        val currentBookmark = us.blindmint.codex.domain.bookmark.Bookmark(
                            bookId = _state.value.book.id,
                            scrollIndex = _state.value.listState.firstVisibleItemIndex,
                            scrollOffset = _state.value.listState.firstVisibleItemScrollOffset,
                            timestamp = System.currentTimeMillis()
                        )
                        insertBookmark.execute(currentBookmark)

                        // Reload bookmarks to update the list
                        val bookmarks = getBookmarksByBookId.execute(_state.value.book.id)
                        _state.update {
                            it.copy(bookmarks = bookmarks)
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
                            drawer = ReaderScreen.BOOKMARKS_DRAWER,
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
                            showMenu = false
                        )
                    }
                }

                is ReaderEvent.OnHideSearch -> {
                    _state.update {
                        it.copy(
                            showSearch = false,
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
            }
        }
    }

    fun init(
        bookId: Int,
        fullscreenMode: Boolean,
        activity: ComponentActivity,
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
                ReaderState(book = book)
            }

            onEvent(
                ReaderEvent.OnLoadText(
                    activity = activity,
                    fullscreenMode = fullscreenMode
                )
            )
        }
    }

    @OptIn(FlowPreview::class)
    fun updateProgress(listState: LazyListState) {
        viewModelScope.launch(Dispatchers.Main) {
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.distinctUntilChanged().debounce(300).collectLatest { (index, offset) ->
                val progress = calculateProgress(index)
                if (progress == _state.value.book.progress) return@collectLatest
                val (currentChapter, currentChapterProgress) = calculateCurrentChapter(index)

                Log.i(
                    READER,
                    "Changed progress|currentChapter: $progress; ${currentChapter?.title}"
                )
                _state.update {
                    it.copy(
                        book = it.book.copy(
                            progress = progress,
                            scrollIndex = index,
                            scrollOffset = offset
                        ),
                        currentChapter = currentChapter,
                        currentChapterProgress = currentChapterProgress
                    )
                }

                updateBook.execute(_state.value.book)

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

    private fun updateChapter(index: Int) {
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
                results.add(SearchResult(textIndex = index, text = text))
            }
        }

        return results
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

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}