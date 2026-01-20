/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.book_info

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.library.category.Category
import us.blindmint.codex.domain.ui.UIText

@Immutable
sealed class BookInfoEvent {

    data object OnShowDetailsBottomSheet : BookInfoEvent()

    data object OnShowEditBottomSheet : BookInfoEvent()

    data object OnShowChangeCoverBottomSheet : BookInfoEvent()

    data class OnChangeCover(
        val uri: Uri,
        val context: Context
    ) : BookInfoEvent()

    data class OnResetCover(
        val context: Context
    ) : BookInfoEvent()

    data class OnDeleteCover(
        val context: Context
    ) : BookInfoEvent()

    data object OnCheckCoverReset : BookInfoEvent()

    data object OnDismissBottomSheet : BookInfoEvent()

    data object OnShowTitleDialog : BookInfoEvent()

    data class OnActionTitleDialog(
        val title: String,
        val context: Context
    ) : BookInfoEvent()

    data object OnShowAuthorDialog : BookInfoEvent()

    data class OnActionAuthorDialog(
        val author: UIText,
        val context: Context
    ) : BookInfoEvent()

    data object OnShowDescriptionDialog : BookInfoEvent()

    data class OnActionDescriptionDialog(
        val description: String?,
        val context: Context
    ) : BookInfoEvent()

    data object OnShowPathDialog : BookInfoEvent()

    data class OnActionPathDialog(
        val path: String,
        val context: Context
    ) : BookInfoEvent()

    data object OnShowDeleteDialog : BookInfoEvent()

    data object OnShowResetProgressDialog : BookInfoEvent()

    data class OnActionDeleteDialog(
        val context: Context,
        val navigateBack: () -> Unit
    ) : BookInfoEvent()

    data class OnActionResetProgressDialog(
        val context: Context
    ) : BookInfoEvent()

    data object OnShowMoveDialog : BookInfoEvent()

    data class OnActionMoveDialog(
        val category: Category,
        val context: Context,
        val navigateToLibrary: () -> Unit
    ) : BookInfoEvent()

    data class OnClearProgressHistory(
        val context: Context
    ) : BookInfoEvent()

    data class OnResetReadingProgress(
        val context: Context
    ) : BookInfoEvent()

    data object OnDismissDialog : BookInfoEvent()

    data class OnResetTitle(
        val context: Context
    ) : BookInfoEvent()

    data class OnResetAuthor(
        val context: Context
    ) : BookInfoEvent()

    data class OnResetDescription(
        val context: Context
    ) : BookInfoEvent()

    // Metadata editing events (Tags, Series, Languages)
    data object OnShowTagsDialog : BookInfoEvent()

    data class OnActionTagsDialog(
        val tags: List<String>,
        val context: Context
    ) : BookInfoEvent()

    data class OnResetTags(
        val context: Context
    ) : BookInfoEvent()

    data object OnShowSeriesDialog : BookInfoEvent()

    data class OnActionSeriesDialog(
        val series: List<String>,
        val context: Context
    ) : BookInfoEvent()

    data class OnResetSeries(
        val context: Context
    ) : BookInfoEvent()

    data object OnShowLanguagesDialog : BookInfoEvent()

    data class OnActionLanguagesDialog(
        val languages: List<String>,
        val context: Context
    ) : BookInfoEvent()

    data class OnResetLanguages(
        val context: Context
    ) : BookInfoEvent()

    data class OnRefreshMetadataFromOpds(
        val context: Context
    ) : BookInfoEvent()

    data object OnToggleFavorite : BookInfoEvent()

    data object OnEnterEditMode : BookInfoEvent()

    data object OnConfirmEditMetadata : BookInfoEvent()

    data object OnCancelEditMetadata : BookInfoEvent()

    data object OnSilentCancelEditMetadata : BookInfoEvent()

    data class OnConfirmSaveChanges(
        val context: Context
    ) : BookInfoEvent()

    data object OnDismissSaveDialog : BookInfoEvent()

    data object OnDismissCancelDialog : BookInfoEvent()

    data class OnUpdateEditedBook(
        val updatedBook: Book
    ) : BookInfoEvent()

    data class OnChangeCategory(
        val category: us.blindmint.codex.domain.library.category.Category
    ) : BookInfoEvent()
}