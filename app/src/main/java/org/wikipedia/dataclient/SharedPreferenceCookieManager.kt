package org.wikipedia.dataclient

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class SharedPreferenceCookieManager(
    // Map: domain -> list of cookies
    private val cookieJar: MutableMap<String, MutableList<Cookie>> = mutableMapOf()
) : CookieJar {

    private fun persistCookies() {
        Prefs.cookies = cookieJar
    }

    @Synchronized
    fun clearAllCookies() {
        cookieJar.clear()
        persistCookies()
    }

    @Synchronized
    fun getCookieByName(name: String): String? {
        for (domainSpec in cookieJar.keys) {
            for (cookie in cookieJar[domainSpec]!!) {
                if (cookie.name == name) {
                    return cookie.value
                }
            }
        }
        return null
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) {
            return
        }
        var cookieJarModified = false
        for (cookie in cookies) {
            // Default to the URI's domain if cookie's domain is not explicitly set
            val domainSpec = cookie.domain.ifEmpty { url.toUri().authority }
            if (!cookieJar.containsKey(domainSpec)) {
                cookieJar[domainSpec] = mutableListOf()
            }
            val cookieList = cookieJar[domainSpec]!!
            if (cookie.expiresAt < System.currentTimeMillis() || "deleted" == cookie.value) {
                val i = cookieList.iterator()
                while (i.hasNext()) {
                    if (i.next().name == cookie.name) {
                        i.remove()
                        cookieJarModified = true
                    }
                }
            } else {
                val i = cookieList.iterator()
                var exists = false
                while (i.hasNext()) {
                    val c = i.next()
                    if (c == cookie) {
                        // an identical cookie already exists, so we don't need to update it.
                        exists = true
                        break
                    } else if (c.name == cookie.name) {
                        // it's a cookie with the same name, but different contents, so remove the
                        // current cookie, so that the new one will be added.
                        i.remove()
                    }
                }
                if (!exists) {
                    cookieList.add(cookie)
                    cookieJarModified = true
                }
            }
        }
        if (cookieJarModified) {
            persistCookies()
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieList = mutableListOf<Cookie>()
        val domain = url.toUri().authority
        for (domainSpec in cookieJar.keys) {
            val cookiesForDomainSpec = cookieJar[domainSpec]!!
            if (domain.endsWith(domainSpec)) {
                buildCookieList(cookieList, cookiesForDomainSpec, null)
            } else if (domainSpec.endsWith(WikiSite.BASE_DOMAIN)) {
                // For sites outside the wikipedia.org domain, transfer the centralauth cookies
                // from wikipedia.org unconditionally.
                buildCookieList(cookieList, cookiesForDomainSpec, CENTRALAUTH_PREFIX)
            }
        }
        return cookieList
    }

    private fun buildCookieList(outList: MutableList<Cookie>, inList: MutableList<Cookie>, prefix: String?) {
        val i = inList.iterator()
        var cookieJarModified = false
        while (i.hasNext()) {
            val cookie = i.next()
            if (prefix != null && !cookie.name.startsWith(prefix)) {
                continue
            }
            // But wait, is the cookie expired?
            if (cookie.expiresAt < System.currentTimeMillis()) {
                i.remove()
                cookieJarModified = true
            } else {
                outList.add(cookie)
            }
        }
        if (cookieJarModified) {
            persistCookies()
        }
    }

    companion object {
        private const val CENTRALAUTH_PREFIX = "centralauth_"
        private var INSTANCE: SharedPreferenceCookieManager? = null

        val instance: SharedPreferenceCookieManager
            get() {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = SharedPreferenceCookieManager(Prefs.cookies.toMutableMap())
                    } catch (e: Exception) {
                        L.logRemoteErrorIfProd(e)
                    }
                }
                if (INSTANCE == null) {
                    INSTANCE = SharedPreferenceCookieManager()
                }
                return INSTANCE!!
            }
    }
}
