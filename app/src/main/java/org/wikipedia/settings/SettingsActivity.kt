package org.wikipedia.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.BuildConfig
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.SingleWebViewActivity
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.donate.donationreminder.DonationReminderActivity
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.edit.showEditorChoiceDialog
import org.wikipedia.events.ReadingListsEnabledStatusEvent
import org.wikipedia.json.JsonUtil
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.readinglist.recommended.RecommendedReadingListSettingsActivity
import org.wikipedia.settings.dev.DeveloperSettingsActivity
import org.wikipedia.settings.homefeed.HomeFeedSettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.theme.ThemeFittingRoomActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.YearInReviewViewModel

class SettingsActivity : BaseActivity() {
    private lateinit var initialLanguageList: String
    private lateinit var initialFeedCardsEnabled: List<Boolean>
    private lateinit var initialFeedCardsLangDisabled: Map<Int, List<String>>
    private lateinit var initialFeedCardsOrder: List<Int>
    private val app = WikipediaApp.instance
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialLanguageList = JsonUtil.encodeToString(app.languageState.appLanguageCodes).orEmpty()
        initialFeedCardsEnabled = Prefs.feedCardsEnabled
        initialFeedCardsLangDisabled = Prefs.feedCardsLangDisabled
        initialFeedCardsOrder = Prefs.feedCardsOrder

