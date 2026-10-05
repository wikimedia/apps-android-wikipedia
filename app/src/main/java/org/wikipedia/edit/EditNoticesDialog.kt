package org.wikipedia.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.defaultLinkInteractionListener
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil

/**
 * Dialog that presents the edit notices for the page being edited, with an option to keep showing
 * them automatically. Links within the notices are opened in an external browser, resolved against
 * [wikiSite] when given.
 */
@Composable
fun EditNoticesDialog(
    editNotices: List<String>,
    autoShowEnabled: Boolean,
    onAutoShowChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    wikiSite: WikiSite? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = AlertDialogDefaults.shape,
            color = WikipediaTheme.colors.paperColor
        ) {
            Box {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                            .size(64.dp),
                        painter = painterResource(R.drawable.ic_warning_24),
                        tint = WikipediaTheme.colors.warningColor,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.edit_notices),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = WikipediaTheme.colors.primaryColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.edit_notices_please_read),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = WikipediaTheme.colors.primaryColor,
                        textAlign = TextAlign.Center
                    )
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .weight(1f, fill = false)
                    ) {
                        items(editNotices) { notice ->
                            EditNoticeDivider()
                            HtmlText(
                                modifier = Modifier.padding(vertical = 16.dp),
                                text = StringUtil.removeStyleTags(notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WikipediaTheme.colors.primaryColor,
                                lineHeight = 20.sp,
                                linkInteractionListener = defaultLinkInteractionListener(wikiSite)
                            )
                        }
                        item {
                            EditNoticeDivider()
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = autoShowEnabled,
                                onValueChange = onAutoShowChange,
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            modifier = Modifier.padding(vertical = 16.dp),
                            checked = autoShowEnabled,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = WikipediaTheme.colors.progressiveColor,
                                uncheckedColor = WikipediaTheme.colors.secondaryColor
                            )
                        )
                        Text(
                            text = stringResource(R.string.edit_notices_show_auto),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                }
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = onDismissRequest
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_black_24dp),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = stringResource(R.string.table_close)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditNoticeDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = WikipediaTheme.colors.borderColor
    )
}

@Preview
@Composable
private fun EditNoticesDialogPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        var autoShowEnabled by remember { mutableStateOf(true) }
        EditNoticesDialog(
            editNotices = listOf(
                "This article is subject to <b>discretionary sanctions</b>. Please read the " +
                        "<a href=\"#guidelines\">guidelines</a> before editing.",
                "Please do not add unsourced content to this article.",
                "This page is <i>semi-protected</i>."
            ),
            autoShowEnabled = autoShowEnabled,
            onAutoShowChange = { autoShowEnabled = it },
            onDismissRequest = { }
        )
    }
}
