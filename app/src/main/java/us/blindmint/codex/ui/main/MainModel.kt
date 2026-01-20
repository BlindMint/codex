/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.main

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import us.blindmint.codex.domain.browse.display.toBrowseLayout
import us.blindmint.codex.domain.browse.display.toBrowseSortOrder
import us.blindmint.codex.domain.library.display.LibraryLayout
import us.blindmint.codex.domain.library.display.toLibraryTitlePosition
import us.blindmint.codex.domain.library.sort.toLibrarySortOrder
import us.blindmint.codex.domain.reader.BackgroundImage
import us.blindmint.codex.domain.reader.CustomFont
import us.blindmint.codex.domain.reader.toBackgroundScaleMode
import us.blindmint.codex.domain.reader.toColorEffects
import us.blindmint.codex.domain.reader.toFontThickness
import us.blindmint.codex.domain.reader.toHorizontalGesture
import us.blindmint.codex.domain.reader.toProgressCount
import us.blindmint.codex.domain.reader.toReaderScreenOrientation
import us.blindmint.codex.domain.reader.toTextAlignment
import us.blindmint.codex.domain.ui.toDarkTheme
import us.blindmint.codex.domain.ui.toPureDark
import us.blindmint.codex.domain.ui.toThemeContrast
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.use_case.data_store.ChangeLanguage
import us.blindmint.codex.domain.use_case.data_store.GetAllSettings
import us.blindmint.codex.domain.use_case.data_store.SetDatastore
import us.blindmint.codex.domain.util.toHorizontalAlignment
import us.blindmint.codex.presentation.core.constants.DataStoreConstants
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.core.constants.provideMainState
import us.blindmint.codex.ui.theme.toTheme
import javax.inject.Inject


