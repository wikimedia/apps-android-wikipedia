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
                    navController = navController, startDestination = YearInReviewNavigation.GetStarted.name
                ) {
                    composable(route = YearInReviewNavigation.GetStarted.name) {
                        YearInReviewScreenScaffold(
                            customBottomBar = { GetStartedBottomBar(onGetStartedClick = { navController.navigate(route = YearInReviewNavigation.ScreenDeck.name) }) },
                            screenContent = { innerPadding, scrollState, contentData ->

                                YearInReviewScreenContent(
                                    innerPadding = innerPadding,
                                    scrollState = scrollState,
                                    screenData = contentData)
                            },
                            totalPages = getStartedList.size,
                            pagerState = pagerState,
                            contentData = getStartedList,
                            scrollState = scrollState
                        )
                    }
                    composable(route = YearInReviewNavigation.ScreenDeck.name) {
                        YearInReviewScreenScaffold(
                            customBottomBar = { MainBottomBar (onNavigationRightClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(pagerState.currentPage + 1) } }) },
                            screenContent = { innerPadding, scrollState, contentData ->

                                    YearInReviewScreenContent(
                                        innerPadding = innerPadding,
                                        scrollState = scrollState,
                                        screenData = contentData
                                    )
                            },
                            totalPages = personalizedScreenList.size,
                            pagerState = pagerState,
                            contentData = personalizedScreenList,
                            scrollState = scrollState
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
