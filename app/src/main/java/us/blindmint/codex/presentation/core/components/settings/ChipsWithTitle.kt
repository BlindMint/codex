/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

/**
 * Chips with title. Use list of [ButtonItem]s to display chips.
 */
@Composable
fun ChipsWithTitle(
    modifier: Modifier = Modifier,
    title: String,
    chips: List<ButtonItem>,
    horizontalPadding: Dp = SettingsHorizontalPadding,
    verticalPadding: Dp = 8.dp,
    onClick: (ButtonItem) -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        SettingsSubcategoryTitle(title = title, padding = 0.dp)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                chips.forEach { item ->
                    FilterChip(
                        modifier = Modifier.height(36.dp),
                        selected = item.selected,
                        label = {
                            StyledText(
                                text = item.title,
                                style = item.textStyle,
                                maxLines = 1
                            )
                        },
                        onClick = { onClick(item) },
                    )
                }
            },
        )
    }
}