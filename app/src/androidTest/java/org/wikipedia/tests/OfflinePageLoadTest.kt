package org.wikipedia.tests

import org.junit.Test
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot

class OfflinePageLoadTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java
) {
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // @TODO: add test steps for new jetpack compose feed to test offline page load
        // 1. open any articles from explore feed and navigate back
        // 2. turn off internet
        // 3. open any articles and check if its loading offline
    }
}
