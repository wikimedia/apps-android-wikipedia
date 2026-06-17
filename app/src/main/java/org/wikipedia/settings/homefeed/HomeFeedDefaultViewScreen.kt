package org.wikipedia.settings.homefeed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.SettingsRow
import org.wikipedia.compose.components.SettingsSection
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun HomeFeedDefaultViewScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onForYouModulesClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(id = R.string.home_feed_settings_default_view_title),
                onNavigationClick = {
                    onBackClick()
                }
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clickable(onClick = {

                    }),
                verticalAlignment = Alignment.Top
            ) {
                RadioButton(
                    selected = true,
                    onClick = { },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WikipediaTheme.colors.primaryColor,
                        unselectedColor = WikipediaTheme.colors.primaryColor,
                    )
                )
                Column(

                ) {
                    Text(
                        text = stringResource(R.string.explore_feed_community_tab_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.explore_feed_preference_community_content_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clickable(onClick = {

                    }),
                verticalAlignment = Alignment.Top
            ) {
                RadioButton(
                    selected = true,
                    onClick = { },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WikipediaTheme.colors.primaryColor,
                        unselectedColor = WikipediaTheme.colors.primaryColor,
                    )
                )
                Column(

                ) {
                    Text(
                        text = stringResource(R.string.explore_feed_for_you_tab_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.explore_feed_preference_personalized_content_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeFeedDefaultViewScreenLightPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HomeFeedDefaultViewScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeFeedDefaultViewScreenDarkPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        HomeFeedDefaultViewScreen()
    }
}
