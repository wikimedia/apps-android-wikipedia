package org.wikipedia.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.auth.AccountUtil

@RunWith(RobolectricTestRunner::class)
class AccountUtilTest {
    @Test
    fun testTempAccountName() {
        assertFalse(AccountUtil.isUserNameTemporary(""))
        assertFalse(AccountUtil.isUserNameTemporary("Foo"))
        assertFalse(AccountUtil.isUserNameTemporary("~Awesome~"))
        assertTrue(AccountUtil.isUserNameTemporary("~2025-12345"))
    }
}
