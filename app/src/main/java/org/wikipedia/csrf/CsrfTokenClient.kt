package org.wikipedia.csrf

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
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

    suspend fun getTokenBlocking(site: WikiSite, type: String = "csrf", svc: Service? = null): String {
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
                            LoginClient().loginBlocking(site, AccountUtil.userName!!, AccountUtil.password!!, "")
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
                    } catch (e: Exception) {
                        L.e(e)
                        lastError = e
                    }
                    if (token.isEmpty() || (AccountUtil.isLoggedIn && token == ANON_TOKEN)) {
                        continue
                    }
                    break
                }
                if (token.isEmpty() || (AccountUtil.isLoggedIn && token == ANON_TOKEN)) {
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

    // TODO: remove this after all usages are converted to coroutines
    fun getToken(site: WikiSite, type: String = "csrf", svc: Service? = null): Observable<String> {
        return Observable.create { emitter ->
            var token = ""
            try {
                MUTEX.acquire()
                val service = svc ?: ServiceFactory.get(site)

                if (emitter.isDisposed) {
                    return@create
                }
                var lastError: Throwable? = null
                for (retry in 0 until MAX_RETRIES) {
                    if (retry > 0) {
                        // Log in explicitly
                        // TODO: convert this with coroutines
                        runBlocking {
                            try {
                                LoginClient().loginBlocking(site, AccountUtil.userName!!, AccountUtil.password!!, "")
                            } catch (e: Exception) {
                                L.e(e)
                                lastError = e
                            }
                        }
                    }
                    if (emitter.isDisposed) {
                        return@create
                    }

                    service.getTokenObservable(type)
                            .subscribeOn(Schedulers.io())
                            .blockingSubscribe({
                                if (type == "rollback") {
                                    token = it.query?.rollbackToken().orEmpty()
                                } else {
                                    token = it.query?.csrfToken().orEmpty()
                                }
                                if (AccountUtil.isLoggedIn && token == ANON_TOKEN) {
                                    throw RuntimeException("App believes we're logged in, but got anonymous token.")
                                }
                            }, {
                                L.e(it)
                                lastError = it
                            })
                    if (emitter.isDisposed) {
                        return@create
                    }

                    if (token.isEmpty() || (AccountUtil.isLoggedIn && token == ANON_TOKEN)) {
                        continue
                    }
                    break
                }
                if (token.isEmpty() || (AccountUtil.isLoggedIn && token == ANON_TOKEN)) {
                    if (token == ANON_TOKEN) {
                        bailWithLogout()
                    }
                    throw lastError ?: IOException("Invalid token, or login failure.")
                }
            } catch (t: Throwable) {
                emitter.onError(t)
            } finally {
                MUTEX.release()
            }
            emitter.onNext(token)
            emitter.onComplete()
        }
    }

    private fun bailWithLogout() {
        // Signal to the rest of the app that we're explicitly logging out in the background.
        WikipediaApp.instance.logOut()
        Prefs.loggedOutInBackground = true
        WikipediaApp.instance.bus.post(LoggedOutInBackgroundEvent())
    }
}
