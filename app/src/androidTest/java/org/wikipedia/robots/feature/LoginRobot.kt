package org.wikipedia.robots.feature

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class LoginRobot : BaseRobot() {

    fun logInUser() = apply {
        try {
            clickLoginButton()
            setLoginUserNameFromBuildConfig()
            setPasswordFromBuildConfig()
            loginUser()
        } catch (e: Exception) {
            Log.e("LoginRobotError:", "User already logged in.")
        }
    }

    private fun clickLoginButton() = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.create_account_login_button, text = "Log in")
        delay(TestConfig.DELAY_MEDIUM)
    }

    private fun setLoginUserNameFromBuildConfig() = apply {
        onView(
            allOf(
                TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(
                    Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))
            )
        )
            .perform(replaceText(BuildConfig.TEST_LOGIN_USERNAME), closeSoftKeyboard())
    }

    private fun setPasswordFromBuildConfig() = apply {
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))))
            .perform(replaceText(BuildConfig.TEST_LOGIN_PASSWORD), closeSoftKeyboard())
    }

    private fun loginUser() = apply {
        scrollToViewAndClick(R.id.login_button)
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifyLoginFailed() = apply {
        checkViewExists(com.google.android.material.R.id.snackbar_text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
