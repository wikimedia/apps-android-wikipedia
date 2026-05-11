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

enum class CommunityModuleSetting(
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
    );

    fun toEntry() = ModuleEntry(title, subtitle, name)

    companion object {
        fun entries() = entries.map { it.toEntry() }
    }
}

@Composable
fun CommunityModulesScreen(
    viewModel: ModulesViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    hiddenModules?.let {
        ToggleListScreen(
            screenTitle = stringResource(R.string.home_feed_settings_community_title),
            description = stringResource(R.string.home_feed_settings_community_modules_description),
            modules = CommunityModuleSetting.entries(),
            hiddenModules = it,
            onToggle = viewModel::toggleModuleVisibility,
            onBack = onBack
        )
    }
}
