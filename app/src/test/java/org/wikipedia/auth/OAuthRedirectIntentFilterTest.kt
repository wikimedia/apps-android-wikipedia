package org.wikipedia.auth

import android.content.Intent
import androidx.core.net.toUri
import net.openid.appauth.RedirectUriReceiverActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageActivity

@RunWith(RobolectricTestRunner::class)
class OAuthRedirectIntentFilterTest {
    @Test
    fun testOAuthRedirectIsOnlyHandledByAppAuth() {
        assertEquals(listOf(RedirectUriReceiverActivity::class.java.name), handlersOf(OAuthClient.REDIRECT_URI + "?code=abc&state=xyz"))
    }

    @Test
    fun testArticleLinkIsNotHandledByAppAuth() {
        assertEquals(listOf(PageActivity::class.java.name), handlersOf("wikipedia://en.wikipedia.org/wiki/Earth"))
    }

    private fun handlersOf(uri: String): List<String> {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        return WikipediaApp.instance.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
    }
}
