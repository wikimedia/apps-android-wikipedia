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
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikimedia.testkitchen.context.PageData
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.menu.PageOverflowMenuItem
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.donate.DonateDialog
import org.wikipedia.extensions.instrument
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.games.GamesHubActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.SaveArticleSheetDialog
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewEndScreenABTest
import kotlin.collections.plus

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel by viewModels()
    private val pageOverflowMenuViewModel: PageOverflowMenuViewModel by viewModels()

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
                val isSoundOn by viewModel.isSoundOn.collectAsStateWithLifecycle()

                // The music should only play when the slides are active, not on the loading or error screen
                if (uiState is YearInReviewUiState.Content) {
                    LifecycleStartEffect(viewModel) {
                        viewModel.updateAudio()
                        onStopOrDispose { viewModel.pauseAudio() }
                    }
                }

                YearInReviewScreen(
                    uiState = uiState,
                    tabArticles = viewModel.tabArticles,
                    tabArticleOverflowMenuState = pageOverflowMenuViewModel.pageOverflowMenuState,
                    isSoundOn = isSoundOn,
                    onCloseClick = { page ->
                        if (page != null) {
                            submitInteraction(uiState, action = "click", elementId = "close_button", actionContext = mapOf("page" to page.id))
                        }
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
                    onSoundToggleClick = {
                        viewModel.toggleSound()
                    },
                    onShareClick = { page, bitmap ->
                        submitInteraction(uiState, action = "click", elementId = "share_button", actionContext = mapOf("page" to page.id))
                        shareImage(bitmap)
                    },
                    onDonateClick = { page ->
                        val campaignId = "appmenu_yir_${page.id}"
                        YearInReviewViewModel.currentCampaignId = campaignId
                        submitInteraction(uiState, action = "click", elementId = "donate_button", actionContext = mapOf(
                            "page" to page.id,
                            "campaign_id" to campaignId
                        ))
                        ExclusiveBottomSheetPresenter.show(supportFragmentManager, DonateDialog.newInstance(campaignId = campaignId, fromYiR = true))
                    },
                    onEndCardClick = { card ->
                        when (card) {
                            YearInReviewEndCard.GAMES -> {
                                submitInteraction(uiState, action = "click", elementId = "play_game_button")
                                startActivity(GamesHubActivity.newIntent(this, Constants.InvokeSource.YEAR_IN_REVIEW))
                            }
                            YearInReviewEndCard.CUSTOMIZE_HOME -> {
                                submitInteraction(uiState, action = "click", elementId = "customize_interests_button")
                                startActivity(PersonalizationActivity.newIntent(this, showInterestsOnly = true))
                            }
                            YearInReviewEndCard.STORE_DISCOUNT -> {
                                submitInteraction(uiState, action = "click", elementId = "visit_store_button")
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_store_discount_url).toUri())
                            }
                            YearInReviewEndCard.TOP_READ -> {
                                submitInteraction(uiState, action = "click", elementId = "explore_top_read_button")
                                UriUtil.visitInExternalBrowser(context = this, uri = getString(R.string.year_in_review_top_read_url).toUri())
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
                    onTabArticleClick = { title ->
                        submitInteraction(uiState, action = "click", elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = title))
                        openInExistingTab(HistoryEntry(title, HistoryEntry.SOURCE_YEAR_IN_REVIEW))
                    },
                    onTabArticleOverflowClick = { title, menuKey ->
                        pageOverflowMenuViewModel.onPageOverflowClick(HistoryEntry(title, HistoryEntry.SOURCE_YEAR_IN_REVIEW), menuKey)
                    },
                    onTabArticleOverflowDismiss = {
                        pageOverflowMenuViewModel.dismissPageOverflowMenu()
                    },
                    onTabArticleOverflowItemClick = { item, entry ->
                        val elementId = when (item) {
                            PageOverflowMenuItem.OPEN_PAGE -> "article_open"
                            PageOverflowMenuItem.OPEN_IN_NEW_TAB -> "article_open_new_tab"
                            PageOverflowMenuItem.SAVE -> "article_save"
                            PageOverflowMenuItem.SHARE -> "article_share"
                            PageOverflowMenuItem.COPY_LINK -> "article_copy_link"
                        }
                        submitInteraction(uiState, action = "click", actionSubtype = "tab_article_overflow", elementId = elementId, pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                        when (item) {
                            PageOverflowMenuItem.OPEN_PAGE -> openInExistingTab(entry)
                            PageOverflowMenuItem.OPEN_IN_NEW_TAB -> startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                            PageOverflowMenuItem.SAVE -> SaveArticleSheetDialog.show(this, entry.title)
                            PageOverflowMenuItem.SHARE -> ShareUtil.shareText(this, entry.title.displayText, entry.title.uri)
                            PageOverflowMenuItem.COPY_LINK -> {
                                ClipboardUtil.setPlainText(this, text = entry.title.uri)
                                FeedbackUtil.showMessage(this, R.string.address_copied)
                            }
                        }
                    },
                    onStatusBarIconColorChange = { useDarkIcons ->
                        WindowCompat.getInsetsController(window, window.decorView)
                            .isAppearanceLightStatusBars = useDarkIcons
                    },
                    onPageSettled = viewModel::onPageSettled,
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

    private fun openInExistingTab(entry: HistoryEntry) {
        startActivity(PageActivity.newIntentForExistingTab(this, entry, entry.title))
    }

    private fun shareImage(bitmap: Bitmap) {
        val shareUrl = getString(R.string.year_in_review_share_url) + YearInReviewViewModel.YIR_TAG
        val shareText = String.format(
            getString(R.string.yir_share_text),
            YearInReviewConfig.YEAR,
            shareUrl
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

    private fun submitInteraction(uiState: YearInReviewUiState, action: String, actionSubtype: String? = null, elementId: String? = null, pageData: PageData? = null, actionContext: Map<String, Any> = emptyMap()) {
        if (uiState !is YearInReviewUiState.Content) {
            return
        }
        instrument?.submitInteraction(
            action = action,
            actionSubtype = actionSubtype,
            elementId = elementId,
            pageData = pageData,
            actionContext = actionContext.plus("personalized" to uiState.isPersonalized)
        )
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewActivity::class.java)
        }
    }
}
