package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
                val personalizedScreenList = listOf(readCountData, editCountData)
                val getStartedList = listOf(getStartedData)
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()
                val scrollState = rememberScrollState()
                val pagerState = rememberPagerState(pageCount = { personalizedScreenList.size })

                NavHost(
                    navController = navController, startDestination = YearInReviewNavigation.Onboarding.name
                ) {
                    composable(route = YearInReviewNavigation.Onboarding.name) {
                        YearInReviewScreenScaffold(
                            scrollState = scrollState,
                            pagerState = pagerState,
                            totalPages = getStartedList.size,
                            contentData = getStartedList,
                            navController = navController,
                            customBottomBar = {
                                GetStartedBottomBar(
                                    onGetStartedClick = {
                                        navController.navigate(
                                            route = YearInReviewNavigation.ScreenDeck.name
                                        )
                                    }
                                )
                            },
                            screenContent = { innerPadding, scrollState, contentData ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    scrollState = scrollState,
                                    screenData = contentData)
                            },
                        )
                    }
                    composable(route = YearInReviewNavigation.ScreenDeck.name) {
                        YearInReviewScreenScaffold(
                            scrollState = scrollState,
                            pagerState = pagerState,
                            totalPages = personalizedScreenList.size,
                            contentData = personalizedScreenList,
                            navController = navController,
                            customBottomBar = { MainBottomBar(
                                onNavigationRightClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                pagerState = pagerState,
                                totalPages = personalizedScreenList.size) },
                            screenContent = { innerPadding, scrollState, contentData ->
                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    scrollState = scrollState,
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
