/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable

/**
 * Sealed class representing a settings preference.
 *
 * Based on Mihon's Preference hierarchy.
 *
 * @property title The display title for the preference
 * @property enabled Whether the preference is enabled and interactive
 */
sealed class Preference {
    abstract val title: String
    abstract val enabled: Boolean

    /**
     * Sealed class representing individual preference items.
     *
     * @param T The value type for this preference
     * @param R The result type returned by the value change callback
     * @property subtitle Optional subtitle/description text
     * @property icon Optional icon (ImageVector or Painter)
     * @property onValueChanged Callback invoked when the preference value changes
     */
    sealed class PreferenceItem<T, R> : Preference() {
        abstract val subtitle: String?
        abstract val icon: Any?
        abstract val onValueChanged: suspend (value: T) -> R

        /**
         * A preference item that provides a two-state toggleable option.
         */
        data class SwitchPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val checked: Boolean,
            val onCheckedChanged: (Boolean) -> Unit,
        ) : PreferenceItem<Boolean, Unit>() {
            override val onValueChanged: suspend (Boolean) -> Unit = { onCheckedChanged(it) }
        }

        /**
         * A preference item that provides a slider to select an integer number.
         */
        data class SliderPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val value: Int,
            val valueRange: IntProgression,
            val valueString: String? = null,
            @IntRange(from = 0) val steps: Int = with(valueRange) { (last - first) - 1 },
            override val onValueChanged: suspend (value: Int) -> Unit,
        ) : PreferenceItem<Int, Unit>()

        /**
         * A preference item that displays a list of entries as a dialog.
         */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val value: T,
            val entries: Map<T, String>,
            val onValueChange: (T) -> Unit,
        ) : PreferenceItem<T, Unit>() {
            override val onValueChanged: suspend (T) -> Unit = { onValueChange(it) }
        }

        /**
         * A preference item that displays text and is clickable.
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val onClick: () -> Unit,
        ) : PreferenceItem<String, Unit>() {
            override val onValueChanged: suspend (value: String) -> Unit = { }
        }

        /**
         * A preference item that shows an EditText in a dialog.
         */
        data class EditTextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val icon: Any? = null,
            val value: String,
            val onValueChange: (String) -> Boolean,
        ) : PreferenceItem<String, Boolean>() {
            override val onValueChanged: suspend (String) -> Boolean = { onValueChange(it) }
        }

        /**
         * A preference item that displays informational text.
         */
        data class InfoPreference(
            override val title: String,
        ) : PreferenceItem<String, Unit>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: Any? = null
            override val onValueChanged: suspend (value: String) -> Unit = { }
        }

        /**
         * A preference item that displays custom content.
         */
        data class CustomPreference(
            override val title: String,
            override val enabled: Boolean = true,
            override val subtitle: String? = null,
            override val icon: Any? = null,
            val content: @Composable () -> Unit,
        ) : PreferenceItem<Unit, Unit>() {
            override val onValueChanged: suspend (value: Unit) -> Unit = { }
        }
    }

    /**
     * A group of related preference items with a header title.
     *
     * @property title The group header title
     * @property enabled Whether the group is enabled (disabled groups are hidden)
     * @property preferenceItems The list of preference items in this group
     */
    data class PreferenceGroup(
        override val title: String,
        override val enabled: Boolean = true,
        val preferenceItems: List<Preference.PreferenceItem<out Any, out Any>>,
    ) : Preference()
}
