package org.wikipedia.settings

sealed interface SettingsAction {
    data object BackClick : SettingsAction
    data object DeveloperSettingsClick : SettingsAction
    data object AppLanguagesClick : SettingsAction
    data object HomeFeedClick : SettingsAction
    data object AppThemeClick : SettingsAction
    data object EditorChoiceClick : SettingsAction
    data object AppIconClick : SettingsAction
    data object RecommendedReadingListClick : SettingsAction
    data object DonationRemindersClick : SettingsAction
    data object DeleteLocalDonationHistoryClick : SettingsAction
    data object AboutAppClick : SettingsAction
    data object SendFeedbackClick : SettingsAction
    data object LogoutClick : SettingsAction
    data object VanishAccountClick : SettingsAction
    data class ExternalLinkClick(val url: String) : SettingsAction
    data class YearInReviewToggled(val enabled: Boolean) : SettingsAction
    data class LinkPreviewsToggled(val enabled: Boolean) : SettingsAction
    data class CollapseTablesToggled(val enabled: Boolean) : SettingsAction
    data class ReadingListSyncToggled(val enabled: Boolean) : SettingsAction
    data class DownloadReadingListArticlesToggled(val enabled: Boolean) : SettingsAction
    data class DownloadOnlyOverWiFiToggled(val enabled: Boolean) : SettingsAction
    data class ImageDownloadToggled(val enabled: Boolean) : SettingsAction
    data class PreferOfflineContentToggled(val enabled: Boolean) : SettingsAction
}
