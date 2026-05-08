package org.wikipedia.settings.homefeed

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.SettingsRow
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

enum class ForYouModules(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
) {
    BASED_ON_INTEREST(
        title = R.string.home_feed_settings_based_on_interest_title,
        subtitle = R.string.home_feed_settings_based_on_interest_subtitle
    ),
    BECAUSE_YOU_READ(
        title = R.string.home_feed_settings_because_you_read_title,
        subtitle = R.string.home_feed_settings_because_you_read_subtitle
    ),
    CONTINUE_READING(
        title = R.string.home_feed_settings_continue_reading_title,
        subtitle = R.string.home_feed_settings_continue_reading_subtitle
    )
}

@Composable
fun ForYouModulesScreen(
    viewModel: ModulesViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    hiddenModules?.let {
        ForYouModulesContent(
            hiddenModules = it,
            onToggle = { module, isVisible ->
                viewModel.toggleForYouModuleVisibility(module, isVisible)
            },
            onBack = onBack
        )
    }
}

@Composable
fun ForYouModulesContent(
    hiddenModules : Set<String>,
    onToggle: (ForYouModules, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val forYourModules = remember { ForYouModules.entries }
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.home_feed_settings_for_you_title),
                onNavigationClick = onBack,
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = stringResource(R.string.home_feed_settings_for_you_modules_description),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )
            Spacer(Modifier.height(8.dp))
            forYourModules.forEach { forYouModule ->
                val isVisible = forYouModule.name !in hiddenModules
                SettingsRow(
                    title = stringResource(forYouModule.title),
                    subtitle = stringResource(forYouModule.subtitle),
                    trailingContent = {
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { newChecked ->
                                onToggle(forYouModule, newChecked)
                            },
                            colors = SwitchDefaults.colors(
                                uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                                checkedThumbColor = WikipediaTheme.colors.paperColor,
                            ),
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ForYourModulesPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ForYouModulesContent (
            onBack = {},
            onToggle = { _, _ -> },
            hiddenModules = emptySet()
        )
    }
}

