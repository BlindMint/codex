/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import android.app.Activity
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
import javax.inject.Inject

@HiltViewModel
class SpeedReaderModel @Inject constructor(
    private val getBookById: GetBookById,
    private val getText: GetText
) : ViewModel() {

    val book = mutableStateOf<Book?>(null)
    val text = mutableStateOf<List<ReaderText>>(emptyList())
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf<String?>(null)

    fun loadBook(bookId: Int, activity: Activity, onError: () -> Unit) {
        viewModelScope.launch {
            val loadedBook = getBookById.execute(bookId)
            if (loadedBook == null) {
                onError()
                return@launch
            }

            book.value = loadedBook

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
}