@HiltViewModel
class MainModel @Inject constructor(
    private val stateHandle: SavedStateHandle,

    private val setDatastore: SetDatastore,
    private val changeLanguage: ChangeLanguage,
    private val getAllSettings: GetAllSettings,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val mutex = Mutex()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val mainModelReady = MutableStateFlow(false)

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(
        stateHandle[provideMainState()] ?: MainState()
    )
    val state = _state.asStateFlow()

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.OnChangeLanguage -> handleLanguageUpdate(event)

            is MainEvent.OnChangeDarkTheme -> handleDatastoreUpdate(
                key = DataStoreConstants.DARK_THEME,
                value = event.value,
                updateState = {
                    it.copy(darkTheme = toDarkTheme())
                }
            )

            is MainEvent.OnChangePureDark -> handleDatastoreUpdate(
                key = DataStoreConstants.PURE_DARK,
                value = event.value,
                updateState = {
                    it.copy(pureDark = toPureDark())
                }
            )

            is MainEvent.OnChangeThemeContrast -> handleDatastoreUpdate(
                key = DataStoreConstants.THEME_CONTRAST,
                value = event.value,
                updateState = {
                    it.copy(themeContrast = toThemeContrast())
                }
            )

            is MainEvent.OnChangeTheme -> handleDatastoreUpdate(
                key = DataStoreConstants.THEME,
                value = event.value,
                updateState = {
                    it.copy(theme = toTheme())
                }
            )

            is MainEvent.OnChangeFontFamily -> handleDatastoreUpdate(
                key = DataStoreConstants.FONT,
                value = event.value,
                updateState = {
                    it.copy(
                        fontFamily = if (event.value.startsWith("custom_")) {
                            // Preserve custom font IDs as-is
                            event.value
                        } else {
                            // For built-in fonts, validate against available fonts
                            provideFonts().run {
                                find { font ->
                                    font.id == event.value
                                }?.id ?: get(0).id
                            }
                        }
                    )
                }
            )

            is MainEvent.OnAddCustomFont -> handleAddCustomFont(event)

            is MainEvent.OnRemoveCustomFont -> handleRemoveCustomFont(event)

            is MainEvent.OnChangeFontStyle -> handleDatastoreUpdate(
                key = DataStoreConstants.IS_ITALIC,
                value = event.value,
                updateState = {
                    it.copy(isItalic = this)
                }
            )

            is MainEvent.OnChangeFontSize -> handleDatastoreUpdate(
                key = DataStoreConstants.FONT_SIZE,
                value = event.value,
                updateState = {
                    it.copy(fontSize = this)
                }
            )

            is MainEvent.OnChangeLineHeight -> handleDatastoreUpdate(
                key = DataStoreConstants.LINE_HEIGHT,
                value = event.value,
                updateState = {
                    it.copy(lineHeight = this)
                }
            )

            is MainEvent.OnChangeParagraphHeight -> handleDatastoreUpdate(
                key = DataStoreConstants.PARAGRAPH_HEIGHT,
                value = event.value,
                updateState = {
                    it.copy(paragraphHeight = this)
                }
            )

            is MainEvent.OnChangeParagraphIndentation -> handleDatastoreUpdate(
                key = DataStoreConstants.PARAGRAPH_INDENTATION,
                value = event.value,
                updateState = {
                    it.copy(paragraphIndentation = this)
                }
            )

            is MainEvent.OnChangeShowStartScreen -> handleDatastoreUpdate(
                key = DataStoreConstants.SHOW_START_SCREEN,
                value = event.value,
                updateState = {
                    it.copy(showStartScreen = this)
                }
            )

            is MainEvent.OnChangeSidePadding -> handleDatastoreUpdate(
                key = DataStoreConstants.SIDE_PADDING,
                value = event.value,
                updateState = {
                    it.copy(sidePadding = this)
                }
            )

            is MainEvent.OnChangeDoubleClickTranslation -> handleDatastoreUpdate(
                key = DataStoreConstants.DOUBLE_CLICK_TRANSLATION,
                value = event.value,
                updateState = {
                    it.copy(doubleClickTranslation = this)
                }
            )

            is MainEvent.OnChangeFastColorPresetChange -> handleDatastoreUpdate(
                key = DataStoreConstants.FAST_COLOR_PRESET_CHANGE,
                value = event.value,
                updateState = {
                    it.copy(fastColorPresetChange = this)
                }
            )

            is MainEvent.OnChangeBrowseLayout -> handleDatastoreUpdate(
                key = DataStoreConstants.BROWSE_LAYOUT,
                value = event.value,
                updateState = {
                    it.copy(browseLayout = toBrowseLayout())
                }
            )

            is MainEvent.OnChangeBrowseAutoGridSize -> handleDatastoreUpdate(
                key = DataStoreConstants.BROWSE_AUTO_GRID_SIZE,
                value = event.value,
                updateState = {
                    it.copy(browseAutoGridSize = this)
                }
            )

            is MainEvent.OnChangeBrowseGridSize -> handleDatastoreUpdate(
                key = DataStoreConstants.BROWSE_GRID_SIZE,
                value = event.value,
                updateState = {
                    it.copy(browseGridSize = this)
                }
            )

            is MainEvent.OnChangeBrowseGridSizeSettings -> handleBrowseGridSizeSettingsUpdate(event)

            is MainEvent.OnChangeBrowseSortOrder -> handleDatastoreUpdate(
                key = DataStoreConstants.BROWSE_SORT_ORDER,
                value = event.value,
                updateState = {
                    it.copy(browseSortOrder = toBrowseSortOrder())
                }
            )

            is MainEvent.OnChangeBrowseSortOrderDescending -> handleDatastoreUpdate(
                key = DataStoreConstants.BROWSE_SORT_ORDER_DESCENDING,
                value = event.value,
                updateState = {
                    it.copy(browseSortOrderDescending = this)
                }
            )

            is MainEvent.OnChangeBrowseIncludedFilterItem -> handleBrowseIncludedFilterItemUpdate(
                event = event
            )

            is MainEvent.OnChangeUseCalibreOpfMetadata -> handleDatastoreUpdate(
                key = DataStoreConstants.USE_CALIBRE_OPF_METADATA,
                value = event.value,
                updateState = {
                    it.copy(useCalibreOpfMetadata = this)
                }
            )

            is MainEvent.OnChangeTextAlignment -> handleDatastoreUpdate(
                key = DataStoreConstants.TEXT_ALIGNMENT,
                value = event.value,
                updateState = {
                    it.copy(textAlignment = toTextAlignment())
                }
            )

            is MainEvent.OnChangeDoublePressExit -> handleDatastoreUpdate(
                key = DataStoreConstants.DOUBLE_PRESS_EXIT,
                value = event.value,
                updateState = {
                    it.copy(doublePressExit = this)
                }
            )

            is MainEvent.OnChangeLetterSpacing -> handleDatastoreUpdate(
                key = DataStoreConstants.LETTER_SPACING,
                value = event.value,
                updateState = {
                    it.copy(letterSpacing = this)
                }
            )

            is MainEvent.OnChangeAbsoluteDark -> handleDatastoreUpdate(
                key = DataStoreConstants.ABSOLUTE_DARK,
                value = event.value,
                updateState = {
                    it.copy(absoluteDark = this)
                }
            )

            is MainEvent.OnChangeCutoutPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.CUTOUT_PADDING,
                value = event.value,
                updateState = {
                    it.copy(cutoutPadding = this)
                }
            )

            is MainEvent.OnChangeFullscreen -> handleDatastoreUpdate(
                key = DataStoreConstants.FULLSCREEN,
                value = event.value,
                updateState = {
                    it.copy(fullscreen = this)
                }
            )

            is MainEvent.OnChangeKeepScreenOn -> handleDatastoreUpdate(
                key = DataStoreConstants.KEEP_SCREEN_ON,
                value = event.value,
                updateState = {
                    it.copy(keepScreenOn = this)
                }
            )

            is MainEvent.OnChangeVerticalPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.VERTICAL_PADDING,
                value = event.value,
                updateState = {
                    it.copy(verticalPadding = this)
                }
            )

            is MainEvent.OnChangeHideBarsOnFastScroll -> handleDatastoreUpdate(
                key = DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL,
                value = event.value,
                updateState = {
                    it.copy(hideBarsOnFastScroll = this)
                }
            )

            is MainEvent.OnChangePerceptionExpander -> handleDatastoreUpdate(
                key = DataStoreConstants.PERCEPTION_EXPANDER,
                value = event.value,
                updateState = {
                    it.copy(perceptionExpander = this)
                }
            )

            is MainEvent.OnChangePerceptionExpanderPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.PERCEPTION_EXPANDER_PADDING,
                value = event.value,
                updateState = {
                    it.copy(perceptionExpanderPadding = this)
                }
            )

            is MainEvent.OnChangePerceptionExpanderThickness -> handleDatastoreUpdate(
                key = DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS,
                value = event.value,
                updateState = {
                    it.copy(perceptionExpanderThickness = this)
                }
            )

            is MainEvent.OnChangeScreenOrientation -> handleDatastoreUpdate(
                key = DataStoreConstants.SCREEN_ORIENTATION,
                value = event.value,
                updateState = {
                    it.copy(screenOrientation = toReaderScreenOrientation())
                }
            )

            is MainEvent.OnChangeCustomScreenBrightness -> handleDatastoreUpdate(
                key = DataStoreConstants.CUSTOM_SCREEN_BRIGHTNESS,
                value = event.value,
                updateState = {
                    it.copy(customScreenBrightness = this)
                }
            )

            is MainEvent.OnChangeScreenBrightness -> handleDatastoreUpdate(
                key = DataStoreConstants.SCREEN_BRIGHTNESS,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(screenBrightness = this.toFloat())
                }
            )

            is MainEvent.OnChangeHorizontalGesture -> handleDatastoreUpdate(
                key = DataStoreConstants.HORIZONTAL_GESTURE,
                value = event.value,
                updateState = {
                    it.copy(horizontalGesture = toHorizontalGesture())
                }
            )

            is MainEvent.OnChangeHorizontalGestureScroll -> handleDatastoreUpdate(
                key = DataStoreConstants.HORIZONTAL_GESTURE_SCROLL,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(horizontalGestureScroll = this.toFloat())
                }
            )

            is MainEvent.OnChangeHorizontalGestureSensitivity -> handleDatastoreUpdate(
                key = DataStoreConstants.HORIZONTAL_GESTURE_SENSITIVITY,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(horizontalGestureSensitivity = this.toFloat())
                }
            )

            is MainEvent.OnChangeBottomBarPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.BOTTOM_BAR_PADDING,
                value = event.value,
                updateState = {
                    it.copy(bottomBarPadding = this)
                }
            )

            is MainEvent.OnChangeHighlightedReading -> handleDatastoreUpdate(
                key = DataStoreConstants.HIGHLIGHTED_READING,
                value = event.value,
                updateState = {
                    it.copy(highlightedReading = this)
                }
            )

            is MainEvent.OnChangeHighlightedReadingThickness -> handleDatastoreUpdate(
                key = DataStoreConstants.HIGHLIGHTED_READING_THICKNESS,
                value = event.value,
                updateState = {
                    it.copy(highlightedReadingThickness = this)
                }
            )

            is MainEvent.OnChangeChapterTitleAlignment -> handleDatastoreUpdate(
                key = DataStoreConstants.CHAPTER_TITLE_ALIGNMENT,
                value = event.value,
                updateState = {
                    it.copy(chapterTitleAlignment = toTextAlignment())
                }
            )

            is MainEvent.OnChangeImages -> handleDatastoreUpdate(
                key = DataStoreConstants.IMAGES,
                value = event.value,
                updateState = {
                    it.copy(images = this)
                }
            )

            is MainEvent.OnChangeImagesCornersRoundness -> handleDatastoreUpdate(
                key = DataStoreConstants.IMAGES_CORNERS_ROUNDNESS,
                value = event.value,
                updateState = {
                    it.copy(imagesCornersRoundness = this)
                }
            )

            is MainEvent.OnChangeImagesAlignment -> handleDatastoreUpdate(
                key = DataStoreConstants.IMAGES_ALIGNMENT,
                value = event.value,
                updateState = {
                    it.copy(imagesAlignment = this.toHorizontalAlignment())
                }
            )

            is MainEvent.OnChangeImagesWidth -> handleDatastoreUpdate(
                key = DataStoreConstants.IMAGES_WIDTH,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(imagesWidth = this.toFloat())
                }
            )

            is MainEvent.OnChangeImagesColorEffects -> handleDatastoreUpdate(
                key = DataStoreConstants.IMAGES_COLOR_EFFECTS,
                value = event.value,
                updateState = {
                    it.copy(imagesColorEffects = this.toColorEffects())
                }
            )

            is MainEvent.OnChangeProgressBar -> handleDatastoreUpdate(
                key = DataStoreConstants.PROGRESS_BAR,
                value = event.value,
                updateState = {
                    it.copy(progressBar = this)
                }
            )

            is MainEvent.OnChangeProgressBarPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.PROGRESS_BAR_PADDING,
                value = event.value,
                updateState = {
                    it.copy(progressBarPadding = this)
                }
            )

            is MainEvent.OnChangeProgressBarAlignment -> handleDatastoreUpdate(
                key = DataStoreConstants.PROGRESS_BAR_ALIGNMENT,
                value = event.value,
                updateState = {
                    it.copy(progressBarAlignment = this.toHorizontalAlignment())
                }
            )

            is MainEvent.OnChangeProgressBarFontSize -> handleDatastoreUpdate(
                key = DataStoreConstants.PROGRESS_BAR_FONT_SIZE,
                value = event.value,
                updateState = {
                    it.copy(progressBarFontSize = this)
                }
            )

            is MainEvent.OnChangeBrowsePinnedPaths -> handleBrowsePinnedPathsUpdate(
                event = event
            )

            is MainEvent.OnChangeFontThickness -> handleDatastoreUpdate(
                key = DataStoreConstants.FONT_THICKNESS,
                value = event.value,
                updateState = {
                    it.copy(fontThickness = this.toFontThickness())
                }
            )

            is MainEvent.OnChangeProgressCount -> handleDatastoreUpdate(
                key = DataStoreConstants.PROGRESS_COUNT,
                value = event.value,
                updateState = {
                    it.copy(progressCount = this.toProgressCount())
                }
            )

            is MainEvent.OnChangeHorizontalGestureAlphaAnim -> handleDatastoreUpdate(
                key = DataStoreConstants.HORIZONTAL_GESTURE_ALPHA_ANIM,
                value = event.value,
                updateState = {
                    it.copy(horizontalGestureAlphaAnim = this)
                }
            )

            is MainEvent.OnChangeHorizontalGesturePullAnim -> handleDatastoreUpdate(
                key = DataStoreConstants.HORIZONTAL_GESTURE_PULL_ANIM,
                value = event.value,
                updateState = {
                    it.copy(horizontalGesturePullAnim = this)
                }
            )

            is MainEvent.OnChangeSearchHighlightColor -> handleDatastoreUpdate(
                key = DataStoreConstants.SEARCH_HIGHLIGHT_COLOR,
                value = event.value.toString(),
                updateState = {
                    it.copy(searchHighlightColor = this.toLongOrNull() ?: 0x80FFEB3B)
                }
            )

            is MainEvent.OnChangeSearchScrollbarOpacity -> handleDatastoreUpdate(
                key = DataStoreConstants.SEARCH_SCROLLBAR_OPACITY,
                value = event.value,
                updateState = {
                    it.copy(searchScrollbarOpacity = this.toDouble())
                }
            )

            is MainEvent.OnChangeShowSearchScrollbar -> handleDatastoreUpdate(
                key = DataStoreConstants.SHOW_SEARCH_SCROLLBAR,
                value = event.value,
                updateState = {
                    it.copy(showSearchScrollbar = this)
                }
            )

            // Dictionary Events
            is MainEvent.OnChangeDictionarySource -> handleDatastoreUpdate(
                key = DataStoreConstants.DICTIONARY_SOURCE,
                value = event.value.id,
                updateState = {
                    it.copy(dictionarySource = event.value)
                }
            )

            is MainEvent.OnChangeCustomDictionaryUrl -> handleDatastoreUpdate(
                key = DataStoreConstants.CUSTOM_DICTIONARY_URL,
                value = event.value,
                updateState = {
                    it.copy(customDictionaryUrl = this)
                }
            )

            is MainEvent.OnChangeDoubleTapDictionary -> handleDatastoreUpdate(
                key = DataStoreConstants.DOUBLE_TAP_DICTIONARY,
                value = event.value,
                updateState = {
                    it.copy(doubleTapDictionary = this)
                }
            )

            is MainEvent.OnChangeOfflineDictionaryEnabled -> handleDatastoreUpdate(
                key = DataStoreConstants.OFFLINE_DICTIONARY_ENABLED,
                value = event.value,
                updateState = {
                    it.copy(offlineDictionaryEnabled = this)
                }
            )

            is MainEvent.OnChangeOpenLookupsInApp -> handleDatastoreUpdate(
                key = DataStoreConstants.OPEN_LOOKUPS_IN_APP,
                value = event.value,
                updateState = {
                    it.copy(openLookupsInApp = this)
                }
            )

            // Library Events
            is MainEvent.OnChangeLibraryLayout -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_LAYOUT,
                value = event.value.name,
                updateState = {
                    it.copy(libraryLayout = LibraryLayout.valueOf(this))
                }
            )

            is MainEvent.OnChangeLibraryAutoGridSize -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_AUTO_GRID_SIZE,
                value = event.value,
                updateState = {
                    it.copy(libraryAutoGridSize = this)
                }
            )

            is MainEvent.OnChangeLibraryGridSize -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_GRID_SIZE,
                value = event.value,
                updateState = {
                    it.copy(libraryGridSize = this)
                }
            )

            is MainEvent.OnChangeLibraryGridSizeSettings -> handleLibraryGridSizeSettingsUpdate(event)

            is MainEvent.OnChangeLibrarySortOrder -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SORT_ORDER,
                value = event.value,
                updateState = {
                    it.copy(librarySortOrder = toLibrarySortOrder())
                }
            )

            is MainEvent.OnChangeLibrarySortOrderDescending -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SORT_ORDER_DESCENDING,
                value = event.value,
                updateState = {
                    it.copy(librarySortOrderDescending = this)
                }
            )

            is MainEvent.OnChangeLibraryShowCategoryTabs -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SHOW_CATEGORY_TABS,
                value = event.value,
                updateState = {
                    it.copy(libraryShowCategoryTabs = this)
                }
            )

            is MainEvent.OnChangeLibraryAlwaysShowDefaultTab -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_ALWAYS_SHOW_DEFAULT_TAB,
                value = event.value,
                updateState = {
                    it.copy(libraryAlwaysShowDefaultTab = this)
                }
            )

            is MainEvent.OnChangeLibraryShowBookCount -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SHOW_BOOK_COUNT,
                value = event.value,
                updateState = {
                    it.copy(libraryShowBookCount = this)
                }
            )

            is MainEvent.OnChangeLibraryTitlePosition -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_TITLE_POSITION,
                value = event.value.name,
                updateState = {
                    it.copy(libraryTitlePosition = event.value)
                }
            )

            is MainEvent.OnChangeLibraryShowReadButton -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SHOW_READ_BUTTON,
                value = event.value,
                updateState = {
                    it.copy(libraryShowReadButton = this)
                }
            )

            is MainEvent.OnChangeLibraryShowProgress -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_SHOW_PROGRESS,
                value = event.value,
                updateState = {
                    it.copy(libraryShowProgress = this)
                }
            )

            is MainEvent.OnChangeLibraryListSize -> handleDatastoreUpdate(
                key = DataStoreConstants.LIBRARY_LIST_SIZE,
                value = event.value,
                updateState = {
                    it.copy(libraryListSize = this)
                }
            )

            // Background Image Events
            is MainEvent.OnChangeBackgroundImage -> handleDatastoreUpdate(
                key = DataStoreConstants.BACKGROUND_IMAGE,
                value = event.value?.let { BackgroundImage.toString(it) } ?: "",
                updateState = {
                    it.copy(backgroundImage = if (this.isEmpty()) null else BackgroundImage.fromString(this))
                }
            )

            is MainEvent.OnAddCustomBackgroundImage -> handleAddCustomBackgroundImage(event)

            is MainEvent.OnRemoveCustomBackgroundImage -> handleRemoveCustomBackgroundImage(event)

            is MainEvent.OnChangeBackgroundImageOpacity -> handleDatastoreUpdate(
                key = DataStoreConstants.BACKGROUND_IMAGE_OPACITY,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(backgroundImageOpacity = this.toFloat())
                }
            )

            is MainEvent.OnChangeBackgroundScaleMode -> handleDatastoreUpdate(
                key = DataStoreConstants.BACKGROUND_SCALE_MODE,
                value = event.value.name,
                updateState = {
                    it.copy(backgroundScaleMode = this.toBackgroundScaleMode())
                }
            )

            // Comic Reader Events
            is MainEvent.OnChangeComicReadingDirection -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_READING_DIRECTION,
                value = event.value,
                updateState = {
                    it.copy(comicReadingDirection = this)
                }
            )

            is MainEvent.OnChangeComicReaderMode -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_READER_MODE,
                value = event.value,
                updateState = {
                    it.copy(comicReaderMode = this)
                }
            )

            is MainEvent.OnChangeComicTapZone -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_TAP_ZONE,
                value = event.value,
                updateState = {
                    it.copy(comicTapZone = this)
                }
            )

            is MainEvent.OnChangeComicInvertTaps -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_INVERT_TAPS,
                value = event.value,
                updateState = {
                    it.copy(comicInvertTaps = this)
                }
            )

            is MainEvent.OnChangeComicScaleType -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_SCALE_TYPE,
                value = event.value,
                updateState = {
                    it.copy(comicScaleType = this)
                }
            )

            is MainEvent.OnChangeComicProgressBar -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_PROGRESS_BAR,
                value = event.value,
                updateState = {
                    it.copy(comicProgressBar = this)
                }
            )

            is MainEvent.OnChangeComicProgressBarPadding -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_PROGRESS_BAR_PADDING,
                value = event.value,
                updateState = {
                    it.copy(comicProgressBarPadding = this)
                }
            )

            is MainEvent.OnChangeComicProgressBarAlignment -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_PROGRESS_BAR_ALIGNMENT,
                value = event.value,
                updateState = {
                    it.copy(comicProgressBarAlignment = this.toHorizontalAlignment())
                }
            )

            is MainEvent.OnChangeComicProgressBarFontSize -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_PROGRESS_BAR_FONT_SIZE,
                value = event.value,
                updateState = {
                    it.copy(comicProgressBarFontSize = this)
                }
            )

            is MainEvent.OnChangeComicProgressCount -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_PROGRESS_COUNT,
                value = event.value,
                updateState = {
                    it.copy(comicProgressCount = this.toProgressCount())
                }
            )

            is MainEvent.OnChangeComicBackgroundColor -> handleDatastoreUpdate(
                key = DataStoreConstants.COMIC_BACKGROUND_COLOR,
                value = event.value,
                updateState = {
                    it.copy(comicBackgroundColor = this)
                }
            )

            // Speed Reader Events
            is MainEvent.OnChangeSpeedReadingWpm -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_WPM,
                value = event.value,
                updateState = {
                    it.copy(speedReadingWpm = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingManualSentencePauseEnabled -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_MANUAL_SENTENCE_PAUSE_ENABLED,
                value = event.value,
                updateState = {
                    it.copy(speedReadingManualSentencePauseEnabled = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingSentencePauseDuration -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_SENTENCE_PAUSE_DURATION,
                value = event.value,
                updateState = {
                    it.copy(speedReadingSentencePauseDuration = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingOsdEnabled -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_OSD_ENABLED,
                value = event.value,
                updateState = {
                    it.copy(speedReadingOsdEnabled = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingWordSize -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_WORD_SIZE,
                value = event.value,
                updateState = {
                    it.copy(speedReadingWordSize = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingAccentCharacterEnabled -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_ACCENT_CHARACTER_ENABLED,
                value = event.value,
                updateState = {
                    it.copy(speedReadingAccentCharacterEnabled = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingAccentColor -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_ACCENT_COLOR,
                value = event.value.toInt(),
                updateState = {
                    it.copy(speedReadingAccentColor = this.toLong())
                }
            )

            is MainEvent.OnChangeSpeedReadingAccentOpacity -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_ACCENT_OPACITY,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingAccentOpacity = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingShowVerticalIndicators -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_SHOW_VERTICAL_INDICATORS,
                value = event.value,
                updateState = {
                    it.copy(speedReadingShowVerticalIndicators = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingVerticalIndicatorsSize -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_VERTICAL_INDICATORS_SIZE,
                value = event.value,
                updateState = {
                    it.copy(speedReadingVerticalIndicatorsSize = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingVerticalIndicatorType -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_VERTICAL_INDICATOR_TYPE,
                value = event.value,
                updateState = {
                    it.copy(speedReadingVerticalIndicatorType = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingShowHorizontalBars -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_SHOW_HORIZONTAL_BARS,
                value = event.value,
                updateState = {
                    it.copy(speedReadingShowHorizontalBars = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingHorizontalBarsThickness -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_THICKNESS,
                value = event.value,
                updateState = {
                    it.copy(speedReadingHorizontalBarsThickness = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingHorizontalBarsLength -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_LENGTH,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingHorizontalBarsLength = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingHorizontalBarsDistance -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_DISTANCE,
                value = event.value,
                updateState = {
                    it.copy(speedReadingHorizontalBarsDistance = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingHorizontalBarsColor -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_COLOR,
                value = event.value.toInt(),
                updateState = {
                    it.copy(speedReadingHorizontalBarsColor = this.toLong())
                }
            )

            is MainEvent.OnChangeSpeedReadingHorizontalBarsOpacity -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_HORIZONTAL_BARS_OPACITY,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingHorizontalBarsOpacity = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingFocalPointPosition -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_FOCAL_POINT_POSITION,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingFocalPointPosition = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingOsdHeight -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_OSD_HEIGHT,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingOsdHeight = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingOsdSeparation -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_OSD_SEPARATION,
                value = event.value.toDouble(),
                updateState = {
                    it.copy(speedReadingOsdSeparation = this.toFloat())
                }
            )

            is MainEvent.OnChangeSpeedReadingCenterWord -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_CENTER_WORD,
                value = event.value,
                updateState = {
                    it.copy(speedReadingCenterWord = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingCustomFontEnabled -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_CUSTOM_FONT_ENABLED,
                value = event.value,
                updateState = {
                    it.copy(speedReadingCustomFontEnabled = this)
                }
            )

            is MainEvent.OnChangeSpeedReadingSelectedFontFamily -> handleDatastoreUpdate(
                key = DataStoreConstants.SPEED_READING_SELECTED_FONT_FAMILY,
                value = event.value,
                updateState = {
                    it.copy(speedReadingSelectedFontFamily = this)
                }
            )
        }
    }

    fun init(settingsModelReady: StateFlow<Boolean>) {
        viewModelScope.launch(Dispatchers.Main) {
            val settings = getAllSettings.execute()

            /* All additional execution */
            changeLanguage.execute(settings.language)

            updateStateWithSavedHandle { settings }
            mainModelReady.update { true }

            // Preload recent books text for instant loading
            launch(Dispatchers.IO) {
                bookRepository.preloadRecentBooksText()
            }
        }

        val isReady = combine(
            mainModelReady.asStateFlow(),
            settingsModelReady
        ) { values ->
            values.all { it }
        }

        viewModelScope.launch(Dispatchers.Main) {
            isReady.first { bool ->
                if (bool) {
                    _isReady.update {
                        true
                    }
                }
                bool
            }
        }
    }

    fun reloadSettings() {
        viewModelScope.launch(Dispatchers.Main) {
            val settings = getAllSettings.execute()

            /* All additional execution */
            changeLanguage.execute(settings.language)

            updateStateWithSavedHandle { settings }
        }
    }

    private fun handleLanguageUpdate(event: MainEvent.OnChangeLanguage) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            changeLanguage.execute(event.value)
            updateStateWithSavedHandle {
                it.copy(language = event.value)
            }
        }
    }

    private fun handleBrowseIncludedFilterItemUpdate(
        event: MainEvent.OnChangeBrowseIncludedFilterItem
    ) {
        val set = _state.value.browseIncludedFilterItems.toMutableSet()
        if (!set.add(event.value)) {
            set.remove(event.value)
        }
        handleDatastoreUpdate(
            key = DataStoreConstants.BROWSE_INCLUDED_FILTER_ITEMS,
            value = set,
            updateState = {
                it.copy(browseIncludedFilterItems = toList())
            }
        )
    }

    private fun handleBrowsePinnedPathsUpdate(
        event: MainEvent.OnChangeBrowsePinnedPaths
    ) {
        val set = _state.value.browsePinnedPaths.toMutableSet()
        if (!set.add(event.value)) {
            set.remove(event.value)
        }
        handleDatastoreUpdate(
            key = DataStoreConstants.BROWSE_PINNED_PATHS,
            value = set,
            updateState = {
                it.copy(browsePinnedPaths = toList())
            }
        )
    }

    private fun handleAddCustomFont(event: MainEvent.OnAddCustomFont) {
        val list = _state.value.customFonts.toMutableList()
        list.add(event.value)
        val stringSet = list.map { CustomFont.toString(it) }.toSet()
        handleDatastoreUpdate(
            key = DataStoreConstants.CUSTOM_FONTS,
            value = stringSet,
            updateState = {
                it.copy(customFonts = list)
            }
        )
    }

    private fun handleRemoveCustomFont(event: MainEvent.OnRemoveCustomFont) {
        val list = _state.value.customFonts.toMutableList()
        list.remove(event.value)
        val stringSet = list.map { CustomFont.toString(it) }.toSet()
        handleDatastoreUpdate(
            key = DataStoreConstants.CUSTOM_FONTS,
            value = stringSet,
            updateState = {
                it.copy(customFonts = list)
            }
        )
        // Clean up the font file
        try {
            java.io.File(event.value.filePath).delete()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun handleAddCustomBackgroundImage(event: MainEvent.OnAddCustomBackgroundImage) {
        val list = _state.value.customBackgroundImages.toMutableList()
        list.add(event.value)
        val stringSet = list.map { BackgroundImage.toString(it) }.toSet()
        handleDatastoreUpdate(
            key = DataStoreConstants.CUSTOM_BACKGROUND_IMAGES,
            value = stringSet,
            updateState = {
                it.copy(customBackgroundImages = list)
            }
        )
    }

    private fun handleRemoveCustomBackgroundImage(event: MainEvent.OnRemoveCustomBackgroundImage) {
        val list = _state.value.customBackgroundImages.toMutableList()
        list.remove(event.value)
        val stringSet = list.map { BackgroundImage.toString(it) }.toSet()
        handleDatastoreUpdate(
            key = DataStoreConstants.CUSTOM_BACKGROUND_IMAGES,
            value = stringSet,
            updateState = {
                it.copy(customBackgroundImages = list)
            }
        )
        // If this was the currently selected background, clear the selection
        if (_state.value.backgroundImage == event.value) {
            handleDatastoreUpdate(
                key = DataStoreConstants.BACKGROUND_IMAGE,
                value = "",
                updateState = {
                    it.copy(backgroundImage = null)
                }
            )
        }
        // Clean up the image file (only for non-default images)
        if (!event.value.isDefault) {
            try {
                java.io.File(event.value.filePath).delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun handleLibraryGridSizeSettingsUpdate(event: MainEvent.OnChangeLibraryGridSizeSettings) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            setDatastore.execute(key = DataStoreConstants.LIBRARY_AUTO_GRID_SIZE, value = event.value == 0)
            setDatastore.execute(key = DataStoreConstants.LIBRARY_GRID_SIZE, value = event.value)
            updateStateWithSavedHandle {
                it.copy(libraryAutoGridSize = event.value == 0, libraryGridSize = event.value)
            }
        }
    }

    private fun handleBrowseGridSizeSettingsUpdate(event: MainEvent.OnChangeBrowseGridSizeSettings) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            setDatastore.execute(key = DataStoreConstants.BROWSE_AUTO_GRID_SIZE, value = event.value == 0)
            setDatastore.execute(key = DataStoreConstants.BROWSE_GRID_SIZE, value = event.value)
            updateStateWithSavedHandle {
                it.copy(browseAutoGridSize = event.value == 0, browseGridSize = event.value)
            }
        }
    }

    /**
     * Handles and updates Datastore.
     */
    private fun <V> handleDatastoreUpdate(
        key: Preferences.Key<V>,
        value: V,
        updateState: V.(MainState) -> MainState
    ) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            setDatastore.execute(key = key, value = value)
            updateStateWithSavedHandle {
                value.updateState(it)
            }
        }
    }

    /**
     * Updates [MainState] along with [SavedStateHandle].
     */
    private suspend fun updateStateWithSavedHandle(
        function: (MainState) -> MainState
    ) {
        withContext(Dispatchers.Main.immediate) {
            _state.update {
                stateHandle[provideMainState()] = function(it)
                function(it)
            }
        }
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            yield()
            this.value = function(this.value)
        }
    }
}