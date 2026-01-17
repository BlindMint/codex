/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.comic.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

// Reading direction options
enum class ComicReadingDirection(val displayName: Int) {
    LTR(R.string.reading_direction_ltr),
    RTL(R.string.reading_direction_rtl),
    VERTICAL(R.string.reading_direction_vertical)
}

// Reader mode options
enum class ComicReaderModeType(val displayName: Int) {
    PAGED(R.string.reader_mode_paged),
    WEBTOON(R.string.reader_mode_webtoon)
}

// Tap zone inversion options
enum class ComicTapInversion(val displayName: Int) {
    NONE(R.string.tap_invert_none),
    HORIZONTAL(R.string.tap_invert_horizontal),
    VERTICAL(R.string.tap_invert_vertical),
    BOTH(R.string.tap_invert_both)
}

// Image scale type options
enum class ComicImageScale(val displayName: Int, val value: Int) {
    FIT_SCREEN(R.string.scale_fit_screen, 1),
    STRETCH(R.string.scale_stretch, 2),
    FIT_WIDTH(R.string.scale_fit_width, 3),
    FIT_HEIGHT(R.string.scale_fit_height, 4),
    ORIGINAL(R.string.scale_original, 5),
    SMART_FIT(R.string.scale_smart_fit, 6)
}

// Zoom start position options
enum class ComicZoomStart(val displayName: Int, val value: Int) {
    AUTOMATIC(R.string.zoom_automatic, 1),
    LEFT(R.string.zoom_left, 2),
    RIGHT(R.string.zoom_right, 3),
    CENTER(R.string.zoom_center, 4)
}

@Composable
fun ComicReadingDirectionOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.reading_direction),
        chips = ComicReadingDirection.entries.map { direction ->
            ButtonItem(
                id = direction.name,
                title = stringResource(direction.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = direction.name == state.value.comicReadingDirection
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicReadingDirection(item.id))
        }
    )
}

@Composable
fun ComicReaderModeOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.reader_mode),
        chips = ComicReaderModeType.entries.map { mode ->
            ButtonItem(
                id = mode.name,
                title = stringResource(mode.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = mode.name == state.value.comicReaderMode
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicReaderMode(item.id))
        }
    )
}

@Composable
fun ComicTapZoneOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    // Simple Enable/Disable for tap navigation
    // Enabled = zone 0 (default), Disabled = zone 5
    val isEnabled = state.value.comicTapZone != 5

    val tapZoneOptions = listOf(
        ButtonItem(
            id = "enabled",
            title = stringResource(R.string.enabled),
            textStyle = MaterialTheme.typography.labelLarge,
            selected = isEnabled
        ),
        ButtonItem(
            id = "disabled",
            title = stringResource(R.string.disabled),
            textStyle = MaterialTheme.typography.labelLarge,
            selected = !isEnabled
        )
    )

    ChipsWithTitle(
        title = stringResource(R.string.tap_zones),
        chips = tapZoneOptions,
        onClick = { item ->
            // Set tap zone to 0 (default with all zones) or 5 (disabled)
            val tapZone = if (item.id == "enabled") 0 else 5
            mainModel.onEvent(MainEvent.OnChangeComicTapZone(tapZone))
        }
    )
}

@Composable
fun ComicInvertTapsOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.invert_taps),
        chips = ComicTapInversion.entries.map { invert ->
            ButtonItem(
                id = invert.name,
                title = stringResource(invert.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = invert.name == state.value.comicInvertTaps
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicInvertTaps(item.id))
        }
    )
}

@Composable
fun ComicImageScaleOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.image_scale),
        chips = ComicImageScale.entries.map { scale ->
            ButtonItem(
                id = scale.value.toString(),
                title = stringResource(scale.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = scale.value == state.value.comicScaleType
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicScaleType(item.id.toIntOrNull() ?: 1))
        }
    )
}

@Composable
fun ComicZoomStartOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    ChipsWithTitle(
        title = stringResource(R.string.zoom_start),
        chips = ComicZoomStart.entries.map { zoom ->
            ButtonItem(
                id = zoom.value.toString(),
                title = stringResource(zoom.displayName),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = zoom.value == state.value.comicZoomStart
            )
        },
        onClick = { item ->
            mainModel.onEvent(MainEvent.OnChangeComicZoomStart(item.id.toIntOrNull() ?: 1))
        }
    )
}

@Composable
fun ComicCropBordersOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        selected = state.value.comicCropBorders,
        title = stringResource(R.string.crop_borders),
        description = stringResource(R.string.crop_borders_desc),
        onClick = {
            mainModel.onEvent(MainEvent.OnChangeComicCropBorders(!state.value.comicCropBorders))
        }
    )
}

@Composable
fun ComicLandscapeZoomOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    SwitchWithTitle(
        selected = state.value.comicLandscapeZoom,
        title = stringResource(R.string.landscape_zoom),
        description = stringResource(R.string.landscape_zoom_desc),
        onClick = {
            mainModel.onEvent(MainEvent.OnChangeComicLandscapeZoom(!state.value.comicLandscapeZoom))
        }
    )
}