package org.wikipedia.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.SettingsRow
import org.wikipedia.compose.components.SettingsSection
import org.wikipedia.compose.components.SettingsSwitch
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.donationreminder.DonationReminderConfig
import org.wikipedia.edit.EDITOR_CHOICE_VE
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.YearInReviewViewModel

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.settings_activity_title),
                onNavigationClick = { onAction(SettingsAction.BackClick) },
                actions = {
                    if (uiState.isDeveloperSettingsVisible) {
                        IconButton(onClick = { onAction(SettingsAction.DeveloperSettingsClick) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_developer_mode_white_24dp),
                                contentDescription = stringResource(R.string.menu_developer_settings),
                                tint = WikipediaTheme.colors.secondaryColor
                            )
                        }
                    }
                }
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GeneralSection(uiState, onAction)
            RecommendationsSection(uiState, onAction)
            DonationsSection(uiState, onAction)
            if (uiState.isSyncSectionVisible) {
                SyncSection(uiState, onAction)
            }
            DataUsageSection(uiState, onAction)
            AboutSection(onAction)
            uiState.account?.let {
                SettingsAccountSection(
                    account = it,
                    onLogoutClick = { onAction(SettingsAction.LogoutClick) },
                    onVanishClick = { onAction(SettingsAction.VanishAccountClick) }
                )
            }
        }
    }
}

@Composable
private fun GeneralSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SettingsSection(title = stringResource(R.string.preferences_general_heading)) {
        ClickableSetting(
            title = stringResource(R.string.preference_title_language),
            subtitle = uiState.appLanguageNames,
            onClick = { onAction(SettingsAction.AppLanguagesClick) }
        )
        ClickableSetting(
            title = stringResource(R.string.preference_title_customize_home_feed),
            subtitle = stringResource(R.string.preference_summary_customize_home_feed),
            onClick = { onAction(SettingsAction.HomeFeedClick) }
        )
        if (uiState.isYearInReviewVisible) {
            SwitchSetting(
                title = stringResource(R.string.nav_item_year_in_review),
                subtitle = stringResource(R.string.year_in_review_setting_subtitle),
                checked = uiState.isYearInReviewEnabled,
                onCheckedChange = { onAction(SettingsAction.YearInReviewToggled(it)) }
            )
        }
        SwitchSetting(
            title = stringResource(R.string.preference_title_show_link_previews),
            subtitle = stringResource(R.string.preference_summary_show_link_previews),
            checked = uiState.isLinkPreviewEnabled,
            onCheckedChange = { onAction(SettingsAction.LinkPreviewsToggled(it)) }
        )
        SwitchSetting(
            title = stringResource(R.string.preference_title_collapse_tables),
            subtitle = stringResource(R.string.preference_summary_collapse_tables),
            checked = uiState.isCollapseTablesEnabled,
            onCheckedChange = { onAction(SettingsAction.CollapseTablesToggled(it)) }
        )
        ClickableSetting(
            title = stringResource(R.string.preference_title_app_theme),
            subtitle = stringResource(uiState.themeNameId),
            onClick = { onAction(SettingsAction.AppThemeClick) }
        )
        if (uiState.isEditorChoiceVisible) {
            ClickableSetting(
                title = stringResource(R.string.editor_select_title_settings_screen),
                subtitle = stringResource(
                    if (uiState.editorModeChoice == EDITOR_CHOICE_VE) R.string.editor_select_dialog_ve_title
                    else R.string.editor_select_dialog_source_title
                ),
                onClick = { onAction(SettingsAction.EditorChoiceClick) }
            )
        }
        if (uiState.isAppIconVisible) {
            ClickableSetting(
                title = stringResource(R.string.settings_app_icon_title),
                subtitle = stringResource(R.string.settings_app_icon_preference_subtitle, YearInReviewViewModel.YIR_YEAR),
                onClick = { onAction(SettingsAction.AppIconClick) }
            )
        }
    }
}

@Composable
private fun RecommendationsSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SettingsSection(title = stringResource(R.string.recommended_reading_list_settings_section_header)) {
        ClickableSetting(
            title = stringResource(R.string.recommended_reading_list_settings_toggle),
            subtitle = stringResource(
                if (uiState.isRecommendedReadingListEnabled) R.string.recommended_reading_list_settings_toggle_enable_message
                else R.string.recommended_reading_list_settings_toggle_disable_message
            ),
            onClick = { onAction(SettingsAction.RecommendedReadingListClick) }
        )
    }
}

@Composable
private fun DonationsSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SettingsSection(title = stringResource(R.string.donation_settings_section_header)) {
        if (uiState.isDonationRemindersVisible) {
            val config = uiState.donationReminderConfig
            val description = if (config.userEnabled) {
                stringResource(
                    R.string.donation_reminders_settings_description_on,
                    DonateUtil.currencyFormat.format(config.donateAmount),
                    pluralStringResource(R.plurals.donation_reminders_text_articles, config.articleFrequency, config.articleFrequency)
                )
            } else {
                stringResource(R.string.donation_reminders_settings_description_off)
            }
            ClickableSetting(
                title = stringResource(R.string.donation_reminders_settings_option_title),
                subtitle = description,
                onClick = { onAction(SettingsAction.DonationRemindersClick) }
            )
        }
        ClickableSetting(
            title = stringResource(R.string.preference_title_delete_local_donation_history),
            onClick = { onAction(SettingsAction.DeleteLocalDonationHistoryClick) }
        )
    }
}