        if (YearInReviewViewModel.isAccessible) {
            YearInReviewEvent.submit(action = "impression", slide = "setting")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                FlowEventBus.events.collectLatest { event ->
                    if (event is ReadingListsEnabledStatusEvent) {
                        viewModel.setReadingListSyncChecked(true)
                    }
                }
            }
        }

        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onAction = ::onAction
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        DonationReminderHelper.maybeShowSettingSnackbar(this)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val finalLanguageList = JsonUtil.encodeToString(app.languageState.appLanguageCodes)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE &&
                finalLanguageList != initialLanguageList) {
            setResult(ACTIVITY_RESULT_LANGUAGE_CHANGED)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_FEED_CONFIGURE &&
                (Prefs.feedCardsEnabled != initialFeedCardsEnabled || Prefs.feedCardsOrder != initialFeedCardsOrder || Prefs.feedCardsOrder != initialFeedCardsLangDisabled)) {
            setResult(ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED)
        }
    }

    private fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.BackClick -> onBackPressedDispatcher.onBackPressed()
            SettingsAction.DeveloperSettingsClick -> startActivity(DeveloperSettingsActivity.newIntent(this))
            SettingsAction.AppLanguagesClick -> startActivityForResult(
                WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.SETTINGS),
                Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE
            )
            SettingsAction.HomeFeedClick -> startActivityForResult(
                HomeFeedSettingsActivity.newIntent(this),
                Constants.ACTIVITY_REQUEST_FEED_CONFIGURE
            )
            SettingsAction.AppThemeClick -> startActivity(ThemeFittingRoomActivity.newIntent(this))
            SettingsAction.EditorChoiceClick -> showEditorChoiceDialog(this, isSettingsScreen = true) { editorChoice, _ ->
                viewModel.setEditorModeChoice(editorChoice)
            }
            SettingsAction.AppIconClick -> ExclusiveBottomSheetPresenter.show(supportFragmentManager, AppIconDialog())
            SettingsAction.RecommendedReadingListClick -> onRecommendedReadingListClick()
            SettingsAction.DonationRemindersClick -> {
                DonorExperienceEvent.logDonationReminderAction(
                    activeInterface = "global_setting",
                    action = "donation_reminder_click"
                )
                startActivity(DonationReminderActivity.newIntent(this, isFromSettings = true))
            }
            SettingsAction.DeleteLocalDonationHistoryClick -> onDeleteLocalDonationHistoryClick()
            SettingsAction.AboutAppClick -> startActivity(Intent(this, AboutActivity::class.java))
            SettingsAction.SendFeedbackClick -> FeedbackUtil.composeEmail(
                this,
                subject = "Android App ${BuildConfig.VERSION_NAME} Feedback",
                body = deviceInformation()
            )
            SettingsAction.LogoutClick -> showLogoutDialog()
            SettingsAction.VanishAccountClick -> showVanishAccountDialog()
            is SettingsAction.ExternalLinkClick -> UriUtil.visitInExternalBrowser(this, action.url.toUri())
            is SettingsAction.YearInReviewToggled -> onYearInReviewToggled(action.enabled)
            is SettingsAction.ReadingListSyncToggled -> onReadingListSyncToggled(action.enabled)
            is SettingsAction.LinkPreviewsToggled -> viewModel.setLinkPreviewEnabled(action.enabled)
            is SettingsAction.CollapseTablesToggled -> viewModel.setCollapseTablesEnabled(action.enabled)
            is SettingsAction.DownloadReadingListArticlesToggled -> viewModel.setDownloadReadingListArticlesEnabled(action.enabled)
            is SettingsAction.DownloadOnlyOverWiFiToggled -> viewModel.setDownloadOnlyOverWiFiEnabled(action.enabled)
            is SettingsAction.ImageDownloadToggled -> viewModel.setImageDownloadEnabled(action.enabled)
            is SettingsAction.PreferOfflineContentToggled -> viewModel.setPreferOfflineContent(action.enabled)
        }
    }

    private fun onYearInReviewToggled(enabled: Boolean) {
        if (enabled) {
            YearInReviewEvent.submit(action = "yir_on_click", slide = "setting")
            viewModel.enableYearInReview()
            return
        }
        YearInReviewEvent.submit(action = "yir_off_click", slide = "setting")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.year_in_review_disable_title)
            .setMessage(R.string.year_in_review_setting_subtitle)
            .setPositiveButton(R.string.year_in_review_disable_positive_button) { _, _ ->
                YearInReviewEvent.submit(action = "yir_off_confirm_click", slide = "setting")
                viewModel.disableYearInReview()
            }
            .setNegativeButton(R.string.year_in_review_disable_negative_button) { _, _ ->
                YearInReviewEvent.submit(action = "yir_off_cancel_click", slide = "setting")
            }
            .show()
    }

    private fun onReadingListSyncToggled(enabled: Boolean) {
        if (!AccountUtil.isLoggedIn) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reading_list_preference_login_to_enable_sync_dialog_title)
                .setMessage(R.string.reading_list_preference_login_to_enable_sync_dialog_text)
                .setPositiveButton(R.string.reading_list_preference_login_to_enable_sync_dialog_login) { _: DialogInterface, _: Int ->
                    startActivity(LoginActivity.newIntent(this, LoginActivity.SOURCE_SETTINGS))
                }
                .setNegativeButton(R.string.reading_list_preference_login_to_enable_sync_dialog_cancel, null)
                .show()
            return
        }
        if (enabled) {
            viewModel.enableReadingListSync()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.preference_dialog_of_turning_off_reading_list_sync_title, AccountUtil.userName))
            .setMessage(getString(R.string.preference_dialog_of_turning_off_reading_list_sync_text, AccountUtil.userName))
            .setPositiveButton(R.string.reading_lists_confirm_remote_delete_yes) { _, _ ->
                viewModel.disableReadingListSyncAndDeleteRemote()
            }
            .setNegativeButton(R.string.reading_lists_confirm_remote_delete_no, null)
            .show()
    }

    private fun onRecommendedReadingListClick() {
        RecommendedReadingListEvent.submit("discover_click", "global_settings")
        if (viewModel.shouldShowRecommendedReadingListOnboarding()) {
            startActivity(RecommendedReadingListOnboardingActivity.newIntent(this))
        } else {
            startActivity(RecommendedReadingListSettingsActivity.newIntent(this))
        }
    }

    private fun onDeleteLocalDonationHistoryClick() {
        DonorExperienceEvent.logDonationReminderAction(
            activeInterface = "global_setting",
            action = "clear_donation_hist_click"
        )
        val messageResId = if (viewModel.deleteLocalDonationHistory()) {
            R.string.donation_history_deleted_message_snackbar
        } else R.string.donation_history_no_history_message_snackbar
        FeedbackUtil.showMessage(this, getString(messageResId))
    }

    private fun showLogoutDialog() {
        val instrument = TestKitchenAdapter.client.getInstrument("apps-authentication")
            .startFunnel("logout_account")
        instrument.submitInteraction("click", actionSource = "settings", elementId = "logout_button")
        instrument.submitInteraction("impression", actionSource = "logout_warning")
        MaterialAlertDialogBuilder(this)
            .setMessage(if (AccountUtil.isTemporaryAccount) R.string.temp_account_end_session_confirm else R.string.logout_prompt)
            .setNegativeButton(R.string.logout_dialog_cancel_button_text) { _, _ ->
                instrument.submitInteraction("click", actionSource = "logout_warning", elementId = "cancel_button")
            }
            .setPositiveButton(if (AccountUtil.isTemporaryAccount) R.string.temp_account_end_session else R.string.preference_title_logout) { _, _ ->
                instrument.submitInteraction("click", actionSource = "logout_warning", elementId = "confirm_button")
                viewModel.logOut()
                setResult(ACTIVITY_RESULT_LOG_OUT)
                finish()
            }.show()
    }

    private fun showVanishAccountDialog() {
        val instrument = TestKitchenAdapter.client.getInstrument("apps-authentication")
            .startFunnel("vanish_account")
            .setDefaultActionSource("vanish_warning")
        instrument.submitInteraction("click", actionSource = "settings", elementId = "vanish_button")
        instrument.submitInteraction("impression")
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon_Delete)
            .setIcon(R.drawable.ic_person_remove)
            .setTitle(R.string.account_vanish_request_confirm_title)
            .setMessage(StringUtil.fromHtml(getString(R.string.account_vanish_request_confirm)))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                instrument.submitInteraction("click", elementId = "cancel_button")
            }
            .setPositiveButton(R.string.account_vanish_request_title) { _, _ ->
                instrument.submitInteraction("click", elementId = "confirm_button")
                finish()
                startActivity(SingleWebViewActivity.newIntent(this, getString(R.string.account_vanish_url), isWebForm = true))
            }.show()
    }

    private fun deviceInformation(): String {
        return "\n\nVersion: ${BuildConfig.VERSION_NAME} \nDevice: ${Build.BRAND} ${Build.MODEL} (SDK: ${Build.VERSION.SDK_INT})\nAppInstallId: ${app.appInstallID}"
    }

    companion object {
        const val ACTIVITY_RESULT_LANGUAGE_CHANGED = 1
        const val ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED = 2
        const val ACTIVITY_RESULT_LOG_OUT = 3

        fun newIntent(ctx: Context) = Intent(ctx, SettingsActivity::class.java)
    }
}
