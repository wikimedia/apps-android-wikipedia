package org.wikipedia.donate.donationreminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity.Companion.EXTRA_FROM_SETTINGS
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.UriUtil

class DonationReminderActivity : BaseActivity() {
    private val viewModel: DonationReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                DonationReminderScreen(
                    viewModel = viewModel,
                    onBackButtonClick = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                    onConfirmBtnClick = { message ->
                        DonationReminderHelper.shouldShowSettingSnackbar = true
                        setResult(RESULT_OK_FROM_DONATION_REMINDER)
                        finish()
                    },
                    onFooterButtonClick = {
                        if (viewModel.isFromSettings) {
                            UriUtil.visitInExternalBrowser(this, getString(R.string.donation_reminders_experiment_url).toUri())
                            val activeInterface = if (viewModel.isFromSettings) "global_setting" else "reminder_config"
                            DonorExperienceEvent.logDonationReminderAction(
                                activeInterface = activeInterface,
                                action = "reminder_about_click"
                            )
                        } else {
                            setResult(RESULT_OK_FROM_DONATION_REMINDER)
                            finish()
                        }
                    },
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        backClickListener = {
                            finish()
                        },
                        retryClickListener = {
                            viewModel.loadData()
                        }
                    )
                )
            }
        }
        sendAnalysis()
    }

    private fun sendAnalysis() {
        if (!viewModel.isFromSettings) {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_config",
                action = "impression"
            )
        }
    }

    companion object {
        const val RESULT_OK_FROM_DONATION_REMINDER = 100
        fun newIntent(context: Context, isFromSettings: Boolean = false): Intent {
            return Intent(context, DonationReminderActivity::class.java)
                .putExtra(EXTRA_FROM_SETTINGS, isFromSettings)
        }
    }
}
