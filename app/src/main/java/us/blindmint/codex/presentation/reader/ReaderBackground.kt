/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import us.blindmint.codex.domain.reader.BackgroundImage
import us.blindmint.codex.domain.reader.BackgroundScaleMode

/**
 * Renders a background for the reader with an optional image and color overlay.
 *
 * @param backgroundImage The background image to display, or null for solid color
 * @param backgroundColor The background color (used as overlay when image is present)
 * @param backgroundImageOpacity Opacity of the background image (0-1)
 * @param backgroundScaleMode How the image should be scaled
 * @param modifier Modifier for the container
 * @param content The content to display over the background
 */
@Composable
fun ReaderBackground(
    backgroundImage: BackgroundImage?,
    backgroundColor: Color,
    backgroundImageOpacity: Float,
    backgroundScaleMode: BackgroundScaleMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(backgroundImage?.filePath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(backgroundImage?.filePath) {
        if (backgroundImage != null) {
            bitmap = try {
                if (backgroundImage.isDefault) {
                    context.assets.open(backgroundImage.filePath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                } else {
                    BitmapFactory.decodeFile(backgroundImage.filePath)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        } else {
            bitmap = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Image (if present)
        if (backgroundImage != null && bitmap != null && backgroundImageOpacity > 0f) {
            val contentScale = when (backgroundScaleMode) {
                BackgroundScaleMode.COVER -> ContentScale.Crop
                BackgroundScaleMode.FIT -> ContentScale.Fit
                BackgroundScaleMode.TILE -> ContentScale.None // We'll handle tiling separately
            }

            if (backgroundScaleMode == BackgroundScaleMode.TILE) {
                // For tiling, we'd need a custom implementation
                // For now, just use Crop as fallback
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = backgroundImageOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    contentScale = contentScale,
                    alpha = backgroundImageOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer 2: Color overlay on top of image
            // The overlay opacity is inverse of image opacity
            // At 100% image opacity, we still want some color (around 20%)
            // At 0% image opacity, full color (100%)
            val overlayAlpha = 1f - (backgroundImageOpacity * 0.8f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor.copy(alpha = overlayAlpha))
            )
        } else {
            // No image, just solid background color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )
        }

        // Layer 3: Content
        content()
    }
}
