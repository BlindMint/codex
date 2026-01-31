package us.blindmint.codex.presentation.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.navigator.NavigatorBackIconButton
import us.blindmint.codex.domain.navigator.Screen
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.utils.FuzzySearchHelper
import us.blindmint.codex.ui.settings.ReaderSettingsScreen
import us.blindmint.codex.ui.settings.AppearanceSettingsScreen
import us.blindmint.codex.ui.library.LibrarySettingsScreen
import us.blindmint.codex.ui.settings.BrowseSettingsScreen
import us.blindmint.codex.ui.settings.GeneralSettingsScreen
import us.blindmint.codex.ui.settings.ImportExportSettingsScreen
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
class SettingsSearchScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val softKeyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val listState = rememberLazyListState()

        DisposableEffect(Unit) {
            onDispose {
                softKeyboardController?.hide()
            }
        }

        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        val textFieldState = rememberTextFieldState()

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                        navigationIcon = {
                            NavigatorBackIconButton(
                                navigateBack = { navigator?.pop() }
                            )
                        },
                        title = {
                BasicTextField(
                    state = textFieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { focusManager.clearFocus() },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorator = {
                        if (textFieldState.text.isEmpty()) {
                            Text(
                                text = "Search settings...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        it()
                    },
                )
                        },
                        actions = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            },
        ) { contentPadding ->
            SearchResult(
                searchKey = textFieldState.text.toString(),
                listState = listState,
                contentPadding = contentPadding,
            ) { result ->
                SearchableSettings.highlightKey = result.preference.title
                navigator?.push(result.targetScreen, saveInBackStack = false)
            }
        }
    }

@Composable
private fun SearchResult(
    searchKey: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    onItemClick: (SearchResultItem) -> Unit,
) {
    if (searchKey.isEmpty()) return

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val screens = getSearchableSettingsScreens()

    val results by produceState<List<SearchResultItem>?>(initialValue = null, searchKey, screens) {
        if (searchKey.isEmpty()) {
            value = emptyList()
        } else {
            delay(150.milliseconds)
            value = buildSearchResults(screens, searchKey, isLtr)
        }
    }

    Crossfade(
        targetState = results,
        label = "search_results",
    ) {
        when {
            it == null -> {}
            it.isEmpty() -> {
                LazyColumnWithScrollbar(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    item {
                        StyledText(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            }
            else -> {
                LazyColumnWithScrollbar(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    items(
                        items = it,
                        key = { i -> i.hashCode() },
                    ) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        ) {
                            StyledText(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            StyledText(
                                text = item.breadcrumbs,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getSearchableSettingsScreens(): List<SearchableSettingsData> {
    return listOf(
        SearchableSettingsData(
            title = "Reader",
            screen = ReaderSettingsScreen,
            preferences = ReaderSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = "Appearance",
            screen = AppearanceSettingsScreen,
            preferences = AppearanceSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = "Library",
            screen = LibrarySettingsScreen,
            preferences = LibrarySettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = "Browse",
            screen = BrowseSettingsScreen,
            preferences = BrowseSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = "General",
            screen = GeneralSettingsScreen,
            preferences = GeneralSettingsScreen.getPreferences(),
        ),
        SearchableSettingsData(
            title = "Import/Export",
            screen = ImportExportSettingsScreen,
            preferences = ImportExportSettingsScreen.getPreferences(),
        ),
    )
}

private fun buildSearchResults(
    screens: List<SearchableSettingsData>,
    searchKey: String,
    isLtr: Boolean
): List<SearchResultItem> {
    return screens.flatMap { screenData ->
        FuzzySearchHelper.searchPreferences(
            preferences = screenData.preferences,
            query = searchKey,
            threshold = 60,
        ).map { searchResult ->
            SearchResultItem(
                targetScreen = screenData.screen,
                title = searchResult.preference.title,
                breadcrumbs = getLocalizedBreadcrumb(
                    path = screenData.title,
                    node = searchResult.breadcrumbs,
                    isLtr = isLtr,
                ),
                preference = searchResult.preference,
            )
        }
    }
}

private fun getLocalizedBreadcrumb(path: String, node: String, isLtr: Boolean): String {
    return if (node.isBlank()) {
        path
    } else {
        if (isLtr) {
            "$path > $node"
        } else {
            "$node < $path"
        }
    }
}

data class SearchableSettingsData(
    val title: String,
    val screen: Screen,
    val preferences: List<Preference>,
)

data class SearchResultItem(
    val targetScreen: Screen,
    val title: String,
    val breadcrumbs: String,
    val preference: Preference.PreferenceItem<*, *>,
)
}
