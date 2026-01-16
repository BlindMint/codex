/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.ui.main.MainModel

@Composable
fun ComicPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage < 0 || totalPages <= 0) return

    val text = "${currentPage + 1} / $totalPages"

    val style = TextStyle(
        color = Color(235, 235, 235),
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
    val strokeStyle = style.copy(
        color = Color(45, 45, 45),
        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = text,
            style = strokeStyle,
        )
        Text(
            text = text,
            style = style,
        )
    }
}