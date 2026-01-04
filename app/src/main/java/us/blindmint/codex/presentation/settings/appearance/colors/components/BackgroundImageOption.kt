/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.appearance.colors.components

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.BackgroundImage
import us.blindmint.codex.domain.reader.BackgroundScaleMode
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.BackgroundImageConstants
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundImageOption(
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<BackgroundImage?>(null) }

    val defaultImages = remember {
        BackgroundImageConstants.getDefaultBackgroundImages(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("_display_name")
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }

            fileName?.let { name ->
                val backgroundsDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                val imageFile = File(backgroundsDir, name)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(imageFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val customImage = BackgroundImage(
                        name = name.substringBeforeLast(".").replace("_", " "),
                        filePath = imageFile.absolutePath,
                        isDefault = false
                    )
                    mainModel.onEvent(MainEvent.OnAddCustomBackgroundImage(customImage))
                    mainModel.onEvent(MainEvent.OnChangeBackgroundImage(customImage))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val allImages = remember(defaultImages, state.value.customBackgroundImages) {
        defaultImages + state.value.customBackgroundImages
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.background_image_option),
            padding = 18.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Image Grid
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "None" option
            BackgroundImageThumbnail(
                image = null,
                isSelected = state.value.backgroundImage == null,
                onClick = {
                    mainModel.onEvent(MainEvent.OnChangeBackgroundImage(null))
                },
                onRemove = null
            )

            // All images (default + custom)
            allImages.forEach { image ->
                BackgroundImageThumbnail(
                    image = image,
                    isSelected = state.value.backgroundImage?.filePath == image.filePath,
                    onClick = {
                        mainModel.onEvent(MainEvent.OnChangeBackgroundImage(image))
                    },
                    onRemove = if (!image.isDefault) {
                        {
                            imageToDelete = image
                            showDeleteDialog = true
                        }
                    } else null
                )
            }

            // Add custom image button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_background_image),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && imageToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    imageToDelete = null
                },
                title = {
                    Text(text = stringResource(id = R.string.delete_background_image_title))
                },
                text = {
                    Text(text = stringResource(
                        id = R.string.delete_background_image_message,
                        imageToDelete?.name ?: ""
                    ))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            imageToDelete?.let { image ->
                                mainModel.onEvent(MainEvent.OnRemoveCustomBackgroundImage(image))
                            }
                            showDeleteDialog = false
                            imageToDelete = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            imageToDelete = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            val isEnabled = state.value.backgroundImage != null
            // Opacity Slider
            StyledText(
                text = stringResource(id = R.string.background_opacity_option),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Slider(
                    value = state.value.backgroundImageOpacity,
                    onValueChange = {
                        if (isEnabled) mainModel.onEvent(MainEvent.OnChangeBackgroundImageOpacity(it))
                    },
                    valueRange = 0f..1f,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f)
                )
                    Spacer(modifier = Modifier.width(8.dp))
                StyledText(
                    text = "${(state.value.backgroundImageOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Scale Mode
            StyledText(
                text = stringResource(id = R.string.background_scale_mode_option),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                BackgroundScaleMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.value.backgroundScaleMode == mode,
                        onClick = {
                            if (isEnabled) mainModel.onEvent(MainEvent.OnChangeBackgroundScaleMode(mode))
                        },
                        enabled = isEnabled,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BackgroundScaleMode.entries.size
                        )
                    ) {
                        StyledText(
                            text = when (mode) {
                                BackgroundScaleMode.COVER -> stringResource(R.string.scale_mode_cover)
                                BackgroundScaleMode.FIT -> stringResource(R.string.scale_mode_fit)
                                BackgroundScaleMode.TILE -> stringResource(R.string.scale_mode_tile)
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundImageThumbnail(
    image: BackgroundImage?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val context = LocalContext.current
    var bitmap by remember(image?.filePath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(image?.filePath) {
        if (image != null) {
            bitmap = try {
                if (image.isDefault) {
                    context.assets.open(image.filePath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                } else {
                    BitmapFactory.decodeFile(image.filePath)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (image == null) {
            // "None" option
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = stringResource(R.string.no_background_image),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = image.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Loading or error state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                StyledText(
                    text = image.name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Remove button for custom images
        if (onRemove != null && !isSelected) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_background_image),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
