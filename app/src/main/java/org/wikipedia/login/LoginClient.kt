package org.wikipedia.login

import android.widget.Toast
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.settings.Prefs
import java.io.IOException
import java.time.LocalDateTime

class LoginClient {

    suspend fun loginBlocking(wiki: WikiSite, userName: String, password: String, twoFactorCode: String? = null,
            emailAuthCode: String? = null, captchaId: String? = null, captchaWord: String? = null): LoginResult {

        // Prevent the app from re-logging in more than once per 1-day period.
        // TODO: investigate the root cause of why this happens.
        // https://phabricator.wikimedia.org/T415675
        if (!Prefs.lastBackgroundLoginDateTime.isNullOrEmpty()) {
            val loginDate = LocalDateTime.parse(Prefs.lastBackgroundLoginDateTime)
            if (loginDate.plusDays(1).isAfter(LocalDateTime.now())) {
                return LoginResult(wiki, LoginResult.STATUS_FAIL, userName, password, "Background login limit reached.")
            }
        }
        Prefs.lastBackgroundLoginDateTime = LocalDateTime.now().toString()

        val loginToken = getLoginToken(wiki)
        val isContinuation = false
        val loginResponse = ServiceFactory.get(wiki).postLogIn(user = userName, pass = password,
            twoFactorCode = twoFactorCode, emailAuthToken = emailAuthCode,
            captchaId = captchaId, captchaWord = captchaWord, loginToken = loginToken,
            loginContinue = if (isContinuation) true else null,
            returnUrl = if (isContinuation) null else Service.WIKIPEDIA_URL)
        val loginResult = loginResponse.toLoginResult(wiki, password) ?: throw IOException("Unexpected response when logging in.")
        if (loginResult.pass() && !loginResult.userName.isNullOrEmpty()) {
            return loginResult
        }
        // Make a call to authmanager to see if we need to provide a captcha.
        val captchaId = ServiceFactory.get(wiki).getAuthManagerForLogin().query?.captchaId()
        if (!captchaId.isNullOrEmpty()) {
            // TODO: Find a better way to boil up the warning about Captcha
            showToast(R.string.login_background_error_msg)
        } else if (LoginResult.STATUS_UI == loginResult.status) {
            if (loginResult is LoginOATHResult || loginResult is LoginModuleSelectResult) {
                // TODO: Find a better way to boil up the warning about 2FA
                showToast(R.string.login_2fa_other_workflow_error_msg)
            } else if (loginResult is LoginEmailAuthResult) {
                // TODO: Find a better way to boil up the warning about Email auth
                showToast(R.string.login_email_auth_other_workflow_error_msg)
            }
        }
        return loginResult
    }

    private suspend fun getLoginToken(wiki: WikiSite): String {
        val response = ServiceFactory.get(wiki).getLoginToken()
        return response.query?.loginToken() ?: throw RuntimeException("Received empty login token.")
    }

    private fun showToast(@StringRes stringId: Int) {
        WikipediaApp.instance.mainThreadHandler.post {
            Toast.makeText(WikipediaApp.instance, stringId, Toast.LENGTH_LONG).show()
        }
    }
}
