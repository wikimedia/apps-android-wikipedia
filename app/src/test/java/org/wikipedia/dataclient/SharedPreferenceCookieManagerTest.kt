package org.wikipedia.dataclient

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import okhttp3.Cookie
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wikipedia.settings.Prefs

class SharedPreferenceCookieManagerTest {

    @Before
    fun setup() {
        mockkObject(Prefs)
        every { Prefs.cookies = any() } just Runs
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `centralAuth cookies are transferred to another Wikimedia authority`() {
        val cookieManager = SharedPreferenceCookieManager(
            mutableMapOf(
                COMMONS_AUTHORITY to mutableListOf(
                    cookie(CENTRALAUTH_TOKEN, COMMONS_AUTHORITY),
                    cookie(CENTRALAUTH_USER, COMMONS_AUTHORITY),
                    cookie(LOCAL_SESSION, COMMONS_AUTHORITY)
                )
            )
        )

        val cookies = cookieManager.loadForRequest("https://en.wikipedia.org/w/api.php")

        assertEquals(listOf(CENTRALAUTH_TOKEN, CENTRALAUTH_USER), cookies.map { it.name })
    }

    @Test
    fun `centralAuth cookies are transferred to every approved authority`() {
        val approvedAuthorities = listOf(
            "wikipedia.org",
            "en.wikipedia.org",
            "zh-yue.m.wikipedia.org",
            "wikimedia.org",
            "meta.wikimedia.org",
            "wikidata.org",
            "www.wikidata.org"
        )

        for (authority in approvedAuthorities) {
            val cookieManager = SharedPreferenceCookieManager(
                mutableMapOf(COMMONS_AUTHORITY to mutableListOf(cookie(CENTRALAUTH_TOKEN, COMMONS_AUTHORITY)))
            )

            val cookies = cookieManager.loadForRequest("https://$authority/w/api.php")

            assertEquals(authority, listOf(CENTRALAUTH_TOKEN), cookies.map { it.name })
        }
    }

    @Test
    fun `centralAuth cookies are not transferred to unapproved authorities`() {
        val unapprovedAuthorities = listOf(
            "example.com",
            "wikipedia.org.example.com",
            "en.wikipedia.org.example.com",
            "commons.wikimedia.org.example.com",
            "evil-wikipedia.org",
            "wikipediaxorg.example.com"
        )

        for (authority in unapprovedAuthorities) {
            val cookieManager = SharedPreferenceCookieManager(
                mutableMapOf(
                    COMMONS_AUTHORITY to mutableListOf(
                        cookie(CENTRALAUTH_TOKEN, COMMONS_AUTHORITY),
                        cookie(CENTRALAUTH_USER, COMMONS_AUTHORITY)
                    )
                )
            )

            val cookies = cookieManager.loadForRequest("https://$authority/w/api.php")

            assertTrue(authority, cookies.isEmpty())
        }
    }

    @Test
    fun `cookies stored under an unapproved authority are not transferred to a Wikimedia authority`() {
        val cookieManager = SharedPreferenceCookieManager(
            mutableMapOf(
                "example.com" to mutableListOf(
                    cookie(CENTRALAUTH_TOKEN, "example.com"),
                    cookie(LOCAL_SESSION, "example.com")
                )
            )
        )

        val cookies = cookieManager.loadForRequest("https://en.wikipedia.org/w/api.php")

        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `non-centralAuth cookies are not transferred to another Wikimedia authority`() {
        val cookieManager = SharedPreferenceCookieManager(
            mutableMapOf(
                COMMONS_AUTHORITY to mutableListOf(
                    cookie(LOCAL_SESSION, COMMONS_AUTHORITY),
                    cookie("CentralAuth_Token", COMMONS_AUTHORITY),
                    cookie("foo_centralauth_Token", COMMONS_AUTHORITY)
                )
            )
        )

        val cookies = cookieManager.loadForRequest("https://en.wikipedia.org/w/api.php")

        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `all cookies are returned for the authority they are stored under`() {
        val cookieManager = SharedPreferenceCookieManager(
            mutableMapOf(
                EN_WIKIPEDIA_AUTHORITY to mutableListOf(
                    cookie(CENTRALAUTH_TOKEN, EN_WIKIPEDIA_AUTHORITY),
                    cookie(LOCAL_SESSION, EN_WIKIPEDIA_AUTHORITY)
                )
            )
        )

        val cookies = cookieManager.loadForRequest("https://en.wikipedia.org/w/api.php")

        assertEquals(listOf(CENTRALAUTH_TOKEN, LOCAL_SESSION), cookies.map { it.name })
    }

    @Test
    fun `expired centralAuth cookies are neither transferred nor kept in the jar`() {
        val cookiesForCommons = mutableListOf(
            expiredCookie(CENTRALAUTH_TOKEN, COMMONS_AUTHORITY),
            cookie(CENTRALAUTH_USER, COMMONS_AUTHORITY)
        )
        val cookieManager = SharedPreferenceCookieManager(mutableMapOf(COMMONS_AUTHORITY to cookiesForCommons))

        val cookies = cookieManager.loadForRequest("https://en.wikipedia.org/w/api.php")

        assertEquals(listOf(CENTRALAUTH_USER), cookies.map { it.name })
        assertEquals(listOf(CENTRALAUTH_USER), cookiesForCommons.map { it.name })
    }

    private fun cookie(name: String, domain: String): Cookie {
        return Cookie.Builder()
            .name(name)
            .value("value_of_$name")
            .domain(domain)
            .path("/")
            .build()
    }

    private fun expiredCookie(name: String, domain: String): Cookie {
        return Cookie.Builder()
            .name(name)
            .value("value_of_$name")
            .domain(domain)
            .path("/")
            .expiresAt(System.currentTimeMillis() - 1000)
            .build()
    }

    companion object {
        private const val COMMONS_AUTHORITY = "commons.wikimedia.org"
        private const val EN_WIKIPEDIA_AUTHORITY = "en.wikipedia.org"
        private const val CENTRALAUTH_TOKEN = "centralauth_Token"
        private const val CENTRALAUTH_USER = "centralauth_User"
        private const val LOCAL_SESSION = "enwikiSession"
    }
}
