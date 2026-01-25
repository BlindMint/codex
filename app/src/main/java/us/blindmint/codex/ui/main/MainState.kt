/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("UNCHECKED_CAST")

package us.blindmint.codex.ui.main

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.Preferences
import kotlinx.parcelize.Parcelize
import us.blindmint.codex.domain.browse.display.BrowseLayout
import us.blindmint.codex.domain.dictionary.DictionarySource
import us.blindmint.codex.domain.dictionary.toDictionarySource
import us.blindmint.codex.domain.browse.display.BrowseSortOrder
import us.blindmint.codex.domain.browse.display.toBrowseLayout
import us.blindmint.codex.domain.browse.display.toBrowseSortOrder
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.display.LibraryTitlePosition
import us.blindmint.codex.domain.library.display.toLibraryTitlePosition
import us.blindmint.codex.domain.library.sort.LibrarySortOrder
import us.blindmint.codex.domain.library.sort.toLibrarySortOrder
import us.blindmint.codex.domain.reader.BackgroundImage
import us.blindmint.codex.domain.reader.BackgroundScaleMode
import us.blindmint.codex.domain.reader.CustomFont
import us.blindmint.codex.domain.reader.ReaderColorEffects
import us.blindmint.codex.domain.reader.toBackgroundScaleMode
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.domain.reader.ReaderHorizontalGesture
import us.blindmint.codex.domain.reader.ReaderProgressCount
import us.blindmint.codex.domain.reader.ReaderScreenOrientation
import us.blindmint.codex.domain.reader.ReaderTextAlignment
import us.blindmint.codex.domain.reader.toColorEffects
import us.blindmint.codex.domain.reader.toFontThickness
import us.blindmint.codex.domain.reader.toHorizontalGesture
import us.blindmint.codex.domain.reader.toProgressCount
import us.blindmint.codex.domain.reader.toReaderScreenOrientation
import us.blindmint.codex.domain.reader.toTextAlignment
import us.blindmint.codex.domain.ui.DarkTheme
import us.blindmint.codex.domain.ui.PureDark
import us.blindmint.codex.domain.ui.ThemeContrast
import us.blindmint.codex.domain.ui.toDarkTheme
import us.blindmint.codex.domain.ui.toPureDark
import us.blindmint.codex.domain.ui.toThemeContrast
import us.blindmint.codex.domain.util.HorizontalAlignment
import us.blindmint.codex.domain.util.toHorizontalAlignment
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.core.constants.provideLanguages
import us.blindmint.codex.ui.theme.Theme
import us.blindmint.codex.ui.theme.toTheme
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Main State.
 * All app's settings/preferences/permanent-variables are here.
 * Wrapped in SavedStateHandle, so it won't reset.
 */
