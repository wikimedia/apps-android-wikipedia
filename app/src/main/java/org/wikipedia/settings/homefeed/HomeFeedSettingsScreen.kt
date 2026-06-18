package org.wikipedia.settings.homefeed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
fun HomeFeedSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onForYouModulesClick: () -> Unit = {},
    onCommunityModulesClick: () -> Unit = {},
    onFeedConfigurationClick: () -> Unit = {},
    onDefaultFeedViewClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(id = R.string.home_feed_settings_title),
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
                .padding(top = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(34.dp)
        ) {
            SettingsRow(
                title = stringResource(R.string.home_feed_settings_default_view_title),
                subtitle = stringResource(R.string.home_feed_settings_default_view_subtitle),
                onClick = onDefaultFeedViewClick
            )

            SettingsSection(
                title = stringResource(R.string.explore_feed_community_tab_label)
            ) {
                SettingsRow(
                    title = stringResource(R.string.home_feed_settings_modules_title),
                    subtitle = stringResource(R.string.home_feed_settings_community_subtitle),
                    onClick = onCommunityModulesClick
                )
            }

            SettingsSection(
                title = stringResource(R.string.explore_feed_for_you_tab_label)
            ) {
                SettingsRow(
                    title = stringResource(R.string.home_feed_settings_modules_title),
                    subtitle = stringResource(R.string.home_feed_settings_for_you_subtitle),
                    onClick = onForYouModulesClick
                )
                SettingsRow(
                    title = stringResource(R.string.home_feed_settings_whats_driving_title),
                    subtitle = stringResource(R.string.home_feed_settings_whats_driving_subtitle),
                    onClick = onFeedConfigurationClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeFeedSettingsScreenLightPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HomeFeedSettingsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeFeedSettingsScreenDarkPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        HomeFeedSettingsScreen()
    }
}
