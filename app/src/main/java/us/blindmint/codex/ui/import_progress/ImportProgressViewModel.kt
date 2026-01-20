/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.import_progress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.domain.import_progress.ImportStatus
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import java.util.UUID
import javax.inject.Inject


/**
 * App-level ViewModel for managing import operations.
 * Maintains import state across screen transitions and configuration changes.
 * Singleton-scoped to survive throughout app lifecycle.
 */
@HiltViewModel
class ImportProgressViewModel @Inject constructor(
    private val bulkImportBooksFromFolder: BulkImportBooksFromFolder
) : ViewModel() {

    private val _importOperations = MutableStateFlow<List<ImportOperation>>(emptyList())
    val importOperations: StateFlow<List<ImportOperation>> = _importOperations.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    /**
     * Start importing books from a folder.
     * Creates a new import operation and begins the import process.
     */
    fun startImport(folderUri: Uri, folderName: String, folderPath: String) {
        viewModelScope.launch {
            val operationId = UUID.randomUUID().toString()
            val operation = ImportOperation(
                id = operationId,
                folderName = folderName,
                folderPath = folderPath,
                totalBooks = 0,
                currentProgress = 0,
                status = ImportStatus.STARTING,
                currentFile = ""
            )

            // Add operation to list
            _importOperations.value = _importOperations.value + operation
            _isImporting.value = true

            try {
                // Perform import with progress tracking
                bulkImportBooksFromFolder.execute(
                    folderUri = folderUri,
                    onProgress = { progress ->
                        // Update operation with progress
                        _importOperations.value = _importOperations.value.map { op ->
                            if (op.id == operationId) {
                                op.copy(
                                    status = ImportStatus.IN_PROGRESS,
                                    totalBooks = progress.total,
                                    currentProgress = progress.current,
                                    currentFile = progress.currentFile
                                )
                            } else {
                                op
                            }
                        }
                    }
                )

                // Mark as completed
                _importOperations.value = _importOperations.value.map { op ->
                    if (op.id == operationId) {
                        op.copy(status = ImportStatus.COMPLETED)
                    } else {
                        op
                    }
                }
            } catch (e: Exception) {
                // Mark as failed with error message
                _importOperations.value = _importOperations.value.map { op ->
                    if (op.id == operationId) {
                        op.copy(
                            status = ImportStatus.FAILED,
                            errorMessage = e.message ?: "Unknown error"
                        )
                    } else {
                        op
                    }
                }
            } finally {
                _isImporting.value = false
            }
        }
    }

    /**
     * Cancel an ongoing import operation.
     */
    fun cancelImport(operationId: String) {
        _importOperations.value = _importOperations.value.map { op ->
            if (op.id == operationId) {
                op.copy(status = ImportStatus.CANCELLED)
            } else {
                op
            }
        }
    }

    /**
     * Clear a completed/failed import operation from the list.
     */
    fun clearOperation(operationId: String) {
        _importOperations.value = _importOperations.value.filter { it.id != operationId }
    }

    /**
     * Clear all operations.
     */
    fun clearAllOperations() {
        _importOperations.value = emptyList()
    }

    /**
     * Get a specific operation by ID.
     */
    fun getOperation(operationId: String): ImportOperation? {
        return _importOperations.value.find { it.id == operationId }
    }
}