@Immutable
@Keep
@Parcelize
data class MainState(
    // General Settings
    val language: String = provideDefaultValue {
        "en" // Default language is always English
    },
    val theme: Theme = provideDefaultValue { Theme.entries().first() },
    val darkTheme: DarkTheme = provideDefaultValue { DarkTheme.FOLLOW_SYSTEM },
    val pureDark: PureDark = provideDefaultValue { PureDark.OFF },
    val absoluteDark: Boolean = provideDefaultValue { false },
    val themeContrast: ThemeContrast = provideDefaultValue { ThemeContrast.STANDARD },
    val showStartScreen: Boolean = provideDefaultValue { false },
    val doublePressExit: Boolean = provideDefaultValue { false },

    // Reader Settings
    val fontFamily: String = provideDefaultValue { provideFonts()[0].id },
    val customFonts: List<CustomFont> = provideDefaultValue { emptyList() },
    val fontThickness: ReaderFontThickness = provideDefaultValue { ReaderFontThickness.NORMAL },
    val isItalic: Boolean = provideDefaultValue { false },
    val fontSize: Int = provideDefaultValue { 16 },
    val lineHeight: Int = provideDefaultValue { 4 },
    val paragraphHeight: Int = provideDefaultValue { 8 },
    val paragraphIndentation: Int = provideDefaultValue { 0 },
    val sidePadding: Int = provideDefaultValue { 6 },
    val verticalPadding: Int = provideDefaultValue { 0 },
    val doubleClickTranslation: Boolean = provideDefaultValue { false },
    val fastColorPresetChange: Boolean = provideDefaultValue { true },
    val textAlignment: ReaderTextAlignment = provideDefaultValue { ReaderTextAlignment.JUSTIFY },
    val letterSpacing: Int = provideDefaultValue { 0 },
    val cutoutPadding: Boolean = provideDefaultValue { false },
    val fullscreen: Boolean = provideDefaultValue { true },
    val keepScreenOn: Boolean = provideDefaultValue { true },
    val hideBarsOnFastScroll: Boolean = provideDefaultValue { false },
    val perceptionExpander: Boolean = provideDefaultValue { false },
    val perceptionExpanderPadding: Int = provideDefaultValue { 5 },
    val perceptionExpanderThickness: Int = provideDefaultValue { 4 },
    val screenOrientation: ReaderScreenOrientation = provideDefaultValue {
        ReaderScreenOrientation.DEFAULT
    },
    val customScreenBrightness: Boolean = provideDefaultValue { false },
    val screenBrightness: Float = provideDefaultValue { 0.5f },
    val horizontalGesture: ReaderHorizontalGesture = provideDefaultValue {
        ReaderHorizontalGesture.OFF
    },
    val horizontalGestureScroll: Float = provideDefaultValue { 0.7f },
    val horizontalGestureSensitivity: Float = provideDefaultValue { 0.6f },
    val horizontalGestureAlphaAnim: Boolean = provideDefaultValue { true },
    val horizontalGesturePullAnim: Boolean = provideDefaultValue { true },
    val bottomBarPadding: Int = provideDefaultValue { 0 },
    val highlightedReading: Boolean = provideDefaultValue { false },
    val highlightedReadingThickness: Int = provideDefaultValue { 2 },
    val chapterTitleAlignment: ReaderTextAlignment = provideDefaultValue { ReaderTextAlignment.JUSTIFY },
    val images: Boolean = provideDefaultValue { true },
    val imagesCornersRoundness: Int = provideDefaultValue { 8 },
    val imagesAlignment: HorizontalAlignment = provideDefaultValue { HorizontalAlignment.START },
    val imagesWidth: Float = provideDefaultValue { 0.8f },
    val imagesColorEffects: ReaderColorEffects = provideDefaultValue { ReaderColorEffects.OFF },
    val progressBar: Boolean = provideDefaultValue { false },
    val progressBarPadding: Int = provideDefaultValue { 4 },
    val progressBarAlignment: HorizontalAlignment = provideDefaultValue { HorizontalAlignment.CENTER },
    val progressBarFontSize: Int = provideDefaultValue { 8 },
    val progressCount: ReaderProgressCount = provideDefaultValue { ReaderProgressCount.PERCENTAGE },
    // Default: Yellow with 50% alpha (#80FFEB3B)
    val searchHighlightColor: Long = provideDefaultValue { 0x80FFEB3B },
    // Default: 0.9 to match readerBarsColor (surfaceContainer.copy(0.9f))
    val searchScrollbarOpacity: Double = provideDefaultValue { 0.9 },
    val showSearchScrollbar: Boolean = provideDefaultValue { false },

    // Comic Reader Settings
    val comicReadingDirection: String = provideDefaultValue { "LTR" },
    val comicReaderMode: String = provideDefaultValue { "PAGED" },
    val comicTapZone: Int = provideDefaultValue { 0 },
    val comicInvertTaps: String = provideDefaultValue { "NONE" },
    val comicScaleType: Int = provideDefaultValue { 1 },
    val comicProgressBar: Boolean = provideDefaultValue { true },
    val comicProgressBarPadding: Int = provideDefaultValue { 4 },
    val comicProgressBarAlignment: HorizontalAlignment = provideDefaultValue { HorizontalAlignment.CENTER },
    val comicProgressBarFontSize: Int = provideDefaultValue { 8 },
    val comicProgressCount: ReaderProgressCount = provideDefaultValue { ReaderProgressCount.PAGE },
    val comicBackgroundColor: String = provideDefaultValue { "DEFAULT" },

    // Background Image Settings
    val backgroundImage: BackgroundImage? = provideDefaultValue { null },
    val customBackgroundImages: List<BackgroundImage> = provideDefaultValue { emptyList() },
    val backgroundImageOpacity: Float = provideDefaultValue { 0.3f },
    val backgroundScaleMode: BackgroundScaleMode = provideDefaultValue { BackgroundScaleMode.COVER },

    // Dictionary Settings
    val dictionarySource: DictionarySource = provideDefaultValue { DictionarySource.SYSTEM_DEFAULT },
    val customDictionaryUrl: String = provideDefaultValue { "" },
    val doubleTapDictionary: Boolean = provideDefaultValue { false },
    val offlineDictionaryEnabled: Boolean = provideDefaultValue { false },
    val openLookupsInApp: Boolean = provideDefaultValue { true },

    // Browse Settings
    val browseLayout: BrowseLayout = provideDefaultValue { BrowseLayout.LIST },
    val browseAutoGridSize: Boolean = provideDefaultValue { true },
    val browseGridSize: Int = provideDefaultValue { 0 },
    val browseSortOrder: BrowseSortOrder = provideDefaultValue { BrowseSortOrder.LAST_MODIFIED },
    val browseSortOrderDescending: Boolean = provideDefaultValue { true },
    val browseIncludedFilterItems: List<String> = provideDefaultValue { emptyList() },
    val browsePinnedPaths: List<String> = provideDefaultValue { emptyList() },
    val useCalibreOpfMetadata: Boolean = provideDefaultValue { true },

    // Library Settings
    val libraryLayout: LibraryLayout = provideDefaultValue { LibraryLayout.GRID },
    val libraryAutoGridSize: Boolean = provideDefaultValue { true },
    val libraryGridSize: Int = provideDefaultValue { 0 },
    val libraryListSize: Int = provideDefaultValue { 1 }, // 0=Small, 1=Medium, 2=Large
    val librarySortOrder: LibrarySortOrder = provideDefaultValue { LibrarySortOrder.LAST_READ },
    val librarySortOrderDescending: Boolean = provideDefaultValue { true },
    val libraryShowCategoryTabs: Boolean = provideDefaultValue { true },
    val libraryAlwaysShowDefaultTab: Boolean = provideDefaultValue { false },
    val libraryShowBookCount: Boolean = provideDefaultValue { true },
    val libraryTitlePosition: LibraryTitlePosition = provideDefaultValue { LibraryTitlePosition.BELOW },
    val libraryShowReadButton: Boolean = provideDefaultValue { true },
    val libraryShowProgress: Boolean = provideDefaultValue { true },

    // Settings
    val autoColorPresetSelected: Boolean = provideDefaultValue { false },

    // Speed Reader Settings
    val speedReadingWpm: Int = provideDefaultValue { 300 },
    val speedReadingManualSentencePauseEnabled: Boolean = provideDefaultValue { false },
    val speedReadingSentencePauseDuration: Int = provideDefaultValue { 350 },
    val speedReadingOsdEnabled: Boolean = provideDefaultValue { true },
    val speedReadingWordSize: Int = provideDefaultValue { 48 },
    val speedReadingAccentCharacterEnabled: Boolean = provideDefaultValue { true },
    val speedReadingAccentColor: Long = provideDefaultValue { Color(0xFFFF0000).toArgb().toLong() }, // Bright red
    val speedReadingAccentOpacity: Float = provideDefaultValue { 1.0f },
    val speedReadingShowVerticalIndicators: Boolean = provideDefaultValue { true },
    val speedReadingVerticalIndicatorsSize: Int = provideDefaultValue { 8 },
    val speedReadingVerticalIndicatorType: String = provideDefaultValue { "LINE" },
    val speedReadingShowHorizontalBars: Boolean = provideDefaultValue { true },
    val speedReadingHorizontalBarsThickness: Int = provideDefaultValue { 2 },
    val speedReadingHorizontalBarsLength: Float = provideDefaultValue { 0.9f },
    val speedReadingHorizontalBarsDistance: Int = provideDefaultValue { 8 },
    val speedReadingHorizontalBarsColor: Long = provideDefaultValue { Color(0xFF424242).toArgb().toLong() }, // Medium-dark gray
    val speedReadingHorizontalBarsOpacity: Float = provideDefaultValue { 1.0f },
    val speedReadingFocalPointPosition: Float = provideDefaultValue { 0.38f },
    val speedReadingOsdHeight: Float = provideDefaultValue { 0.2f },
    val speedReadingOsdSeparation: Float = provideDefaultValue { 0.5f },
    val speedReadingAutoHideOsd: Boolean = provideDefaultValue { true },
    val speedReadingCenterWord: Boolean = provideDefaultValue { false },
    val speedReadingFocusIndicators: String = provideDefaultValue { "OFF" },
    val speedReadingCustomFontEnabled: Boolean = provideDefaultValue { false },
    val speedReadingSelectedFontFamily: String = provideDefaultValue { "default" },
) : Parcelable {
    companion object {
        private fun <D> provideDefaultValue(calculation: () -> D): D {
            return calculation()
        }

        /**
         * Initializes [MainState] by given [Map].
         * If no value provided in [data], assigns default value.
         */
        fun initialize(data: Map<String, Any>): MainState {
            val defaultState = MainState()
            fun <V, T> provideValue(
                key: Preferences.Key<T>,
                convert: T.() -> V = { this as V },
                default: MainState.() -> V
            ): V {
                return (data[key.name] as? T)?.convert() ?: defaultState.default()
            }

            return DataStoreConstants.run {
                MainState(
                    language = provideValue(
                        LANGUAGE
                    ) { language },

                    theme = provideValue(
                        THEME, convert = { toTheme() }
                    ) { theme },

                    darkTheme = provideValue(
                        DARK_THEME, convert = { toDarkTheme() }
                    ) { darkTheme },

                    pureDark = provideValue(
                        PURE_DARK, convert = { toPureDark() }
                    ) { pureDark },

                    absoluteDark = provideValue(
                        ABSOLUTE_DARK
                    ) { absoluteDark },

                    themeContrast = provideValue(
                        THEME_CONTRAST, convert = { toThemeContrast() }
                    ) { themeContrast },

                    showStartScreen = provideValue(
                        SHOW_START_SCREEN
                    ) { showStartScreen },

                    fontFamily = provideValue(
                        FONT
                    ) { fontFamily },

                    customFonts = provideValue(
                        CUSTOM_FONTS, convert = {
                            this.mapNotNull { CustomFont.fromString(it) }
                        }
                    ) { customFonts },

                    isItalic = provideValue(
                        IS_ITALIC
                    ) { isItalic },

                    fontSize = provideValue(
                        FONT_SIZE
                    ) { fontSize },

                    lineHeight = provideValue(
                        LINE_HEIGHT
                    ) { lineHeight },

                    paragraphHeight = provideValue(
                        PARAGRAPH_HEIGHT
                    ) { paragraphHeight },

                    paragraphIndentation = provideValue(
                        PARAGRAPH_INDENTATION
                    ) { paragraphIndentation },

                    sidePadding = provideValue(
                        SIDE_PADDING
                    ) { sidePadding },

                    doubleClickTranslation = provideValue(
                        DOUBLE_CLICK_TRANSLATION
                    ) { doubleClickTranslation },

                    fastColorPresetChange = provideValue(
                        FAST_COLOR_PRESET_CHANGE
                    ) { fastColorPresetChange },

                    browseLayout = provideValue(
                        BROWSE_LAYOUT, convert = { toBrowseLayout() }
                    ) { browseLayout },

                    browseAutoGridSize = provideValue(
                        BROWSE_AUTO_GRID_SIZE
                    ) { browseAutoGridSize },

                    browseGridSize = provideValue(
                        BROWSE_GRID_SIZE
                    ) { browseGridSize },

                    browseSortOrder = provideValue(
                        BROWSE_SORT_ORDER, convert = { toBrowseSortOrder() }
                    ) { browseSortOrder },

                    browseSortOrderDescending = provideValue(
                        BROWSE_SORT_ORDER_DESCENDING
                    ) { browseSortOrderDescending },

                    browseIncludedFilterItems = provideValue(
                        BROWSE_INCLUDED_FILTER_ITEMS, convert = { toList() }
                    ) { browseIncludedFilterItems },

                    textAlignment = provideValue(
                        TEXT_ALIGNMENT, convert = { toTextAlignment() }
                    ) { textAlignment },

                    doublePressExit = provideValue(
                        DOUBLE_PRESS_EXIT
                    ) { doublePressExit },

                    letterSpacing = provideValue(
                        LETTER_SPACING
                    ) { letterSpacing },

                    cutoutPadding = provideValue(
                        CUTOUT_PADDING
                    ) { cutoutPadding },

                    fullscreen = provideValue(
                        FULLSCREEN
                    ) { fullscreen },

                    keepScreenOn = provideValue(
                        KEEP_SCREEN_ON
                    ) { keepScreenOn },

                    verticalPadding = provideValue(
                        VERTICAL_PADDING
                    ) { verticalPadding },

                    hideBarsOnFastScroll = provideValue(
                        HIDE_BARS_ON_FAST_SCROLL
                    ) { hideBarsOnFastScroll },

                    perceptionExpander = provideValue(
                        PERCEPTION_EXPANDER
                    ) { perceptionExpander },

                    perceptionExpanderPadding = provideValue(
                        PERCEPTION_EXPANDER_PADDING
                    ) { perceptionExpanderPadding },

                    perceptionExpanderThickness = provideValue(
                        PERCEPTION_EXPANDER_THICKNESS
                    ) { perceptionExpanderThickness },

                    screenOrientation = provideValue(
                        SCREEN_ORIENTATION, convert = { toReaderScreenOrientation() }
                    ) { screenOrientation },

                    customScreenBrightness = provideValue(
                        CUSTOM_SCREEN_BRIGHTNESS
                    ) { customScreenBrightness },

                    screenBrightness = provideValue(
                        SCREEN_BRIGHTNESS, convert = { this.toFloat() }
                    ) { screenBrightness },

                    horizontalGesture = provideValue(
                        HORIZONTAL_GESTURE, convert = { toHorizontalGesture() }
                    ) { horizontalGesture },

                    horizontalGestureScroll = provideValue(
                        HORIZONTAL_GESTURE_SCROLL, convert = { toFloat() }
                    ) { horizontalGestureScroll },

                    horizontalGestureSensitivity = provideValue(
                        HORIZONTAL_GESTURE_SENSITIVITY, convert = { toFloat() }
                    ) { horizontalGestureSensitivity },

                    bottomBarPadding = provideValue(
                        BOTTOM_BAR_PADDING
                    ) { bottomBarPadding },

                    highlightedReading = provideValue(
                        HIGHLIGHTED_READING
                    ) { highlightedReading },

                    highlightedReadingThickness = provideValue(
                        HIGHLIGHTED_READING_THICKNESS
                    ) { highlightedReadingThickness },

                    chapterTitleAlignment = provideValue(
                        CHAPTER_TITLE_ALIGNMENT, convert = { toTextAlignment() }
                    ) { chapterTitleAlignment },

                    images = provideValue(
                        IMAGES
                    ) { images },

                    imagesCornersRoundness = provideValue(
                        IMAGES_CORNERS_ROUNDNESS
                    ) { imagesCornersRoundness },

                    imagesAlignment = provideValue(
                        IMAGES_ALIGNMENT, convert = { toHorizontalAlignment() }
                    ) { imagesAlignment },

                    imagesWidth = provideValue(
                        IMAGES_WIDTH, convert = { toFloat() }
                    ) { imagesWidth },

                    imagesColorEffects = provideValue(
                        IMAGES_COLOR_EFFECTS, convert = { toColorEffects() }
                    ) { imagesColorEffects },

                    progressBar = provideValue(
                        PROGRESS_BAR
                    ) { progressBar },

                    progressBarPadding = provideValue(
                        PROGRESS_BAR_PADDING
                    ) { progressBarPadding },

                    progressBarAlignment = provideValue(
                        PROGRESS_BAR_ALIGNMENT, convert = { toHorizontalAlignment() }
                    ) { progressBarAlignment },

                    progressBarFontSize = provideValue(
                        PROGRESS_BAR_FONT_SIZE
                    ) { progressBarFontSize },

                    browsePinnedPaths = provideValue(
                        BROWSE_PINNED_PATHS, convert = { toList() }
                    ) { browsePinnedPaths },

                    useCalibreOpfMetadata = provideValue(
                        USE_CALIBRE_OPF_METADATA
                    ) { useCalibreOpfMetadata },

                    fontThickness = provideValue(
                        FONT_THICKNESS, convert = { toFontThickness() }
                    ) { fontThickness },

                    progressCount = provideValue(
                        PROGRESS_COUNT, convert = { toProgressCount() }
                    ) { progressCount },

                    searchHighlightColor = provideValue(
                        SEARCH_HIGHLIGHT_COLOR, convert = { toLongOrNull() ?: 0x80FFEB3B }
                    ) { searchHighlightColor },

                    searchScrollbarOpacity = provideValue(
                        SEARCH_SCROLLBAR_OPACITY, convert = { toDouble() }
                    ) { searchScrollbarOpacity },

                    showSearchScrollbar = provideValue(
                        SHOW_SEARCH_SCROLLBAR
                    ) { showSearchScrollbar },

                    backgroundImage = provideValue(
                        BACKGROUND_IMAGE, convert = { BackgroundImage.fromString(this) }
                    ) { backgroundImage },

                    customBackgroundImages = provideValue(
                        CUSTOM_BACKGROUND_IMAGES, convert = {
                            this.mapNotNull { BackgroundImage.fromString(it) }
                        }
                    ) { customBackgroundImages },

                    backgroundImageOpacity = provideValue(
                        BACKGROUND_IMAGE_OPACITY, convert = { toFloat() }
                    ) { backgroundImageOpacity },

                    backgroundScaleMode = provideValue(
                        BACKGROUND_SCALE_MODE, convert = { toBackgroundScaleMode() }
                    ) { backgroundScaleMode },

                    dictionarySource = provideValue(
                        DICTIONARY_SOURCE, convert = { toDictionarySource() }
                    ) { dictionarySource },

                    customDictionaryUrl = provideValue(
                        CUSTOM_DICTIONARY_URL
                    ) { customDictionaryUrl },

                    doubleTapDictionary = provideValue(
                        DOUBLE_TAP_DICTIONARY
                    ) { doubleTapDictionary },

                    offlineDictionaryEnabled = provideValue(
                        OFFLINE_DICTIONARY_ENABLED
                    ) { offlineDictionaryEnabled },

                    openLookupsInApp = provideValue(
                        OPEN_LOOKUPS_IN_APP
                    ) { openLookupsInApp },

                    horizontalGestureAlphaAnim = provideValue(
                        HORIZONTAL_GESTURE_ALPHA_ANIM
                    ) { horizontalGestureAlphaAnim },

                    horizontalGesturePullAnim = provideValue(
                        HORIZONTAL_GESTURE_PULL_ANIM
                    ) { horizontalGesturePullAnim },

                    autoColorPresetSelected = provideValue(
                        DataStoreConstants.AUTO_COLOR_PRESET_SELECTED
                    ) { autoColorPresetSelected },

                    libraryTitlePosition = provideValue(
                        LIBRARY_TITLE_POSITION, convert = { toLibraryTitlePosition() }
                    ) { libraryTitlePosition },

                    libraryShowReadButton = provideValue(
                        LIBRARY_SHOW_READ_BUTTON
                    ) { libraryShowReadButton },

                    libraryShowProgress = provideValue(
                        LIBRARY_SHOW_PROGRESS
                    ) { libraryShowProgress },

                    // Comic Reader Settings
                    comicReadingDirection = provideValue(
                        COMIC_READING_DIRECTION
                    ) { comicReadingDirection },

                    comicReaderMode = provideValue(
                        COMIC_READER_MODE
                    ) { comicReaderMode },

                    comicTapZone = provideValue(
                        COMIC_TAP_ZONE
                    ) { comicTapZone },

                    comicInvertTaps = provideValue(
                        COMIC_INVERT_TAPS
                    ) { comicInvertTaps },

                    comicScaleType = provideValue(
                        COMIC_SCALE_TYPE
                    ) { comicScaleType },

                    comicProgressBar = provideValue(
                        COMIC_PROGRESS_BAR
                    ) { comicProgressBar },

                    comicProgressBarPadding = provideValue(
                        COMIC_PROGRESS_BAR_PADDING
                    ) { comicProgressBarPadding },

                    comicProgressBarAlignment = provideValue(
                        COMIC_PROGRESS_BAR_ALIGNMENT, convert = { toHorizontalAlignment() }
                    ) { comicProgressBarAlignment },

                    comicProgressBarFontSize = provideValue(
                        COMIC_PROGRESS_BAR_FONT_SIZE
                    ) { comicProgressBarFontSize },

                    comicProgressCount = provideValue(
                        COMIC_PROGRESS_COUNT, convert = { toProgressCount() }
                    ) { comicProgressCount },

                    comicBackgroundColor = provideValue(
                        COMIC_BACKGROUND_COLOR
                    ) { comicBackgroundColor },

                    // Speed Reader Settings
                    speedReadingWpm = provideValue(
                        SPEED_READING_WPM
                    ) { speedReadingWpm },

                    speedReadingManualSentencePauseEnabled = provideValue(
                        SPEED_READING_MANUAL_SENTENCE_PAUSE_ENABLED
                    ) { speedReadingManualSentencePauseEnabled },

                    speedReadingSentencePauseDuration = provideValue(
                        SPEED_READING_SENTENCE_PAUSE_DURATION
                    ) { speedReadingSentencePauseDuration },

                    speedReadingOsdEnabled = provideValue(
                        SPEED_READING_OSD_ENABLED
                    ) { speedReadingOsdEnabled },

                    speedReadingWordSize = provideValue(
                        SPEED_READING_WORD_SIZE
                    ) { speedReadingWordSize },

                    speedReadingAccentCharacterEnabled = provideValue(
                        SPEED_READING_ACCENT_CHARACTER_ENABLED
                    ) { speedReadingAccentCharacterEnabled },

                    speedReadingAccentColor = provideValue(
                        SPEED_READING_ACCENT_COLOR, convert = { toLongOrNull(16) ?: 0xFFFF0000 }
                    ) { speedReadingAccentColor },

                    speedReadingAccentOpacity = provideValue(
                        SPEED_READING_ACCENT_OPACITY, convert = { toFloat() }
                    ) { speedReadingAccentOpacity },

                    speedReadingShowVerticalIndicators = provideValue(
                        SPEED_READING_SHOW_VERTICAL_INDICATORS
                    ) { speedReadingShowVerticalIndicators },

                    speedReadingVerticalIndicatorsSize = provideValue(
                        SPEED_READING_VERTICAL_INDICATORS_SIZE
                    ) { speedReadingVerticalIndicatorsSize },

                    speedReadingVerticalIndicatorType = provideValue(
                        SPEED_READING_VERTICAL_INDICATOR_TYPE
                    ) { speedReadingVerticalIndicatorType },

                    speedReadingShowHorizontalBars = provideValue(
                        SPEED_READING_SHOW_HORIZONTAL_BARS
                    ) { speedReadingShowHorizontalBars },

                    speedReadingHorizontalBarsThickness = provideValue(
                        SPEED_READING_HORIZONTAL_BARS_THICKNESS
                    ) { speedReadingHorizontalBarsThickness },

                    speedReadingHorizontalBarsLength = provideValue(
                        SPEED_READING_HORIZONTAL_BARS_LENGTH, convert = { toFloat() }
                    ) { speedReadingHorizontalBarsLength },

                    speedReadingHorizontalBarsDistance = provideValue(
                        SPEED_READING_HORIZONTAL_BARS_DISTANCE
                    ) { speedReadingHorizontalBarsDistance },

                    speedReadingHorizontalBarsColor = provideValue(
                        SPEED_READING_HORIZONTAL_BARS_COLOR, convert = { toLongOrNull(16) ?: 0xFF424242 }
                    ) { speedReadingHorizontalBarsColor },

                    speedReadingHorizontalBarsOpacity = provideValue(
                        SPEED_READING_HORIZONTAL_BARS_OPACITY, convert = { toFloat() }
                    ) { speedReadingHorizontalBarsOpacity },

                    speedReadingFocalPointPosition = provideValue(
                        SPEED_READING_FOCAL_POINT_POSITION, convert = { toFloat() }
                    ) { speedReadingFocalPointPosition },

                    speedReadingOsdHeight = provideValue(
                        SPEED_READING_OSD_HEIGHT, convert = { toFloat() }
                    ) { speedReadingOsdHeight },

                    speedReadingOsdSeparation = provideValue(
                        SPEED_READING_OSD_SEPARATION, convert = { toFloat() }
                    ) { speedReadingOsdSeparation },

                    speedReadingAutoHideOsd = provideValue(
                        SPEED_READING_AUTO_HIDE_OSD
                    ) { speedReadingAutoHideOsd },

                    speedReadingCenterWord = provideValue(
                        SPEED_READING_CENTER_WORD
                    ) { speedReadingCenterWord },

                    speedReadingFocusIndicators = provideValue(
                        SPEED_READING_FOCUS_INDICATORS
                    ) { speedReadingFocusIndicators },

                    speedReadingCustomFontEnabled = provideValue(
                        SPEED_READING_CUSTOM_FONT_ENABLED
                    ) { speedReadingCustomFontEnabled },

                    speedReadingSelectedFontFamily = provideValue(
                        SPEED_READING_SELECTED_FONT_FAMILY
                    ) { speedReadingSelectedFontFamily },

                )
            }
        }
    }
}