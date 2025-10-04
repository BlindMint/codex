/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import ua.blindmint.codex.R
import ua.blindmint.codex.domain.ui.ThemeContrast
import ua.blindmint.codex.ui.theme.color.uranusTheme
import ua.blindmint.codex.ui.theme.color.blackTheme
import ua.blindmint.codex.ui.theme.color.neptuneTheme
import ua.blindmint.codex.ui.theme.color.mercuryTheme
import ua.blindmint.codex.ui.theme.color.ioTheme
import ua.blindmint.codex.ui.theme.color.enceladusTheme
import ua.blindmint.codex.ui.theme.color.earthTheme
import ua.blindmint.codex.ui.theme.color.erisTheme
import ua.blindmint.codex.ui.theme.color.plutoTheme
import ua.blindmint.codex.ui.theme.color.ganymedeTheme
import ua.blindmint.codex.ui.theme.color.saturnTheme
import ua.blindmint.codex.ui.theme.color.ceresTheme
import ua.blindmint.codex.ui.theme.color.jupiterTheme
import ua.blindmint.codex.ui.theme.color.callistoTheme
import ua.blindmint.codex.ui.theme.color.marsTheme
import ua.blindmint.codex.ui.theme.color.makemakeTheme
import ua.blindmint.codex.ui.theme.color.venusTheme


@Immutable
enum class Theme(
    val hasThemeContrast: Boolean,
    @StringRes val title: Int
) {
    DYNAMIC(hasThemeContrast = false, title = R.string.dynamic_theme),
    BLUE(hasThemeContrast = true, title = R.string.blue_theme),
    GREEN(hasThemeContrast = true, title = R.string.green_theme),
    GREEN2(hasThemeContrast = false, title = R.string.green2_theme),
    GREEN_GRAY(hasThemeContrast = false, title = R.string.green_gray_theme),
    MARSH(hasThemeContrast = true, title = R.string.marsh_theme),
    RED(hasThemeContrast = true, title = R.string.red_theme),
    RED_GRAY(hasThemeContrast = false, title = R.string.red_gray_theme),
    PURPLE(hasThemeContrast = true, title = R.string.purple_theme),
    PURPLE_GRAY(hasThemeContrast = false, title = R.string.purple_gray_theme),
    LAVENDER(hasThemeContrast = true, title = R.string.lavender_theme),
    PINK(hasThemeContrast = true, title = R.string.pink_theme),
    PINK2(hasThemeContrast = false, title = R.string.pink2_theme),
    YELLOW(hasThemeContrast = true, title = R.string.yellow_theme),
    YELLOW2(hasThemeContrast = false, title = R.string.yellow2_theme),
    AQUA(hasThemeContrast = true, title = R.string.aqua_theme);

    companion object {
        fun entries(): List<Theme> {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> Theme.entries
                else -> Theme.entries.dropWhile { it == DYNAMIC }
            }
        }
    }
}

/**
 * Converting [String] into [Theme].
 */
fun String.toTheme(): Theme {
    return Theme.valueOf(this)
}

/**
 * Creates a colorscheme based on [Theme].
 *
 * @param theme a [Theme].
 *
 * @return a [ColorScheme].
 */
@Composable
fun colorScheme(
    theme: Theme,
    darkTheme: Boolean,
    isPureDark: Boolean,
    themeContrast: ThemeContrast
): ColorScheme {
    val colorScheme = when (theme) {
        Theme.DYNAMIC -> {
            /* Mercury Theme */
            mercuryTheme(isDark = darkTheme)
        }

        Theme.BLUE -> {
            /* Neptune Theme */
            neptuneTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.PURPLE -> {
            /* Jupiter Theme */
            jupiterTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.PURPLE_GRAY -> {
            /* Ceres Theme */
            ceresTheme(isDark = darkTheme)
        }

        Theme.GREEN -> {
            /* Earth Theme */
            earthTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.GREEN2 -> {
            /* Io Theme */
            ioTheme(isDark = darkTheme)
        }

        Theme.GREEN_GRAY -> {
            /* Enceladus Theme */
            enceladusTheme(isDark = darkTheme)
        }

        Theme.MARSH -> {
            /* Pluto Theme */
            plutoTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.PINK -> {
            /* Saturn Theme */
            saturnTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.PINK2 -> {
            /* Ganymede Theme */
            ganymedeTheme(isDark = darkTheme)
        }

        Theme.LAVENDER -> {
            /* Eris Theme */
            erisTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.YELLOW -> {
            /* Venus Theme */
            venusTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.YELLOW2 -> {
            /* Makemake Theme */
            makemakeTheme(isDark = darkTheme)
        }

        Theme.RED -> {
            /* Mars Theme */
            marsTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.RED_GRAY -> {
            /* Callisto Theme */
            callistoTheme(isDark = darkTheme)
        }

        Theme.AQUA -> {
            /* Uranus Theme */
            uranusTheme(isDark = darkTheme, themeContrast = themeContrast)
        }
    }

    return if (isPureDark && darkTheme) {
        blackTheme(initialTheme = colorScheme)
    } else {
        colorScheme
    }
}