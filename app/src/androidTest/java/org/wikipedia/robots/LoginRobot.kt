package org.wikipedia.robots

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

    fun clickLoginButton() = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.create_account_login_button, text = "Log in")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun setLoginUserNameFromBuildConfig() = apply {
        onView(
            allOf(
                TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(
                    Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))
            )
        )
            .perform(replaceText(BuildConfig.TEST_LOGIN_USERNAME), closeSoftKeyboard())
    }

    fun setPasswordFromBuildConfig() = apply {
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))))
            .perform(replaceText(BuildConfig.TEST_LOGIN_PASSWORD), closeSoftKeyboard())
    }

    fun loginUser() = apply {
        scrollToViewAndClick(R.id.login_button)
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifyLoginFailed() = apply {
        checkViewExists(com.google.android.material.R.id.snackbar_text)
        delay(TestConfig.DELAY_SHORT)
    }
}
