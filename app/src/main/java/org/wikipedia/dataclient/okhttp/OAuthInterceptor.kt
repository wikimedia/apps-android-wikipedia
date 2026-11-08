package org.wikipedia.dataclient.okhttp

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import java.io.IOException

/**
 * Authorizes requests to Wikimedia APIs with the logged-in user's OAuth access token, refreshing
 * the token beforehand if it's about to expire, and once more if the server rejects it anyway
 * (e.g. if the device clock is wrong, or the token was revoked).
 *
 * This must be added after OfflineCacheInterceptor, so that offline content can still be served if
 * we fail to refresh the token, and before UnsuccessfulResponseInterceptor, so that we can see a
 * rejected token's response before it's turned into an exception.
 */
class OAuthInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val oauthClient = WikipediaApp.instance.oauthClient
        if (!shouldAuthorize(request) || !oauthClient.isLoggedIn) {
            return chain.proceed(request)
        }

        val accessToken = oauthClient.getFreshAccessToken() ?: return chain.proceed(request)
        val response = chain.proceed(request.withAccessToken(accessToken))
        if (!isRejectedAccessToken(response) || request.body?.isOneShot() == true) {
            return response
        }

        val newAccessToken = oauthClient.getFreshAccessToken(rejectedAccessToken = accessToken)
        if (newAccessToken == null || newAccessToken == accessToken) {
            return response
        }
        response.close()
        return chain.proceed(request.withAccessToken(newAccessToken))
    }

    private fun Request.withAccessToken(accessToken: String): Request {
        return newBuilder().header("Authorization", "Bearer $accessToken").build()
    }

    companion object {
        private val API_PATH_PREFIXES = listOf("/w/api.php", "/w/rest.php/", "/api/rest_v1/")

        // Only send the token to Wikimedia APIs, and not to e.g. image or map servers, analytics
        // endpoints, or third parties, which don't need it, and shouldn't be able to identify the user.
        fun shouldAuthorize(request: Request): Boolean {
            val url = request.url
            return url.isHttps && request.header("Authorization") == null &&
                    Service.isWikimediaAuthority(url.host) &&
                    API_PATH_PREFIXES.any { url.encodedPath.startsWith(it) }
        }

        // Wikimedia's API gateway rejects an expired or invalid token with a 401, except for the
        // OAuth endpoints, where MediaWiki itself rejects it with a 403.
        private fun isRejectedAccessToken(response: Response): Boolean {
            return response.code == 401 ||
                    (response.code == 403 && response.peekBody(1024).string().contains("mwoauth-invalid-authorization"))
        }
    }
}
