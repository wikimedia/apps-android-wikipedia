package org.wikipedia.csrf

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.login.LoginClient
import org.wikipedia.util.log.L
import java.io.IOException

object CsrfTokenClient {
    private val MUTEX = Mutex()
    private const val ANON_TOKEN = "+\\"
    private const val MAX_RETRIES = 3

    /**
     * Returns a token for the current user. If the user is logged in, this never returns an
     * anonymous token, even if they're logged out while it's being requested (e.g. because their
     * session couldn't be renewed), so that whatever they were doing isn't done anonymously instead.
     */
    suspend fun getToken(site: WikiSite, type: String = "csrf", svc: Service? = null): String {
        val requiresUserToken = AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount
        return withContext(Dispatchers.IO) {
            MUTEX.withLock {
                val service = svc ?: ServiceFactory.get(site)
                var lastError: Throwable? = null
                for (retry in 0 until MAX_RETRIES) {
                    try {
                        val token = fetchToken(service, type)
                        if (token == ANON_TOKEN && requiresUserToken) {
                            if (!renewSession()) {
                                lastError = IOException("Logged out, since the session could not be renewed.")
                                break
                            }
                        } else if (token.isNotEmpty()) {
                            return@withLock token
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // Retrying is intended only for network errors.
                        L.e(e)
                        lastError = e
                    }
                }
                throw lastError ?: IOException("Invalid token, or login failure.")
            }
        }
    }

    private suspend fun fetchToken(service: Service, type: String): String {
        val tokenResponse = service.getToken(type)
        return if (type == "rollback") {
            tokenResponse.query?.rollbackToken().orEmpty()
        } else {
            tokenResponse.query?.csrfToken().orEmpty()
        }
    }

    /**
     * Called when the server returns an anonymous token although the user is logged in, which means
     * that it no longer recognizes their session. Returns whether the session was renewed, and if
     * not, logs the user out.
     */
    private suspend fun renewSession(): Boolean {
        if (!AccountUtil.isLoggedIn) {
            // Already logged out in the meantime, e.g. by OAuthClient when the refresh token expired.
            return false
        }
        // Accounts that logged in with OAuth have no password, and don't need one: OAuthInterceptor
        // already refreshes the access token whenever it expires or is rejected, so if the server
        // still doesn't recognize us, we no longer have a usable token, and the user must log in again.
        val renewed = AccountUtil.password?.let { password ->
            L.d("App believes we're logged in, but got anonymous token. Logging in explicitly...")
            // Regardless of which WikiSite the token is being requested from, the login call
            // should be done on the primary WikiSite of the app itself.
            LoginClient().loginBlocking(WikipediaApp.instance.wikiSite, AccountUtil.userName, password).pass()
        } ?: false
        if (!renewed) {
            AccountUtil.bailWithLogout()
        }
        return renewed
    }
}
