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

private val primaryLight = Color(0xFF7D5800)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDF9E)
private val onPrimaryContainerLight = Color(0xFF271900)
private val secondaryLight = Color(0xFF6E5C40)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFF9DFBB)
private val onSecondaryContainerLight = Color(0xFF261A04)
private val tertiaryLight = Color(0xFF4F6442)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFD0EABE)
private val onTertiaryContainerLight = Color(0xFF0E2006)
private val errorLight = Color(0xFFB3261E)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFF9DEDC)
private val onErrorContainerLight = Color(0xFF410E0B)
private val backgroundLight = Color(0xFFFFF8F2)
private val onBackgroundLight = Color(0xFF1F1B13)
private val surfaceLight = Color(0xFFFFF8F2)
private val onSurfaceLight = Color(0xFF1F1B13)
private val surfaceVariantLight = Color(0xFFEEE0CF)
private val onSurfaceVariantLight = Color(0xFF4E4639)
private val outlineLight = Color(0xFF807567)
private val outlineVariantLight = Color(0xFFD2C4B4)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF343027)
private val inverseOnSurfaceLight = Color(0xFFF8EFE2)
private val inversePrimaryLight = Color(0xFFFABD00)
private val surfaceDimLight = Color(0xFFE3D9CC)
private val surfaceBrightLight = Color(0xFFFFF8F2)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFEF2E5)
private val surfaceContainerLight = Color(0xFFF8ECDE)
private val surfaceContainerHighLight = Color(0xFFF2E7D9)
private val surfaceContainerHighestLight = Color(0xFFECE1D3)


private val primaryDark = Color(0xFFFABD00)
private val onPrimaryDark = Color(0xFF402D00)
private val primaryContainerDark = Color(0xFF5D4200)
private val onPrimaryContainerDark = Color(0xFFFFDF9E)
private val secondaryDark = Color(0xFFDBC7A1)
private val onSecondaryDark = Color(0xFF3B2E16)
private val secondaryContainerDark = Color(0xFF54442A)
private val onSecondaryContainerDark = Color(0xFFF9DFBB)
private val tertiaryDark = Color(0xFFB4CFA3)
private val onTertiaryDark = Color(0xFF1F3619)
private val tertiaryContainerDark = Color(0xFF374D2D)
private val onTertiaryContainerDark = Color(0xFFD0EABE)
private val errorDark = Color(0xFFF2B8B5)
private val onErrorDark = Color(0xFF601410)
private val errorContainerDark = Color(0xFF8C1D18)
private val onErrorContainerDark = Color(0xFFF9DEDC)
private val backgroundDark = Color(0xFF16130B)
private val onBackgroundDark = Color(0xFFECE1D3)
private val surfaceDark = Color(0xFF16130B)
private val onSurfaceDark = Color(0xFFECE1D3)
private val surfaceVariantDark = Color(0xFF4E4639)
private val onSurfaceVariantDark = Color(0xFFD2C4B4)
private val outlineDark = Color(0xFF9A8F80)
private val outlineVariantDark = Color(0xFF4E4639)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFECE1D3)
private val inverseOnSurfaceDark = Color(0xFF343027)
private val inversePrimaryDark = Color(0xFF7D5800)
private val surfaceDimDark = Color(0xFF16130B)
private val surfaceBrightDark = Color(0xFF3C392F)
private val surfaceContainerLowestDark = Color(0xFF110E07)
private val surfaceContainerLowDark = Color(0xFF1F1B13)
private val surfaceContainerDark = Color(0xFF231F17)
private val surfaceContainerHighDark = Color(0xFF2E2A21)
private val surfaceContainerHighestDark = Color(0xFF39352B)


@Composable
fun titanTheme(isDark: Boolean): ColorScheme {
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