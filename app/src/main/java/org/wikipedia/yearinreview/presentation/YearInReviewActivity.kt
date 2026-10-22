package org.wikipedia.yearinreview.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.donate.DonateDialog
import org.wikipedia.extensions.instrument
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.games.GamesHubActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.data.YearInReviewEndScreenABTest
import kotlin.collections.plus

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _instrument = TestKitchenAdapter.client.getInstrument("apps-year-in-review")
            .setDefaultActionSource("main")
            .startFunnel("year-in-review")
            .setExperiment(TestKitchenAdapter.getExperiment(YearInReviewEndScreenABTest()))

        initializeYearInReviewRive(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collectLatest {
                    if (it is YearInReviewUiState.Error) {
                        instrument?.submitInteraction(action = "error", actionContext = mapOf("message" to it.error.message.orEmpty()))
                    }
                }
            }
        }

        setContent {
            BaseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                YearInReviewScreen(
                    uiState = uiState,
                    onCloseClick = {
                        submitInteraction(uiState, action = "click", elementId = "close_button")
                        finish()
                    },
                    onLearnMoreClick = {
                        submitInteraction(uiState, action = "click", elementId = "learn_more_button")
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_url).toUri())
                    },
                    onAboutInsightsClick = {
                        submitInteraction(uiState, action = "click", elementId = "about_insights_button")
                        UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_media_wiki_faq_url).toUri())
                    },
                    onShareFeedbackClick = {
                        submitInteraction(uiState, action = "click", elementId = "share_feedback_button")
                        FeedbackUtil.composeEmail(
                            context = this,
                            subject = getString(R.string.year_in_review_feedback_email_subject)
                        )
                    },
                    onShareClick = { bitmap ->
                        submitInteraction(uiState, action = "click", elementId = "share_button")
                        shareImage(bitmap)
                    },
                    onDonateClick = { currentSlide ->
                        val campaignId = "appmenu_yir_$currentSlide"
                        YearInReviewViewModel.currentCampaignId = campaignId
                        submitInteraction(uiState, action = "click", elementId = "donate_button", actionContext = mapOf(
                            "slide" to currentSlide,
                            "campaign_id" to campaignId
                        ))
                        ExclusiveBottomSheetPresenter.show(supportFragmentManager, DonateDialog.newInstance(campaignId = campaignId, fromYiR = true))
                    },
                    onEndCardClick = { card ->
                        when (card) {
                            YearInReviewEndCard.GAMES -> {
                                submitInteraction(uiState, action = "click", elementId = "play_game_button")
                                startActivity(GamesHubActivity.newIntent(this))
                            }
                            YearInReviewEndCard.CUSTOMIZE_HOME -> {
                                submitInteraction(uiState, action = "click", elementId = "customize_interests_button")
                                startActivity(PersonalizationActivity.newIntent(this, showInterestsOnly = true))
                            }
                            YearInReviewEndCard.STORE_DISCOUNT -> {
                                submitInteraction(uiState, action = "click", elementId = "visit_store_button")
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_store_discount_url).toUri())
                            }
                            YearInReviewEndCard.QUIZ -> {
                                submitInteraction(uiState, action = "click", elementId = "take_quiz_button")
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_quiz_url, WikipediaApp.instance.appOrSystemLanguageCode).toUri())
                            }
                        }
                    },
                    onShareHighlightsClick = { bitmap ->
                        submitInteraction(uiState, action = "click", elementId = "share_highlights_button")
                        shareImage(bitmap)
                    },
                    onStatusBarIconColorChange = { useDarkIcons ->
                        WindowCompat.getInsetsController(window, window.decorView)
                            .isAppearanceLightStatusBars = useDarkIcons
                    },
                    onPageImpression = { page ->
                        submitInteraction(uiState, action = "impression", actionContext = mapOf("page" to page.id))
                        if (page is YearInReviewPage.End && YearInReviewEndScreenABTest().isUserEligible()) {
                            YearInReviewEndScreenABTest().maybeSendExposureEvent()
                        }
                    },
                    onRetryClick = {
                        viewModel.loadYearInReview()
                    },
                    onRiveError = { throwable ->
                        submitInteraction(uiState, action = "error", elementId = "rive", actionContext = mapOf("message" to throwable.message.orEmpty()))
                        FeedbackUtil.showError(this, throwable)
                    }
                )
            }
        }
    }

    private fun shareImage(bitmap: Bitmap) {
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
    }

    private fun submitInteraction(uiState: YearInReviewUiState, action: String, elementId: String? = null, actionContext: Map<String, Any> = emptyMap()) {
        if (uiState !is YearInReviewUiState.Content) {
            return
        }
        instrument?.submitInteraction(action = action, elementId = elementId, actionContext = actionContext.plus("personalized" to uiState.isPersonalized))
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewActivity::class.java)
        }
    }
}
