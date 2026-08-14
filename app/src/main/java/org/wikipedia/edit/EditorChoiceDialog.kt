package org.wikipedia.edit

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

const val EDITOR_CHOICE_VE = 0
const val EDITOR_CHOICE_SOURCE = 1

data class EditorChoiceDialogConfig(
    @param:StringRes val dialogTitle: Int,
    @param:StringRes val confirmButtonText: Int,
    val isInSettingsScreen: Boolean
)

private val instrument = TestKitchenAdapter.client.getInstrument("apps-editing")

fun showEditorChoiceDialog(
    context: Context,
    isSettingsScreen: Boolean,
    onResult: (editorChoice: Int, dontShowAgain: Boolean) -> Unit
) {

    val dialogConfig = if (isSettingsScreen) {
        instrument.submitInteraction(
            action = "click",
            actionSource = "settings",
            elementId = "editing_method"
        )
        EditorChoiceDialogConfig(
            dialogTitle = R.string.editor_select_title_settings_screen,
            confirmButtonText = R.string.editor_select_save_btn_settings_screen,
            isInSettingsScreen = true
        )
    } else {
        instrument.submitInteraction(
            action = "impression",
            actionSource = "edit_choice_select"
        )
        EditorChoiceDialogConfig(
            dialogTitle = R.string.editor_select_dialog_title,
            confirmButtonText = R.string.editor_select_dialog_continue,
            isInSettingsScreen = false
        )
    }

    val composeView = ComposeView(context)

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(composeView)
        .show()

    composeView.setContent {
        BaseTheme {
            EditorChoiceContent(
                initialChoice = Prefs.editorModeChoice,
                dialogConfigData = dialogConfig,
                onCancel = {
                    if (!isSettingsScreen) {
                        instrument.submitInteraction(
                            action = "click",
                            actionSource = "edit_choice_select",
                            elementId = "edit_choice_cancel"
                        )
                    }
                    dialog.dismiss()
                },
                onConfirm = { editorChoice, dontShowAgain ->
                    if (!isSettingsScreen) {
                        instrument.submitInteraction(
                            action = "click",
                            actionSource = "edit_choice_select",
                            elementId = "edit_choice_submit",
                            actionContext = mapOf(
                                "edit_choice" to if (editorChoice == EDITOR_CHOICE_VE) "visual" else "source",
                                "is_default" to dontShowAgain
                            )
                        )
                    }
                    onResult(editorChoice, dontShowAgain)
                    dialog.dismiss()
                }
            )
        }
    }
}

@Composable
private fun EditorChoiceContent(
    initialChoice: Int,
    dialogConfigData: EditorChoiceDialogConfig,
    onCancel: () -> Unit = {},
    onConfirm: (editorChoice: Int, dontShowAgain: Boolean) -> Unit = { _, _ -> }
) {
    var selectedEditor by remember { mutableIntStateOf(initialChoice) }
    var dontShowAgain by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            text = stringResource(dialogConfigData.dialogTitle),
            style = MaterialTheme.typography.headlineSmall,
            color = WikipediaTheme.colors.primaryColor
        )

        Column(modifier = Modifier.selectableGroup()) {
            EditorOption(
                title = stringResource(R.string.editor_select_dialog_ve_title),
                subtitle = stringResource(R.string.editor_select_dialog_ve_subtitle),
                selected = selectedEditor == EDITOR_CHOICE_VE,
                onClick = { selectedEditor = EDITOR_CHOICE_VE },
                shouldShowOpenInNewIcon = !dialogConfigData.isInSettingsScreen
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

        if (!dialogConfigData.isInSettingsScreen) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
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
                onClick = { onConfirm(selectedEditor, dontShowAgain) },
            ) {
                Text(stringResource(dialogConfigData.confirmButtonText))
            }
        }
    }
}

@Composable
private fun EditorOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    shouldShowOpenInNewIcon: Boolean = false,
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
            .heightIn(min = 88.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
            if (shouldShowOpenInNewIcon) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new_black_24px),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(R.string.editor_select_icon_ve_title),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight()
        ) {
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
}

@Preview(showBackground = true)
@Composable
private fun EditorChoiceDialogNonSettingsScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        EditorChoiceContent(
            initialChoice = EDITOR_CHOICE_SOURCE,
            dialogConfigData = EditorChoiceDialogConfig(
                dialogTitle = R.string.editor_select_dialog_title,
                confirmButtonText = R.string.editor_select_dialog_continue,
                isInSettingsScreen = false
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorChoiceDialogSettingsScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        EditorChoiceContent(
            initialChoice = EDITOR_CHOICE_SOURCE,
            EditorChoiceDialogConfig(
                dialogTitle = R.string.editor_select_title_settings_screen,
                confirmButtonText = R.string.editor_select_save_btn_settings_screen,
                isInSettingsScreen = true
            )
        )
    }
}
