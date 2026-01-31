package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FontThicknessDialog(
    onDismissRequest: () -> Unit,
    onThicknessSelected: (ReaderFontThickness) -> Unit
) {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    val fontFamily = remember(state.value.fontFamily) {
        if (state.value.fontFamily.startsWith("custom_")) {
            val customFontName = state.value.fontFamily.removePrefix("custom_").lowercase()
            when {
                customFontName.contains("serif") ||
                customFontName.contains("times") ||
                customFontName.contains("garamond") ||
                customFontName.contains("georgia") -> {
                    provideFonts().find { it.id == "noto_serif" } ?: provideFonts().first()
                }
                customFontName.contains("mono") ||
                customFontName.contains("code") ||
                customFontName.contains("fira") ||
                customFontName.contains("jetbrains") ||
                customFontName.contains("cascadia") ||
                customFontName.contains("source") -> {
                    provideFonts().find { it.id == "roboto" } ?: provideFonts().first()
                }
                customFontName.contains("script") ||
                customFontName.contains("hand") ||
                customFontName.contains("brush") -> {
                    provideFonts().find { it.id == "lora" } ?: provideFonts().first()
                }
                customFontName.contains("sans") ||
                customFontName.contains("arial") ||
                customFontName.contains("helvetica") ||
                customFontName.contains("verdana") -> {
                    provideFonts().find { it.id == "open_sans" } ?: provideFonts().first()
                }
                else -> {
                    provideFonts().find { it.id == "jost" } ?: provideFonts().first()
                }
            }
        } else {
            provideFonts().run {
                find {
                    it.id == state.value.fontFamily
                } ?: get(0)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(id = R.string.font_thickness_option)) },
        text = {
            Column(
                modifier = Modifier.padding(horizontal = SettingsHorizontalPadding)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(
                        ReaderFontThickness.THIN,
                        ReaderFontThickness.EXTRA_LIGHT,
                        ReaderFontThickness.LIGHT
                    ).forEach { thickness ->
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = thickness == state.value.fontThickness,
                            label = {
                                StyledText(
                                    text = when (thickness) {
                                        ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
                                        ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
                                        ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
                                        ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
                                        ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontFamily.font,
                                        fontWeight = thickness.thickness
                                    ),
                                    maxLines = 1
                                )
                            },
                            onClick = {
                                onThicknessSelected(thickness)
                                onDismissRequest()
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(
                        ReaderFontThickness.NORMAL,
                        ReaderFontThickness.MEDIUM
                    ).forEach { thickness ->
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = thickness == state.value.fontThickness,
                            label = {
                                StyledText(
                                    text = when (thickness) {
                                        ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
                                        ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
                                        ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
                                        ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
                                        ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontFamily.font,
                                        fontWeight = thickness.thickness
                                    ),
                                    maxLines = 1
                                )
                            },
                            onClick = {
                                onThicknessSelected(thickness)
                                onDismissRequest()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Done")
            }
        }
    )
}
