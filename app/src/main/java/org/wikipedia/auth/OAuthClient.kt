package org.wikipedia.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.auth0.android.jwt.JWT
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import org.wikipedia.settings.Prefs
import androidx.core.net.toUri
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ResponseTypeValues
import org.wikipedia.util.log.L
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthClient(val context: Context) {
    private var authState = AuthState()
    private var jwt : JWT? = null
    private val authorizationService : AuthorizationService
    val authServiceConfig : AuthorizationServiceConfiguration

    val isLoggedIn : Boolean
        get() = authState.isAuthorized

    val accessToken : String?
        get() = authState.accessToken

    init {
        loadState()

        authServiceConfig = AuthorizationServiceConfiguration(
            "https://meta.wikimedia.org/w/rest.php/oauth2/authorize".toUri(),
            "https://meta.wikimedia.org/w/rest.php/oauth2/access_token".toUri(),
            null,
            "https://meta.wikimedia.org/w/rest.php/oauth2/logout".toUri()) //?

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

    fun loadState() {
        try {
            authState = AuthState.jsonDeserialize(Prefs.oauthState)
        } catch (_: Exception) {
            authState = AuthState()
        }
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
            "50ad79ffa34f64853c96b729e4aa5d8c",
            ResponseTypeValues.CODE,
            "wikipedia://oauth/callback".toUri())
            .setCodeVerifier(codeVerifier,
                codeChallenge,
                "S256")

        // builder.setScopes(...)

        return authorizationService.getAuthorizationRequestIntent(builder.build())
    }

    fun handleAuthorizationResponse(intent: Intent) {
        val authorizationResponse = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        authState = AuthState(authorizationResponse, error)

        if (authorizationResponse == null) {
            L.e("Authorization response is null")
            return
        }

        val tokenExchangeRequest = authorizationResponse.createTokenExchangeRequest()

        authorizationService.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (exception != null) {
                authState = AuthState()
            } else {
                if (response != null) {
                    authState.update(response, exception)
                    //jwt = JWT(response.idToken!!)
                }
            }
            persistState()
        }
    }
}
