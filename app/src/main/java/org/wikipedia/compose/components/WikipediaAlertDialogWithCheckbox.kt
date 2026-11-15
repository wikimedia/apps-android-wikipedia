package org.wikipedia.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.theme.Theme

/**
 * Alert dialog that shows a (possibly HTML-formatted) message with a checkbox beneath it, which is
 * usually an opt-out of seeing the dialog again. Links in the message are opened in an external
 * browser, resolved against [wikiSite] when given.
 */
@Composable
fun WikipediaAlertDialogWithCheckbox(
    message: String,
    checkboxText: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    confirmButtonText: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleTextAlign: TextAlign = TextAlign.Start,
    wikiSite: WikiSite? = null,
    confirmButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    dismissButtonText: String? = null,
    dismissButtonColor: Color = WikipediaTheme.colors.progressiveColor,
    onConfirmButtonClick: () -> Unit = onDismissRequest,
    onDismissButtonClick: () -> Unit = onDismissRequest
) {
    AlertDialog(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        title = if (title != null) {
            {
                Text(
                    text = title,
                    color = WikipediaTheme.colors.primaryColor,
                    textAlign = titleTextAlign
                )
            }
        } else null,
        text = {
            Column {
                HtmlText(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor,
                    lineHeight = 20.sp,
                    linkInteractionListener = defaultLinkInteractionListener(wikiSite)
                )
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .toggleable(
                            value = isChecked,
                            onValueChange = onCheckedChange,
                            role = Role.Checkbox
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = WikipediaTheme.colors.progressiveColor,
                            uncheckedColor = WikipediaTheme.colors.secondaryColor
                        )
                    )
                    Text(
                        text = checkboxText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = confirmButtonColor),
                onClick = onConfirmButtonClick
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            dismissButtonText?.let {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = dismissButtonColor),
                    onClick = onDismissButtonClick
                ) {
                    Text(it)
                }
            }
        }
    )
}

@Preview
@Composable
private fun WikipediaAlertDialogWithCheckboxPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        var isChecked by remember { mutableStateOf(false) }
        WikipediaAlertDialogWithCheckbox(
            message = stringResource(R.string.talk_edit_disclaimer),
            checkboxText = stringResource(R.string.reading_list_prompt_turned_sync_on_dialog_do_not_show),
            isChecked = isChecked,
            onCheckedChange = { isChecked = it },
            confirmButtonText = stringResource(R.string.onboarding_got_it),
            onDismissRequest = { }
        )
    }
}

@Preview
@Composable
private fun WikipediaAlertDialogWithCheckboxTitledPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        var isChecked by remember { mutableStateOf(true) }
        WikipediaAlertDialogWithCheckbox(
            title = stringResource(R.string.edit_notices_please_read),
            message = stringResource(R.string.talk_edit_disclaimer),
            checkboxText = stringResource(R.string.reading_list_prompt_turned_sync_on_dialog_do_not_show),
            isChecked = isChecked,
            onCheckedChange = { isChecked = it },
            confirmButtonText = stringResource(R.string.onboarding_got_it),
            dismissButtonText = stringResource(android.R.string.cancel),
            onDismissRequest = { }
        )
    }
}
