package org.wikipedia.analytics.eventplatform

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import java.time.Instant

class SerializationTest {
    @Before
    fun setup() {
        val testInstant = Instant.parse("2026-03-06T14:00:00Z")
        mockkObject(WikipediaApp, Prefs, AccountUtil)
        every { AccountUtil.isLoggedIn } returns false
        every { AccountUtil.isTemporaryAccount } returns true
        every { Prefs.eventPlatformSessionId } returns "12345"
        every { WikipediaApp.instance } returns WikipediaApp()
        every { WikipediaApp.instance.appInstallID } returns "abcde"
        mockkStatic(Instant::class)
        every { Instant.now() } returns testInstant
    }

    @After
    fun tearDown() {
        unmockkStatic(Instant::class)
        unmockkObject(WikipediaApp, Prefs, AccountUtil)
    }

    @Test
    fun testAnalyticsEventDeserialization() {
        // For the polymorphic class discriminator ($schema) to work properly, the type must be
        // specified explicitly at compile time.
        val event: Event = AppInteractionEvent(
            action = "test_action",
            active_interface = "test_interface",
            action_data = "test_data",
            primary_language = "en",
            wiki_id = "enwiki",
            streamName = "/stream/test"
        )
        val json = JsonUtil.encodeToString(event)
        assertEquals("""{"${"\$schema"}":"/analytics/mobile_apps/app_interaction/1.1.0","meta":{"stream":"/stream/test"},"dt":"2026-03-06T14:00:00Z","is_anon":true,"app_session_id":"12345","app_install_id":"abcde","is_temp":true,"action":"test_action","active_interface":"test_interface","action_data":"test_data","primary_language":"en","wiki_id":"enwiki","platform":"android"}""", json)
    }

    @Test
    fun testLoggingEventDeserialization() {
        // For the polymorphic class discriminator ($schema) to work properly, the type must be
        // specified explicitly at compile time.
        val event: Event = ClientErrorEvent.ClientErrorEventImpl(
            message = "test_message",
            errorClass = "test_class",
            stackTrace = "abcde",
            http = ClientErrorEvent.Http(
                method = "GET",
                protocol = "http",
                statusCode = 404
            ),
            url = "http://example.com"
        )
        val json = JsonUtil.encodeToString(event)
        assertEquals("""{"${"\$schema"}":"/mediawiki/client/error/2.0.0","meta":{"stream":"mediawiki.client.error"},"message":"test_message","error_class":"test_class","stack_trace":"abcde","http":{"method":"GET","protocol":"http","status_code":404},"url":"http://example.com"}""", json)
    }
}
