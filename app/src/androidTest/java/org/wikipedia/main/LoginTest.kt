package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.LoginRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val homeScreenRobot = HomeScreenRobot()
    private val loginRobot = LoginRobot()

    @Test
    fun startLoginTest() {
        homeScreenRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
            .verifyLoginFailed()
    }
}
