package org.wikipedia.settings.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs

@Composable
fun DiscoverScreen(
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    ) {
    var isDiscoverReadingOn by remember { mutableStateOf(Prefs.isDiscoverReadingListOn) }
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.recommended_reading_list_settings_title),
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (!isDiscoverReadingOn) {
                DiscoverReadingListSwitch(
                    isDiscoverReadingOn = false,
                    onCheckedChange = {
                        isDiscoverReadingOn = it
                        Prefs.isDiscoverReadingListOn = it
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.recommended_reading_list_settings_toggle_disable_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = WikipediaTheme.colors.secondaryColor
                )
            } else {
                DiscoverReadingListSwitch(
                    isDiscoverReadingOn = true,
                    onCheckedChange = {
                        isDiscoverReadingOn = it
                        Prefs.isDiscoverReadingListOn = it
                    }
                )
            }
        }
    }
}

@Composable
fun DiscoverReadingListSwitch(
    isDiscoverReadingOn: Boolean,
    onCheckedChange: ((Boolean) -> Unit),

) {
    var checked by remember { mutableStateOf(isDiscoverReadingOn) }

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp)),
        headlineContent = {
            Text(
                text = stringResource(R.string.recommended_reading_list_settings_toggle),
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onCheckedChange(checked)
                },
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = WikipediaTheme.colors.inactiveColor,
                    uncheckedThumbColor = WikipediaTheme.colors.borderColor,
                    uncheckedBorderColor = WikipediaTheme.colors.borderColor,
                    checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                    checkedThumbColor = WikipediaTheme.colors.paperColor,
                    checkedBorderColor = WikipediaTheme.colors.borderColor
                )
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = WikipediaTheme.colors.backgroundColor
        )
    )
}
