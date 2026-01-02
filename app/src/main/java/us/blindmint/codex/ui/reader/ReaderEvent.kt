/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Immutable
import us.blindmint.codex.domain.dictionary.DictionarySource
import us.blindmint.codex.domain.lookup.WebDictionary
import us.blindmint.codex.domain.lookup.WebSearchEngine
import us.blindmint.codex.domain.reader.ReaderText.Chapter
import us.blindmint.codex.domain.reader.TextSelectionContext

@Immutable
sealed class ReaderEvent {

    data class OnLoadText(
        val activity: ComponentActivity,
        val fullscreenMode: Boolean
    ) : ReaderEvent()

    data class OnMenuVisibility(
        val show: Boolean,
        val fullscreenMode: Boolean,
        val saveCheckpoint: Boolean,
        val activity: ComponentActivity
    ) : ReaderEvent()

    data class OnChangeProgress(
        val progress: Float,
        val firstVisibleItemIndex: Int,
        val firstVisibleItemOffset: Int
    ) : ReaderEvent()

    data class OnScrollToChapter(
        val chapter: Chapter
    ) : ReaderEvent()

    data class OnScroll(
        val progress: Float
    ) : ReaderEvent()

    data object OnRestoreCheckpoint : ReaderEvent()

    data class OnLeave(
        val activity: ComponentActivity,
        val navigate: () -> Unit
    ) : ReaderEvent()

    data class OnOpenTranslator(
        val textToTranslate: String,
        val translateWholeParagraph: Boolean,
        val activity: ComponentActivity
    ) : ReaderEvent()

    data class OnOpenShareApp(
        val textToShare: String,
        val activity: ComponentActivity
    ) : ReaderEvent()

    data class OnOpenWebBrowser(
        val textToSearch: String,
        val activity: ComponentActivity
    ) : ReaderEvent()

    data class OnOpenDictionary(
        val textToDefine: String,
        val activity: ComponentActivity,
        val dictionarySource: DictionarySource = DictionarySource.SYSTEM_DEFAULT,
        val customDictionaryUrl: String = ""
    ) : ReaderEvent()

    data class OnLookupWordOffline(
        val word: String
    ) : ReaderEvent()

    data object OnShowDictionaryBottomSheet : ReaderEvent()

    // Text selection bottom sheet events
    data class OnTextSelected(
        val selectedText: String,
        val paragraphText: String = ""
    ) : ReaderEvent()

    data object OnDismissTextSelection : ReaderEvent()

    data class OnExpandSelection(
        val expandLeading: Boolean
    ) : ReaderEvent()

    data class OnCopySelection(
        val text: String
    ) : ReaderEvent()

    data object OnBookmarkSelection : ReaderEvent()

    data class OnWebSearch(
        val engine: WebSearchEngine,
        val query: String,
        val activity: ComponentActivity,
        val openInApp: Boolean = true
    ) : ReaderEvent()

    data class OnDictionaryLookup(
        val dictionary: WebDictionary,
        val word: String,
        val activity: ComponentActivity,
        val openInApp: Boolean = true
    ) : ReaderEvent()

    data class OnOpenInAppWebView(
        val url: String
    ) : ReaderEvent()

    data object OnDismissWebView : ReaderEvent()

    data object OnShowSettingsBottomSheet : ReaderEvent()

    data object OnDismissBottomSheet : ReaderEvent()

    data object OnShowChaptersDrawer : ReaderEvent()

    data object OnDismissDrawer : ReaderEvent()

    data object OnShowSearch : ReaderEvent()

    data object OnHideSearch : ReaderEvent()

    data class OnSearchQueryChange(
        val query: String
    ) : ReaderEvent()

    data object OnNextSearchResult : ReaderEvent()

    data object OnPrevSearchResult : ReaderEvent()

    data class OnScrollToSearchResult(
        val resultIndex: Int
    ) : ReaderEvent()
}