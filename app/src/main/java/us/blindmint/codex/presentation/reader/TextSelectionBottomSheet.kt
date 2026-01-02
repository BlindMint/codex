/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.blindmint.codex.R
import us.blindmint.codex.domain.lookup.LookupDefaults
import us.blindmint.codex.domain.lookup.WebDictionary
import us.blindmint.codex.domain.lookup.WebSearchEngine
import us.blindmint.codex.domain.reader.TextSelectionContext
import us.blindmint.codex.presentation.core.components.modal_bottom_sheet.ModalBottomSheet

/**
 * Submenu state for the text selection bottom sheet.
 */
enum class TextSelectionSubmenu {
    NONE,
    WEB_SEARCH,
    DICTIONARY,
    BOOKMARK
}

/**
 * Discord-style bottom sheet for text selection actions.
 *
 * @param selectionContext The text selection context
 * @param onDismiss Called when the bottom sheet is dismissed
 * @param onCopy Called when copy action is selected
 * @param onBookmark Called when bookmark action is selected (placeholder for future)
 * @param onWebSearch Called when a web search engine is selected
 * @param onDictionary Called when a dictionary is selected
 * @param onExpandSelection Called when user taps a context word to expand selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionBottomSheet(
    selectionContext: TextSelectionContext,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkWithName: (customName: String) -> Unit,
    onWebSearch: (WebSearchEngine) -> Unit,
    onDictionary: (WebDictionary) -> Unit,
    onExpandSelection: (expandLeading: Boolean) -> Unit
) {
    var currentSubmenu by remember { mutableStateOf(TextSelectionSubmenu.NONE) }

    ModalBottomSheet(
        hasFixedHeight = true,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f),
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = true
    ) {
        AnimatedContent(
            targetState = currentSubmenu,
            transitionSpec = {
                if (targetState == TextSelectionSubmenu.NONE) {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                } else {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                }
            },
            label = "submenu_transition"
        ) { submenu ->
            when (submenu) {
                TextSelectionSubmenu.NONE -> {
                    MainSelectionContent(
                        selectionContext = selectionContext,
                        onCopy = {
                            onCopy()
                            onDismiss()
                        },
                        onBookmarkClick = { currentSubmenu = TextSelectionSubmenu.BOOKMARK },
                        onWebSearchClick = { currentSubmenu = TextSelectionSubmenu.WEB_SEARCH },
                        onDictionaryClick = { currentSubmenu = TextSelectionSubmenu.DICTIONARY },
                        onExpandSelection = onExpandSelection
                    )
                }
                TextSelectionSubmenu.WEB_SEARCH -> {
                    WebSearchSubmenu(
                        selectedText = selectionContext.selectedText,
                        onBack = { currentSubmenu = TextSelectionSubmenu.NONE },
                        onSelect = { engine ->
                            onWebSearch(engine)
                            onDismiss()
                        }
                    )
                }
                TextSelectionSubmenu.DICTIONARY -> {
                    DictionarySubmenu(
                        selectedText = selectionContext.selectedText,
                        onBack = { currentSubmenu = TextSelectionSubmenu.NONE },
                        onSelect = { dictionary ->
                            onDictionary(dictionary)
                            onDismiss()
                        }
                    )
                }
                TextSelectionSubmenu.BOOKMARK -> {
                    BookmarkNamingSubmenu(
                        selectedText = selectionContext.selectedText,
                        onBack = { currentSubmenu = TextSelectionSubmenu.NONE },
                        onSelect = { customName ->
                            onBookmarkWithName(customName)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSelectionContent(
    selectionContext: TextSelectionContext,
    onCopy: () -> Unit,
    onBookmarkClick: () -> Unit,
    onWebSearchClick: () -> Unit,
    onDictionaryClick: () -> Unit,
    onExpandSelection: (expandLeading: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Selected text preview with context
        SelectedTextPreview(
            selectionContext = selectionContext,
            onExpandSelection = onExpandSelection
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Primary actions card
        ActionCard {
            ActionItem(
                icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                label = stringResource(R.string.copy),
                onClick = onCopy
            )
            ActionItem(
                icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                label = stringResource(R.string.bookmark),
                onClick = onBookmarkClick,
                showArrow = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lookup actions card
        ActionCard {
            ActionItem(
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = stringResource(R.string.web_search),
                onClick = onWebSearchClick,
                showArrow = true
            )
            ActionItem(
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                label = stringResource(R.string.dictionary),
                onClick = onDictionaryClick,
                showArrow = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SelectedTextPreview(
    selectionContext: TextSelectionContext,
    onExpandSelection: (expandLeading: Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading context words (tappable to expand)
            selectionContext.leadingContext.forEachIndexed { index, word ->
                ContextWord(
                    word = word,
                    onClick = {
                        // Expand selection to include this word and all words after it
                        repeat(selectionContext.leadingContext.size - index) {
                            onExpandSelection(true)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Selected text (highlighted)
            Text(
                text = selectionContext.selectedText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 4.dp)
            )

            // Trailing context words (tappable to expand)
            selectionContext.trailingContext.forEachIndexed { index, word ->
                Spacer(modifier = Modifier.width(4.dp))
                ContextWord(
                    word = word,
                    onClick = {
                        // Expand selection to include this word and all words before it
                        repeat(index + 1) {
                            onExpandSelection(false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextWord(
    word: String,
    onClick: () -> Unit
) {
    Text(
        text = word,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    )
}

@Composable
private fun ActionCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun ActionItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WebSearchSubmenu(
    selectedText: String,
    onBack: () -> Unit,
    onSelect: (WebSearchEngine) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SubmenuHeader(
            title = stringResource(R.string.web_search),
            subtitle = selectedText,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard {
            LookupDefaults.searchEngines.forEach { engine ->
                ActionItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = engine.name,
                    onClick = { onSelect(engine) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DictionarySubmenu(
    selectedText: String,
    onBack: () -> Unit,
    onSelect: (WebDictionary) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SubmenuHeader(
            title = stringResource(R.string.dictionary),
            subtitle = selectedText,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard {
            LookupDefaults.dictionaries.forEach { dictionary ->
                ActionItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = dictionary.name,
                    onClick = { onSelect(dictionary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SubmenuHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.go_back_content_desc),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "\"$subtitle\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookmarkNamingSubmenu(
    selectedText: String,
    onBack: () -> Unit,
    onSelect: (customName: String) -> Unit
) {
    var customName by remember { mutableStateOf(selectedText) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SubmenuHeader(
            title = stringResource(R.string.bookmark),
            subtitle = selectedText,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text(stringResource(R.string.bookmark_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onBack
            ) {
                Text(stringResource(R.string.cancel_button))
            }
            TextButton(
                onClick = {
                    onSelect(if (customName.isBlank()) selectedText else customName)
                }
            ) {
                Text(stringResource(R.string.confirm_button))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
