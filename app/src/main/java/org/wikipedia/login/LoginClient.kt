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
        fun uiPrompt(result: LoginResult, caught: Throwable, token: String?)
        fun passwordResetPrompt(token: String?)
        fun error(caught: Throwable)
    }

    fun login(coroutineScope: CoroutineScope, wiki: WikiSite, userName: String, password: String,
              retypedPassword: String?, twoFactorCode: String?, emailAuthCode: String?, token: String?, cb: LoginCallback) {
        coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e("Login process failed. $throwable")
            cb.error(throwable)
        }) {
            if (Prefs.loginForceEmailAuth) {
                enqueueForceEmailAuth = true
            }
            val loginToken = token ?: getLoginToken(wiki)
            val loginResult = getLoginResponse(wiki, userName, password, retypedPassword, twoFactorCode, emailAuthCode, loginToken).toLoginResult(wiki, password)
            if (loginResult != null) {
                if (loginResult.pass() && userName.isNotEmpty()) {
                    ServiceFactory.get(wiki).getUserInfo().query?.userInfo?.let {
                        loginResult.userId = it.id
                        loginResult.groups = it.groups()
                        L.v("Found user ID " + it.id + " for " + wiki.subdomain())
                    }
                    cb.success(loginResult)
                } else if (LoginResult.STATUS_UI == loginResult.status) {
                    val parsedMessage = loginResult.message?.let { ServiceFactory.get(wiki).parseText(it) }?.text ?: loginResult.message
                    when (loginResult) {
                        is LoginOAuthResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), loginToken)
                        is LoginEmailAuthResult -> cb.uiPrompt(loginResult, LoginFailedException(parsedMessage), loginToken)
                        is LoginResetPasswordResult -> cb.passwordResetPrompt(loginToken)
                        else -> cb.error(LoginFailedException(parsedMessage))
                    }
                } else if (LoginResult.STATUS_FAIL == loginResult.status) {
                    // If the result is FAIL, it's still possible that the authmanager expects a CAPTCHA.
                    // We need to make one more call to authmanager to make sure.
                    val response = ServiceFactory.get(wiki).getAuthManagerForLogin().query?.captchaId()
                    if (response.isNullOrEmpty()) {
                        cb.error(LoginFailedException(loginResult.message))
                    } else {
                        cb.uiPrompt(loginResult, LoginFailedException(loginResult.message), loginToken)
                    }
                } else {
                    cb.error(LoginFailedException(loginResult.message))
                }
            } else {
                cb.error(IOException("Login failed. Unexpected response."))
            }
        }
    }

    suspend fun loginBlocking(wiki: WikiSite, userName: String, password: String, twoFactorCode: String? = null, emailAuthCode: String? = null): LoginResponse {
        val loginToken = getLoginToken(wiki)
        val loginResponse = getLoginResponse(wiki, userName, password, null, twoFactorCode, emailAuthCode, loginToken)
        val loginResult = loginResponse.toLoginResult(wiki, password) ?: throw IOException("Unexpected response when logging in.")
        if (LoginResult.STATUS_UI == loginResult.status) {
            if (loginResult is LoginOAuthResult) {
                // TODO: Find a better way to boil up the warning about 2FA
                Toast.makeText(WikipediaApp.instance,
                    R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show()
            } else if (loginResult is LoginEmailAuthResult) {
                // TODO: Find a better way to boil up the warning about Email auth
                Toast.makeText(WikipediaApp.instance,
                    R.string.login_email_auth_other_workflow_error_msg, Toast.LENGTH_LONG).show()
            }
            throw LoginFailedException(loginResult.message)
        } else if (!loginResult.pass() || loginResult.userName.isNullOrEmpty()) {
            throw LoginFailedException(loginResult.message)
        }
        return loginResponse
    }

    private suspend fun getLoginToken(wiki: WikiSite): String {
        val response = ServiceFactory.get(wiki).getLoginToken()
        return response.query?.loginToken() ?: throw RuntimeException("Received empty login token.")
    }

    private suspend fun getLoginResponse(wiki: WikiSite, userName: String, password: String, retypedPassword: String?,
        twoFactorCode: String?, emailAuthCode: String?, loginToken: String?): LoginResponse {
        return if ((!twoFactorCode.isNullOrEmpty() || !emailAuthCode.isNullOrEmpty()) || !retypedPassword.isNullOrEmpty())
            ServiceFactory.get(wiki).postLogIn(userName, password, retypedPassword, twoFactorCode, emailAuthCode, loginToken, true)
        else
            ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
    }

    companion object {
        var enqueueForceEmailAuth = false
    }
}
