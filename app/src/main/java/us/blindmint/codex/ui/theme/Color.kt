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
import androidx.compose.ui.graphics.Color
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
 * Returns the background and onBackground colors for a given theme.
 * This is a non-Composable version for use in ViewModels where
 * Compose context is not available.
 *
 * For themes with dynamic colors (Mercury), uses generic Dark/Light presets.
 */
fun getThemeReaderColors(
    theme: Theme,
    isDark: Boolean,
    themeContrast: ThemeContrast = ThemeContrast.STANDARD
): Pair<Color, Color> {
    return when (theme) {
        Theme.CATPPUCCIN -> {
            if (isDark) {
                Color(0xFF181825) to Color(0xFFCDD6F4)
            } else {
                Color(0xFFE6E9EF) to Color(0xFF4C4F69)
            }
        }
        Theme.MERCURY -> {
            // Mercury uses dynamic colors, fall back to generic presets
            if (isDark) {
                Color(0xFF0F1419) to Color(0xFFE6E1E5)
            } else {
                Color(0xFFFFFFFF) to Color(0xFF1C1B1F)
            }
        }
        Theme.NEPTUNE -> {
            if (isDark) {
                Color(0xFF121318) to Color(0xFFE2E2E9)
            } else {
                Color(0xFFFAF8FF) to Color(0xFF1A1B21)
            }
        }
        Theme.EARTH -> {
            if (isDark) {
                Color(0xFF1C1F1B) to Color(0xFFE2E4DE)
            } else {
                Color(0xFFF8FAF5) to Color(0xFF191C19)
            }
        }
        Theme.IO -> {
            if (isDark) {
                Color(0xFF1C1F1B) to Color(0xFFDDE4DA)
            } else {
                Color(0xFFE1E8DE) to Color(0xFF191C1A)
            }
        }
        Theme.ENCELADUS -> {
            if (isDark) {
                Color(0xFF1C1F22) to Color(0xFFE1E3E6)
            } else {
                Color(0xFFE6E8EB) to Color(0xFF191C1F)
            }
        }
        Theme.PLUTO -> {
            if (isDark) {
                Color(0xFF1C1F24) to Color(0xFFE2E3E0)
            } else {
                Color(0xFFF1F5EF) to Color(0xFF191C1F)
            }
        }
        Theme.MARS -> {
            if (isDark) {
                Color(0xFF1C1B1E) to Color(0xFFE5E2E6)
            } else {
                Color(0xFFFBF8FB) to Color(0xFF191B1D)
            }
        }
        Theme.CALLISTO -> {
            if (isDark) {
                Color(0xFF1C1C1E) to Color(0xFFE2E2E3)
            } else {
                Color(0xFFE8E8E9) to Color(0xFF191C1C)
            }
        }
        Theme.JUPITER -> {
            if (isDark) {
                Color(0xFF1C1B22) to Color(0xFFE4E2E9)
            } else {
                Color(0xFFF9F6FC) to Color(0xFF1B1A20)
            }
        }
        Theme.CERES -> {
            if (isDark) {
                Color(0xFF1C1F22) to Color(0xFFE2E3E6)
            } else {
                Color(0xFFE6E9EC) to Color(0xFF191C1F)
            }
        }
        Theme.ERIS -> {
            if (isDark) {
                Color(0xFF1E1B1D) to Color(0xFFE8E4E6)
            } else {
                Color(0xFFFCF8FA) to Color(0xFF1B191A)
            }
        }
        Theme.SATURN -> {
            if (isDark) {
                Color(0xFF1E1B1D) to Color(0xFFE8E4E5)
            } else {
                Color(0xFFFBF7F8) to Color(0xFF1B1919)
            }
        }
        Theme.GANYMEDE -> {
            if (isDark) {
                Color(0xFF1C1B1E) to Color(0xFFE4E2E4)
            } else {
                Color(0xFFEAE8EA) to Color(0xFF191B1C)
            }
        }
        Theme.VENUS -> {
            if (isDark) {
                Color(0xFF1F1E1B) to Color(0xFFE6E4DF)
            } else {
                Color(0xFFFEF9E9) to Color(0xFF1D1B17)
            }
        }
        Theme.MAKEMAKE -> {
            if (isDark) {
                Color(0xFF1F1F1C) to Color(0xFFE5E5DE)
            } else {
                Color(0xFFEDEAD9) to Color(0xFF1D1D18)
            }
        }
        Theme.URANUS -> {
            if (isDark) {
                Color(0xFF1B1F20) to Color(0xFFDEE3E6)
            } else {
                Color(0xFFE6EBED) to Color(0xFF181C1E)
            }
        }
        Theme.TRITON -> {
            if (isDark) {
                Color(0xFF1C1D21) to Color(0xFFE2E3E7)
            } else {
                Color(0xFFE8E9ED) to Color(0xFF191B1F)
            }
        }
        Theme.EUROPA -> {
            if (isDark) {
                Color(0xFF1D1F24) to Color(0xFFE2E4E9)
            } else {
                Color(0xFFE6E9EF) to Color(0xFF1A1C20)
            }
        }
        Theme.TITAN -> {
            if (isDark) {
                Color(0xFF1D1F1E) to Color(0xFFE2E4E4)
            } else {
                Color(0xFFE7EAE9) to Color(0xFF1A1C1C)
            }
        }
        Theme.GRUVBOX -> {
            if (isDark) {
                Color(0xFF282828) to Color(0xFFD4BE98)
            } else {
                Color(0xFFFBF1C7) to Color(0xFF3C3836)
            }
        }
        Theme.NORD -> {
            if (isDark) {
                Color(0xFF2E3440) to Color(0xFFECEFF4)
            } else {
                Color(0xFFECEFF4) to Color(0xFF2E3440)
            }
        }
        Theme.DRACULA -> {
            if (isDark) {
                Color(0xFF282A36) to Color(0xFFF8F8F2)
            } else {
                Color(0xFF282A36) to Color(0xFFF8F8F2)
            }
        }
        Theme.TOKYO_NIGHT -> {
            if (isDark) {
                Color(0xFF1A1B26) to Color(0xFFA9B1D6)
            } else {
                Color(0xFF1A1B26) to Color(0xFFA9B1D6)
            }
        }
        Theme.HACKERMAN -> {
            if (isDark) {
                Color(0xFF0D0D0D) to Color(0xFF00FF00)
            } else {
                Color(0xFF0D0D0D) to Color(0xFF00FF00)
            }
        }
        Theme.ROSE_PINE -> {
            if (isDark) {
                Color(0xFF191724) to Color(0xFFE0DEF4)
            } else {
                Color(0xFF191724) to Color(0xFFE0DEF4)
            }
        }
        Theme.KANAGAWA -> {
            if (isDark) {
                Color(0xFF1F2335) to Color(0xFFC0CAF0)
            } else {
                Color(0xFFFDF6E3) to Color(0xFF54546D)
            }
        }
    }
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