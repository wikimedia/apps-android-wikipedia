package org.wikipedia.test

import androidx.core.net.toUri
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.wikipedia.auth.AccountUtil
import org.wikipedia.auth.OAuthClient
import org.wikipedia.auth.OAuthProfile
import org.wikipedia.json.JsonUtil

object TestOAuthUtil {
    const val HOUR_MILLIS = 60 * 60 * 1000L

    // Creates an account with an authorized state whose token endpoint is the given server, so that
    // an OAuthClient constructed afterwards will refresh its tokens against it.
    fun persistAuthState(server: MockWebServer, accessToken: String, refreshToken: String, expiresInMillis: Long) {
        val config = AuthorizationServiceConfiguration(server.url("/authorize").toString().toUri(), server.url("/token").toString().toUri())
        val authRequest = AuthorizationRequest.Builder(config, OAuthClient.CLIENT_ID, ResponseTypeValues.CODE, OAuthClient.REDIRECT_URI.toUri()).build()
        val authResponse = AuthorizationResponse.Builder(authRequest)
            .setState(authRequest.state)
            .setAuthorizationCode("code")
            .build()
        val tokenResponse = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setTokenType("Bearer")
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)
            .setAccessTokenExpirationTime(System.currentTimeMillis() + expiresInMillis)
            .build()
        AccountUtil.updateAccount(profile("Example"), AuthState(authResponse, tokenResponse, null).jsonSerializeString())
    }

    fun profile(userName: String, groups: List<String> = listOf("user")): OAuthProfile {
        return JsonUtil.decodeFromString("""{"username":"$userName","groups":${JsonUtil.encodeToString(groups)}}""")!!
    }

    fun tokenResponse(accessToken: String, refreshToken: String): MockResponse {
        return MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""{"token_type":"Bearer","expires_in":14400,"access_token":"$accessToken","refresh_token":"$refreshToken"}""")
    }

    // What the server returns for a refresh token that has expired, been revoked, or already been used.
    fun invalidGrantResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"error":"invalid_grant","error_description":"The refresh token is invalid.","hint":"Token has been revoked"}""")
    }
}
