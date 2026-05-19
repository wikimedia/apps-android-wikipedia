package org.wikipedia.settings.homefeed

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

enum class FeedConfigurationType(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int
) {
    INTERESTS(
        title = R.string.home_feed_settings_feed_configuration_interest_title,
        description = R.string.home_feed_settings_feed_configuration_interest_subtitle,
        icon = R.drawable.ic_baseline_tune_24
    ),
    READING_HISTORY(
        title = R.string.home_feed_settings_feed_configuration_reading_history_title,
        description = R.string.home_feed_settings_feed_configuration_reading_history_subtitle,
        icon = R.drawable.ic_history_24
    ),
    LANGUAGES(
        title = R.string.home_feed_settings_feed_configuration_languages_title,
        description = R.string.home_feed_settings_feed_configuration_languages_subtitle,
        icon = R.drawable.ic_translate_white_24dp
    )
}

@Composable
fun FeedConfigurationScreen(
    onBack: () -> Unit,
    onInterestsClick: () -> Unit,
    onReadingHistoryClick: () -> Unit,
    onLanguagesClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.home_feed_settings_whats_driving_title),
                onNavigationClick = onBack,
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp),
                text = stringResource(R.string.home_feed_settings_feed_configuration_description),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )

            FeedConfigurationType.entries.forEach { type ->
                FeedConfigurationItem(
                    type = type,
                    onClick = { type ->
                        when (type) {
                            FeedConfigurationType.INTERESTS -> onInterestsClick()
                            FeedConfigurationType.READING_HISTORY -> onReadingHistoryClick()
                            FeedConfigurationType.LANGUAGES -> onLanguagesClick()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FeedConfigurationItem(
    type: FeedConfigurationType,
    onClick: (FeedConfigurationType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onClick(type) }
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp),
            painter = painterResource(type.icon),
            tint = WikipediaTheme.colors.primaryColor,
            contentDescription = null
        )
        Column {
            Text(
                text = stringResource(type.title),
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = stringResource(type.description),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        }
    }
}

@Preview
@Composable
private fun FeedConfigurationScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedConfigurationScreen(
            onBack = {},
            onInterestsClick = {},
            onReadingHistoryClick = {},
            onLanguagesClick = {}
        )
    }
}
