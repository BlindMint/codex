/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.library

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.use_case.book.RegenerateMissingCovers
import javax.inject.Inject
import javax.inject.Singleton

data class CoverRegenerationState(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val currentBook: String = "",
    val resultMessage: String? = null
)

@Singleton
class CoverRegenerationService @Inject constructor(
    private val regenerateMissingCovers: RegenerateMissingCovers
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(CoverRegenerationState())
    val state: StateFlow<CoverRegenerationState> = _state.asStateFlow()

    fun regenerateMissing() {
        if (_state.value.running) return

        scope.launch {
            _state.value = CoverRegenerationState(running = true)
            try {
                val result = regenerateMissingCovers.execute { progress ->
                    _state.update {
                        it.copy(
                            processed = progress.processed,
                            total = progress.total,
                            updated = progress.updated,
                            failed = progress.failed,
                            currentBook = progress.currentBook
                        )
                    }
                }
                _state.update {
                    it.copy(
                        resultMessage = if (result.checked == 0) {
                            "No missing covers found."
                        } else {
                            "Updated ${result.updated} of ${result.checked} missing covers" +
                                if (result.failed > 0) " (${result.failed} unavailable)." else "."
                        }
                    )
                }
                LibraryScreen.refreshListChannel.trySend(0)
            } catch (e: Exception) {
                _state.update {
                    it.copy(resultMessage = "Cover regeneration failed: ${e.message ?: "unknown error"}")
                }
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }
}

@HiltViewModel
class CoverRegenerationModel @Inject constructor(
    private val service: CoverRegenerationService
) : ViewModel() {
    val state = service.state

    fun regenerateMissing() = service.regenerateMissing()
}
