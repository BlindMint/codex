package us.blindmint.codex.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import us.blindmint.codex.presentation.core.components.common.LazyColumnWithScrollbar
import us.blindmint.codex.presentation.core.components.settings.SliderWithTitle
import us.blindmint.codex.presentation.core.components.common.StyledText
import us.blindmint.codex.presentation.core.components.settings.SwitchWithTitle
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryNote
import us.blindmint.codex.presentation.settings.components.SettingsSubcategoryTitle

val LocalPreferenceHighlighted = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    LazyColumnWithScrollbar(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item {
                        Column {
                            SettingsSubcategoryTitle(title = preference.title)
                        }
                    }
                    items(preference.preferenceItems) { item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                        )
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                SwitchWithTitle(
                    selected = item.checked,
                    title = item.title,
                    description = item.subtitle,
                    onClick = {
                        scope.launch {
                            item.onValueChanged(!item.checked)
                        }
                    },
                )
            }
            is Preference.PreferenceItem.SliderPreference -> {
                SliderWithTitle(
                    value = item.value to (item.valueString ?: item.value.toString()),
                    fromValue = item.valueRange.first,
                    toValue = item.valueRange.last,
                    title = item.title,
                    onValueChange = {
                        scope.launch {
                            item.onValueChanged(it)
                        }
                    },
                    steps = item.steps,
                )
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                var showDialog by remember { mutableStateOf(false) }

                StyledText(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { showDialog = true },
                )

                if (showDialog) {
                }
            }
            is Preference.PreferenceItem.TextPreference -> {
                StyledText(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable(onClick = item.onClick),
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                var showDialog by remember { mutableStateOf(false) }

                StyledText(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { showDialog = true },
                )

                if (showDialog) {
                }
            }
            is Preference.PreferenceItem.InfoPreference -> {
                SettingsSubcategoryNote(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content()
            }
        }
    }
}

@Composable
private fun StatusWrapper(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey

    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        CompositionLocalProvider(
            LocalPreferenceHighlighted provides highlighted,
        ) {
            if (highlighted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(null)
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null)
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
