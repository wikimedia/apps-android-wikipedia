package org.wikipedia.settings

import androidx.annotation.StringRes
import org.wikipedia.donate.donationreminder.DonationReminderConfig

data class SettingsUiState(
    val appLanguageNames: String,
    @param:StringRes val themeNameId: Int,
    val isYearInReviewVisible: Boolean,
    val isYearInReviewEnabled: Boolean,
    val isLinkPreviewEnabled: Boolean,
    val isCollapseTablesEnabled: Boolean,
    val isEditorChoiceVisible: Boolean,
    val editorModeChoice: Int,
    val isAppIconVisible: Boolean,
    val isRecommendedReadingListEnabled: Boolean,
    val isDonationRemindersVisible: Boolean,
    val donationReminderConfig: DonationReminderConfig,
    val isSyncSectionVisible: Boolean,
    val isReadingListSyncEnabled: Boolean,
    val isDownloadReadingListArticlesEnabled: Boolean,
    val isDownloadOnlyOverWiFiEnabled: Boolean,
    val isImageDownloadEnabled: Boolean,
    val isPreferOfflineContentEnabled: Boolean,
    val account: AccountState?,
    val isDeveloperSettingsVisible: Boolean
)

data class AccountState(
    val userName: String,
    val isTemporary: Boolean,
    val tempAccountDaysLeft: Int
)
