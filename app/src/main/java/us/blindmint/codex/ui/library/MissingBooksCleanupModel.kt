/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.use_case.book.RemoveMissingBooks
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.history.HistoryScreen
import javax.inject.Inject
import javax.inject.Singleton

data class MissingBooksCleanupState(
    val scanning: Boolean = false,
    val removing: Boolean = false,
    val pendingRemovalCount: Int? = null,
    val resultMessage: String? = null
)

@Singleton
class MissingBooksCleanupService @Inject constructor(
    private val removeMissingBooks: RemoveMissingBooks
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(MissingBooksCleanupState())
    val state: StateFlow<MissingBooksCleanupState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.scanning || _state.value.removing) return

        scope.launch {
            _state.value = MissingBooksCleanupState(scanning = true)
            try {
                val count = removeMissingBooks.count()
                _state.update {
                    it.copy(
                        pendingRemovalCount = count.takeIf { it > 0 },
                        resultMessage = if (count == 0) "No missing books found." else null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(resultMessage = "Could not scan the library: ${e.message ?: "unknown error"}")
                }
            } finally {
                _state.update { it.copy(scanning = false) }
            }
        }
    }

    fun dismissConfirmation() {
        _state.update { it.copy(pendingRemovalCount = null) }
    }

    fun confirmRemoval() {
        if (_state.value.removing || _state.value.pendingRemovalCount == null) return

        scope.launch {
            _state.update { it.copy(removing = true, pendingRemovalCount = null) }
            try {
                val removed = removeMissingBooks.execute()
                _state.update {
                    it.copy(
                        resultMessage = when (removed) {
                            0 -> "No missing books found."
                            1 -> "Removed 1 missing book from the library."
                            else -> "Removed $removed missing books from the library."
                        }
                    )
                }
                LibraryScreen.refreshListChannel.trySend(0)
                HistoryScreen.refreshListChannel.trySend(0)
                BrowseScreen.refreshListChannel.trySend(Unit)
            } catch (e: Exception) {
                _state.update {
                    it.copy(resultMessage = "Could not remove missing books: ${e.message ?: "unknown error"}")
                }
            } finally {
                _state.update { it.copy(removing = false) }
            }
        }
    }
}

@HiltViewModel
class MissingBooksCleanupModel @Inject constructor(
    private val service: MissingBooksCleanupService
) : ViewModel() {
    val state = service.state

    fun scan() = service.scan()

    fun dismissConfirmation() = service.dismissConfirmation()

    fun confirmRemoval() = service.confirmRemoval()
}
