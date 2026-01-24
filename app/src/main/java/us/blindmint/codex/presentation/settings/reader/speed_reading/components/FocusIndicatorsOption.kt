package us.blindmint.codex.presentation.settings.reader.speed_reading.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import us.blindmint.codex.R
import us.blindmint.codex.domain.ui.ButtonItem
import us.blindmint.codex.presentation.core.components.settings.SegmentedButtonWithTitle

@Composable
fun FocusIndicatorsOption(
    selected: String,
    onSelectionChange: (String) -> Unit
) {
    SegmentedButtonWithTitle(
        title = stringResource(id = R.string.speed_reading_focus_indicators),
        buttons = listOf(
            ButtonItem(
                id = "OFF",
                title = stringResource(id = R.string.speed_reading_focus_indicators_off),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = selected == "OFF"
            ),
            ButtonItem(
                id = "LINES",
                title = stringResource(id = R.string.speed_reading_focus_indicators_lines),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = selected == "LINES"
            ),
            ButtonItem(
                id = "ARROWS",
                title = stringResource(id = R.string.speed_reading_focus_indicators_arrows),
                textStyle = MaterialTheme.typography.labelLarge,
                selected = selected == "ARROWS"
            )
        ),
        onClick = { onSelectionChange(it.id) }
    )
}