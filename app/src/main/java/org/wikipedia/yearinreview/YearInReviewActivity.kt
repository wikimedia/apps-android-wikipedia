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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
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
                var isSurveyVisible by remember { mutableStateOf(false) }

                BackHandler {
                    if (viewModel.canShowSurvey) {
                        isSurveyVisible = true
                    } else {
                        finish()
                    }
                }

                if (isSurveyVisible) {
                    Prefs.yirSurveyShown = true
                    YearInReviewSurvey(
                        onCancelButtonClick = {
                            isSurveyVisible = false
                            finish()
                        },
                        onSubmitButtonClick = { selectedOption, userInput ->
                            PatrollerExperienceEvent.logAction(
                                action = "yir_survey_submit",
                                activeInterface = "yir_survey_form",
                                actionData = PatrollerExperienceEvent
                                    .getActionDataString(
                                        feedbackOption = selectedOption,
                                        feedbackText = userInput
                                    )
                            )
                            isSurveyVisible = false
                            finish()
                        }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = YearInReviewNavigation.ScreenDeck.name,
                    enterTransition = { EnterTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    composable(route = YearInReviewNavigation.ScreenDeck.name) {
                        val screenState = viewModel.uiScreenListState.collectAsState().value
                        YearInReviewScreenDeck(
                            state = screenState,
                            onBackButtonClick = { pagerState ->
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onNextButtonClick = { pagerState ->
                                viewModel.canShowSurvey = true
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
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
