/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.compose.runtime.Composable
import us.blindmint.codex.domain.navigator.Screen

/**
 * Interface for settings screens that support search.
 *
 * Based on Mihon's SearchableSettings interface.
 *
 * Implementing screens should:
 * - Provide a title for use in search breadcrumbs
 * - Provide a list of Preference objects for search indexing
 *
 * The companion object provides a highlightKey mechanism for highlighting
 * matching items when navigating from search results.
 */
interface SearchableSettings : Screen {

    /**
     * Returns the screen's title for use in search breadcrumbs.
     *
     * @return The localized screen title
     */
    @Composable
    fun getTitle(): String

    /**
     * Returns the screen's preferences for search indexing.
     *
     * This method should return a list of Preference objects that
     * represent all settings on this screen. The preferences can
     * include PreferenceGroups for organization.
     *
     * @return List of preferences for this screen
     */
    @Composable
    fun getPreferences(): List<Preference>

    companion object {
        /**
         * The title of the target PreferenceItem to highlight.
         *
         * This should be set before navigating to a settings screen
         * and will be reset after the highlight animation completes.
         *
         * HACK: This static property is used for cross-screen
         * communication to trigger the highlight animation when
         * navigating from search results.
         */
        var highlightKey: String? = null
    }
}
