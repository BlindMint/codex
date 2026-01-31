package us.blindmint.codex.presentation.settings.reader.font.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.reader.ReaderFontThickness
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.OptionConfig
import us.blindmint.codex.presentation.core.constants.SettingsHorizontalPadding
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun FontThicknessOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

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

    val selectedThickness = state.value.fontThickness
    val thicknessText = when (selectedThickness) {
        ReaderFontThickness.THIN -> stringResource(id = R.string.font_thickness_thin)
        ReaderFontThickness.EXTRA_LIGHT -> stringResource(id = R.string.font_thickness_extra_light)
        ReaderFontThickness.LIGHT -> stringResource(id = R.string.font_thickness_light)
        ReaderFontThickness.NORMAL -> stringResource(id = R.string.font_thickness_normal)
        ReaderFontThickness.MEDIUM -> stringResource(id = R.string.font_thickness_medium)
    }

    GenericOption(
        OptionConfig(
            stateSelector = { it.fontThickness },
            eventCreator = { MainEvent.OnChangeFontThickness(it.toString()) },
            component = { value, onChange ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialog = true }
                        .padding(horizontal = SettingsHorizontalPadding, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.font_thickness_option),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(36.dp),
                            selected = true,
                            label = {
                                StyledText(
                                    text = thicknessText,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontFamily.font,
                                        fontWeight = value.thickness
                                    ),
                                    maxLines = 1
                                )
                            },
                            onClick = { showDialog = true },
                        )
                    }
                }

                if (showDialog) {
                    FontThicknessDialog(
                        onDismissRequest = { showDialog = false },
                        onThicknessSelected = { thickness ->
                            mainModel.onEvent(MainEvent.OnChangeFontThickness(thickness.toString()))
                        }
                    )
                }
            }
        )
    )
}
