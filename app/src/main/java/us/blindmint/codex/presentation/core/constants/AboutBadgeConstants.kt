/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.constants

import us.blindmint.codex.R
import us.blindmint.codex.domain.about.Badge

fun provideAboutBadges() = listOf(
    Badge(
        id = "github",
        drawable = R.drawable.github,
        imageVector = null,
        contentDescription = R.string.github_badge,
        url = "https://www.github.com/BlindMint"
    ),
    Badge(
        id = "gitlab",
        drawable = R.drawable.gitlab,
        imageVector = null,
        contentDescription = R.string.gitlab_badge,
        url = "https://gitlab.com/BlindMint/codex"
    ),
    Badge(
        id = "codeberg",
        drawable = R.drawable.codeberg,
        imageVector = null,
        contentDescription = R.string.codeberg_badge,
        url = "https://www.codeberg.org/BlindMint"
    ),
)