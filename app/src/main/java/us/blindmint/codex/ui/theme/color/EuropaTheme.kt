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

private val primaryLight = Color(0xFF006493)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFCAE6FF)
private val onPrimaryContainerLight = Color(0xFF001E31)
private val secondaryLight = Color(0xFF50606F)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFD3E5F5)
private val onSecondaryContainerLight = Color(0xFF0C1D29)
private val tertiaryLight = Color(0xFF66587B)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFEDDCFF)
private val onTertiaryContainerLight = Color(0xFF211634)
private val errorLight = Color(0xFFB3261E)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFF9DEDC)
private val onErrorContainerLight = Color(0xFF410E0B)
private val backgroundLight = Color(0xFFF8F9FF)
private val onBackgroundLight = Color(0xFF191C20)
private val surfaceLight = Color(0xFFF8F9FF)
private val onSurfaceLight = Color(0xFF191C20)
private val surfaceVariantLight = Color(0xFFDDE3EA)
private val onSurfaceVariantLight = Color(0xFF41484D)
private val outlineLight = Color(0xFF71787E)
private val outlineVariantLight = Color(0xFFC1C7CE)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2E3135)
private val inverseOnSurfaceLight = Color(0xFFEFF1F6)
private val inversePrimaryLight = Color(0xFF91CDFF)
private val surfaceDimLight = Color(0xFFD8DAE0)
private val surfaceBrightLight = Color(0xFFF8F9FF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF2F3F9)
private val surfaceContainerLight = Color(0xFFECEEF4)
private val surfaceContainerHighLight = Color(0xFFE6E8EE)
private val surfaceContainerHighestLight = Color(0xFFE0E2E8)


private val primaryDark = Color(0xFF91CDFF)
private val onPrimaryDark = Color(0xFF003351)
private val primaryContainerDark = Color(0xFF004B71)
private val onPrimaryContainerDark = Color(0xFFCAE6FF)
private val secondaryDark = Color(0xFFB7C9D9)
private val onSecondaryDark = Color(0xFF21323F)
private val secondaryContainerDark = Color(0xFF374956)
private val onSecondaryContainerDark = Color(0xFFD3E5F5)
private val tertiaryDark = Color(0xFFD1BCDF)
private val onTertiaryDark = Color(0xFF362C4A)
private val tertiaryContainerDark = Color(0xFF4D4162)
private val onTertiaryContainerDark = Color(0xFFEDDCFF)
private val errorDark = Color(0xFFF2B8B5)
private val onErrorDark = Color(0xFF601410)
private val errorContainerDark = Color(0xFF8C1D18)
private val onErrorContainerDark = Color(0xFFF9DEDC)
private val backgroundDark = Color(0xFF101418)
private val onBackgroundDark = Color(0xFFE0E2E8)
private val surfaceDark = Color(0xFF101418)
private val onSurfaceDark = Color(0xFFE0E2E8)
private val surfaceVariantDark = Color(0xFF41484D)
private val onSurfaceVariantDark = Color(0xFFC1C7CE)
private val outlineDark = Color(0xFF8B9198)
private val outlineVariantDark = Color(0xFF41484D)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE0E2E8)
private val inverseOnSurfaceDark = Color(0xFF2E3135)
private val inversePrimaryDark = Color(0xFF006493)
private val surfaceDimDark = Color(0xFF101418)
private val surfaceBrightDark = Color(0xFF363A40)
private val surfaceContainerLowestDark = Color(0xFF0B0F12)
private val surfaceContainerLowDark = Color(0xFF191C20)
private val surfaceContainerDark = Color(0xFF1D2024)
private val surfaceContainerHighDark = Color(0xFF272A2F)
private val surfaceContainerHighestDark = Color(0xFF32353A)


@Composable
fun europaTheme(isDark: Boolean): ColorScheme {
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