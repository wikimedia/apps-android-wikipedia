package org.wikipedia.login

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.auth.WikimediaAuthenticator
import org.wikipedia.extensions.parcelable

@RunWith(RobolectricTestRunner::class)
class LoginActivityTest {
    @Test
    fun testAddingAccountFromSystemSettingsIsCanceledWithoutLogin() {
        val response = mockk<AccountAuthenticatorResponse>(relaxed = true)
        val result = WikimediaAuthenticator(WikipediaApp.instance).addAccount(response, AccountUtil.accountType(), null, null, null)
        val intent = result.parcelable<Intent>(AccountManager.KEY_INTENT)!!
        assertEquals(LoginActivity::class.java.name, intent.component?.className)

        Robolectric.buildActivity(LoginActivity::class.java, intent).setup().use {
            it.get().finish()
        }

        verify { response.onRequestContinued() }
        verify { response.onError(AccountManager.ERROR_CODE_CANCELED, any()) }
        verify(exactly = 0) { response.onResult(any()) }
    }
}
