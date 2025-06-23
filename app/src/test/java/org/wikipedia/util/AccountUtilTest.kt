package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.auth.AccountUtil

@RunWith(RobolectricTestRunner::class)
class AccountUtilTest {
    @Test
    fun testTempAccountName() {
        MatcherAssert.assertThat(AccountUtil.isUserNameTemporary(""), Matchers.`is`(false))
        MatcherAssert.assertThat(AccountUtil.isUserNameTemporary("Foo"), Matchers.`is`(false))
        MatcherAssert.assertThat(AccountUtil.isUserNameTemporary("~Awesome~"), Matchers.`is`(false))
        MatcherAssert.assertThat(AccountUtil.isUserNameTemporary("~2025-12345"), Matchers.`is`(true))
    }
}
