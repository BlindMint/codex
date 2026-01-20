/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.app.Activity
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
import us.blindmint.codex.presentation.history.HistoryScreen
import us.blindmint.codex.presentation.library.LibraryScreen
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

    fun loadBook(bookId: Int, activity: Activity, onError: () -> Unit) {
        viewModelScope.launch {
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

    fun updateProgress(progress: Float, wordIndex: Int) {
        viewModelScope.launch {
            currentProgress.floatValue = progress
            currentWordIndex.intValue = wordIndex

            // Save to database
            book.value?.let { currentBook ->
                val updatedBook = currentBook.copy(progress = progress)
                updateBook.execute(updatedBook)
                lastSavedProgress = progress

                // Refresh library and history
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
            }
        }
    }

    fun onLeave() {
        viewModelScope.launch {
            book.value?.let { currentBook ->
                // Save final progress
                val updatedBook = currentBook.copy(progress = currentProgress.floatValue)
                updateBook.execute(updatedBook)

                // Refresh screens
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
            }
        }
    }
}