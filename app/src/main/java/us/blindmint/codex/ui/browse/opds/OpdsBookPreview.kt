/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import okhttp3.Credentials
import us.blindmint.codex.domain.opds.OpdsEntry
import us.blindmint.codex.presentation.core.components.common.StyledText

/**
 * Resolves a potentially relative URL against a base URL.
 * Returns the resolved absolute URL, or the original href if resolution fails.
 */
fun resolveUrl(baseUrl: String, href: String): String {
    return try {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            href
        } else {
            java.net.URI(baseUrl).resolve(href).toString()
        }
    } catch (e: Exception) {
        href // Fallback to original
    }
}

@Composable
fun OpdsBookPreview(
    entry: OpdsEntry,
    baseUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    username: String? = null,
    password: String? = null
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .padding(3.dp)
            .background(
                Color.Transparent,
                MaterialTheme.shapes.large
            )
            .padding(3.dp)
            .padding(bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f / 1.5f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            // Resolve cover URL against base URL
            val coverUrl = remember(entry, baseUrl) {
                val rawUrl = entry.links.firstOrNull { it.rel == "http://opds-spec.org/image/thumbnail" }?.href
                    ?: entry.links.firstOrNull { it.rel == "http://opds-spec.org/image" }?.href
                    ?: entry.links.firstOrNull { it.type?.startsWith("image/") == true }?.href
                rawUrl?.let { resolveUrl(baseUrl, it) }
            }

            // Create ImageRequest with auth headers if credentials are provided
            val imageRequest = remember(coverUrl, username, password) {
                coverUrl?.let { url ->
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

            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Book cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "No cover available",
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection or download icon overlay
            if (isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.shapes.small
                        )
                        .padding(4.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download book",
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        )
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = entry.title ?: "Unknown Title",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Author
        entry.author?.let { author ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Summary (if available and short)
        entry.summary?.let { summary ->
            if (summary.length < 100) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}