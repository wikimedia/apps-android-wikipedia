package org.wikipedia.edit

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

const val EDITOR_CHOICE_VE = 0
const val EDITOR_CHOICE_SOURCE = 1

fun showEditorChoiceDialog(
    context: Context,
    allowShowAgainCheckbox: Boolean = true,
    onResult: (editorChoice: Int, dontShowAgain: Boolean) -> Unit
) {
    val composeView = ComposeView(context)

    MaterialAlertDialogBuilder(context)
        .setView(composeView)
        .create()
        .also { dialog ->
            composeView.setContent {
                BaseTheme {
                    EditorChoiceContent(
                        initialChoice = Prefs.editorModeChoice,
                        allowShowAgainCheckbox,
                        onCancel = { dialog.dismiss() },
                        onContinue = { editorChoice, dontShowAgain ->
                            onResult(editorChoice, dontShowAgain)
                            dialog.dismiss()
                        }
                    )
                }
            }
            dialog.show()
        }
}

@Composable
private fun EditorChoiceContent(
    initialChoice: Int,
    allowShowAgainCheckbox: Boolean = true,
    onCancel: () -> Unit = {},
    onContinue: (editorChoice: Int, dontShowAgain: Boolean) -> Unit = { _, _ -> }
) {
    var selectedEditor by remember { mutableIntStateOf(initialChoice) }
    var dontShowAgain by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.editor_select_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.editor_select_dialog_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.secondaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.selectableGroup()) {
            EditorOption(
                title = stringResource(R.string.editor_select_dialog_ve_title),
                subtitle = stringResource(R.string.editor_select_dialog_ve_subtitle),
                selected = selectedEditor == EDITOR_CHOICE_VE,
                onClick = { selectedEditor = EDITOR_CHOICE_VE }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = WikipediaTheme.colors.borderColor
            )

            EditorOption(
                title = stringResource(R.string.editor_select_dialog_source_title),
                subtitle = stringResource(R.string.editor_select_dialog_source_subtitle),
                selected = selectedEditor == EDITOR_CHOICE_SOURCE,
                onClick = { selectedEditor = EDITOR_CHOICE_SOURCE }
            )
        }

        if (allowShowAgainCheckbox) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .selectable(
                        selected = dontShowAgain,
                        onClick = { dontShowAgain = !dontShowAgain },
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = WikipediaTheme.colors.progressiveColor,
                        uncheckedColor = WikipediaTheme.colors.secondaryColor,
                    )
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.editor_select_dialog_dont_show_again),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(
                onClick = { onContinue(selectedEditor, dontShowAgain) },
            ) {
                Text(stringResource(R.string.editor_select_dialog_continue))
            }
        }
    }
}

@Composable
private fun EditorOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = WikipediaTheme.colors.progressiveColor,
                unselectedColor = WikipediaTheme.colors.secondaryColor,
            )
        )
    }
}

@Preview
@Composable
private fun EditorChoiceDialogPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        EditorChoiceContent(initialChoice = EDITOR_CHOICE_SOURCE)
    }
}