@Composable
private fun SyncSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SettingsSection(title = stringResource(R.string.preferences_heading_syncing)) {
        SwitchSetting(
            title = stringResource(R.string.preference_title_sync_reading_lists_from_account),
            subtitle = uiState.account?.let {
                stringResource(R.string.preference_summary_sync_reading_lists_from_account, it.userName)
            } ?: stringResource(R.string.preference_summary_sync_reading_lists),
            checked = uiState.isReadingListSyncEnabled,
            onCheckedChange = { onAction(SettingsAction.ReadingListSyncToggled(it)) }
        )
        SwitchSetting(
            title = stringResource(R.string.preference_title_download_reading_list_articles),
            checked = uiState.isDownloadReadingListArticlesEnabled,
            onCheckedChange = { onAction(SettingsAction.DownloadReadingListArticlesToggled(it)) }
        )
    }
}

@Composable
private fun DataUsageSection(uiState: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    SettingsSection(title = stringResource(R.string.preferences_heading_data_usage)) {
        SwitchSetting(
            title = stringResource(R.string.preference_title_download_only_over_wifi),
            checked = uiState.isDownloadOnlyOverWiFiEnabled,
            onCheckedChange = { onAction(SettingsAction.DownloadOnlyOverWiFiToggled(it)) }
        )
        SwitchSetting(
            title = stringResource(R.string.preference_title_show_images),
            subtitle = stringResource(R.string.preference_summary_show_images),
            checked = uiState.isImageDownloadEnabled,
            onCheckedChange = { onAction(SettingsAction.ImageDownloadToggled(it)) }
        )
        SwitchSetting(
            title = stringResource(R.string.preference_title_prefer_offline_content),
            subtitle = stringResource(R.string.preference_summary_prefer_offline_content),
            checked = uiState.isPreferOfflineContentEnabled,
            onCheckedChange = { onAction(SettingsAction.PreferOfflineContentToggled(it)) }
        )
    }
}

@Composable
private fun AboutSection(onAction: (SettingsAction) -> Unit) {
    val faqUrl = stringResource(R.string.android_app_faq_url)
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val termsOfUseUrl = stringResource(R.string.terms_of_use_url)
    val legalSafetyContactUrl = stringResource(R.string.legal_safety_contact_url)
    SettingsSection(title = stringResource(R.string.about_activity_title)) {
        ClickableSetting(
            title = stringResource(R.string.about_description),
            onClick = { onAction(SettingsAction.AboutAppClick) }
        )
        ExternalLinkSetting(
            title = stringResource(R.string.send_feedback),
            onClick = { onAction(SettingsAction.SendFeedbackClick) }
        )
        ExternalLinkSetting(
            title = stringResource(R.string.wikipedia_app_faq),
            onClick = { onAction(SettingsAction.ExternalLinkClick(faqUrl)) }
        )
        ExternalLinkSetting(
            title = stringResource(R.string.privacy_policy_description),
            onClick = { onAction(SettingsAction.ExternalLinkClick(privacyPolicyUrl)) }
        )
        ExternalLinkSetting(
            title = stringResource(R.string.terms_of_use_description),
            onClick = { onAction(SettingsAction.ExternalLinkClick(termsOfUseUrl)) }
        )
        ExternalLinkSetting(
            title = stringResource(R.string.legal_safety_contacts_description),
            onClick = { onAction(SettingsAction.ExternalLinkClick(legalSafetyContactUrl)) }
        )
    }
}

@Composable
private fun ClickableSetting(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    SettingsRow(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        title = title,
        subtitle = subtitle,
        verticalAlignment = Alignment.CenterVertically,
        trailingContent = trailingContent
    )
}

@Composable
private fun ExternalLinkSetting(
    title: String,
    onClick: () -> Unit
) {
    ClickableSetting(
        title = title,
        onClick = onClick,
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_open_in_new_black_24px),
                contentDescription = null,
                tint = WikipediaTheme.colors.secondaryColor
            )
        }
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    SettingsRow(
        modifier = Modifier
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        title = title,
        subtitle = subtitle,
        verticalAlignment = Alignment.CenterVertically,
        trailingContent = {
            SettingsSwitch(
                checked = checked,
                onCheckedChange = null
            )
        }
    )
}

private val previewUiState = SettingsUiState(
    appLanguageNames = "English",
    themeNameId = R.string.color_theme_light,
    isYearInReviewVisible = true,
    isYearInReviewEnabled = true,
    isLinkPreviewEnabled = true,
    isCollapseTablesEnabled = true,
    isEditorChoiceVisible = true,
    editorModeChoice = EDITOR_CHOICE_VE,
    isAppIconVisible = false,
    isRecommendedReadingListEnabled = false,
    isDonationRemindersVisible = true,
    donationReminderConfig = DonationReminderConfig(),
    isSyncSectionVisible = true,
    isReadingListSyncEnabled = false,
    isDownloadReadingListArticlesEnabled = true,
    isDownloadOnlyOverWiFiEnabled = false,
    isImageDownloadEnabled = true,
    isPreferOfflineContentEnabled = false,
    account = null,
    isDeveloperSettingsVisible = true
)

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun SettingsScreenLoggedOutPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SettingsScreen(
            uiState = previewUiState,
            onAction = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun SettingsScreenLoggedInDarkPreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        SettingsScreen(
            uiState = previewUiState.copy(
                themeNameId = R.string.color_theme_dark,
                isReadingListSyncEnabled = true,
                account = AccountState(userName = "ExampleUser", isTemporary = false, tempAccountDaysLeft = 0)
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun SettingsScreenTemporaryAccountPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SettingsScreen(
            uiState = previewUiState.copy(
                account = AccountState(userName = "~2026-12345", isTemporary = true, tempAccountDaysLeft = 42)
            ),
            onAction = {}
        )
    }
}
