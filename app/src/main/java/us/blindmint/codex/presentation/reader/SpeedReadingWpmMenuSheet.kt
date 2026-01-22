/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet
import us.blindmint.codex.presentation.core.util.noRippleClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReadingWpmMenuSheet(
    show: Boolean,
    currentWpm: Int,
    onDismiss: () -> Unit,
    onWpmChange: (Int) -> Unit
) {
    if (show) {
        ModalBottomSheet(
            scrimColor = BottomSheetDefaults.ScrimColor,
            onDismissRequest = onDismiss,
            sheetGesturesEnabled = true,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Top row: -100, -50, current WPM, +50, +100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "-100",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .noRippleClickable {
                                onWpmChange((currentWpm - 100).coerceAtLeast(200))
                            }
                    )

                    Text(
                        text = "-50",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .noRippleClickable {
                                onWpmChange((currentWpm - 50).coerceAtLeast(200))
                            }
                    )

                    Text(
                        text = "$currentWpm",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Text(
                        text = "+50",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .noRippleClickable {
                                onWpmChange((currentWpm + 50).coerceAtMost(1200))
                            }
                    )

                    Text(
                        text = "+100",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .noRippleClickable {
                                onWpmChange((currentWpm + 100).coerceAtMost(1200))
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom row: slider with - and + symbols
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.noRippleClickable {
                            onWpmChange((currentWpm - 10).coerceAtLeast(200))
                        }
                    )

                    Slider(
                        value = currentWpm.toFloat(),
                        onValueChange = { onWpmChange((it / 5).toInt() * 5) },
                        valueRange = 200f..1200f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(0.15f),
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = "+",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.noRippleClickable {
                            onWpmChange((currentWpm + 10).coerceAtMost(1200))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
