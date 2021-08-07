package org.wikipedia.login

import android.widget.Toast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.GsonUtil
import org.wikipedia.util.log.L
import java.io.IOException

class LoginClient {

    private val disposables = CompositeDisposable()

    interface LoginCallback {
        fun success(result: LoginResult)
        fun twoFactorPrompt(caught: Throwable, token: String?)
        fun passwordResetPrompt(token: String?)
        fun error(caught: Throwable)
    }

    fun request(wiki: WikiSite, userName: String, password: String, cb: LoginCallback) {
        cancel()
        disposables.add(getLoginToken(wiki)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ loginToken ->
                login(wiki, userName, password, null, null, loginToken, cb)
            }, { caught -> cb.error(caught) }))
    }

    fun login(wiki: WikiSite, userName: String, password: String, retypedPassword: String?,
        twoFactorCode: String?, loginToken: String?, cb: LoginCallback) {
        disposables.add(getLoginResponse(wiki, userName, password, retypedPassword, twoFactorCode, loginToken)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { loginResponse ->
                val loginResult = loginResponse.toLoginResult(wiki, password)
                if (loginResult != null) {
                    if (loginResult.pass() && userName.isNotEmpty()) {
                        return@flatMap getExtendedInfo(wiki, loginResult)
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
                Observable.empty()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ loginResult ->
                if (loginResult != null) {
                    cb.success(loginResult)
                } else {
                    cb.error(Throwable("Login succeeded but getting group information failed. "))
                }
            }) { caught ->
                L.e("Login process failed. $caught")
                cb.error(caught)
            })
    }

    fun loginBlocking(wiki: WikiSite, userName: String, password: String, twoFactorCode: String?): Observable<LoginResponse> {
        return getLoginToken(wiki)
            .flatMap { loginToken ->
                getLoginResponse(wiki, userName, password, null, twoFactorCode, loginToken)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { loginResponse ->
                if (loginResponse == null) {
                    throw IOException("Unexpected response when logging in.")
                }
                val loginResult = loginResponse.toLoginResult(wiki, password) ?: throw IOException("Unexpected response when logging in.")
                if (LoginResult.STATUS_UI == loginResult.status) {
                    if (loginResult is LoginOAuthResult) {
                        // TODO: Find a better way to boil up the warning about 2FA
                        Toast.makeText(WikipediaApp.getInstance(),
                            R.string.login_2fa_other_workflow_error_msg, Toast.LENGTH_LONG).show()
                    }
                    throw LoginFailedException(loginResult.message)
                } else if (!loginResult.pass() || loginResult.userName.isNullOrEmpty()) {
                    throw LoginFailedException(loginResult.message)
                }
                loginResponse
            }
    }

    private fun getLoginToken(wiki: WikiSite): Observable<String?> {
        return ServiceFactory.get(wiki).loginToken
            .subscribeOn(Schedulers.io())
            .map { response ->
                val queryResponse =
                    GsonUtil.getDefaultGson().fromJson(response, MwQueryResponse::class.java)
                val loginToken = queryResponse.query?.loginToken
                if (loginToken.isNullOrEmpty()) {
                    throw RuntimeException("Received empty login token: " + GsonUtil.getDefaultGson().toJson(response))
                }
                loginToken
            }
    }

    private fun getLoginResponse(wiki: WikiSite, userName: String, password: String, retypedPassword: String?,
        twoFactorCode: String?, loginToken: String?): Observable<LoginResponse> {
        return if (twoFactorCode.isNullOrEmpty() && retypedPassword.isNullOrEmpty())
            ServiceFactory.get(wiki).postLogIn(userName, password, loginToken, Service.WIKIPEDIA_URL)
        else ServiceFactory.get(wiki).postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken, true)
    }

    private fun getExtendedInfo(wiki: WikiSite, loginResult: LoginResult): Observable<LoginResult> {
        return ServiceFactory.get(wiki).userInfo
            .subscribeOn(Schedulers.io())
            .map { response ->
                val id = response.query?.userInfo!!.id
                loginResult.userId = id
                loginResult.groups = response.query?.userInfo!!.groups
                L.v("Found user ID " + id + " for " + wiki.subdomain())
                loginResult
            }
    }

    fun cancel() {
        disposables.clear()
    }

    @JsonClass(generateAdapter = true)
    class LoginResponse(@Json(name = "clientlogin") internal val clientLogin: ClientLogin? = null) : MwResponse() {
        fun toLoginResult(site: WikiSite, password: String): LoginResult? {
            return clientLogin?.toLoginResult(site, password)
        }

        @JsonClass(generateAdapter = true)
        class ClientLogin(internal val status: String = "",
                          internal val requests: List<Request> = emptyList(),
                          internal val message: String? = null,
                          @Json(name = "username") internal val userName: String = "") {
            fun toLoginResult(site: WikiSite, password: String): LoginResult {
                var userMessage = message
                if (LoginResult.STATUS_UI == status) {
                    for (req in requests) {
                        if (req.id.endsWith("TOTPAuthenticationRequest")) {
                            return LoginOAuthResult(site, status, userName, password, message)
                        } else if (req.id.endsWith("PasswordAuthenticationRequest")) {
                            return LoginResetPasswordResult(site, status, userName, password, message)
                        }
                    }
                } else if (LoginResult.STATUS_PASS != status && LoginResult.STATUS_FAIL != status) {
                    // TODO: String resource -- Looks like needed for others in this class too
                    userMessage = "An unknown error occurred."
                }
                return LoginResult(site, status, userName, password, userMessage)
            }
        }

        @JsonClass(generateAdapter = true)
        class Request(val id: String = "")
    }

    class LoginFailedException(message: String?) : Throwable(message)
}
