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

enum class ForYouModuleType(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
) {
    BASED_ON_INTEREST(
        title = R.string.home_feed_settings_based_on_interest_title,
        subtitle = R.string.home_feed_settings_based_on_interest_subtitle
    ),
    BECAUSE_YOU_READ(
        title = R.string.view_because_you_read_card_title,
        subtitle = R.string.home_feed_settings_because_you_read_subtitle
    ),
    CONTINUE_READING(
        title = R.string.home_feed_settings_continue_reading_title,
        subtitle = R.string.home_feed_settings_continue_reading_subtitle
    ),
    PLACES_OF_INTEREST(
        title = R.string.home_feed_settings_places_of_interest_title,
        subtitle = R.string.home_feed_settings_places_of_interest_subtitle
    ),
    DISCOVER(
        title = R.string.home_feed_settings_discover_title,
        subtitle = R.string.home_feed_settings_discover_subtitle
    ),
    RANDOM(
        title = R.string.home_feed_random_title,
        subtitle = R.string.home_feed_random_subtitle
    );

    fun toEntry() = ToggleSettingItem(title, subtitle, name)

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
    val context = LocalContext.current
    val hiddenModules by viewModel.hiddenModules.collectAsState()
    var showAllOffDialog by remember { mutableStateOf(false) }
    var lastToggledOffKey by remember { mutableStateOf("") }

    hiddenModules?.let {
        ToggleListScreen(
            title = stringResource(R.string.explore_feed_for_you_tab_label),
            description = stringResource(R.string.home_feed_settings_for_you_modules_description),
            modules = ForYouModuleType.entries(),
            hiddenModules = it,
            onToggle = { key, isVisible ->
                context.instrument?.submitInteraction(if (isVisible) "enable" else "disable", actionSubtype = "feed_for_you", elementId = key)
                viewModel.toggleModuleVisibility(key, isVisible)
                if (!isVisible) {
                    val newHiddenModules = (viewModel.hiddenModules.value ?: emptySet()) + key
                    if (ForYouModuleType.entries().all { entry -> newHiddenModules.contains(entry.key) }) {
                        lastToggledOffKey = key
                        showAllOffDialog = true
                    }
                }
            },
            onBack = onBack,
            onSubtitleLinkClick = { href ->
                when (href) {
                    "#drivingFeed" -> navigateToFeedConfigurationScreen()
                }
            }
        )
    }

    if (showAllOffDialog) {
        WikipediaAlertDialog(
            title = stringResource(R.string.home_feed_settings_for_you_empty_dialog_title),
            message = stringResource(R.string.home_feed_settings_empty_dialog_text),
            confirmButtonText = stringResource(R.string.home_feed_settings_empty_dialog_positive),
            dismissButtonText = stringResource(android.R.string.cancel),
            onDismissRequest = { showAllOffDialog = false },
            onConfirmButtonClick = {
                showAllOffDialog = false
                context.instrument?.submitInteraction("click", actionSubtype = "feed_for_you", elementId = "modules_off_confirm")
            },
            onDismissButtonClick = {
                showAllOffDialog = false
                context.instrument?.submitInteraction("click", actionSubtype = "feed_for_you", elementId = "modules_off_cancel")
                viewModel.toggleModuleVisibility(lastToggledOffKey, true)
            }
        )
    }
}
