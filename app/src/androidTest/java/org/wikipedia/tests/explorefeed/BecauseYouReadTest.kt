package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class BecauseYouReadTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        // sometimes notification dialog may appear
        systemRobot
            .clickOnSystemDialogWithText("Allow")

        // @TODO: update the steps for new explore feed
        // 1. Click on the featured article and stay on the article for 30 seconds
        // 2. press back and go to for you content tab, scroll until you find because you read
    }
}
