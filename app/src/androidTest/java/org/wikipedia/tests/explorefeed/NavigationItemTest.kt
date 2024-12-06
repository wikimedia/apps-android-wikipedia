package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.navigation.BottomNavRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavigationItemTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val bottomNavRobot = BottomNavRobot()

    @Test
    fun runTest() {
        // Checking the navigation menu items
        bottomNavRobot
            .navigateToSavedPage()
            .navigateToSearchPage()
            .navigateToSuggestedEdits()
            .navigateToMoreMenu()
            .pressBack()
            .navigateToExploreFeed()
    }
}
