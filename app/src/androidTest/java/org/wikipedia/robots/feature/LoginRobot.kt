package org.wikipedia.robots.feature

import BaseRobot
import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.auth.AccountUtil
import org.wikipedia.base.TestConfig

class LoginRobot : BaseRobot() {

    fun loginState(
        loggedIn: () -> Unit,
        loggedOut: () -> Unit
    ) = apply {
        if (AccountUtil.isLoggedIn) loggedIn.invoke()
        else loggedOut.invoke()
    }

    fun logInUser() = apply {
        clickLoginButton()
        setLoginUserNameFromBuildConfig()
        setPasswordFromBuildConfig()
        loginUser()
    }

    fun logOutUser(context: Context) = apply {
        click.onViewWithId(R.id.main_drawer_settings_container)
        SettingsRobot()
            .clickLogOut(context)
        click.onViewWithText("Log out")
    }

    private fun clickLoginButton() = apply {
        click.onDisplayedViewWithText(viewId = R.id.create_account_login_button, text = "Log in")
        delay(TestConfig.DELAY_SHORT)
    }

    private fun setLoginUserNameFromBuildConfig() = apply {
        val username = getArgument("username") ?: BuildConfig.TEST_LOGIN_USERNAME
        onView(
            allOf(
                TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(
                    Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))
            )
        )
            .perform(replaceText(username), closeSoftKeyboard())
    }

    private fun setPasswordFromBuildConfig() = apply {
        val password = getArgument("password") ?: BuildConfig.TEST_LOGIN_PASSWORD
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))))
            .perform(replaceText(password), closeSoftKeyboard())
    }

    private fun loginUser() = apply {
        scroll.toViewAndClick(R.id.login_button)
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifyLoginFailed() = apply {
        verify.viewExists(com.google.android.material.R.id.snackbar_text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    private fun getArgument(key: String): String? {
        return InstrumentationRegistry.getArguments().getString(key)
    }
}
