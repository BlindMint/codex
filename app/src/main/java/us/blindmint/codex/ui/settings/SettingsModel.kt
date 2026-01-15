/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ColorPreset
// import us.blindmint.codex.domain.use_case.book.AutoImportOpdsBooksUseCase
import us.blindmint.codex.domain.use_case.book.AutoImportCodexBooksUseCase
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import us.blindmint.codex.domain.use_case.book.BulkImportProgress
import us.blindmint.codex.domain.use_case.color_preset.DeleteColorPreset
import us.blindmint.codex.domain.use_case.color_preset.GetColorPresets
import us.blindmint.codex.domain.use_case.color_preset.ReorderColorPresets
import us.blindmint.codex.domain.use_case.color_preset.SelectColorPreset
import us.blindmint.codex.domain.use_case.color_preset.UpdateColorPreset
import us.blindmint.codex.domain.use_case.data_store.GetAllSettings
import us.blindmint.codex.domain.use_case.data_store.SetDatastore
import us.blindmint.codex.domain.use_case.permission.GrantPersistableUriPermission
import us.blindmint.codex.domain.use_case.permission.ReleasePersistableUriPermission
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.presentation.core.constants.provideDefaultColorPreset
import us.blindmint.codex.presentation.core.constants.provideDefaultColorPresets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import us.blindmint.codex.presentation.core.util.showToast
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay

