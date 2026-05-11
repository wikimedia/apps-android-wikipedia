package org.wikipedia.settings.homefeed

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.ModuleEntry
import org.wikipedia.compose.components.ToggleListScreen

enum class ForYouModuleSetting(
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
    );

    fun toEntry() = ModuleEntry(title, subtitle, name)

    companion object {
        fun entries() = entries.map { it.toEntry() }
    }
}

@Composable
fun ForYouModulesScreen(
    viewModel: ModulesViewModel = viewModel(),
    navigateToFeedConfigurationScreen: () -> Unit,
    onBack: () -> Unit,
) {
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    hiddenModules?.let {
        ToggleListScreen(
            screenTitle = stringResource(R.string.home_feed_settings_for_you_title),
            description = stringResource(R.string.home_feed_settings_for_you_modules_description),
            modules = ForYouModuleSetting.entries(),
            hiddenModules = it,
            onToggle = viewModel::toggleModuleVisibility,
            onBack = onBack,
            onSubtitleLinkClick = { href ->
                when (href) {
                    "#drivingFeed" -> navigateToFeedConfigurationScreen()
                }
            }
        )
    }
}
