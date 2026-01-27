/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.ui.browse.opds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading card that matches OpdsBookPreview layout.
 * Provides visual placeholder while OPDS books are being loaded.
 */
@Composable
fun BookCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxHeight(250.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f / 1.5f)  // Matches OpdsBookPreview aspect ratio
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxHeight(120.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
