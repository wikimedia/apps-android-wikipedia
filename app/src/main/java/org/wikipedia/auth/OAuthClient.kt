package org.wikipedia.auth

import com.auth0.android.jwt.JWT
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import org.wikipedia.settings.Prefs

object OAuthClient {

    private var authState = AuthState()
    private var jwt : JWT? = null
    private lateinit var authorizationService : AuthorizationService
    lateinit var authServiceConfig : AuthorizationServiceConfiguration

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

    private fun initAuthServiceConfig() {
        authServiceConfig = AuthorizationServiceConfiguration(
            Uri.parse(Constants.URL_AUTHORIZATION),
            Uri.parse(Constants.URL_TOKEN_EXCHANGE),
            null,
            Uri.parse(Constants.URL_LOGOUT))
    }
}
