package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.donate.DonateDialog
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.settings.Prefs

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()

                BackHandler {
                    finish()
                }

                NavHost(
                    navController = navController,
                    startDestination = YearInReviewViewModel.YIR_TAG,
                    enterTransition = { EnterTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    composable(route = YearInReviewViewModel.YIR_TAG) {
                        val screenState = viewModel.uiScreenListState.collectAsState().value
                        YearInReviewScreenDeck(
                            state = screenState,
                            requestScreenshotBitmap = { width, height -> viewModel.requestScreenshotHeaderBitmap(width, height) },
                            onCloseButtonClick = {
                                if ((YearInReviewViewModel.getYearInReviewModel()?.slideViewedCount ?: 0) >= YearInReviewViewModel.MIN_SLIDES_BEFORE_SURVEY && Prefs.yearInReviewSurveyState == YearInReviewSurveyState.NOT_TRIGGERED) {
                                    Prefs.yearInReviewSurveyState = YearInReviewSurveyState.SHOULD_SHOW
                                }
                                finish()
                            },
                            onNextButtonClick = { pagerState, currentSlideData ->
                                YearInReviewViewModel.updateYearInReviewModel { it.copy(slideViewedCount = it.slideViewedCount + 1) }

                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                if (currentSlideData is YearInReviewScreenData.HighlightsScreen) {
                                    if (Prefs.yearInReviewSurveyState == YearInReviewSurveyState.NOT_TRIGGERED) {
                                        Prefs.yearInReviewSurveyState = YearInReviewSurveyState.SHOULD_SHOW
                                    }
                                    finish()
                                }
                            },
                            onDonateClick = { currentSlide ->
                                EventPlatformClient.submit(
                                    BreadCrumbLogEvent(
                                        screen_name = "year_in_review",
                                        action = "donate_click")
                                )
                                val campaignId = "appmenu_yir_$currentSlide"
                                YearInReviewEvent.submit(
                                    action = "donate_start_click_yir",
                                    slide = currentSlide,
                                    campaignId = campaignId
                                )
                                ExclusiveBottomSheetPresenter.show(supportFragmentManager, DonateDialog.newInstance(campaignId = campaignId, fromYiR = true))
                            },
                            onRetryClick = {
                                viewModel.fetchPersonalizedData()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewActivity::class.java)
        }
    }
}
