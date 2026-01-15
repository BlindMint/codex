/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings.opds

import androidx.compose.runtime.Immutable
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import us.blindmint.codex.domain.backup.OpdsSourceData

@Immutable
data class OpdsSourcesState(
    val sources: List<OpdsSourceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showBackupImportPrompt: Boolean = false,
    val backupSourcesToImport: List<OpdsSourceData> = emptyList()
)