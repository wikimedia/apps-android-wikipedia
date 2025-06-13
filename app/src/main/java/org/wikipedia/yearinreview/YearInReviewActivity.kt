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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class YearInReviewActivity : BaseActivity() {

    private val viewModel: YearInReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                /*
                personalizedScreenList is temporarily populated with screens
                for testing purposes. This is will adjusted in future iterations
                 */
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()
                val canShowSurvey = viewModel.uiCanShowSurvey.collectAsState().value
                var isSurveyVisible by remember { mutableStateOf(false) }

                BackHandler {
                    if (canShowSurvey) {
                        isSurveyVisible = true
                    } else {
                        endYearInReviewActivity(coroutineScope, this)
                    }
                }

                if (isSurveyVisible) {
                    Prefs.yirSurveyShown = true
                    YearInReviewSurvey(
                        onCancelButtonClick = {
                            isSurveyVisible = false
                            endYearInReviewActivity(coroutineScope, this)
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
                            endYearInReviewActivity(coroutineScope, this)
                        }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = YearInReviewNavigation.Onboarding.name,
                    enterTransition = { EnterTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    composable(route = YearInReviewNavigation.Onboarding.name) {
                        YearInReviewScreen(
                            viewModel = viewModel,
                            contentData = listOf(YearInReviewViewModel.getStartedData),
                            navController = navController,
                            showSurvey = { showSurvey -> isSurveyVisible = showSurvey },
                            customBottomBar = {
                                OnboardingBottomBar(
                                    onGetStartedClick = {
                                        navController.navigate(
                                            route = YearInReviewNavigation.ScreenDeck.name
                                        )
                                    },
                                    context = this@YearInReviewActivity
                                )
                            },
                            screenContent = { innerPadding, contentData, _ ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    screenData = contentData,
                                    context = this@YearInReviewActivity,
                                    screenCaptureMode = false,
                                    isOnboardingScreen = true
                                )
                            },
                        )
                    }
                    composable(route = YearInReviewNavigation.ScreenDeck.name) {
                        val screenState = viewModel.uiScreenListState.collectAsState().value
                        when (screenState) {
                            is Resource.Loading -> {
                                LoadingIndicator()
                            }
                            is Resource.Success -> {
                                YearInReviewScreen(
                                    viewModel = viewModel,
                                    contentData = screenState.data,
                                    navController = navController,
                                    customBottomBar = { pagerState -> MainBottomBar(
                                        onNavigationRightClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        pagerState = pagerState,
                                        totalPages = screenState.data.size,
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
                                    ) },
                                    screenContent = { innerPadding, contentData, pagerState ->
                                        if (pagerState.currentPage >= 1 && !canShowSurvey) {
                                            viewModel.updateUiShowSurvey()
                                        }
                                        YearInReviewScreenContent(
                                            innerPadding = innerPadding,
                                            context = this@YearInReviewActivity,
                                            screenData = contentData,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewActivity::class.java)
        }

        fun endYearInReviewActivity(scope: CoroutineScope, activity: YearInReviewActivity) {
            scope.launch {
                delay(200)
                activity.finish()
            }
        }
    }
}
