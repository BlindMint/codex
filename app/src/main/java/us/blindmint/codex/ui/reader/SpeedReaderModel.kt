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
import us.blindmint.codex.domain.reader.SpeedReaderWord
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.use_case.book.GetBookById
import us.blindmint.codex.domain.use_case.book.GetSpeedReaderWords
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryScreen
import javax.inject.Inject

@HiltViewModel
class SpeedReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val getSpeedReaderWords: GetSpeedReaderWords,
    private val bookRepository: BookRepository
) : ViewModel() {

    val book = mutableStateOf<Book?>(null)
    val words = mutableStateOf<List<SpeedReaderWord>>(emptyList())
    val totalWords = mutableIntStateOf(0)
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf<String?>(null)

    // Progress tracking
    val currentProgress = mutableFloatStateOf(0f)
    val currentWordIndex = mutableIntStateOf(0)

    fun loadBook(bookId: Int, activity: Activity, onError: () -> Unit) {
        viewModelScope.launch {
            Log.d("SPEED_READER_LOAD", "[1] loadBook() called - bookId=$bookId")
            Log.d("SPEED_READER_LOAD", "[1] Current book in state: ${book.value?.id}")
            Log.d("SPEED_READER_LOAD", "[1] currentWordIndex before load: ${currentWordIndex.intValue}")
            Log.d("SPEED_READER_LOAD", "[1] currentProgress before load: ${currentProgress.floatValue}")

            // Clear previous state when loading a new book
            if (book.value?.id != bookId) {
                Log.d("SPEED_READER_LOAD", "[2] New book detected, clearing state")
                book.value = null
                words.value = emptyList()
                isLoading.value = true
                errorMessage.value = null
                currentProgress.floatValue = 0f
                currentWordIndex.intValue = -1 // Invalid until book loads
                Log.d("SPEED_READER_LOAD", "[2] After clear - currentWordIndex=${currentWordIndex.intValue}, currentProgress=${currentProgress.floatValue}")
            }

            Log.d("SPEED_READER_LOAD", "[3] Fetching book from database...")
            val loadedBook = getBookById.execute(bookId)
            if (loadedBook == null) {
                Log.e("SPEED_READER_LOAD", "[3] Book not found in database!")
                onError()
                return@launch
            }

            Log.d("SPEED_READER_LOAD", "[3] Book loaded from DB - title='${loadedBook.title}', speedReaderWordIndex=${loadedBook.speedReaderWordIndex}, isComic=${loadedBook.isComic}")

            // Mark that this book has been opened in speed reader
            val updatedBook = loadedBook.copy(speedReaderHasBeenOpened = true)
            book.value = updatedBook
            Log.d("SPEED_READER_LOAD", "[4] Updated book stored in state - id=${updatedBook.id}")

            // Set initial progress to 0; will be updated after text loads
            currentProgress.floatValue = 0f
            Log.d("SPEED_READER_LOAD", "[4] currentProgress set to 0f as temporary value")

            // Update database to mark as opened
            viewModelScope.launch {
                try {
                    bookRepository.markSpeedReaderOpened(updatedBook.id)
                    Log.d("SPEED_READER_LOAD", "[4] Database updated: marked as opened")
                } catch (e: Exception) {
                    Log.e("SPEED_READER_LOAD", "[4] Failed to mark book as opened in speed reader", e)
                }
            }

            if (!loadedBook.isComic) {
                try {
                    Log.d("SPEED_READER_LOAD", "[5] Loading words from repository...")
                    val loadedWords = getSpeedReaderWords.execute(bookId)
                    Log.d("SPEED_READER_LOAD", "[5] Words loaded from repository - count=${loadedWords.size}")

                    if (loadedWords.isEmpty()) {
                        Log.e("SPEED_READER_LOAD", "[5] No words loaded! Setting error message")
                        errorMessage.value = "Could not load text"
                        isLoading.value = false
                    } else {
                        words.value = loadedWords
                        totalWords.intValue = loadedWords.size

                        Log.d("SPEED_READER_LOAD", "[6] Setting state values:")
                        Log.d("SPEED_READER_LOAD", "[6]   words.size = ${loadedWords.size}")
                        Log.d("SPEED_READER_LOAD", "[6]   totalWords = ${loadedWords.size}")
                        Log.d("SPEED_READER_LOAD", "[6]   loadedBook.speedReaderWordIndex = ${loadedBook.speedReaderWordIndex}")

                        // Save total words to database if not already saved or if it differs
                        if (loadedBook.speedReaderTotalWords != loadedWords.size) {
                            viewModelScope.launch {
                                try {
                                    bookRepository.updateSpeedReaderTotalWords(updatedBook.id, loadedWords.size)
                                    Log.d("SPEED_READER_LOAD", "[6] Saved totalWords to database: ${loadedWords.size}")
                                } catch (e: Exception) {
                                    Log.e("SPEED_READER_LOAD", "[6] Failed to save totalWords to database", e)
                                }
                            }
                        }

                        val initialIndex = loadedBook.speedReaderWordIndex.coerceIn(0, loadedWords.size - 1)
                        Log.d("SPEED_READER_LOAD", "[6]   coerced initialIndex = $initialIndex (range: 0-${loadedWords.size - 1})")

                        // Set the correct word index AFTER text is loaded
                        currentWordIndex.intValue = initialIndex

                        // Update progress based on current word index and total words
                        val progress = if (loadedWords.size > 0) {
                            initialIndex.toFloat() / loadedWords.size
                        } else 0f
                        currentProgress.floatValue = progress

                        isLoading.value = false

                        Log.d("SPEED_READER_LOAD", "[7] FINAL STATE:")
                        Log.d("SPEED_READER_LOAD", "[7]   currentWordIndex.intValue = ${currentWordIndex.intValue}")
                        Log.d("SPEED_READER_LOAD", "[7]   currentProgress.floatValue = ${currentProgress.floatValue}")
                        Log.d("SPEED_READER_LOAD", "[7]   isLoading.value = ${isLoading.value}")
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
            Log.d("SPEED_READER", "Model updateProgress: progress=$progress, wordIndex=$wordIndex")
            currentProgress.floatValue = progress
            currentWordIndex.intValue = wordIndex
            saveProgressToDatabase(progress)
        }
    }

    private suspend fun saveProgressToDatabase(progress: Float) {
        book.value?.let { currentBook ->
            val wordIndex = currentWordIndex.intValue
            Log.d("SPEED_READER", "SpeedReaderModel saving to database: progress=$progress, wordIndex=$wordIndex, bookId=${currentBook.id}")

            try {
                bookRepository.updateSpeedReaderProgress(currentBook.id, wordIndex)
                Log.d("SPEED_READER", "Successfully saved speed reader progress to database: wordIndex=$wordIndex")
            } catch (e: Exception) {
                Log.e("SPEED_READER", "Failed to save speed reader progress to database", e)
            }
        } ?: Log.w("SPEED_READER", "Cannot save speed reader progress: book is null")
    }

    fun onLeave() {
        viewModelScope.launch {
            saveProgressToDatabase(currentProgress.floatValue)
        }
    }
}