package org.wikipedia.csrf

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
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

class CsrfTokenClient(private val loginWikiSite: WikiSite, private val numRetries: Int,
                      private val csrfService: Service) {
    constructor(site: WikiSite) : this(site, site)
    constructor(csrfWikiSite: WikiSite, loginWikiSite: WikiSite) : this(loginWikiSite, MAX_RETRIES, ServiceFactory.get(csrfWikiSite))

    val token: Observable<String>
        get() {
            return Observable.create { emitter ->
                var token = ""
                try {
                    MUTEX.acquire()
                    if (emitter.isDisposed) {
                        return@create
                    }
                    for (retry in 0 until numRetries) {
                        if (retry > 0) {
                            // Log in explicitly
                            LoginClient().loginBlocking(loginWikiSite, AccountUtil.userName!!, AccountUtil.password!!, "")
                                    .subscribeOn(Schedulers.io())
                                    .blockingSubscribe({ }) { L.e(it) }
                        }
                        if (emitter.isDisposed) {
                            return@create
                        }

                        csrfService.csrfToken
                                .subscribeOn(Schedulers.io())
                                .blockingSubscribe({
                                    token = it.query()!!.csrfToken().orEmpty()
                                    if (AccountUtil.isLoggedIn && token == ANON_TOKEN) {
                                        throw RuntimeException("App believes we're logged in, but got anonymous token.")
                                    }
                                }, { L.e(it) })
                        if (emitter.isDisposed) {
                            return@create
                        }

                        if (token.isEmpty() || token == ANON_TOKEN) {
                            continue
                        }
                        break
                    }
                    if (token.isEmpty() || token == ANON_TOKEN) {
                        if (token == ANON_TOKEN) {
                            bailWithLogout()
                        }
                        throw IOException("Invalid token, or login failure.")
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
        WikipediaApp.getInstance().logOut()
        Prefs.setLoggedOutInBackground(true)
        WikipediaApp.getInstance().bus.post(LoggedOutInBackgroundEvent())
    }

    companion object {
        private val MUTEX: Semaphore = Semaphore(1)
        private const val ANON_TOKEN = "+\\"
        private const val MAX_RETRIES = 3
    }
}
