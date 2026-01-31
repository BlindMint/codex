/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package us.blindmint.codex.presentation.settings.appearance

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.components.SettingsSubcategory
import us.blindmint.codex.presentation.settings.reader.misc.components.FullscreenOption
import us.blindmint.codex.presentation.settings.reader.misc.components.HideBarsOnFastScrollOption
import us.blindmint.codex.presentation.settings.reader.misc.components.KeepScreenOnOption
import us.blindmint.codex.presentation.settings.reader.system.components.CustomScreenBrightnessOption
import us.blindmint.codex.presentation.settings.reader.system.components.ScreenBrightnessOption
import us.blindmint.codex.presentation.settings.reader.system.components.ScreenOrientationOption

fun LazyListScope.SystemSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary }
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = { stringResource(id = R.string.system_reader_settings) },
        showTitle = true,
        showDivider = false
    ) {
        item {
            FullscreenOption()
        }

        item {
            KeepScreenOnOption()
        }

        item {
            HideBarsOnFastScrollOption()
        }

        item {
            CustomScreenBrightnessOption()
        }

        item {
            ScreenBrightnessOption()
        }

        item {
            ScreenOrientationOption()
        }
    }
}
