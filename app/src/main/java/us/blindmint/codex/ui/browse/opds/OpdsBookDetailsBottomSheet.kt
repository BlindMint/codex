/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import okhttp3.Credentials
import us.blindmint.codex.R
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

/**
 * Bottom sheet for displaying detailed information about an OPDS book entry.
 * Shows cover image, metadata, categories, available formats, and download button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpdsBookDetailsBottomSheet(
    entry: OpdsEntry,
    baseUrl: String,
    isDownloadEnabled: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    username: String? = null,
    password: String? = null
) {
    val context = LocalContext.current

    // Resolve cover URL (prefer full image over thumbnail for details view)
    val resolvedCoverUrl = remember(entry, baseUrl) {
        val rawUrl = entry.links.firstOrNull { it.rel == "http://opds-spec.org/image" }?.href
            ?: entry.links.firstOrNull { it.rel == "http://opds-spec.org/image/thumbnail" }?.href
            ?: entry.links.firstOrNull { it.type?.startsWith("image/") == true }?.href
        rawUrl?.let { resolveUrl(baseUrl, it) }
    }

    // Create ImageRequest with auth headers if credentials are provided
    val imageRequest = remember(resolvedCoverUrl, username, password) {
        resolvedCoverUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .apply {
                    if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                        addHeader("Authorization", Credentials.basic(username, password))
                    }
                }
                .crossfade(true)
                .build()
        }
    }

    // Get acquisition links to show available formats
    val acquisitionLinks = remember(entry) {
        entry.links.filter { link ->
            link.rel == "http://opds-spec.org/acquisition" ||
            link.rel?.startsWith("http://opds-spec.org/acquisition/") == true
        }
    }

    // Extract format from MIME type
    fun getFormatFromType(type: String?): String {
        return when {
            type == null -> "Unknown"
            type.contains("epub") -> "EPUB"
            type.contains("pdf") -> "PDF"
            type.contains("mobi") || type.contains("x-mobipocket") -> "MOBI"
            type.contains("fb2") -> "FB2"
            type.contains("cbz") -> "CBZ"
            type.contains("cbr") -> "CBR"
            type.contains("azw") -> "AZW"
            type.contains("txt") || type.contains("plain") -> "TXT"
            type.contains("html") -> "HTML"
            else -> type.substringAfterLast("/").uppercase().take(10)
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = true
    ) {
        LazyColumnWithScrollbar(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Cover image and basic info row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover image
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(1f / 1.5f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        if (imageRequest != null) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Book cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "No cover",
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Title and Author
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        entry.author?.let { author ->
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Series info if available
                        entry.series?.let { series ->
                            val seriesText = if (entry.seriesIndex != null) {
                                "$series #${entry.seriesIndex}"
                            } else {
                                series
                            }
                            Text(
                                text = seriesText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Metadata section
            val hasMetadata = entry.publisher != null || entry.language != null || entry.published != null
            if (hasMetadata) {
                item {
                    SettingsSubcategoryTitle(
                        title = stringResource(id = R.string.details),
                        padding = 16.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                entry.publisher?.let { publisher ->
                    item {
                        MetadataRow(
                            label = stringResource(id = R.string.publisher),
                            value = publisher
                        )
                    }
                }

                entry.language?.let { language ->
                    item {
                        MetadataRow(
                            label = stringResource(id = R.string.language),
                            value = language.uppercase()
                        )
                    }
                }

                entry.published?.let { published ->
                    item {
                        MetadataRow(
                            label = stringResource(id = R.string.publication_date),
                            value = published.take(10) // Show just the date part
                        )
                    }
                }
            }

            // Categories/Tags
            if (entry.categories.isNotEmpty()) {
                item {
                    SettingsSubcategoryTitle(
                        title = stringResource(id = R.string.tags),
                        padding = 16.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        entry.categories.take(10).forEach { category ->
                            AssistChip(
                                onClick = { },
                                label = { Text(category, maxLines = 1) }
                            )
                        }
                        if (entry.categories.size > 10) {
                            AssistChip(
                                onClick = { },
                                label = { Text("+${entry.categories.size - 10} more") }
                            )
                        }
                    }
                }
            }

            // Description/Summary
            entry.summary?.let { summary ->
                if (summary.isNotBlank()) {
                    item {
                        SettingsSubcategoryTitle(
                            title = stringResource(id = R.string.description),
                            padding = 16.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = summary.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Available formats
            if (acquisitionLinks.isNotEmpty()) {
                item {
                    SettingsSubcategoryTitle(
                        title = stringResource(id = R.string.available_formats),
                        padding = 16.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        acquisitionLinks.distinctBy { getFormatFromType(it.type) }.forEach { link ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text(getFormatFromType(link.type)) }
                            )
                        }
                    }
                }
            }

            // Download warning if not enabled
            if (!isDownloadEnabled) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.shapes.medium
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(id = R.string.download_folder_not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Download button
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDownload,
                    enabled = isDownloadEnabled && acquisitionLinks.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.download))
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
