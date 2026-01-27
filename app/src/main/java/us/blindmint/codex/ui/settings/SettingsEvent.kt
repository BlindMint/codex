/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import us.blindmint.codex.domain.util.ID

@Immutable
sealed class SettingsEvent {
    data class OnGrantPersistableUriPermission(
        val uri: Uri
    ) : SettingsEvent()

    data class OnReleasePersistableUriPermission(
        val uri: Uri
    ) : SettingsEvent()

    data class OnRemoveFolder(
        val uri: Uri,
        val removeBooks: Boolean
    ) : SettingsEvent()

    data object OnRemoveCodexRootFolder : SettingsEvent()
}