@HiltViewModel
class SettingsModel @Inject constructor(
    private val getColorPresets: GetColorPresets,
    private val updateColorPreset: UpdateColorPreset,
    private val selectColorPreset: SelectColorPreset,
    private val reorderColorPresets: ReorderColorPresets,
    private val deleteColorPreset: DeleteColorPreset,
    private val grantPersistableUriPermission: GrantPersistableUriPermission,
    private val releasePersistableUriPermission: ReleasePersistableUriPermission,
    private val getAllSettings: GetAllSettings,
    private val setDatastore: SetDatastore,
    val bulkImportBooksFromFolder: BulkImportBooksFromFolder,
    private val autoImportCodexBooksUseCase: AutoImportCodexBooksUseCase,
    private val codexDirectoryManager: CodexDirectoryManager,
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private var selectColorPresetJob: Job? = null
    private var addColorPresetJob: Job? = null
    private var deleteColorPresetJob: Job? = null
    private var updateColorColorPresetJob: Job? = null
    private var updateTitleColorPresetJob: Job? = null
    private var shuffleColorPresetJob: Job? = null
    private var restoreColorPresetJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            var colorPresets = getColorPresets.execute()

            if (colorPresets.isEmpty()) {
                // Create both Light and Dark presets
                val defaultPresets = provideDefaultColorPresets()
                defaultPresets.forEach { preset ->
                    updateColorPreset.execute(preset)
                }
                colorPresets = getColorPresets.execute()

                // Reset auto-selection flag for fresh installation
                setDatastore.execute(DataStoreConstants.AUTO_COLOR_PRESET_SELECTED, false)
            }

            val scrollIndex = colorPresets.indexOfFirst {
                it.isSelected
            }
            if (scrollIndex != -1) {
                launch(Dispatchers.Main) {
                    try {
                        _state.value.colorPresetListState.requestScrollToItem(index = scrollIndex)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val displayPath = codexDirectoryManager.getDisplayPath()

            _state.update {
                it.copy(
                    selectedColorPreset = colorPresets.selected(),
                    colorPresets = colorPresets,
                    codexRootDisplayPath = displayPath
                )
            }

            Log.i("SETTINGS", "SettingsModel is ready.")
            _isReady.update { true }

            // Auto-selection will be triggered from composable context
            // when the system theme is available
        }
    }

    fun selectAppropriateColorPreset(isDarkTheme: Boolean) {
        viewModelScope.launch {
            // Check if auto-selection has already been completed
            val settings = getAllSettings.execute()
            if (settings.autoColorPresetSelected) {
                // Auto-selection already completed, do nothing
                return@launch
            }

            val colorPresets = _state.value.colorPresets
            if (colorPresets.size >= 2) {
                val appropriatePreset = if (isDarkTheme) {
                    colorPresets.find { it.name == "Dark" }
                } else {
                    colorPresets.find { it.name == "Light" }
                }

                appropriatePreset?.let { preset ->
                    if (!preset.isSelected) {
                        preset.select(animate = true)
                        val updatedColorPresets = colorPresets.map {
                            it.copy(isSelected = it.id == preset.id)
                        }
                        _state.update {
                            it.copy(
                                selectedColorPreset = updatedColorPresets.selected(),
                                colorPresets = updatedColorPresets
                            )
                        }

                        // Mark auto-selection as completed in DataStore
                        setDatastore.execute(DataStoreConstants.AUTO_COLOR_PRESET_SELECTED, true)
                    }
                }
            }
        }
    }

    fun performInitialColorPresetSelection(isDarkTheme: Boolean) {
        viewModelScope.launch {
            // Only perform if we haven't done auto-selection before
            val settings = getAllSettings.execute()
            if (!settings.autoColorPresetSelected) {
                selectAppropriateColorPreset(isDarkTheme)
            }
        }
    }

    fun reloadColorPresets() {
        viewModelScope.launch(Dispatchers.IO) {
            val colorPresets = getColorPresets.execute()

            val scrollIndex = colorPresets.indexOfFirst {
                it.isSelected
            }
            if (scrollIndex != -1) {
                launch(Dispatchers.Main) {
                    try {
                        _state.value.colorPresetListState.requestScrollToItem(index = scrollIndex)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            _state.update {
                it.copy(
                    selectedColorPreset = colorPresets.selected(),
                    colorPresets = colorPresets
                )
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnGrantPersistableUriPermission -> {
                viewModelScope.launch {
                    grantPersistableUriPermission.execute(
                        event.uri
                    )
                }
            }

            is SettingsEvent.OnReleasePersistableUriPermission -> {
                viewModelScope.launch {
                    releasePersistableUriPermission.execute(
                        event.uri
                    )
                }
            }

            is SettingsEvent.OnSelectColorPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    selectColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        colorPreset.select(animate = true)
                        val colorPresets = _state.value.colorPresets.map {
                            it.copy(isSelected = colorPreset.id == it.id)
                        }
                        _state.update {
                            it.copy(
                                selectedColorPreset = colorPresets.selected(),
                                colorPresets = colorPresets
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnSelectPreviousPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    selectColorPresetJob = launch {
                        val colorPresets = _state.value.colorPresets
                        val selectedPreset = _state.value.selectedColorPreset

                        if (colorPresets.size == 1) {
                            return@launch
                        }

                        val selectedPresetIndex = colorPresets.indexOf(selectedPreset)
                        if (selectedPresetIndex == -1) {
                            return@launch
                        }

                        val previousColorPresetIndex = when (selectedPresetIndex) {
                            0 -> {
                                colorPresets.lastIndex
                            }

                            else -> {
                                selectedPresetIndex - 1
                            }
                        }
                        val previousColorPreset = colorPresets.getOrNull(previousColorPresetIndex)
                            ?: return@launch

                        yield()

                        previousColorPreset.select()
                        val updatedColorPresets = _state.value.colorPresets.map {
                            it.copy(isSelected = previousColorPreset.id == it.id)
                        }
                        _state.update {
                            it.copy(
                                selectedColorPreset = updatedColorPresets.selected(),
                                colorPresets = updatedColorPresets
                            )
                        }

                        withContext(Dispatchers.Main) {
                            event.context.getString(
                                R.string.color_preset_selected_query,
                                if (previousColorPreset.name.isNullOrBlank()) {
                                    event.context.getString(
                                        R.string.color_preset_query,
                                        previousColorPreset.id.toString()
                                    )
                                } else {
                                    previousColorPreset.name
                                }.trim()
                            ).showToast(event.context, longToast = false)
                        }
                    }
                }
            }

            is SettingsEvent.OnSelectNextPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    selectColorPresetJob = launch {
                        val colorPresets = _state.value.colorPresets
                        val selectedPreset = _state.value.selectedColorPreset

                        if (colorPresets.size == 1) {
                            return@launch
                        }

                        val selectedPresetIndex = colorPresets.indexOf(selectedPreset)
                        if (selectedPresetIndex == -1) {
                            return@launch
                        }

                        val nextColorPresetIndex = when (selectedPresetIndex) {
                            colorPresets.lastIndex -> {
                                0
                            }

                            else -> {
                                selectedPresetIndex + 1
                            }
                        }
                        val nextColorPreset = colorPresets.getOrNull(nextColorPresetIndex)
                            ?: return@launch

                        yield()

                        nextColorPreset.select()
                        val updatedColorPresets = _state.value.colorPresets.map {
                            it.copy(isSelected = nextColorPreset.id == it.id)
                        }
                        _state.update {
                            it.copy(
                                selectedColorPreset = updatedColorPresets.selected(),
                                colorPresets = updatedColorPresets
                            )
                        }

                        withContext(Dispatchers.Main) {
                            event.context.getString(
                                R.string.color_preset_selected_query,
                                if (nextColorPreset.name.isNullOrBlank()) {
                                    event.context.getString(
                                        R.string.color_preset_query,
                                        nextColorPreset.id.toString()
                                    )
                                } else {
                                    nextColorPreset.name
                                }.trim()
                            ).showToast(event.context, longToast = false)
                        }
                    }
                }
            }

            is SettingsEvent.OnDeleteColorPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    deleteColorPresetJob = launch {
                        if (_state.value.colorPresets.size == 1) return@launch
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        val position = _state.value.colorPresets.indexOf(colorPreset)
                        if (position == -1) {
                            return@launch
                        }

                        val nextPosition = if (position == _state.value.colorPresets.lastIndex) {
                            position - 1
                        } else {
                            position
                        }

                        yield()

                        deleteColorPreset.execute(colorPreset)
                        val nextColorPreset = getColorPresets.execute().getOrNull(nextPosition)
                            ?: return@launch

                        nextColorPreset.select()
                        val colorPresets = getColorPresets.execute()

                        _state.update {
                            it.copy(
                                selectedColorPreset = colorPresets.selected(),
                                colorPresets = colorPresets
                            )
                        }

                        onEvent(
                            SettingsEvent.OnScrollToColorPreset(
                                index = nextPosition
                            )
                        )
                    }
                }
            }

            is SettingsEvent.OnToggleColorPresetLock -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    updateColorColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch
                        val updatedPreset = colorPreset.copy(isLocked = !colorPreset.isLocked)
                        updateColorPreset.execute(updatedPreset)

                        val colorPresets = getColorPresets.execute()
                        _state.update {
                            it.copy(
                                selectedColorPreset = colorPresets.selected(),
                                colorPresets = colorPresets
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnUpdateColorPresetTitle -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    updateTitleColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        val updatedColorPreset = colorPreset.copy(
                            name = event.title
                        )

                        yield()

                        updateColorPreset.execute(updatedColorPreset)
                        _state.update {
                            it.copy(
                                selectedColorPreset = updatedColorPreset,
                                colorPresets = it.colorPresets.updateColorPreset(
                                    updatedColorPreset
                                )
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnShuffleColorPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    shuffleColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        val shuffledColorPreset = colorPreset.copy(
                            backgroundColor = colorPreset.backgroundColor.copy(
                                red = Random.nextFloat(),
                                green = Random.nextFloat(),
                                blue = Random.nextFloat()
                            ),
                            fontColor = colorPreset.fontColor.copy(
                                red = Random.nextFloat(),
                                green = Random.nextFloat(),
                                blue = Random.nextFloat()
                            )
                        )

                        yield()

                        updateColorPreset.execute(shuffledColorPreset)
                        _state.update {
                            it.copy(
                                selectedColorPreset = shuffledColorPreset,
                                colorPresets = it.colorPresets.updateColorPreset(
                                    shuffledColorPreset
                                )
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnRestoreDefaultColorPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    restoreColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        val defaultPresets = provideDefaultColorPresets()
                        val defaultPreset = when (colorPreset.name) {
                            "Light" -> defaultPresets.first()
                            "Dark" -> defaultPresets.last()
                            else -> return@launch
                        }

                        val restoredColorPreset = colorPreset.copy(
                            backgroundColor = defaultPreset.backgroundColor,
                            fontColor = defaultPreset.fontColor
                        )

                        yield()

                        updateColorPreset.execute(restoredColorPreset)
                        _state.update {
                            it.copy(
                                selectedColorPreset = restoredColorPreset,
                                colorPresets = it.colorPresets.updateColorPreset(
                                    restoredColorPreset
                                )
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnAddColorPreset -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    addColorPresetJob = launch {
                        val currentPresets = _state.value.colorPresets
                        
                        // Find the lowest available ID starting from 3 (after Light=1 and Dark=2)
                        val existingIds = currentPresets.map { it.id }.toSet()
                        val newId = generateSequence(3) { it + 1 }
                            .first { it !in existingIds }

                        // Create a new preset with the calculated ID
                        val newColorPreset = ColorPreset(
                            id = newId,
                            name = null, // Will show as "Preset X" based on ID
                            backgroundColor = event.backgroundColor,
                            fontColor = event.fontColor,
                            isSelected = true // Mark as selected immediately
                        )

                        // Immediately update local state to deselect old preset and add new one
                        // This prevents the checkmark from briefly appearing on the old preset
                        val updatedPresets = currentPresets.map { it.copy(isSelected = false) } + newColorPreset

                        _state.update {
                            it.copy(
                                selectedColorPreset = newColorPreset,
                                colorPresets = updatedPresets
                            )
                        }

                        // Now persist to database
                        updateColorPreset.execute(newColorPreset)
                        selectColorPreset.execute(newColorPreset)

                        // Scroll to the new preset
                        val newPresetIndex = updatedPresets.indexOfFirst { it.id == newId }
                        if (newPresetIndex != -1) {
                            onEvent(SettingsEvent.OnScrollToColorPreset(newPresetIndex))
                        }
                    }
                }
            }

            is SettingsEvent.OnUpdateColorPresetColor -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    updateColorColorPresetJob = launch {
                        val colorPreset = event.id.getColorPresetById() ?: return@launch

                        yield()

                        val updatedColorPreset = colorPreset.copy(
                            backgroundColor = event.backgroundColor
                                ?: colorPreset.backgroundColor,
                            fontColor = event.fontColor
                                ?: colorPreset.fontColor
                        )

                        yield()

                        updateColorPreset.execute(updatedColorPreset)
                        _state.update {
                            it.copy(
                                selectedColorPreset = updatedColorPreset,
                                colorPresets = it.colorPresets.updateColorPreset(
                                    updatedColorPreset
                                )
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnReorderColorPresets -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    launch {
                        val reorderedColorPresets = _state.value.colorPresets
                            .toMutableList()
                            .apply {
                                add(event.to, removeAt(event.from))
                            }

                        _state.update {
                            it.copy(
                                colorPresets = reorderedColorPresets
                            )
                        }
                    }
                }
            }

            is SettingsEvent.OnConfirmReorderColorPresets -> {
                viewModelScope.launch {
                    cancelColorPresetJobs()
                    launch {
                        reorderColorPresets.execute(_state.value.colorPresets)
                    }
                }
            }

            is SettingsEvent.OnScrollToColorPreset -> {
                viewModelScope.launch {
                    try {
                        _state.value.colorPresetListState.requestScrollToItem(
                            index = event.index
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            is SettingsEvent.OnSetCodexRootFolder -> {
                viewModelScope.launch {
                    Log.i("SettingsModel", "Setting Codex root folder: ${event.uri}")
                    val success = codexDirectoryManager.setCodexRootUri(event.uri)
                    Log.i("SettingsModel", "Codex root folder set successfully: $success")

                    if (success) {
                        val displayPath = codexDirectoryManager.getDisplayPath()
                        Log.i("SettingsModel", "Codex root display path: $displayPath")
                        _state.update {
                            it.copy(codexRootDisplayPath = displayPath)
                        }

                        // Check if directory is configured
                        val isConfigured = codexDirectoryManager.isConfigured()
                        Log.i("SettingsModel", "Codex directory configured: $isConfigured")

                        // Automatically import existing OPDS books from the downloads folder
                        Log.i("SettingsModel", "Starting auto-import of existing OPDS books")
                        val importedCount = autoImportCodexBooksUseCase.execute { progress ->
                            Log.d("SettingsModel", "Auto-import progress: ${progress.current}/${progress.total} - ${progress.currentFolder}")
                        }
                        Log.i("SettingsModel", "Auto-import completed. Imported $importedCount books")
                        if (importedCount > 0) {
                            // Trigger library refresh to show imported books
                            Log.i("SettingsModel", "Triggering library refresh after auto-import")
                            // The library should automatically detect new books, but let's trigger a refresh just in case
                        } else {
                            Log.w("SettingsModel", "No books were imported during auto-import")
                        }
                    } else {
                        Log.e("SettingsModel", "Failed to set Codex root folder")
                    }
                }
            }
        }
    }

    private suspend fun ColorPreset.select(animate: Boolean = false) {
        selectColorPreset.execute(this)
        _state.update {
            it.copy(
                animateColorPreset = animate
            )
        }
    }

    private fun Int.getColorPresetById(): ColorPreset? {
        return _state.value.colorPresets.firstOrNull {
            it.id == this
        }
    }

    private fun List<ColorPreset>?.selected(): ColorPreset {
        val presets = this ?: _state.value.colorPresets

        if (presets.size == 1) {
            return presets.first()
        }

        val selectedPreset = presets.firstOrNull { it.isSelected }

        if (selectedPreset == null) {
            return provideDefaultColorPreset()
        }

        return selectedPreset
    }

    private fun List<ColorPreset>.updateColorPreset(colorPreset: ColorPreset): List<ColorPreset> {
        if (size == 1) {
            return listOf(colorPreset)
        }

        return this.map {
            if (it.id == colorPreset.id) {
                colorPreset
            } else {
                it
            }
        }
    }

    private fun cancelColorPresetJobs() {
        selectColorPresetJob?.cancel()
        addColorPresetJob?.cancel()
        updateTitleColorPresetJob?.cancel()
        shuffleColorPresetJob?.cancel()
        restoreColorPresetJob?.cancel()
        updateColorColorPresetJob?.cancel()
        deleteColorPresetJob?.cancel()
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}