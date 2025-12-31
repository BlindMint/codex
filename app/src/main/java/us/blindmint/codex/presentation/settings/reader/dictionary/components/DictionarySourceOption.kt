/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.settings.reader.dictionary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.blindmint.codex.R
import us.blindmint.codex.domain.dictionary.DictionarySource
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.ChipsWithTitle
import us.blindmint.codex.ui.main.MainEvent
import us.blindmint.codex.ui.main.MainModel

@Composable
fun DictionarySourceOption() {
    val mainModel = hiltViewModel<MainModel>()
    val state = mainModel.state.collectAsStateWithLifecycle()

    Column {
        ChipsWithTitle(
            title = stringResource(id = R.string.dictionary_source_option),
            chips = DictionarySource.entries.map { source ->
                ButtonItem(
                    id = source.id,
                    title = when (source) {
                        DictionarySource.SYSTEM_DEFAULT -> stringResource(id = R.string.dictionary_system_default)
                        DictionarySource.ONELOOK -> stringResource(id = R.string.dictionary_onelook)
                        DictionarySource.WIKTIONARY -> stringResource(id = R.string.dictionary_wiktionary)
                        DictionarySource.GOOGLE_DEFINE -> stringResource(id = R.string.dictionary_google)
                        DictionarySource.MERRIAM_WEBSTER -> stringResource(id = R.string.dictionary_merriam_webster)
                        DictionarySource.CUSTOM -> stringResource(id = R.string.dictionary_custom)
                    },
                    textStyle = MaterialTheme.typography.labelLarge,
                    selected = source == state.value.dictionarySource
                )
            },
            onClick = { chip ->
                mainModel.onEvent(
                    MainEvent.OnChangeDictionarySource(
                        DictionarySource.fromId(chip.id)
                    )
                )
            }
        )

        // Show custom URL input when CUSTOM is selected
        AnimatedVisibility(visible = state.value.dictionarySource == DictionarySource.CUSTOM) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.value.customDictionaryUrl,
                    onValueChange = { newUrl ->
                        mainModel.onEvent(MainEvent.OnChangeCustomDictionaryUrl(newUrl))
                    },
                    label = { Text(stringResource(id = R.string.dictionary_custom_url_label)) },
                    placeholder = { Text("https://example.com/define?word=%s") },
                    supportingText = { Text(stringResource(id = R.string.dictionary_custom_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
