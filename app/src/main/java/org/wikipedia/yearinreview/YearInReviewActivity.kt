package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.wikipedia.compose.theme.BaseTheme

class YearInReviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                val personalizedScreenList = listOf(readCountData, editCountData, readCountData, editCountData, readCountData)
                val getStartedList = listOf(getStartedData)
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
                            totalPages = getStartedList.size,
                            contentData = getStartedList,
                            navController = navController,
                            customBottomBar = {
                                OnboardingBottomBar(
                                    onGetStartedClick = {
                                        navController.navigate(
                                            route = YearInReviewNavigation.ScreenDeck.name
                                        )
                                    }
                                )
                            },
                            screenContent = { innerPadding, contentData ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    screenData = contentData)
                            },
                        )
                    }
                    composable(route = YearInReviewNavigation.ScreenDeck.name) {
                        YearInReviewScreen(
                            totalPages = personalizedScreenList.size,
                            contentData = personalizedScreenList,
                            navController = navController,
                            customBottomBar = { pagerState -> MainBottomBar(
                                onNavigationRightClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                pagerState = pagerState,
                                totalPages = personalizedScreenList.size) },
                            screenContent = { innerPadding, contentData ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    screenData = contentData
                                )
                            },
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
