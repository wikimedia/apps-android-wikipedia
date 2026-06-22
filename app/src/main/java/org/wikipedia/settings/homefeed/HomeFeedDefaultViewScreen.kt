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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.theme.Theme

@Composable
fun HomeFeedDefaultViewScreen(
    modifier: Modifier = Modifier,
    currentDefaultView: HomePreferenceType = HomePreferenceType.COMMUNITY,
    onBackClick: () -> Unit = {},
    onDefaultViewSelect: (HomePreferenceType) -> Unit = {}
) {
    var currentSelection by remember { mutableStateOf(currentDefaultView) }

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        currentSelection = HomePreferenceType.COMMUNITY
                        onDefaultViewSelect(currentSelection)
                    }),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                    selected = currentSelection == HomePreferenceType.COMMUNITY,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WikipediaTheme.colors.primaryColor,
                        unselectedColor = WikipediaTheme.colors.primaryColor,
                    )
                )
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 24.dp).weight(1f)) {
                    Text(
                        text = stringResource(R.string.explore_feed_community_tab_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.explore_feed_preference_community_content_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        currentSelection = HomePreferenceType.PERSONALIZED
                        onDefaultViewSelect(currentSelection)
                    }),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                    selected = currentSelection == HomePreferenceType.PERSONALIZED,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WikipediaTheme.colors.primaryColor,
                        unselectedColor = WikipediaTheme.colors.primaryColor,
                    )
                )
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 24.dp).weight(1f)) {
                    Text(
                        text = stringResource(R.string.explore_feed_for_you_tab_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.explore_feed_preference_personalized_content_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeFeedDefaultViewScreenLightPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HomeFeedDefaultViewScreen()
    }
}

@Preview
@Composable
private fun HomeFeedDefaultViewScreenDarkPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        HomeFeedDefaultViewScreen()
    }
}
