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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.components.SettingsRow
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.SettingsRepository
import org.wikipedia.theme.Theme

enum class CommunityModules(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
) {
    FEATURED_ARTICLE(
        title = R.string.view_featured_article_card_title,
        subtitle = R.string.home_feed_settings_community_module_featured_article_subtitle,
    ),
    TOP_READ(
        title = R.string.view_top_read_card_title,
        subtitle = R.string.home_feed_settings_community_module_top_red_subtitle
    ),
    FEATURED_IMAGE(
        title = R.string.view_featured_image_card_title,
        subtitle = R.string.home_feed_settings_community_module_picture_of_the_day_subtitle
    ),
    NEWS(
        title = R.string.view_card_news_title,
        subtitle = R.string.home_feed_settings_community_module_in_the_news_subtitle
    ),
    ON_THIS_DAY(
        title = R.string.on_this_day_card_title,
        subtitle = R.string.home_feed_settings_community_module_on_this_day_subtitle
    )
}

@Composable
fun CommunityModulesScreen(
    viewModel: CommunityModulesViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    hiddenModules?.let {
        CommunityModulesContent(
            hiddenModules = it,
            onToggle = { module, isVisible ->
                viewModel.toggleModuleVisibility(module, isVisible)
            },
            onBack = onBack
        )
    }
}

@Composable
fun CommunityModulesContent(
    hiddenModules: Set<String>,
    onToggle: (CommunityModules, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val communityModules = remember { CommunityModules.entries }
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.home_feed_settings_community_title),
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
                text = stringResource(R.string.home_feed_settings_community_modules_description),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.primaryColor
            )
            Spacer(Modifier.height(8.dp))
            communityModules.forEach { communityModule ->
                val isVisible = communityModule.name !in hiddenModules
                SettingsRow(
                    title = stringResource(communityModule.title),
                    subtitle = stringResource(communityModule.subtitle),
                    trailingContent = {
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { newChecked ->
                                onToggle(communityModule, newChecked)
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

class CommunityModulesViewModel : ViewModel() {
    val hiddenModules: StateFlow<Set<String>?> = SettingsRepository.hiddenModules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun toggleModuleVisibility(modules: CommunityModules, isVisible: Boolean) {
        viewModelScope.launch {
            if (isVisible) {
                SettingsRepository.removeHiddenModule(modules.name)
            } else {
                SettingsRepository.addHiddenModule(modules.name)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommunityModulesScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        CommunityModulesContent(
            onBack = {},
            onToggle = { _, _ -> },
            hiddenModules = setOf(CommunityModules.NEWS.name)
        )
    }
}
