package org.wikipedia.util

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.auth.AccountUtil

@RunWith(RobolectricTestRunner::class)
class AccountUtilTest {
    private val accountManager = mockk<AccountManager>(relaxed = true)
    private val accountsListener = slot<OnAccountsUpdateListener>()
    private val account = Account("Foo", "org.wikipedia")

    @Before
    fun setUp() {
        mockkStatic(AccountManager::class)
        every { AccountManager.get(any()) } returns accountManager
        every { accountManager.addOnAccountsUpdatedListener(capture(accountsListener), any(), any()) } returns Unit
        AccountUtil.invalidateAccountCache()
    }

    @After
    fun tearDown() {
        AccountUtil.invalidateAccountCache()
        unmockkStatic(AccountManager::class)
    }

    @Test
    fun testTempAccountName() {
        assertFalse(AccountUtil.isUserNameTemporary(""))
        assertFalse(AccountUtil.isUserNameTemporary("Foo"))
        assertFalse(AccountUtil.isUserNameTemporary("~Awesome~"))
        assertTrue(AccountUtil.isUserNameTemporary("~2025-12345"))
    }

    @Test
    fun testAccountLookupIsCached() {
        every { accountManager.getAccountsByType(any()) } returns arrayOf(account)

        assertEquals(account, AccountUtil.account())
        assertEquals(account, AccountUtil.account())
        assertTrue(AccountUtil.isLoggedIn)

        verify(exactly = 1) { accountManager.getAccountsByType(any()) }
    }

    @Test
    fun testAccountsChangedListenerInvalidatesCache() {
        every { accountManager.getAccountsByType(any()) } returns emptyArray()
        assertNull(AccountUtil.account())

        every { accountManager.getAccountsByType(any()) } returns arrayOf(account)
        assertNull(AccountUtil.account())
        accountsListener.captured.onAccountsUpdated(arrayOf(account))

        assertEquals(account, AccountUtil.account())
        verify(exactly = 2) { accountManager.getAccountsByType(any()) }
    }

    @Test
    fun testRemoveAccountInvalidatesCache() {
        every { accountManager.getAccountsByType(any()) } returns arrayOf(account)
        assertEquals(account, AccountUtil.account())

        every { accountManager.getAccountsByType(any()) } returns emptyArray()
        AccountUtil.removeAccount()

        verify { accountManager.removeAccountExplicitly(account) }
        assertNull(AccountUtil.account())
    }
}
