package org.wikipedia.login

import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.log.L
import java.io.IOException

class LoginClient {

    private var clientJob: Job? = null

    interface LoginCallback {
        fun success(result: LoginResult)
        fun twoFactorPrompt(caught: Throwable, token: String?)
        fun passwordResetPrompt(token: String?)
        fun error(caught: Throwable)
    }

    fun login(coroutineScope: LifecycleCoroutineScope, wiki: WikiSite, userName: String, password: String,
              retypedPassword: String?, twoFactorCode: String?, token: String?, cb: LoginCallback) {
        cancel()
        clientJob = coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e("Login process failed. $throwable")
            cb.error(throwable)
        }) {
            val loginToken = token ?: getLoginToken(wiki)
            val loginResult = getLoginResponse(wiki, userName, password, retypedPassword, twoFactorCode, loginToken).toLoginResult(wiki, password)
            if (loginResult != null) {
                if (loginResult.pass() && userName.isNotEmpty()) {
                    ServiceFactory.get(wiki).getUserInfo().query?.userInfo?.let {
                        loginResult.userId = it.id
                        loginResult.groups = it.groups()
                        L.v("Found user ID " + it.id + " for " + wiki.subdomain())
                    }
                    cb.success(loginResult)
                } else if (LoginResult.STATUS_UI == loginResult.status) {
                    when (loginResult) {
                        is LoginOAuthResult -> cb.twoFactorPrompt(LoginFailedException(loginResult.message), loginToken)
                        is LoginResetPasswordResult -> cb.passwordResetPrompt(loginToken)
                        else -> cb.error(LoginFailedException(loginResult.message))
                    }
                } else {
                    cb.error(LoginFailedException(loginResult.message))
                }
            } else {
                cb.error(IOException("Login failed. Unexpected response."))
            }
        }
    }

    suspend fun loginBlocking(wiki: WikiSite, userName: String, password: String, twoFactorCode: String?): LoginResponse {
        val loginToken = getLoginToken(wiki)
        val loginResponse = getLoginResponse(wiki, userName, password, null, twoFactorCode, loginToken)
        val loginResult = loginResponse.toLoginResult(wiki, password) ?: throw IOException("Unexpected response when logging in.")
        if (LoginResult.STATUS_UI == loginResult.status) {
            if (loginResult is LoginOAuthResult) {
                // TODO: Find a better way to boil up the warning about 2FA
                Toast.makeText(WikipediaApp.instance,
                    R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show()
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
        twoFactorCode: String?, loginToken: String?): LoginResponse {
        return if (twoFactorCode.isNullOrEmpty() && retypedPassword.isNullOrEmpty())
            ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
        else ServiceFactory.get(wiki).postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken, true)
    }

    fun cancel() {
        clientJob?.cancel()
    }
}
