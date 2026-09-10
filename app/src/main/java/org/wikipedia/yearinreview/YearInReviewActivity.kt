package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.donate.DonateDialog
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel2 by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                YearInReviewScreen(
                    uiState = uiState,
                    onCloseClick = {
                        finish()
                    },
                    onLearnMoreClick = {
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_reading_list_learn_more).toUri())
                    },
                    onShareFeedbackClick = {
                        FeedbackUtil.composeEmail(
                            context = this,
                            subject = getString(R.string.year_in_review_feedback_email_subject)
                        )
                    },
                    onShareClick = {
                        // @TODO: Implement share functionality
                    },
                    onDonateClick = {
                        ExclusiveBottomSheetPresenter.show(
                            supportFragmentManager,
                            DonateDialog.newInstance(
                                campaignId = YearInReviewViewModel.currentCampaignId,
                                fromYiR = true
                            )
                        )
                    },
                    onRetryClick = {
                        viewModel.loadYearInReview()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewActivity::class.java)
        }
    }
}
