package org.wikipedia.login

import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.io.IOException
import java.time.LocalDateTime

class LoginClient {

    interface LoginCallback {
        fun success(result: LoginResult)
        fun uiPrompt(result: LoginResult, caught: Throwable, captchaId: String? = null, token: String? = null)
        fun passwordResetPrompt(token: String?)
        fun error(caught: Throwable)
    }

    fun login(coroutineScope: CoroutineScope, wiki: WikiSite, userName: String, password: String, retypedPassword: String? = null,
              token: String? = null, twoFactorCode: String? = null, emailAuthCode: String? = null,
              captchaId: String? = null, captchaWord: String? = null, isContinuation: Boolean? = false, cb: LoginCallback) {
        coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e("Login process failed. $throwable")
            cb.error(throwable)
        }) {
            if (Prefs.loginForceEmailAuth) {
                enqueueForceEmailAuth = true
            }
            val loginToken = token ?: getLoginToken(wiki)
            var loginResult = ServiceFactory.get(wiki).postLogIn(user = userName, pass = password, retype = retypedPassword,
                twoFactorCode = twoFactorCode, emailAuthToken = emailAuthCode,
                captchaId = captchaId, captchaWord = captchaWord, loginToken = loginToken,
                loginContinue = if (isContinuation == true) true else null,
                returnUrl = if (isContinuation == true) null else Service.WIKIPEDIA_URL).toLoginResult(wiki, password)
            for (attempt in 0..1) {
                if (loginResult == null) {
                    throw IOException("Login failed. Unexpected response.")
                }
                if (loginResult.pass() && userName.isNotEmpty()) {
                    ServiceFactory.get(wiki).getUserInfo().query?.userInfo?.let {
                        loginResult.userId = it.id
                        loginResult.groups = it.groups()
                        L.v("Found user ID " + it.id + " for " + wiki.subdomain())
                    }
                    cb.success(loginResult)
                } else {
                    if (LoginResult.STATUS_UI == loginResult.status) {
                        val parsedMessage = loginResult.message?.let { ServiceFactory.get(wiki).parseText(it) }?.text ?: loginResult.message
                        when {
                            loginResult is LoginOATHResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), token = loginToken)
                            loginResult is LoginEmailAuthResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), token = loginToken)
                            loginResult is LoginResetPasswordResult -> cb.passwordResetPrompt(loginToken)
                            loginResult is LoginModuleSelectResult && attempt == 0 -> {
                                // User has multi-factor authentication, and we're allowed to select the module to use.
                                // Make an attempt to select TOTP, since this is the only MFA we support for now.
                                loginResult = ServiceFactory.get(wiki).postLogIn(loginToken = loginToken, loginContinue = true,
                                    newModule = "totp").toLoginResult(wiki, password)
                                continue
                            }
                            else -> cb.error(LoginFailedException(parsedMessage))
                        }
                    } else {
                        // Make a call to authmanager to see if we need to provide a captcha.
                        val captchaId = ServiceFactory.get(wiki).getAuthManagerForLogin().query?.captchaId()
                        if (!captchaId.isNullOrEmpty()) {
                            cb.uiPrompt(loginResult, LoginFailedException(loginResult.message), captchaId = captchaId, token = loginToken)
                        } else {
                            cb.error(LoginFailedException(loginResult.message))
                        }
                    }
                }
                break
            }
        }
    }

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

    companion object {
        var enqueueForceEmailAuth = false
    }
}
