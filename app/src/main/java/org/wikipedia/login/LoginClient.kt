package org.wikipedia.login

import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.util.log.L
import java.io.IOException

object LoginClient {
    interface LoginCallback {
        fun success(result: LoginResult)
        fun twoFactorPrompt(caught: Throwable, token: String?)
        fun passwordResetPrompt(token: String?)
        fun error(caught: Throwable)
    }

    suspend fun login(
        userName: String, password: String, retypedPassword: String? = null, twoFactorCode: String?,
        loginToken: String?, cb: LoginCallback
    ) {
        try {
            val wiki = WikipediaApp.instance.wikiSite
            val loginResponse = getLoginResponse(wiki, userName, password, retypedPassword,
                twoFactorCode, loginToken)
            var loginResult = loginResponse.toLoginResult(wiki, password)
            if (loginResult != null) {
                if (loginResult.pass() && userName.isNotEmpty()) {
                    loginResult = getExtendedInfo(wiki, loginResult)
                } else if (LoginResult.STATUS_UI == loginResult.status) {
                    when (loginResult) {
                        is LoginOAuthResult -> cb.twoFactorPrompt(
                            LoginFailedException(loginResult.message),
                            loginToken
                        )

                        is LoginResetPasswordResult -> cb.passwordResetPrompt(loginToken)
                        else -> cb.error(LoginFailedException(loginResult.message))
                    }
                } else {
                    cb.error(LoginFailedException(loginResult.message))
                }
            } else {
                cb.error(IOException("Login failed. Unexpected response."))
            }

            if (loginResult != null) {
                cb.success(loginResult)
            }
        } catch (caught: Exception) {
            L.e("Login process failed. $caught")
            cb.error(caught)
        }
    }

    suspend fun login(wiki: WikiSite, userName: String, password: String) {
        val loginResponse = getLoginResponse(wiki, userName, password, getLoginToken(wiki))
        val loginResult = loginResponse.toLoginResult(wiki, password)
            ?: throw IOException("Unexpected response when logging in.")
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
    }

    suspend fun getLoginToken(wiki: WikiSite): String {
        val response = withContext(Dispatchers.IO) { ServiceFactory.get(wiki).getLoginToken() }
        val loginToken = response.query?.loginToken()
        if (loginToken.isNullOrEmpty()) {
            throw RuntimeException("Received empty login token.")
        }
        return loginToken
    }

    private suspend fun getLoginResponse(
        wiki: WikiSite, userName: String, password: String, loginToken: String?
    ): LoginResponse {
        return ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
    }

    private suspend fun getLoginResponse(
        wiki: WikiSite, userName: String, password: String, retypedPassword: String?,
        twoFactorCode: String?, loginToken: String?
    ): LoginResponse = withContext(Dispatchers.IO) {
        val service = ServiceFactory.get(wiki)
        if (twoFactorCode.isNullOrEmpty() && retypedPassword.isNullOrEmpty()) {
            service.postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
        } else {
            service.postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken, true)
        }
    }

    private suspend fun getExtendedInfo(wiki: WikiSite, loginResult: LoginResult): LoginResult {
        val response = withContext(Dispatchers.IO) { ServiceFactory.get(wiki).getUserInfo() }
        val userInfo = response.query?.userInfo!!
        loginResult.userId = userInfo.id
        loginResult.groups = userInfo.groups()
        L.v("Found user ID ${userInfo.id} for ${wiki.subdomain()}")
        return loginResult
    }

    @Serializable
    class LoginResponse : MwResponse() {

        @SerialName("clientlogin")
        private val clientLogin: ClientLogin? = null

        fun toLoginResult(site: WikiSite, password: String): LoginResult? {
            return clientLogin?.toLoginResult(site, password)
        }

        @Serializable
        private class ClientLogin {

            private val status: String? = null
            private val requests: List<Request>? = null
            private val message: String? = null
            @SerialName("username")
            private val userName: String? = null

            fun toLoginResult(site: WikiSite, password: String): LoginResult {
                var userMessage = message
                if (LoginResult.STATUS_UI == status) {
                    if (requests != null) {
                        for (req in requests) {
                            if (req.id.orEmpty().endsWith("TOTPAuthenticationRequest")) {
                                return LoginOAuthResult(site, status, userName, password, message)
                            } else if (req.id.orEmpty().endsWith("PasswordAuthenticationRequest")) {
                                return LoginResetPasswordResult(site, status, userName, password, message)
                            }
                        }
                    }
                } else if (LoginResult.STATUS_PASS != status && LoginResult.STATUS_FAIL != status) {
                    // TODO: String resource -- Looks like needed for others in this class too
                    userMessage = "An unknown error occurred."
                }
                return LoginResult(site, status!!, userName, password, userMessage)
            }
        }

        @Serializable
        private class Request {
            val id: String? = null
        }
    }

    class LoginFailedException(message: String?) : Throwable(message)
}
