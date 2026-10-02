package org.wikipedia.auth

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OAuthClient(val context: Context) {
    fun interface Callback {
        fun onComplete(e: Exception?)
    }

    // This is read from network threads without locking, so it must never be modified in place.
    // Instead, modify a copy and publish it with updateState().
    @Volatile
    private var authState = AuthState()

    // Held for the duration of a token refresh, so that concurrent requests wait for its result
    // instead of starting refreshes of their own. Besides being wasteful, a second refresh would
    // fail and log the user out, since each refresh token can only be used once.
    private val refreshLock = ReentrantLock()

    // Constructed lazily since it binds to the Custom Tabs service, which is only needed for logging in.
    private val authorizationService by lazy {
        val appAuthConfiguration = AppAuthConfiguration.Builder()
            // .setBrowserMatcher(
            //    BrowserAllowList(
            //        VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
            //        VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
            //    )
            // )
            .build()
        AuthorizationService(context, appAuthConfiguration)
    }

    val authServiceConfig: AuthorizationServiceConfiguration

    val isLoggedIn
        get() = authState.isAuthorized

    init {
        try {
            authState = AuthState.jsonDeserialize(Prefs.oauthState)
        } catch (_: Exception) {
            authState = AuthState()
        }

        val wikiUrl = WikipediaApp.instance.wikiSite.url()
        authServiceConfig = AuthorizationServiceConfiguration(
            (wikiUrl + AUTHORIZATION_ENDPOINT).toUri(),
            (wikiUrl + TOKEN_ENDPOINT).toUri(),
            null,
            (wikiUrl + LOGOUT_ENDPOINT).toUri()) // TODO?
    }

    fun getLoginIntent(): Intent {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)

        val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val codeVerifier = Base64.encodeToString(bytes, encoding)

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray())
        val codeChallenge = Base64.encodeToString(hash, encoding)

        val builder = AuthorizationRequest.Builder(
            authServiceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            REDIRECT_URI.toUri())
            .setCodeVerifier(codeVerifier, codeChallenge, "S256")

        // builder.setScopes(...)

        return authorizationService.getAuthorizationRequestIntent(builder.build())
    }

    fun handleAuthorizationResponse(intent: Intent, callback: Callback) {
        val authorizationResponse = AuthorizationResponse.fromIntent(intent)
        if (authorizationResponse == null) {
            callback.onComplete(AuthorizationException.fromIntent(intent))
            return
        }

        val tokenExchangeRequest = authorizationResponse.createTokenExchangeRequest()

        authorizationService.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (response == null) {
                callback.onComplete(exception)
                return@performTokenRequest
            }
            updateState(AuthState(authorizationResponse, response, null))

            MainScope().launch(CoroutineExceptionHandler { _, t ->
                callback.onComplete(t as Exception)
            }) {
                withContext(Dispatchers.IO) {
                    val profile = ServiceFactory.getCoreRest(WikipediaApp.instance.wikiSite).getOAuthProfile()
                    finishLogin(profile)
                    callback.onComplete(null)
                }
            }
        }
    }

    /**
     * Returns an access token that will remain valid for at least another minute, refreshing it
     * first if necessary, or null if the user is not logged in. If the server has rejected a token
     * despite it not having expired yet, pass it as [rejectedAccessToken] to force a refresh.
     *
     * If the refresh fails because of a network problem, this returns the existing token anyway,
     * so that the request can still be served from cache if we're offline. If the refresh fails
     * because the refresh token is no longer valid, the user is logged out.
     */
    @WorkerThread
    fun getFreshAccessToken(rejectedAccessToken: String? = null): String? {
        val currentState = authState
        if (!currentState.isAuthorized) {
            return null
        }
        currentState.usableAccessToken(rejectedAccessToken)?.let { return it }

        return refreshLock.withLock {
            // Another thread may have refreshed the token, or logged out, while we were waiting.
            val latestState = authState
            if (!latestState.isAuthorized) {
                null
            } else {
                latestState.usableAccessToken(rejectedAccessToken) ?: refreshTokens(latestState)
            }
        }
    }

    fun clearAuthState() {
        updateState(AuthState())
    }

    private fun AuthState.usableAccessToken(rejectedAccessToken: String?): String? {
        return accessToken?.takeIf { !needsTokenRefresh && it != rejectedAccessToken }
    }

    private fun refreshTokens(state: AuthState): String? {
        val tokenResponse = try {
            executeTokenRequest(state.createTokenRefreshRequest())
        } catch (e: IOException) {
            L.w("Failed to refresh OAuth tokens; will try again on the next request.", e)
            return state.accessToken
        } catch (e: Exception) {
            // Either the server rejected our refresh token (because it expired, or the user revoked
            // our access), or we don't have a refresh token at all. Either way, the only way to get
            // new tokens is for the user to log in again.
            L.e("Unable to refresh OAuth tokens; logging out.", e)
            clearAuthState()
            AccountUtil.bailWithLogout()
            return null
        }

        val refreshedState = AuthState.jsonDeserialize(state.jsonSerializeString())
        refreshedState.update(tokenResponse, null)
        synchronized(this) {
            if (authState !== state) {
                // The user logged out (or in again) while we were refreshing, so discard the result.
                return authState.accessToken
            }
            updateState(refreshedState)
        }
        L.d("Refreshed OAuth tokens.")
        return refreshedState.accessToken
    }

    /**
     * Performs the token request synchronously, using our own OkHttp client instead of AppAuth's
     * AuthorizationService, which delivers its result on the main thread, and therefore can't be
     * waited on from a network thread without risking a deadlock.
     *
     * @throws AuthorizationException if the server rejected the grant (e.g. an expired refresh token).
     * @throws IOException for any other failure, which is presumed to be temporary.
     */
    private fun executeTokenRequest(tokenRequest: TokenRequest): TokenResponse {
        val formBody = FormBody.Builder().apply {
            tokenRequest.requestParameters.forEach { (name, value) -> add(name, value) }
            add(TokenRequest.PARAM_CLIENT_ID, tokenRequest.clientId)
        }.build()
        val request = Request.Builder()
            .url(tokenRequest.configuration.tokenEndpoint.toString())
            .header("Accept", "application/json")
            .post(formBody)
            .build()

        OkHttpConnectionFactory.oauthTokenClient.newCall(request).execute().use { response ->
            try {
                val json = JSONObject(response.body.string())
                val error = json.optString(AuthorizationException.PARAM_ERROR)
                if (error == AuthorizationException.TokenRequestErrors.INVALID_GRANT.error) {
                    throw AuthorizationException.fromOAuthTemplate(AuthorizationException.TokenRequestErrors.INVALID_GRANT,
                        error, json.optString(AuthorizationException.PARAM_ERROR_DESCRIPTION), null)
                }
                if (!response.isSuccessful) {
                    throw HttpStatusException(response.code, response.request.url.toString(), error)
                }
                return TokenResponse.Builder(tokenRequest).fromResponseJson(json).build()
            } catch (e: JSONException) {
                throw IOException(e)
            }
        }
    }

    @Synchronized
    private fun updateState(newState: AuthState) {
        Prefs.oauthState = newState.jsonSerializeString()
        authState = newState
    }

    private fun finishLogin(profile: OAuthProfile) {
        AccountUtil.updateAccount(null, profile)
        Prefs.isReadingListSyncEnabled = true
        Prefs.readingListPagesDeletedIds = emptySet()
        Prefs.readingListsDeletedIds = emptySet()
        Prefs.tempAccountWelcomeShown = false
        Prefs.tempAccountCreateDay = 0L
        ReadingListSyncAdapter.manualSyncWithForce()
        PollNotificationWorker.schedulePollNotificationJob(WikipediaApp.instance)
        WikipediaFirebaseMessagingService.updateSubscription()
    }

    companion object {
        const val CLIENT_ID = "50ad79ffa34f64853c96b729e4aa5d8c"
        const val REDIRECT_URI = "wikipedia://oauth/callback"
        const val AUTHORIZATION_ENDPOINT = "/w/rest.php/oauth2/authorize"
        const val TOKEN_ENDPOINT = "/w/rest.php/oauth2/access_token"
        const val PROFILE_ENDPOINT = "/w/rest.php/oauth2/resource/profile"
        const val LOGOUT_ENDPOINT = "/w/rest.php/oauth2/logout" // <--TODO?
    }
}
