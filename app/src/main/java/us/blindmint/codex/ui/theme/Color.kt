/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ThemeContrast
import us.blindmint.codex.ui.theme.color.uranusTheme
import us.blindmint.codex.ui.theme.color.blackTheme
import us.blindmint.codex.ui.theme.color.neptuneTheme
import us.blindmint.codex.ui.theme.color.mercuryTheme
import us.blindmint.codex.ui.theme.color.ioTheme
import us.blindmint.codex.ui.theme.color.enceladusTheme
import us.blindmint.codex.ui.theme.color.earthTheme
import us.blindmint.codex.ui.theme.color.erisTheme
import us.blindmint.codex.ui.theme.color.plutoTheme
import us.blindmint.codex.ui.theme.color.ganymedeTheme
import us.blindmint.codex.ui.theme.color.saturnTheme
import us.blindmint.codex.ui.theme.color.ceresTheme
import us.blindmint.codex.ui.theme.color.jupiterTheme
import us.blindmint.codex.ui.theme.color.callistoTheme
import us.blindmint.codex.ui.theme.color.marsTheme
import us.blindmint.codex.ui.theme.color.makemakeTheme
import us.blindmint.codex.ui.theme.color.venusTheme
import us.blindmint.codex.ui.theme.color.europaTheme
import us.blindmint.codex.ui.theme.color.titanTheme
import us.blindmint.codex.ui.theme.color.tritonTheme
import us.blindmint.codex.ui.theme.color.catppuccinTheme
import us.blindmint.codex.ui.theme.color.gruvboxTheme
import us.blindmint.codex.ui.theme.color.nordTheme
import us.blindmint.codex.ui.theme.color.draculaTheme
import us.blindmint.codex.ui.theme.color.tokyoNightTheme
import us.blindmint.codex.ui.theme.color.hackermanTheme
import us.blindmint.codex.ui.theme.color.rosePineTheme
import us.blindmint.codex.ui.theme.color.kanagawaTheme


@Immutable
enum class Theme(
    val hasThemeContrast: Boolean,
    @StringRes val title: Int
) {
    CATPPUCCIN(hasThemeContrast = false, title = R.string.catppuccin_theme),
    MERCURY(hasThemeContrast = false, title = R.string.dynamic_theme),
    NEPTUNE(hasThemeContrast = true, title = R.string.blue_theme),
    EARTH(hasThemeContrast = true, title = R.string.green_theme),
    IO(hasThemeContrast = false, title = R.string.green2_theme),
    ENCELADUS(hasThemeContrast = false, title = R.string.green_gray_theme),
    PLUTO(hasThemeContrast = true, title = R.string.marsh_theme),
    MARS(hasThemeContrast = true, title = R.string.red_theme),
    CALLISTO(hasThemeContrast = false, title = R.string.red_gray_theme),
    JUPITER(hasThemeContrast = true, title = R.string.purple_theme),
    CERES(hasThemeContrast = false, title = R.string.purple_gray_theme),
    ERIS(hasThemeContrast = true, title = R.string.lavender_theme),
    SATURN(hasThemeContrast = true, title = R.string.pink_theme),
    GANYMEDE(hasThemeContrast = false, title = R.string.pink2_theme),
    VENUS(hasThemeContrast = true, title = R.string.yellow_theme),
    MAKEMAKE(hasThemeContrast = false, title = R.string.yellow2_theme),
    URANUS(hasThemeContrast = true, title = R.string.aqua_theme),
    TRITON(hasThemeContrast = false, title = R.string.triton_theme),
    EUROPA(hasThemeContrast = false, title = R.string.europa_theme),
    TITAN(hasThemeContrast = false, title = R.string.titan_theme),
    GRUVBOX(hasThemeContrast = false, title = R.string.gruvbox_theme),
    NORD(hasThemeContrast = false, title = R.string.nord_theme),
    DRACULA(hasThemeContrast = false, title = R.string.dracula_theme),
    TOKYO_NIGHT(hasThemeContrast = false, title = R.string.tokyo_night_theme),
    HACKERMAN(hasThemeContrast = false, title = R.string.hackerman_theme),
    ROSE_PINE(hasThemeContrast = false, title = R.string.rose_pine_theme),
    KANAGAWA(hasThemeContrast = false, title = R.string.kanagawa_theme);

    companion object {
        fun entries(): List<Theme> {
            return Theme.entries
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
        Theme.MERCURY -> {
            /* Mercury Theme */
            mercuryTheme(isDark = darkTheme)
        }

        Theme.NEPTUNE -> {
            /* Neptune Theme */
            neptuneTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.JUPITER -> {
            /* Jupiter Theme */
            jupiterTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.CERES -> {
            /* Ceres Theme */
            ceresTheme(isDark = darkTheme)
        }

        Theme.EARTH -> {
            /* Earth Theme */
            earthTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.IO -> {
            /* Io Theme */
            ioTheme(isDark = darkTheme)
        }

        Theme.ENCELADUS -> {
            /* Enceladus Theme */
            enceladusTheme(isDark = darkTheme)
        }

        Theme.PLUTO -> {
            /* Pluto Theme */
            plutoTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.SATURN -> {
            /* Saturn Theme */
            saturnTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.GANYMEDE -> {
            /* Ganymede Theme */
            ganymedeTheme(isDark = darkTheme)
        }

        Theme.ERIS -> {
            /* Eris Theme */
            erisTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.VENUS -> {
            /* Venus Theme */
            venusTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.MAKEMAKE -> {
            /* Makemake Theme */
            makemakeTheme(isDark = darkTheme)
        }

        Theme.MARS -> {
            /* Mars Theme */
            marsTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.CALLISTO -> {
            /* Callisto Theme */
            callistoTheme(isDark = darkTheme)
        }

        Theme.URANUS -> {
            /* Uranus Theme */
            uranusTheme(isDark = darkTheme, themeContrast = themeContrast)
        }

        Theme.EUROPA -> {
            /* Europa Theme */
            europaTheme(isDark = darkTheme)
        }

        Theme.TITAN -> {
            /* Titan Theme */
            titanTheme(isDark = darkTheme)
        }

        Theme.TRITON -> {
            /* Triton Theme */
            tritonTheme(isDark = darkTheme)
        }

        Theme.CATPPUCCIN -> {
            /* Catppuccin Theme */
            catppuccinTheme(isDark = darkTheme)
        }

        Theme.GRUVBOX -> {
            /* Gruvbox Theme */
            gruvboxTheme(isDark = darkTheme)
        }

        Theme.NORD -> {
            /* Nord Theme */
            nordTheme(isDark = darkTheme)
        }

        Theme.DRACULA -> {
            /* Dracula Theme */
            draculaTheme(isDark = darkTheme)
        }

        Theme.TOKYO_NIGHT -> {
            /* Tokyo Night Theme */
            tokyoNightTheme(isDark = darkTheme)
        }

        Theme.HACKERMAN -> {
            /* Hackerman Theme */
            hackermanTheme(isDark = darkTheme)
        }

        Theme.ROSE_PINE -> {
            /* Rose Pine Theme */
            rosePineTheme(isDark = darkTheme)
        }

        Theme.KANAGAWA -> {
            /* Kanagawa Theme */
            kanagawaTheme(isDark = darkTheme)
        }
    }

    return if (isPureDark && darkTheme) {
        blackTheme(initialTheme = colorScheme)
    } else {
        colorScheme
    }
}