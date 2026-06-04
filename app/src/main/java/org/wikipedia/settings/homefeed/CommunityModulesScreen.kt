package org.wikipedia.settings.homefeed

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.R
import org.wikipedia.compose.components.ToggleListScreen
import org.wikipedia.compose.components.ToggleSettingItem
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.extensions.instrument

enum class CommunityModuleType(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
) {
    FEATURED_ARTICLE(
        title = R.string.view_featured_article_card_title,
        subtitle = R.string.explore_feed_featured_article_subtitle,
    ),
    TOP_READ(
        title = R.string.view_top_read_card_title,
        subtitle = R.string.view_top_read_card_description
    ),
    DID_YOU_KNOW(
        title = R.string.home_feed_did_you_know_title,
        subtitle = R.string.home_feed_did_you_know_subtitle
    ),
    NEWS(
        title = R.string.view_card_news_title,
        subtitle = R.string.explore_feed_in_the_news_subtitle
    ),
    ON_THIS_DAY(
        title = R.string.on_this_day_card_title,
        subtitle = R.string.explore_feed_on_this_day_subtitle
    ),
    FEATURED_IMAGE(
        title = R.string.view_featured_image_card_title,
        subtitle = R.string.explore_feed_potd_subtitle
    );

    fun toEntry() = ToggleSettingItem(title, subtitle, name)

    companion object {
        fun entries() = entries.map { it.toEntry() }
    }
}

@Composable
fun CommunityModulesScreen(
    viewModel: ModulesViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    var showAllOffDialog by remember { mutableStateOf(false) }
    var lastToggledOffKey by remember { mutableStateOf("") }

    hiddenModules?.let {
        ToggleListScreen(
            title = stringResource(R.string.explore_feed_community_tab_label),
            description = stringResource(R.string.home_feed_settings_community_modules_description),
            modules = CommunityModuleType.entries(),
            hiddenModules = it,
            onToggle = { key, isVisible ->
                context.instrument?.submitInteraction(if (isVisible) "enable" else "disable", actionSubtype = "feed_community", elementId = key)
                viewModel.toggleModuleVisibility(key, isVisible)
                if (!isVisible) {
                    val newHiddenModules = (viewModel.hiddenModules.value ?: emptySet()) + key
                    if (CommunityModuleType.entries().all { entry -> newHiddenModules.contains(entry.key) }) {
                        lastToggledOffKey = key
                        showAllOffDialog = true
                    }
                }
            },
            onBack = onBack
        )
    }

    if (showAllOffDialog) {
        WikipediaAlertDialog(
            title = stringResource(R.string.home_feed_settings_community_empty_dialog_title),
            message = stringResource(R.string.home_feed_settings_empty_dialog_text),
            confirmButtonText = stringResource(R.string.home_feed_settings_empty_dialog_positive),
            dismissButtonText = stringResource(android.R.string.cancel),
            onDismissRequest = { showAllOffDialog = false },
            onConfirmButtonClick = {
                showAllOffDialog = false
                context.instrument?.submitInteraction("click", actionSubtype = "feed_community", elementId = "modules_off_confirm")
            },
            onDismissButtonClick = {
                showAllOffDialog = false
                context.instrument?.submitInteraction("click", actionSubtype = "feed_community", elementId = "modules_off_cancel")
                viewModel.toggleModuleVisibility(lastToggledOffKey, true)
            }
        )
    }
}
