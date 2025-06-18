package org.wikipedia.login

import android.widget.Toast
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
            val loginResult = getLoginResponse(wiki, userName, password, retypedPassword = retypedPassword,
                twoFactorCode = twoFactorCode, emailAuthCode = emailAuthCode, captchaId = captchaId, captchaWord = captchaWord,
                loginToken = loginToken, isContinuation = isContinuation).toLoginResult(wiki, password)
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
                // Make a call to authmanager to see if we need to provide a captcha.
                val captchaId = ServiceFactory.get(wiki).getAuthManagerForLogin().query?.captchaId()
                if (!captchaId.isNullOrEmpty()) {
                    cb.uiPrompt(loginResult, LoginFailedException(loginResult.message), captchaId = captchaId, token = loginToken)
                } else if (LoginResult.STATUS_UI == loginResult.status) {
                    val parsedMessage = loginResult.message?.let { ServiceFactory.get(wiki).parseText(it) }?.text ?: loginResult.message
                    when (loginResult) {
                        is LoginOAuthResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), token = loginToken)
                        is LoginEmailAuthResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), token = loginToken)
                        is LoginResetPasswordResult -> cb.passwordResetPrompt(loginToken)
                        else -> cb.error(LoginFailedException(parsedMessage))
                    }
                } else {
                    cb.error(LoginFailedException(loginResult.message))
                }
            }
        }
    }

    suspend fun loginBlocking(wiki: WikiSite, userName: String, password: String, twoFactorCode: String? = null,
            emailAuthCode: String? = null, captchaId: String? = null, captchaWord: String? = null): LoginResponse {
        val loginToken = getLoginToken(wiki)
        val loginResponse = getLoginResponse(wiki, userName, password, twoFactorCode = twoFactorCode, retypedPassword = null,
            emailAuthCode = emailAuthCode, loginToken = loginToken, captchaId = captchaId, captchaWord = captchaWord)
        val loginResult = loginResponse.toLoginResult(wiki, password) ?: throw IOException("Unexpected response when logging in.")
        if (loginResult.pass() && !loginResult.userName.isNullOrEmpty()) {
            return loginResponse
        }
        // Make a call to authmanager to see if we need to provide a captcha.
        val captchaId = ServiceFactory.get(wiki).getAuthManagerForLogin().query?.captchaId()
        if (!captchaId.isNullOrEmpty()) {
            // TODO: Find a better way to boil up the warning about Captcha
            Toast.makeText(WikipediaApp.instance, R.string.login_background_error_msg, Toast.LENGTH_LONG).show()
        } else if (LoginResult.STATUS_UI == loginResult.status) {
            if (loginResult is LoginOAuthResult) {
                // TODO: Find a better way to boil up the warning about 2FA
                Toast.makeText(WikipediaApp.instance,
                    R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show()
            } else if (loginResult is LoginEmailAuthResult) {
                // TODO: Find a better way to boil up the warning about Email auth
                Toast.makeText(WikipediaApp.instance,
                    R.string.login_email_auth_other_workflow_error_msg, Toast.LENGTH_LONG).show()
            }
        }
        throw LoginFailedException(loginResult.message)
    }

    private suspend fun getLoginToken(wiki: WikiSite): String {
        val response = ServiceFactory.get(wiki).getLoginToken()
        return response.query?.loginToken() ?: throw RuntimeException("Received empty login token.")
    }

    private suspend fun getLoginResponse(wiki: WikiSite, userName: String, password: String, retypedPassword: String?,
        twoFactorCode: String?, emailAuthCode: String?, loginToken: String?, captchaId: String?, captchaWord: String?, isContinuation: Boolean? = false): LoginResponse {
        return if (!twoFactorCode.isNullOrEmpty() || !emailAuthCode.isNullOrEmpty() || !captchaId.isNullOrEmpty() || !retypedPassword.isNullOrEmpty())
            ServiceFactory.get(wiki).postLogIn(user = userName, pass = password, retype = retypedPassword,
                twoFactorCode = twoFactorCode, emailAuthToken = emailAuthCode,
                captchaId = captchaId, captchaWord = captchaWord, loginToken = loginToken,
                loginContinue = if (isContinuation == true) true else null,
                returnUrl = if (isContinuation == true) null else Service.WIKIPEDIA_URL)
        else
            ServiceFactory.get(wiki).postLogIn(user = userName, pass = password, retype = retypedPassword, loginToken = loginToken, returnUrl = Service.WIKIPEDIA_URL)
    }

    companion object {
        var enqueueForceEmailAuth = false
    }
}
