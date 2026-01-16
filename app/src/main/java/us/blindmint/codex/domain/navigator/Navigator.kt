/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.navigator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = Navigator.Factory::class)
class Navigator @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val initialScreen: Screen
) : ViewModel() {

    val items = savedStateHandle.getStateFlow("items", mutableListOf(initialScreen))
    private fun StateFlow<MutableList<Screen>>.removeLast() {
        savedStateHandle["items"] = value.dropLast(1)
    }

    private fun StateFlow<MutableList<Screen>>.add(item: Screen) {
        savedStateHandle["items"] = value + item
    }

    val lastItem = items.map {
        it.lastOrNull() ?: initialScreen
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialScreen
    )

    val lastEvent = savedStateHandle.getStateFlow("stack_event", StackEvent.Default)
    private fun changeStackEvent(stackEvent: StackEvent) {
        savedStateHandle["stack_event"] = stackEvent
    }


    fun push(
        targetScreen: Screen,
        popping: Boolean = false,
        saveInBackStack: Boolean = true
    ) {
        android.util.Log.d("NAV_DEBUG", "Navigator.push called with screen: ${targetScreen::class.simpleName}")

        // Special case: Allow multiple OpdsCategoryScreen instances for catalog navigation
        val isOpdsCategoryScreen = targetScreen::class.simpleName == "OpdsCategoryScreen"
        val skipDuplicateCheck = isOpdsCategoryScreen

        if (!skipDuplicateCheck && lastItem.value::class == targetScreen::class) {
            android.util.Log.d("NAV_DEBUG", "Screen type already on top, skipping push")
            return
        }
        if (!saveInBackStack) items.removeLast()

        changeStackEvent(
            if (popping) StackEvent.Pop
            else StackEvent.Default
        )

        if (!skipDuplicateCheck && lastItem.value::class == targetScreen::class) items.removeLast()
        items.add(targetScreen)
        android.util.Log.d("NAV_DEBUG", "Screen pushed successfully. Stack size: ${items.value.size}")
    }

    fun pop(popping: Boolean = true) {
        if (items.value.count() > 1) {
            changeStackEvent(
                if (popping) StackEvent.Pop
                else StackEvent.Default
            )
            items.removeLast()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(startScreen: Screen): Navigator
    }
}