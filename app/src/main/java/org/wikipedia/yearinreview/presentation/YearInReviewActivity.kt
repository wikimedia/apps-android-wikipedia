package org.wikipedia.yearinreview.presentation

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
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.donate.DonateDialog
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.games.GamesHubActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.data.YearInReviewEndScreenABTest

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-year-in-review")
            .startFunnel("year-in-review")
            .setExperiment(TestKitchenAdapter.getExperiment(YearInReviewEndScreenABTest()))

        initializeYearInReviewRive(this)

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
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_url).toUri())
                    },
                    onAboutInsightsClick = {
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_faq_url).toUri())
                    },
                    onShareFeedbackClick = {
                        FeedbackUtil.composeEmail(
                            context = this,
                            subject = getString(R.string.year_in_review_feedback_email_subject)
                        )
                    },
                    onShareClick = { bitmap ->
                        val shareUrl = getString(R.string.year_in_review_share_url) + YearInReviewViewModel.YIR_TAG
                        val shareText = String.format(
                            getString(R.string.year_in_review_share_body),
                            shareUrl,
                            getString(R.string.year_in_review_hashtag)
                        )
                        ShareUtil.shareImage(
                            coroutineScope = lifecycleScope,
                            context = this,
                            bmp = bitmap,
                            imageFileName = YearInReviewViewModel.YIR_TAG,
                            subject = getString(R.string.year_in_review_share_subject),
                            text = shareText
                        )
                    },
                    onDonateClick = { currentSlide ->
                        EventPlatformClient.submit(
                            BreadCrumbLogEvent(
                                screen_name = "year_in_review",
                                action = "donate_click"
                            )
                        )
                        val campaignId = "appmenu_yir_$currentSlide"
                        YearInReviewViewModel.currentCampaignId = campaignId
                        YearInReviewEvent.submit(
                            action = "donate_start_click_yir",
                            slide = currentSlide,
                            campaignId = campaignId
                        )
                        ExclusiveBottomSheetPresenter.show(supportFragmentManager, DonateDialog.newInstance(campaignId = campaignId, fromYiR = true))
                    },
                    onEndCardClick = { card ->
                        when (card) {
                            YearInReviewEndCard.GAMES -> startActivity(GamesHubActivity.newIntent(this))
                            YearInReviewEndCard.CUSTOMIZE_HOME -> startActivity(PersonalizationActivity.newIntent(this, showInterestsOnly = true))
                            YearInReviewEndCard.STORE_DISCOUNT -> {
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_store_discount_url).toUri())
                            }
                            YearInReviewEndCard.QUIZ -> {
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_quiz_url, WikipediaApp.instance.appOrSystemLanguageCode).toUri())
                            }
                        }
                    },
                    onStatusBarIconColorChange = { useDarkIcons ->
                        WindowCompat.getInsetsController(window, window.decorView)
                            .isAppearanceLightStatusBars = useDarkIcons
                    },
                    onPageImpression = { page ->
                        if (page is YearInReviewPage.End) {
                            YearInReviewEndScreenABTest().maybeSendExposureEvent()
                        }
                    },
                    onRetryClick = {
                        viewModel.loadYearInReview()
                    },
                    onRiveError = { throwable ->
                        FeedbackUtil.showError(this, throwable)
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
