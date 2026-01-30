package us.blindmint.codex.presentation.settings

import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.presentation.settings.reader.search.SearchSubcategory
import us.blindmint.codex.presentation.settings.reader.search.components.SearchHighlightColorOption
import us.blindmint.codex.presentation.settings.reader.search.components.ShowSearchScrollbarOption
import us.blindmint.codex.presentation.settings.reader.search.components.SearchScrollbarOpacityOption

fun buildSearchableSettingsIndex(
    navigateToReaderSettings: () -> Unit
): List<SearchableSettingsItem> {
    return mutableListOf<SearchableSettingsItem>().apply {
        add(
            SearchableSettingsItem(
                id = "reader",
                title = stringResource(id = R.string.reader_settings),
                category = stringResource(id = R.string.reader_settings),
                type = SearchableSettingsType.CATEGORY,
                onClick = navigateToReaderSettings
            )
        )

        add(
            SearchableSettingsItem(
                id = "reader.search",
                title = stringResource(id = R.string.search_reader_settings),
                category = stringResource(id = R.string.reader_settings),
                subcategory = stringResource(id = R.string.search_reader_settings),
                type = SearchableSettingsType.SUBCATEGORY,
                onClick = navigateToReaderSettings
            )
        )

        add(
            SearchableSettingsItem(
                id = "reader.search.highlight_color",
                title = stringResource(id = R.string.search_highlight_color_option),
                category = stringResource(id = R.string.reader_settings),
                subcategory = stringResource(id = R.string.search_reader_settings),
                type = SearchableSettingsType.SUBSETTING,
                onClick = navigateToReaderSettings
            )
        )
        add(
            SearchableSettingsItem(
                id = "reader.search.show_scrollbar",
                title = stringResource(id = R.string.show_search_scrollbar_option),
                category = stringResource(id = R.string.reader_settings),
                subcategory = stringResource(id = R.string.search_reader_settings),
                type = SearchableSettingsType.SUBSETTING,
                onClick = navigateToReaderSettings
            )
        )
        add(
            SearchableSettingsItem(
                id = "reader.search.scrollbar_opacity",
                title = stringResource(id = R.string.search_scrollbar_opacity_option),
                category = stringResource(id = R.string.reader_settings),
                subcategory = stringResource(id = R.string.search_reader_settings),
                type = SearchableSettingsType.SUBSETTING,
                onClick = navigateToReaderSettings
            )
        )
    }
}
