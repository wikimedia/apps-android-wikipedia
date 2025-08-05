package org.wikipedia.donate.donationreminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
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
                        onBackPressed()
                    },
                    onConfirmBtnClick = { message ->
                        // @TODO: for showing snackbar on pageFragment
                    },
                    onAboutThisExperimentClick = {
                        UriUtil.visitInExternalBrowser(this, getString(R.string.donation_reminders_experiment_url).toUri())
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
    }

    companion object {
        fun newIntent(contex: Context, isFromSettings: Boolean = false): Intent {
            return Intent(contex, DonationReminderActivity::class.java)
                .putExtra(EXTRA_FROM_SETTINGS, isFromSettings)
        }
    }
}
