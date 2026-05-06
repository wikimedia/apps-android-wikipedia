package org.wikipedia.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = WikipediaTheme.colors.progressiveColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Preview
@Composable
private fun SettingsSectionPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsSection(
            title = "For you"
        ) {
            Column {
                SettingsRow(
                    title = "Modules",
                    subtitle = "Turn on or off For You sections"
                )
                SettingsRow(
                    title = "Notifications",
                    subtitle = "Manage your notification preferences for home feed updates",
                    trailingContent = {
                        Switch(
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsRow(
            title = "Modules",
            subtitle = "Manage the modules that appear on your home feed"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowWithSwitchPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SettingsRow(
            title = "Modules",
            subtitle = "Manage the modules that appear on your home feed",
            trailingContent = {
                Switch(
                    checked = true,
                    onCheckedChange = { /* Handle switch state change */ }
                )
            }
        )
    }
}
