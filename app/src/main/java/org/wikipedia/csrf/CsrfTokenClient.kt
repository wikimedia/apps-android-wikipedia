package org.wikipedia.csrf

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
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
                        Completable.fromAction {
                            runBlocking {
                                LoginClient().loginBlocking(site, AccountUtil.userName, AccountUtil.password!!, "")
                            }
                        }.subscribeOn(Schedulers.io())
                            .blockingSubscribe({ }) {
                                L.e(it)
                                lastError = it
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
        FlowEventBus.post(LoggedOutInBackgroundEvent())
    }
}
