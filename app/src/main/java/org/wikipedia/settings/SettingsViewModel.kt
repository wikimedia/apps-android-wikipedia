package org.wikipedia.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListSource
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.yearinreview.YearInReviewViewModel

class SettingsViewModel : ViewModel() {
    // Bumped on resume, to pick up values that don't live in SharedPreferences
    // (account, app languages, theme, remote config).
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        Prefs.observeKeys(
            R.string.preference_key_year_in_review_is_enabled,
            R.string.preference_key_show_link_previews,
            R.string.preference_key_collapse_tables,
            R.string.preference_key_editor_mode_choice,
            R.string.preference_key_recommended_reading_list_enabled,
            R.string.preference_key_donation_reminder_config,
            R.string.preference_key_sync_reading_lists,
            R.string.preference_key_download_reading_list_articles,
            R.string.preference_key_download_only_over_wifi,
            R.string.preference_key_show_images,
            R.string.preference_key_prefer_offline_content,
            R.string.preference_key_show_developer_settings
        ),
        refreshTrigger
    ) { _, _ -> loadState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = loadState()
        )

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun setLinkPreviewEnabled(enabled: Boolean) {
        Prefs.isLinkPreviewEnabled = enabled
    }

    fun setCollapseTablesEnabled(enabled: Boolean) {
        Prefs.isCollapseTablesEnabled = enabled
    }

    fun setDownloadReadingListArticlesEnabled(enabled: Boolean) {
        Prefs.isDownloadingReadingListArticlesEnabled = enabled
    }

    fun setDownloadOnlyOverWiFiEnabled(enabled: Boolean) {
        Prefs.isDownloadOnlyOverWiFiEnabled = enabled
    }

    fun setImageDownloadEnabled(enabled: Boolean) {
        Prefs.isImageDownloadEnabled = enabled
    }

    fun setPreferOfflineContent(enabled: Boolean) {
        Prefs.setPreferOfflineContent(enabled)
    }

    fun setEditorModeChoice(editorChoice: Int) {
        Prefs.editorModeChoice = editorChoice
    }

    fun enableYearInReview() {
        Prefs.isYearInReviewEnabled = true
    }

    fun disableYearInReview() {
        Prefs.yearInReviewModelData = emptyMap()
        Prefs.isYearInReviewEnabled = false
    }

    fun setReadingListSyncChecked(checked: Boolean) {
        Prefs.isReadingListSyncEnabled = checked
    }

    fun enableReadingListSync() {
        Prefs.isReadingListSyncEnabled = true
        ReadingListSyncAdapter.setSyncEnabledWithSetup()
    }

    fun disableReadingListSyncAndDeleteRemote() {
        Prefs.isReadingListSyncEnabled = false
        Prefs.isReadingListsRemoteSetupPending = false
        Prefs.isReadingListsRemoteDeletePending = true
        ReadingListSyncAdapter.manualSync()
    }

    fun shouldShowRecommendedReadingListOnboarding(): Boolean {
        return Prefs.recommendedReadingListInterests.isEmpty() &&
            Prefs.recommendedReadingListSource == RecommendedReadingListSource.INTERESTS
    }

    fun logOut() {
        WikipediaApp.instance.logOut()
        Prefs.readingListsLastSyncTime = null
        Prefs.isReadingListSyncEnabled = false
        Prefs.isSuggestedEditsHighestPriorityEnabled = false
    }

    /** @return whether there was any local donation history to delete. */
    fun deleteLocalDonationHistory(): Boolean {
        val hasDonations = Prefs.donationResults.isNotEmpty()
        if (hasDonations) {
            Prefs.donationResults = emptyList()
        }
        return hasDonations
    }

    private fun loadState(): SettingsUiState {
        return SettingsUiState(
            appLanguageNames = WikipediaApp.instance.languageState.appLanguageLocalizedNames,
            themeNameId = WikipediaApp.instance.currentTheme.nameId,
            isYearInReviewVisible = YearInReviewViewModel.isAccessible,
            isYearInReviewEnabled = Prefs.isYearInReviewEnabled,
            isLinkPreviewEnabled = Prefs.isLinkPreviewEnabled,
            isCollapseTablesEnabled = Prefs.isCollapseTablesEnabled,
            isEditorChoiceVisible = RemoteConfig.config.androidv1?.visualEditorEnabled ?: false,
            editorModeChoice = Prefs.editorModeChoice,
            isAppIconVisible = YearInReviewViewModel.isCustomIconAllowed,
            isRecommendedReadingListEnabled = Prefs.isRecommendedReadingListEnabled,
            isDonationRemindersVisible = DonationReminderHelper.isEnabled,
            donationReminderConfig = Prefs.donationReminderConfig,
            isSyncSectionVisible = !RemoteConfig.config.disableReadingListSync,
            isReadingListSyncEnabled = Prefs.isReadingListSyncEnabled,
            isDownloadReadingListArticlesEnabled = Prefs.isDownloadingReadingListArticlesEnabled,
            isDownloadOnlyOverWiFiEnabled = Prefs.isDownloadOnlyOverWiFiEnabled,
            isImageDownloadEnabled = Prefs.isImageDownloadEnabled,
            isPreferOfflineContentEnabled = Prefs.preferOfflineContent(),
            account = if (AccountUtil.isLoggedIn) {
                AccountState(
                    userName = AccountUtil.userName,
                    isTemporary = AccountUtil.isTemporaryAccount,
                    tempAccountDaysLeft = AccountUtil.tempAccountDaysLeft()
                )
            } else null,
            isDeveloperSettingsVisible = Prefs.isShowDeveloperSettingsEnabled
        )
    }
}
