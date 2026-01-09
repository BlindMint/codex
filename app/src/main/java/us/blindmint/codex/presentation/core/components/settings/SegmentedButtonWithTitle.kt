/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * SegmentedButtonWithTitle.kt - Material 3 segmented button component with title
 * Used for exclusive selection controls in settings screens
 */

package us.blindmint.codex.presentation.core.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn

import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

/**
 * Segmented Button with Title.
 * Uses Material 3 SingleChoiceSegmentedButtonRow for better accessibility and official styling.
 *
 * @param modifier Modifier.
 * @param title Title of the buttons.
 * @param buttons [ButtonItem]s.
 * @param enabled Whether button is enabled.
 * @param horizontalPadding Horizontal item padding.
 * @param verticalPadding Vertical item padding.
 * @param onClick OnClick callback.
 */

@Composable
fun SegmentedButtonWithTitle(
    modifier: Modifier = Modifier,
    title: String,
    buttons: List<ButtonItem>,
    enabled: Boolean = true,
    horizontalPadding: Dp = SettingsHorizontalPadding,
    verticalPadding: Dp = 8.dp,
    onClick: (ButtonItem) -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        SettingsSubcategoryTitle(title = title, padding = 0.dp)

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            buttons.forEachIndexed { index, buttonItem ->
                SegmentedButton(
                    selected = buttonItem.selected,
                    onClick = { if (enabled) onClick(buttonItem) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = buttons.size
                    )
                ) {
                    StyledText(
                        text = buttonItem.title,
                        style = buttonItem.textStyle
                    )
                }
            }
        }
    }
}

