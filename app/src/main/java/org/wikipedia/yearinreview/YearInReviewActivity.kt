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
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.compose.theme.BaseTheme
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
                                if (Prefs.yearInReviewSlideViewedCount >= YearInReviewViewModel.MIN_SLIDES_BEFORE_SURVEY && Prefs.yearInReviewSurveyState == YearInReviewSurveyState.NOT_TRIGGERED) {
                                    Prefs.yearInReviewSurveyState = YearInReviewSurveyState.SHOULD_SHOW
                                }
                                finish()
                            },
                            onNextButtonClick = { pagerState, currentSlideData ->
                                Prefs.yearInReviewSlideViewedCount += 1
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
                            onDonateClick = {
                                EventPlatformClient.submit(
                                    BreadCrumbLogEvent(
                                        screen_name = "year_in_review",
                                        action = "donate_click")
                                )
                                DonorExperienceEvent.logAction(
                                    action = "donate_start_click_yir",
                                    activeInterface = "wiki_yir",
                                    campaignId = "yir"
                                )
                                launchDonateDialog("yir")
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
