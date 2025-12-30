/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.core.constants

import us.blindmint.codex.R
import us.blindmint.codex.domain.about.Credit
import us.blindmint.codex.domain.ui.UIText

fun provideCredits() = listOf(
    Credit(
        name = "Book's Story",
        source = "GitHub",
        credits = listOf(
            UIText.StringResource(R.string.credits_design),
            UIText.StringResource(R.string.credits_ideas)
        ),
        website = "https://github.com/Acclorite/book-story"
    ),
    Credit(
        name = "Tachiyomi (Mihon)",
        source = "GitHub",
        credits = listOf(
            UIText.StringResource(R.string.credits_design),
            UIText.StringResource(R.string.credits_ideas),
            UIText.StringValue("Readme")
        ),
        website = "https://github.com/mihonapp/mihon"
    ),
    Credit(
        name = "Kitsune",
        source = "GitHub",
        credits = listOf(
            UIText.StringResource(R.string.credits_updates),
            UIText.StringResource(R.string.credits_ideas)
        ),
        website = "https://github.com/Drumber/Kitsune"
    ),
    Credit(
        name = "Voyager",
        source = "Voyager Website",
        credits = listOf(
            UIText.StringResource(R.string.credits_ideas)
        ),
        website = "https://voyager.adriel.cafe/"
    ),
    Credit(
        name = "Material Design Icons",
        source = "Google Fonts",
        credits = listOf(
            UIText.StringResource(R.string.credits_icon)
        ),
        website = "https://fonts.google.com/icons"
    ),
    Credit(
        name = "Material Design Fonts",
        source = "Google Fonts",
        credits = listOf(
            UIText.StringResource(R.string.credits_fonts)
        ),
        website = "https://fonts.google.com"
    ),
    Credit(
        name = "GitHub Badge",
        source = "GitHub",
        credits = listOf(
            UIText.StringResource(R.string.credits_icon)
        ),
        website = "https://github.com/Kunzisoft/Github-badge"
    ),
    Credit(
        name = "Codeberg Badge",
        source = "Codeberg",
        credits = listOf(
            UIText.StringResource(R.string.credits_icon)
        ),
        website = "https://codeberg.org/Codeberg/GetItOnCodeberg"
    ),
)