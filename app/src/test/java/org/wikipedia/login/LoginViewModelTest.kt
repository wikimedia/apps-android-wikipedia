package org.wikipedia.login

import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.openid.appauth.AuthorizationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.auth.OAuthClient

@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {
    private val viewModel = LoginViewModel()

    @Test
    fun testClosingBrowserCancels() {
        viewModel.onAuthorizationResult(AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.toIntent())

        assertEquals(LoginViewModel.Event.Canceled, nextEvent())
        assertNull(viewModel.error.value)
    }

    @Test
    fun testDecliningAuthorizationCancels() {
        val redirect = "${OAuthClient.REDIRECT_URI}?error=access_denied&state=state".toUri()

        viewModel.onAuthorizationResult(AuthorizationException.fromOAuthRedirect(redirect).toIntent())

        assertEquals(LoginViewModel.Event.Canceled, nextEvent())
        assertNull(viewModel.error.value)
    }

    @Test
    fun testMissingResultCancels() {
        viewModel.onAuthorizationResult(null)

        assertEquals(LoginViewModel.Event.Canceled, nextEvent())
    }

    @Test
    fun testAuthorizationErrorIsShown() {
        val exception = AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH

        viewModel.onAuthorizationResult(exception.toIntent())

        assertEquals(LoginViewModel.Event.Failed(exception), nextEvent())
        assertEquals(exception, viewModel.error.value)
    }

    @Test
    fun testRetryClearsError() {
        viewModel.onError(IllegalStateException())

        viewModel.onAuthorizationStarted()

        assertNull(viewModel.error.value)
    }

    private fun nextEvent(): LoginViewModel.Event {
        return runBlocking { withTimeout(1000) { viewModel.events.first() } }
    }
}
