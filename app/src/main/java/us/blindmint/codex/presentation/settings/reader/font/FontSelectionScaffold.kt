package us.blindmint.codex.presentation.settings.reader.font

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.presentation.core.components.settings.GenericOption
import us.blindmint.codex.presentation.core.components.settings.OptionConfig
import us.blindmint.codex.presentation.core.constants.provideFonts
import us.blindmint.codex.presentation.settings.reader.font.components.CustomFontsOption
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    listState: LazyListState,
    navigateBack: () -> Unit
) {
    Scaffold(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            FontSelectionTopBar(
                scrollBehavior = scrollBehavior,
                navigateBack = navigateBack
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                FontFamilyChips()
            }

            item {
                CustomFontsOption()
            }
        }
    }
}

@Composable
private fun FontFamilyChips() {
    GenericOption(
        OptionConfig(
            stateSelector = { it.fontFamily },
            eventCreator = { MainEvent.OnChangeFontFamily(it) },
            component = { value, onChange ->
                val selectedFontId = if (value.startsWith("custom_")) null else value

                val selectedFont = provideFonts().run {
                    find { it.id == selectedFontId } ?: get(0)
                }

                androidx.compose.foundation.layout.Column {
                    ChipsWithTitle(
                        title = stringResource(id = R.string.font_family_option),
                        chips = provideFonts()
                            .map {
                                ButtonItem(
                                    id = it.id,
                                    title = it.fontName.asString(),
                                    textStyle = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = it.font
                                    ),
                                    selected = selectedFontId != null && it.id == selectedFont.id
                                )
                            },
                        onClick = { onChange(it.id) }
                    )
                }
            }
        )
    )
}
