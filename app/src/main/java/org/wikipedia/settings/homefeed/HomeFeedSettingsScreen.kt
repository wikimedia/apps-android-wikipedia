package org.wikipedia.settings.homefeed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun HomeFeedSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onForYouModulesClick: () -> Unit,
    onCommunityModulesClick: () -> Unit,
    onWhatsDrivingFeedClick: () -> Unit
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
        ) {
            // TODO: remove this, its for test only
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onForYouModulesClick,
                ) {
                    Text(text = "For You Modules")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onCommunityModulesClick,
                ) {
                    Text(text = "Community Modules")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onWhatsDrivingFeedClick,
                ) {
                    Text(text = "Whats driving Modules")
                }
            }
        }
    }
}
