/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.import_progress

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.blindmint.codex.domain.import_progress.ImportOperation
import us.blindmint.codex.domain.import_progress.ImportStatus
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import us.blindmint.codex.domain.use_case.book.BulkImportCodexDirectoryUseCase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ImportProgressViewModel"

/**
 * App-level service for managing import operations.
 * Maintains import state across screen transitions and configuration changes.
 * Singleton-scoped to survive throughout app lifecycle.
 */
@Singleton
class ImportProgressService @Inject constructor(
    private val bulkImportBooksFromFolder: BulkImportBooksFromFolder,
    private val bulkImportCodexDirectoryUseCase: BulkImportCodexDirectoryUseCase
) {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val _importOperations = MutableStateFlow<List<ImportOperation>>(emptyList())
    val importOperations: StateFlow<List<ImportOperation>> = _importOperations.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    /**
     * Start importing books from a folder.
     * Creates a new import operation and begins the import process.
     */
    fun startImport(folderUri: Uri, folderName: String, folderPath: String, onComplete: (suspend () -> Unit)? = null) {
        coroutineScope.launch {
            val operationId = UUID.randomUUID().toString()
            val operation = ImportOperation(
                id = operationId,
                folderName = folderName,
                folderPath = folderPath,
                folderUri = folderUri,
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

                // Call completion callback after successful import
                onComplete?.invoke()

                // Auto-clear completed operation after 2 seconds
                delay(2000)
                clearOperation(operationId)
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

                // Auto-clear failed operation after 5 seconds
                delay(5000)
                clearOperation(operationId)
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

    /**
     * Start importing books from Codex Directory.
     * Mirrors local folder import behavior with proper progress tracking.
     */
    fun startCodexImport(folderPath: String, folderName: String, onComplete: (suspend () -> Unit)? = null) {
        coroutineScope.launch {
            val operationId = UUID.randomUUID().toString()
            val operation = ImportOperation(
                id = operationId,
                folderName = "Codex Directory",
                folderPath = folderPath,
                folderUri = Uri.EMPTY,
                totalBooks = 0,
                currentProgress = 0,
                status = ImportStatus.STARTING,
                currentFile = ""
            )

            _importOperations.value = _importOperations.value + operation
            _isImporting.value = true

            try {
                bulkImportCodexDirectoryUseCase.execute(
                    onProgress = { progress ->
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

                _importOperations.value = _importOperations.value.map { op ->
                    if (op.id == operationId) {
                        op.copy(status = ImportStatus.COMPLETED)
                    } else {
                        op
                    }
                }

                onComplete?.invoke()
                delay(2000)
                clearOperation(operationId)
            } catch (e: Exception) {
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
                delay(5000)
                clearOperation(operationId)
            } finally {
                _isImporting.value = false
            }
        }
    }
}

/**
 * ViewModel wrapper for composables to access ImportProgressService.
 */
@HiltViewModel
class ImportProgressViewModel @Inject constructor(
    private val importProgressService: ImportProgressService
) : ViewModel() {

    val importOperations = importProgressService.importOperations
    val isImporting = importProgressService.isImporting

    fun startImport(folderUri: Uri, folderName: String, folderPath: String, onComplete: (suspend () -> Unit)? = null) =
        importProgressService.startImport(folderUri, folderName, folderPath, onComplete)

    fun startCodexImport(folderPath: String, folderName: String) =
        importProgressService.startCodexImport(folderPath, folderName)

    fun cancelImport(operationId: String) =
        importProgressService.cancelImport(operationId)

    fun clearOperation(operationId: String) =
        importProgressService.clearOperation(operationId)

    fun clearAllOperations() =
        importProgressService.clearAllOperations()

    fun getOperation(operationId: String) =
        importProgressService.getOperation(operationId)
}
