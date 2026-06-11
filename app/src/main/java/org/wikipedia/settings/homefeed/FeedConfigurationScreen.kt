package org.wikipedia.settings.homefeed

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

enum class FeedConfigurationType { INTERESTS, LOCATION, READING_HISTORY, LANGUAGES }

data class FeedConfigurationItem(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val type: FeedConfigurationType,
)

sealed class FeedConfigurationState {
    data class Success(val items: List<FeedConfigurationItem>) : FeedConfigurationState()
    object Loading : FeedConfigurationState()
}

@Composable
fun FeedConfigurationScreen(
    viewModel: ModulesViewModel = viewModel(),
    onBack: () -> Unit,
    onInterestsClick: () -> Unit,
    onLocationClick: () -> Unit,
    onReadingHistoryClick: () -> Unit,
    onLanguagesClick: () -> Unit,
) {
    val state by viewModel.feedConfigurationState.collectAsStateWithLifecycle()

    FeedConfigurationContent(
        state = state,
        onBack = onBack,
        onItemClick = { type ->
            when (type) {
                FeedConfigurationType.INTERESTS -> onInterestsClick()
                FeedConfigurationType.READING_HISTORY -> onReadingHistoryClick()
                FeedConfigurationType.LOCATION -> onLocationClick()
                FeedConfigurationType.LANGUAGES -> onLanguagesClick()
            }
        }
    )
}

@Composable
fun FeedConfigurationContent(
    state: FeedConfigurationState,
    onBack: () -> Unit,
    onItemClick: (FeedConfigurationType) -> Unit
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
        when (state) {
            FeedConfigurationState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            }
            is FeedConfigurationState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        text = stringResource(R.string.home_feed_settings_feed_configuration_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    state.items.forEach { item ->
                        FeedConfigurationItemView(
                            item = item,
                            onClick = { onItemClick(item.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedConfigurationItemView(
    item: FeedConfigurationItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                modifier = Modifier
                    .size(22.dp),
                painter = painterResource(item.icon),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = null
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(item.title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
                Text(
                    text = stringResource(item.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor
                )
            }
        }
    }
}

@Preview
@Composable
private fun FeedConfigurationContentPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FeedConfigurationContent(
            state = FeedConfigurationState.Success(
                items = listOf(
                    FeedConfigurationItem(
                        type = FeedConfigurationType.INTERESTS,
                        title = R.string.home_feed_settings_feed_configuration_interest_title,
                        description = R.string.home_feed_settings_feed_configuration_empty_interest_subtitle,
                        icon = R.drawable.ic_baseline_tune_24
                    ),
                    FeedConfigurationItem(
                        type = FeedConfigurationType.LOCATION,
                        title = R.string.home_feed_settings_feed_configuration_location_title,
                        description = R.string.home_feed_settings_feed_configuration_location_subtitle,
                        icon = R.drawable.ic_location_on_24dp
                    ),
                    FeedConfigurationItem(
                        type = FeedConfigurationType.READING_HISTORY,
                        title = R.string.home_feed_settings_feed_configuration_reading_history_title,
                        description = R.string.home_feed_settings_feed_configuration_empty_reading_history_subtitle,
                        icon = R.drawable.ic_history_24
                    ),
                    FeedConfigurationItem(
                        type = FeedConfigurationType.LANGUAGES,
                        title = R.string.home_feed_settings_feed_configuration_languages_title,
                        description = R.string.home_feed_settings_feed_configuration_languages_subtitle,
                        icon = R.drawable.ic_translate_white_24dp
                    )
                )
            ),
            onBack = {},
            onItemClick = {}
        )
    }
}
