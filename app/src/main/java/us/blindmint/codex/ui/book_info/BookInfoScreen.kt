/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.book_info

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.book_info.BookInfoContent
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.reader.ReaderScreen

@Parcelize
data class BookInfoScreen(val bookId: Int) : Screen, Parcelable {

    companion object {
        const val DELETE_DIALOG = "delete_dialog"
        const val MOVE_DIALOG = "move_dialog"
        const val RESET_PROGRESS_DIALOG = "reset_progress_dialog"
        const val TITLE_DIALOG = "title_dialog"
        const val AUTHOR_DIALOG = "author_dialog"
        const val DESCRIPTION_DIALOG = "description_dialog"
        const val PATH_DIALOG = "path_dialog"
        const val TAGS_DIALOG = "tags_dialog"
        const val SERIES_DIALOG = "series_dialog"
        const val LANGUAGES_DIALOG = "languages_dialog"

        const val CHANGE_COVER_BOTTOM_SHEET = "change_cover_bottom_sheet"
        const val DETAILS_BOTTOM_SHEET = "details_bottom_sheet"
        const val EDIT_BOTTOM_SHEET = "edit_bottom_sheet"

        val changePathChannel: Channel<Boolean> = Channel(Channel.CONFLATED)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = hiltViewModel<BookInfoModel>()

        val state = screenModel.state.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) {
            screenModel.init(
                bookId = bookId,
                changePath = changePathChannel.tryReceive().getOrElse { false },
                navigateBack = {
                    navigator.pop()
                }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                screenModel.resetScreen()
            }
        }

        Box(Modifier.fillMaxSize())

        if (state.value.book.id == bookId) {
            BookInfoContent(
                book = state.value.book,
                bottomSheet = state.value.bottomSheet,
                dialog = state.value.dialog,
                listState = listState,
                canResetCover = state.value.canResetCover,
                isEditingMetadata = state.value.isEditingMetadata,
                editedBook = state.value.editedBook,
                showConfirmSaveDialog = state.value.showConfirmSaveDialog,
                showConfirmCancelDialog = state.value.showConfirmCancelDialog,
                showTitleDialog = screenModel::onEvent,
                actionTitleDialog = screenModel::onEvent,
                showAuthorDialog = screenModel::onEvent,
                actionAuthorDialog = screenModel::onEvent,
                showDescriptionDialog = screenModel::onEvent,
                actionDescriptionDialog = screenModel::onEvent,
                showPathDialog = screenModel::onEvent,
                actionPathDialog = screenModel::onEvent,
                showTagsDialog = screenModel::onEvent,
                showSeriesDialog = screenModel::onEvent,
                showLanguagesDialog = screenModel::onEvent,
                resetTitle = screenModel::onEvent,
                resetAuthor = screenModel::onEvent,
                resetDescription = screenModel::onEvent,
                clearProgressHistory = screenModel::onEvent,
                refreshMetadataFromOpds = screenModel::onEvent,
                checkCoverReset = screenModel::onEvent,
                changeCover = screenModel::onEvent,
                resetCover = screenModel::onEvent,
                deleteCover = screenModel::onEvent,
                dismissBottomSheet = screenModel::onEvent,
                dismissDialog = screenModel::onEvent,
                showChangeCoverBottomSheet = screenModel::onEvent,
                showDetailsBottomSheet = screenModel::onEvent,
                showEditBottomSheet = screenModel::onEvent,
                showDeleteDialog = screenModel::onEvent,
                showResetProgressDialog = screenModel::onEvent,
                actionDeleteDialog = screenModel::onEvent,
                actionResetProgressDialog = screenModel::onEvent,
                onEnterEditMode = { screenModel.onEvent(BookInfoEvent.OnEnterEditMode) },
                onConfirmEditMetadata = { screenModel.onEvent(BookInfoEvent.OnConfirmEditMetadata) },
                onCancelEditMetadata = { screenModel.onEvent(BookInfoEvent.OnCancelEditMetadata) },
                onSilentCancelEditMetadata = { screenModel.onEvent(BookInfoEvent.OnSilentCancelEditMetadata) },
                onConfirmSaveChanges = { context -> screenModel.onEvent(BookInfoEvent.OnConfirmSaveChanges(context)) },
                onDismissSaveDialog = { screenModel.onEvent(BookInfoEvent.OnDismissSaveDialog) },
                onDismissCancelDialog = { screenModel.onEvent(BookInfoEvent.OnDismissCancelDialog) },
                onUpdateEditedBook = { updatedBook -> screenModel.onEvent(BookInfoEvent.OnUpdateEditedBook(updatedBook)) },
                onCategoryChange = { category -> screenModel.onEvent(BookInfoEvent.OnChangeCategory(category)) },
                toggleFavorite = { screenModel.onEvent(BookInfoEvent.OnToggleFavorite) },
                navigateToReader = {
                    if (state.value.book.id != -1) {
                        HistoryScreen.insertHistoryChannel.trySend(state.value.book.id)
                        navigator.push(ReaderScreen(state.value.book.id))
                    }
                },
                navigateToSpeedReading = {
                    if (state.value.book.id != -1 && !state.value.book.isComic) {
                        HistoryScreen.insertHistoryChannel.trySend(state.value.book.id)
                        navigator.push(ReaderScreen(state.value.book.id, startInSpeedReading = true))
                    }
                },
                navigateToLibrary = {
                    navigator.push(
                        LibraryScreen,
                        popping = true,
                        saveInBackStack = false
                    )
                },
                navigateBack = {
                    navigator.pop()
                }
            )
        }
    }
}