/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.ReaderText
import us.blindmint.codex.domain.use_case.book.GetBookById
import us.blindmint.codex.domain.use_case.book.GetText
import us.blindmint.codex.domain.use_case.book.UpdateBook
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryScreen
import javax.inject.Inject

@HiltViewModel
class SpeedReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val getText: GetText,
    private val updateBook: UpdateBook
) : ViewModel() {

    val book = mutableStateOf<Book?>(null)
    val text = mutableStateOf<List<ReaderText>>(emptyList())
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf<String?>(null)

    // Progress tracking
    val currentProgress = mutableFloatStateOf(0f)
    val currentWordIndex = mutableIntStateOf(0)
    private var lastSavedProgress = 0f
    private var lastDatabaseSaveWordIndex = 0

    fun loadBook(bookId: Int, activity: Activity, onError: () -> Unit) {
        viewModelScope.launch {
            // Clear previous state when loading a new book
            if (book.value?.id != bookId) {
                book.value = null
                text.value = emptyList()
                isLoading.value = true
                errorMessage.value = null
                currentProgress.floatValue = 0f
                currentWordIndex.intValue = 0
            }

            val loadedBook = getBookById.execute(bookId)
            if (loadedBook == null) {
                onError()
                return@launch
            }

            book.value = loadedBook
            currentProgress.floatValue = loadedBook.progress

            if (!loadedBook.isComic) {
                try {
                    val loadedText = getText.execute(bookId)
                    if (loadedText.isEmpty()) {
                        errorMessage.value = "Could not load text"
                        isLoading.value = false
                    } else {
                        text.value = loadedText
                        isLoading.value = false
                    }
                } catch (e: Exception) {
                    errorMessage.value = "Error loading text: ${e.message}"
                    isLoading.value = false
                }
            } else {
                // Comics not supported in speed reader
                errorMessage.value = "Comics not supported in speed reader"
                isLoading.value = false
            }
        }
    }

    fun updateProgress(progress: Float, wordIndex: Int, forceSave: Boolean = false) {
        viewModelScope.launch {
            Log.d("SPEED_READER", "Model updateProgress: progress=$progress, wordIndex=$wordIndex, forceSave=$forceSave, lastSaved=$lastDatabaseSaveWordIndex")
            // Always update UI state immediately for smooth progress bar
            currentProgress.floatValue = progress
            currentWordIndex.intValue = wordIndex

            // Save to database every 50+ words during reading, or immediately for manual pauses
            val wordsSinceLastSave = wordIndex - lastDatabaseSaveWordIndex
            Log.d("SPEED_READER", "Model wordsSinceLastSave=$wordsSinceLastSave, willSave=${forceSave || wordsSinceLastSave >= 50}")
            if (forceSave || wordsSinceLastSave >= 50) {
                saveProgressToDatabase(progress)
                lastDatabaseSaveWordIndex = wordIndex
            }
        }
    }

    private suspend fun saveProgressToDatabase(progress: Float) {
        book.value?.let { currentBook ->
            val wordIndex = currentWordIndex.intValue
            Log.d("SPEED_READER", "SpeedReaderModel saving to database: progress=$progress, wordIndex=$wordIndex, bookId=${currentBook.id}")

            // Store word index directly in scrollIndex for precision
            val textSize = text.value.size
            val scrollIndex = wordIndex // Store word index directly
            val scrollOffset = -1 // Use -1 to indicate speed reader format

            val updatedBook = currentBook.copy(
                progress = progress,
                scrollIndex = scrollIndex,
                scrollOffset = scrollOffset
            )
            try {
                updateBook.execute(updatedBook)
                Log.d("SPEED_READER", "Successfully saved progress to database: progress=${updatedBook.progress}, wordIndex=$wordIndex")
                lastSavedProgress = progress
                lastDatabaseSaveWordIndex = wordIndex

                // Refresh library and history
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
            } catch (e: Exception) {
                Log.e("SPEED_READER", "Failed to save progress to database", e)
            }
        } ?: Log.w("SPEED_READER", "Cannot save progress: book is null")
    }

    fun onLeave() {
        viewModelScope.launch {
            // Always save final progress, even if < 50 words since last save
            saveProgressToDatabase(currentProgress.floatValue)
        }
    }
}