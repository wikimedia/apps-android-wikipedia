package org.wikipedia.csrf

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.login.LoginClient
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L
import java.io.IOException
import java.util.concurrent.Semaphore

object CsrfTokenClient {
    private val MUTEX: Semaphore = Semaphore(1)
    private const val ANON_TOKEN = "+\\"
    private const val MAX_RETRIES = 3

    suspend fun getToken(site: WikiSite, type: String = "csrf", svc: Service? = null): String {
        var token = ""
        withContext(Dispatchers.IO) {
            try {
                MUTEX.acquire()
                val service = svc ?: ServiceFactory.get(site)
                var lastError: Throwable? = null
                for (retry in 0 until MAX_RETRIES) {
                    if (retry > 0) {
                        // Log in explicitly
                        try {
                            LoginClient().loginBlocking(site, AccountUtil.userName, AccountUtil.password!!, "")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            L.e(e)
                            lastError = e
                        }
                    }
                    try {
                        val tokenResponse = service.getToken(type)
                        token = if (type == "rollback") {
                            tokenResponse.query?.rollbackToken().orEmpty()
                        } else {
                            tokenResponse.query?.csrfToken().orEmpty()
                        }
                        if (AccountUtil.isLoggedIn && token == ANON_TOKEN) {
                            throw RuntimeException("App believes we're logged in, but got anonymous token.")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        L.e(e)
                        lastError = e
                    }
                    if (token.isEmpty() || (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount && token == ANON_TOKEN)) {
                        continue
                    }
                    break
                }
                if (token.isEmpty() || (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount && token == ANON_TOKEN)) {
                    if (token == ANON_TOKEN) {
                        bailWithLogout()
                    }
                    throw lastError ?: IOException("Invalid token, or login failure.")
                }
            } finally {
                MUTEX.release()
            }
        }
        return token
    }

    private fun bailWithLogout() {
        // Signal to the rest of the app that we're explicitly logging out in the background.
        WikipediaApp.instance.logOut()
        Prefs.loggedOutInBackground = true
        FlowEventBus.post(LoggedOutInBackgroundEvent())
    }
}
