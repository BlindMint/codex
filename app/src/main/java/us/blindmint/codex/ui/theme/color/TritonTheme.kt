/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.theme.color

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val primaryLight = Color(0xFF4A4458)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFDED8F0)
private val onPrimaryContainerLight = Color(0xFF181121)
private val secondaryLight = Color(0xFF5B5A6F)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFE0DFF6)
private val onSecondaryContainerLight = Color(0xFF181829)
private val tertiaryLight = Color(0xFF745470)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFED7F8)
private val onTertiaryContainerLight = Color(0xFF2B0F29)
private val errorLight = Color(0xFFB3261E)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFF9DEDC)
private val onErrorContainerLight = Color(0xFF410E0B)
private val backgroundLight = Color(0xFFFCF8FF)
private val onBackgroundLight = Color(0xFF1C1B1F)
private val surfaceLight = Color(0xFFFCF8FF)
private val onSurfaceLight = Color(0xFF1C1B1F)
private val surfaceVariantLight = Color(0xFFE6E0EC)
private val onSurfaceVariantLight = Color(0xFF48454E)
private val outlineLight = Color(0xFF79767F)
private val outlineVariantLight = Color(0xFFC9C5D0)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF313034)
private val inverseOnSurfaceLight = Color(0xFFF3EFF4)
private val inversePrimaryLight = Color(0xFFC4C0D8)
private val surfaceDimLight = Color(0xFFDDD8E0)
private val surfaceBrightLight = Color(0xFFFCF8FF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF7F2FA)
private val surfaceContainerLight = Color(0xFFF1EDF5)
private val surfaceContainerHighLight = Color(0xFFEBE7EF)
private val surfaceContainerHighestLight = Color(0xFFE5E1E9)


private val primaryDark = Color(0xFFC4C0D8)
private val onPrimaryDark = Color(0xFF2D2A37)
private val primaryContainerDark = Color(0xFF353041)
private val onPrimaryContainerDark = Color(0xFFDED8F0)
private val secondaryDark = Color(0xFFC4C3DA)
private val onSecondaryDark = Color(0xFF2D2D3F)
private val secondaryContainerDark = Color(0xFF434256)
private val onSecondaryContainerDark = Color(0xFFE0DFF6)
private val tertiaryDark = Color(0xFFE5BAD7)
private val onTertiaryDark = Color(0xFF42203E)
private val tertiaryContainerDark = Color(0xFF5B3757)
private val onTertiaryContainerDark = Color(0xFFFED7F8)
private val errorDark = Color(0xFFF2B8B5)
private val onErrorDark = Color(0xFF601410)
private val errorContainerDark = Color(0xFF8C1D18)
private val onErrorContainerDark = Color(0xFFF9DEDC)
private val backgroundDark = Color(0xFF131216)
private val onBackgroundDark = Color(0xFFE5E1E9)
private val surfaceDark = Color(0xFF131216)
private val onSurfaceDark = Color(0xFFE5E1E9)
private val surfaceVariantDark = Color(0xFF48454E)
private val onSurfaceVariantDark = Color(0xFFC9C5D0)
private val outlineDark = Color(0xFF938F99)
private val outlineVariantDark = Color(0xFF48454E)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE5E1E9)
private val inverseOnSurfaceDark = Color(0xFF313034)
private val inversePrimaryDark = Color(0xFF4A4458)
private val surfaceDimDark = Color(0xFF131216)
private val surfaceBrightDark = Color(0xFF39383D)
private val surfaceContainerLowestDark = Color(0xFF0E0E11)
private val surfaceContainerLowDark = Color(0xFF1C1B1F)
private val surfaceContainerDark = Color(0xFF201F23)
private val surfaceContainerHighDark = Color(0xFF2A2A2E)
private val surfaceContainerHighestDark = Color(0xFF353438)


@Composable
fun tritonTheme(isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primaryDark,
            onPrimary = onPrimaryDark,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = onPrimaryContainerDark,
            secondary = secondaryDark,
            onSecondary = onSecondaryDark,
            secondaryContainer = secondaryContainerDark,
            onSecondaryContainer = onSecondaryContainerDark,
            tertiary = tertiaryDark,
            onTertiary = onTertiaryDark,
            tertiaryContainer = tertiaryContainerDark,
            onTertiaryContainer = onTertiaryContainerDark,
            error = errorDark,
            onError = onErrorDark,
            errorContainer = errorContainerDark,
            onErrorContainer = onErrorContainerDark,
            background = backgroundDark,
            onBackground = onBackgroundDark,
            surface = surfaceDark,
            onSurface = onSurfaceDark,
            surfaceVariant = surfaceVariantDark,
            onSurfaceVariant = onSurfaceVariantDark,
            outline = outlineDark,
            outlineVariant = outlineVariantDark,
            scrim = scrimDark,
            inverseSurface = inverseSurfaceDark,
            inverseOnSurface = inverseOnSurfaceDark,
            inversePrimary = inversePrimaryDark,
            surfaceDim = surfaceDimDark,
            surfaceBright = surfaceBrightDark,
            surfaceContainerLowest = surfaceContainerLowestDark,
            surfaceContainerLow = surfaceContainerLowDark,
            surfaceContainer = surfaceContainerDark,
            surfaceContainerHigh = surfaceContainerHighDark,
            surfaceContainerHighest = surfaceContainerHighestDark,
        )
    } else {
        lightColorScheme(
            primary = primaryLight,
            onPrimary = onPrimaryLight,
            primaryContainer = primaryContainerLight,
            onPrimaryContainer = onPrimaryContainerLight,
            secondary = secondaryLight,
            onSecondary = onSecondaryLight,
            secondaryContainer = secondaryContainerLight,
            onSecondaryContainer = onSecondaryContainerLight,
            tertiary = tertiaryLight,
            onTertiary = onTertiaryLight,
            tertiaryContainer = tertiaryContainerLight,
            onTertiaryContainer = onTertiaryContainerLight,
            error = errorLight,
            onError = onErrorLight,
            errorContainer = errorContainerLight,
            onErrorContainer = onErrorContainerLight,
            background = backgroundLight,
            onBackground = onBackgroundLight,
            surface = surfaceLight,
            onSurface = onSurfaceLight,
            surfaceVariant = surfaceVariantLight,
            onSurfaceVariant = onSurfaceVariantLight,
            outline = outlineLight,
            outlineVariant = outlineVariantLight,
            scrim = scrimLight,
            inverseSurface = inverseSurfaceLight,
            inverseOnSurface = inverseOnSurfaceLight,
            inversePrimary = inversePrimaryLight,
            surfaceDim = surfaceDimLight,
            surfaceBright = surfaceBrightLight,
            surfaceContainerLowest = surfaceContainerLowestLight,
            surfaceContainerLow = surfaceContainerLowLight,
            surfaceContainer = surfaceContainerLight,
            surfaceContainerHigh = surfaceContainerHighLight,
            surfaceContainerHighest = surfaceContainerHighestLight,
        )
    }
}