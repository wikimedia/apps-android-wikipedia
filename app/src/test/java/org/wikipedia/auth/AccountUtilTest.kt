package org.wikipedia.auth

import android.accounts.Account
import android.accounts.AccountManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.test.TestOAuthUtil.profile

@RunWith(RobolectricTestRunner::class)
class AccountUtilTest {
    private val accountManager = AccountManager.get(WikipediaApp.instance)

    @Test
    fun testOAuthAccountStoresStateInsteadOfPassword() {
        assertTrue(AccountUtil.updateAccount(profile("Example", listOf("user", "sysop")), "state"))

        assertEquals("Example", AccountUtil.account()?.name)
        assertTrue(AccountUtil.isLoggedIn)
        assertNull(AccountUtil.password)
        assertEquals("state", AccountUtil.oauthState)
        assertEquals(setOf("user", "sysop"), AccountUtil.groups)
    }

    @Test
    fun testOAuthLoginRemovesPasswordFromExistingAccount() {
        accountManager.addAccountExplicitly(Account("Example", AccountUtil.accountType()), "hunter2", null)

        assertTrue(AccountUtil.updateAccount(profile("Example"), "state"))

        assertEquals(listOf("Example"), accountNames())
        assertNull(AccountUtil.password)
        assertEquals("state", AccountUtil.oauthState)
    }

    @Test
    fun testOAuthLoginAsDifferentUserReplacesAccount() {
        AccountUtil.updateAccount(profile("Example"), "state1")

        AccountUtil.updateAccount(profile("Other"), "state2")

        assertEquals(listOf("Other"), accountNames())
        assertEquals("state2", AccountUtil.oauthState)
    }

    @Test
    fun testRemovingAccountRemovesOAuthState() {
        AccountUtil.updateAccount(profile("Example"), "state")

        AccountUtil.removeAccount()

        assertFalse(AccountUtil.isLoggedIn)
        assertNull(AccountUtil.oauthState)
    }

    private fun accountNames(): List<String> {
        return accountManager.getAccountsByType(AccountUtil.accountType()).map { it.name }
    }
}
