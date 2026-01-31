/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.settings

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.R
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.top_bar.collapsibleTopAppBarScrollBehavior
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.settings.Preference
import us.blindmint.codex.presentation.settings.SearchableSettings
import us.blindmint.codex.presentation.settings.reader.ReaderSettingsContent
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Parcelize
object ReaderSettingsScreen : Screen, Parcelable, SearchableSettings {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val (scrollBehavior, _) = TopAppBarDefaults.collapsibleTopAppBarScrollBehavior()

        ReaderSettingsContent(
            scrollBehavior = scrollBehavior,
            navigateBack = {
                navigator.pop()
            }
        )
    }

    @Composable
    override fun getTitle(): String {
        return stringResource(id = R.string.reader_settings)
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val mainModel = hiltViewModel<MainModel>()
        val state = mainModel.state.collectAsStateWithLifecycle()

        return listOf(
            getFontPreferencesGroup(mainModel, state),
            getTextPreferencesGroup(mainModel, state),
            getImagesPreferencesGroup(mainModel, state),
            getChaptersPreferencesGroup(mainModel, state),
            getReadingModePreferencesGroup(mainModel, state),
            getPaddingPreferencesGroup(mainModel, state),
            getSystemPreferencesGroup(mainModel, state),
            getReadingSpeedPreferencesGroup(mainModel, state),
            getProgressPreferencesGroup(mainModel, state),
            getSearchPreferencesGroup(mainModel, state),
            getDictionaryPreferencesGroup(mainModel, state),
            getMiscPreferencesGroup(mainModel, state),
        )
    }

    @Composable
    private fun getFontPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.font_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = state.value.fontFamily,
                    entries = mapOf(
                        "sans-serif" to "Sans Serif",
                        "serif" to "Serif",
                        "monospace" to "Monospace",
                    ),
                    title = stringResource(id = R.string.font_family_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeFontFamily(it))
                    },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.fontSize,
                    valueRange = 10..35,
                    title = stringResource(id = R.string.font_size_option),
                    valueString = "${state.value.fontSize} pt",
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeFontSize(it))
                    },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.lineHeight,
                    valueRange = 10..30,
                    title = stringResource(id = R.string.line_height_option),
                    valueString = "${state.value.lineHeight / 100.0f}x",
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeLineHeight(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getTextPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.text_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.paragraphHeight,
                    valueRange = 0..20,
                    title = stringResource(id = R.string.paragraph_height_option),
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeParagraphHeight(it))
                    },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.paragraphIndentation,
                    valueRange = 0..10,
                    title = stringResource(id = R.string.paragraph_indentation_option),
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeParagraphIndentation(it))
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    value = state.value.textAlignment.name,
                    entries = mapOf(
                        "START" to "Left",
                        "CENTER" to "Center",
                        "END" to "Right",
                        "JUSTIFY" to "Justify",
                    ),
                    title = stringResource(id = R.string.text_alignment_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeTextAlignment(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getImagesPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.images_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.InfoPreference(
                    title = "Background Image Settings",
                ),
            ),
        )
    }

    @Composable
    private fun getChaptersPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.chapters_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.verticalPadding,
                    valueRange = 0..50,
                    title = stringResource(id = R.string.vertical_padding_option),
                    valueString = "${state.value.verticalPadding}%",
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeVerticalPadding(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getReadingModePreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.reading_mode_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.InfoPreference(
                    title = "Reading Mode Settings",
                ),
            ),
        )
    }

    @Composable
    private fun getPaddingPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.padding_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.sidePadding,
                    valueRange = 0..30,
                    title = stringResource(id = R.string.side_padding_option),
                    valueString = "${state.value.sidePadding} dp",
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangeSidePadding(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getSystemPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.system_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.cutout_padding_option),
                    checked = state.value.cutoutPadding,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeCutoutPadding(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getReadingSpeedPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.reading_speed_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = state.value.perceptionExpanderPadding,
                    valueRange = 0..20,
                    title = "Perception Expander Padding",
                    onValueChanged = {
                        mainModel.onEvent(MainEvent.OnChangePerceptionExpanderPadding(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getProgressPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.progress_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    value = state.value.progressCount.name,
                    entries = mapOf(
                        "PERCENTAGE" to "Percentage",
                        "PAGES" to "Pages",
                        "CHAPTER" to "Chapter",
                    ),
                    title = stringResource(id = R.string.progress_count_option),
                    onValueChange = {
                        mainModel.onEvent(MainEvent.OnChangeProgressCount(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getSearchPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.search_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = "Show search scrollbar",
                    checked = state.value.showSearchScrollbar,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeShowSearchScrollbar(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDictionaryPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Dictionary",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.double_click_translation_option),
                    checked = state.value.doubleClickTranslation,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeDoubleClickTranslation(it))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getMiscPreferencesGroup(
        mainModel: MainModel,
        state: androidx.compose.runtime.State<us.blindmint.codex.ui.main.MainState>
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(id = R.string.misc_reader_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    title = stringResource(id = R.string.double_press_exit_option),
                    checked = state.value.doublePressExit,
                    onCheckedChanged = {
                        mainModel.onEvent(MainEvent.OnChangeDoublePressExit(it))
                    },
                ),
            ),
        )
    }
}
