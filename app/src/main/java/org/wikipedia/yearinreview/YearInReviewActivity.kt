package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
                            contentData = listOf(YearInReviewViewModel.getStartedData),
                            navController = navController,
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
                            screenContent = { innerPadding, contentData ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    screenData = contentData,
                                    context = this@YearInReviewActivity,
                                    isShareSheetView = false
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
                                    screenContent = { innerPadding, contentData ->
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
    }
}
