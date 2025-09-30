/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.presentation.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.blindmint.codex.R
import ua.blindmint.codex.presentation.core.components.common.StyledText

@Composable
fun StartSettingsScaffold(
    currentPage: Int,
    navigateForward: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            StartSettingsBottomBar(
                currentPage = currentPage,
                navigateForward = navigateForward
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background layer - conditional corner rounding based on page
                val titleCornerShape = if (currentPage == 1) {
                    // First page only: round all corners
                    RoundedCornerShape(16.dp)
                } else {
                    // Pages 2-3: only round top corners for seamless connection
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(titleCornerShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .blur(8.dp)
                )
                // Content layer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    StyledText(
                        text = stringResource(id = R.string.start_welcome),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StyledText(
                        text = stringResource(id = R.string.start_welcome_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(0.dp)) // Remove spacer to connect backgrounds

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp) // Remove top padding to connect with title background
            ) {
                if (currentPage > 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .blur(4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(16.dp) // Match title padding
                ) {
                    content()
                }
            }
        }
    }
}