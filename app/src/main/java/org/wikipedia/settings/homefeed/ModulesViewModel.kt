package org.wikipedia.settings.homefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.SettingsRepository

class ModulesViewModel : ViewModel() {
    val hiddenModules: StateFlow<Set<String>?> = SettingsRepository.hiddenModules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
    val feedConfigurationState: StateFlow<FeedConfigurationState> = combine(
        AppDatabase.instance.topicInterestDao().hasAnyTopics(),
        AppDatabase.instance.articleInterestDao().hasAnyArticles(),
        AppDatabase.instance.historyEntryDao().hasAnyEntries()
    ) { hasTopics, hasArticles, hasReadingHistory ->
        FeedConfigurationState.Success(
            items = listOf(
                FeedConfigurationItem(
                    type = FeedConfigurationType.INTERESTS,
                    title = R.string.home_feed_settings_feed_configuration_interest_title,
                    description = if (hasTopics || hasArticles) R.string.home_feed_settings_feed_configuration_interest_subtitle
                    else R.string.home_feed_settings_feed_configuration_empty_interest_subtitle,
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
                    description = if (hasReadingHistory) R.string.home_feed_settings_feed_configuration_reading_history_subtitle
                    else R.string.home_feed_settings_feed_configuration_empty_reading_history_subtitle,
                    icon = R.drawable.ic_history_24
                ),
                FeedConfigurationItem(
                    type = FeedConfigurationType.LANGUAGES,
                    title = R.string.home_feed_settings_feed_configuration_languages_title,
                    description = R.string.home_feed_settings_feed_configuration_languages_subtitle,
                    icon = R.drawable.ic_translate_white_24dp)
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedConfigurationState.Loading
    )
    fun toggleModuleVisibility(moduleKey: String, isVisible: Boolean) {
        viewModelScope.launch {
            if (isVisible) {
                SettingsRepository.removeHiddenModule(moduleKey)
            } else {
                SettingsRepository.addHiddenModule(moduleKey)
            }
        }
    }
}
