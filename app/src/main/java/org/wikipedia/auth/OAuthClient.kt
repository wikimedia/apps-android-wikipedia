package org.wikipedia.auth

import android.content.Context
import android.content.Intent
import android.util.Base64
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import org.wikipedia.settings.Prefs
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ResponseTypeValues
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.push.WikipediaFirebaseMessagingService
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthClient(val context: Context) {
    fun interface Callback {
        fun onComplete(e: Exception?)
    }

    private var authState = AuthState()
    private val authorizationService : AuthorizationService
    val authServiceConfig : AuthorizationServiceConfiguration

    val isLoggedIn : Boolean
        get() = authState.isAuthorized

    val accessToken : String?
        get() = authState.accessToken

    init {
        try {
            authState = AuthState.jsonDeserialize(Prefs.oauthState)
        } catch (_: Exception) {
            authState = AuthState()
        }

        authServiceConfig = AuthorizationServiceConfiguration(
            (OAUTH_WIKI + AUTHORIZATION_ENDPOINT).toUri(),
            (OAUTH_WIKI + TOKEN_ENDPOINT).toUri(),
            null,
            (OAUTH_WIKI + LOGOUT_ENDPOINT).toUri()) //?

        val appAuthConfiguration = AppAuthConfiguration.Builder()
            //.setBrowserMatcher(
            //    BrowserAllowList(
            //        VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
            //        VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
            //    )
            //)
            .build()

        authorizationService = AuthorizationService(context, appAuthConfiguration)
    }

    fun persistState() {
        Prefs.oauthState = authState.jsonSerializeString()
    }

    fun getLoginIntent() : Intent {
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
        val error = AuthorizationException.fromIntent(intent)

        authState = AuthState(authorizationResponse, error)

        if (authorizationResponse == null) {
            callback.onComplete(error)
            return
        }

        val tokenExchangeRequest = authorizationResponse.createTokenExchangeRequest()

        authorizationService.performTokenRequest(tokenExchangeRequest) { response, exception ->
            try {
                if (exception != null) {
                    authState = AuthState()
                    callback.onComplete(exception)
                    return@performTokenRequest
                } else if (response != null) {
                    authState.update(response, exception)
                }
            } finally {
                persistState()
            }

            MainScope().launch(CoroutineExceptionHandler { _, t ->
                callback.onComplete(t as Exception)
            }) {
                withContext(Dispatchers.IO) {
                    val profile = ServiceFactory.getCoreRest(WikiSite(OAUTH_WIKI)).getOAuthProfile()
                    finishLogin(profile)
                    callback.onComplete(null)
                }
            }
        }

        /*


        TODO: figure out how and when to orchestrate refreshing of tokens.


        fun makeCallWithFreshTokens() {
            authState.performActionWithFreshTokens(authorizationService) { accessToken, idToken, ex ->

            }
        }
        */
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
        Prefs.isPushNotificationOptionsSet = false
        WikipediaFirebaseMessagingService.updateSubscription()
    }

    companion object {
        const val CLIENT_ID = "50ad79ffa34f64853c96b729e4aa5d8c"
        const val REDIRECT_URI = "wikipedia://oauth/callback"
        const val OAUTH_WIKI = "https://auth.wikimedia.org"
        const val AUTHORIZATION_ENDPOINT = "/w/rest.php/oauth2/authorize"
        const val TOKEN_ENDPOINT = "/w/rest.php/oauth2/access_token"
        const val PROFILE_ENDPOINT = "/w/rest.php/oauth2/resource/profile"
        const val LOGOUT_ENDPOINT = "/w/rest.php/oauth2/logout" //<--TODO
    }
}
