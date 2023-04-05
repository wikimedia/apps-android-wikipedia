package org.wikipedia.createaccount

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.createaccount.CreateAccountActivity.Companion.validateInput
import org.wikipedia.createaccount.CreateAccountActivity.ValidateResult

@RunWith(RobolectricTestRunner::class)
class CreateAccountActivityTest {
    @Test
    fun testValidateInputSuccessWithEmail() {
        MatcherAssert.assertThat(
            validateInput("user", "password", "password", "test@example.com"),
            Matchers.`is`(ValidateResult.SUCCESS)
        )
    }

    @Test
    fun testValidateInvalidEmail() {
        MatcherAssert.assertThat(
            validateInput("user", "password", "password", ""),
            Matchers.`is`(ValidateResult.NO_EMAIL)
        )
    }

    @Test
    fun testValidateInputInvalidUser() {
        MatcherAssert.assertThat(
            validateInput("user[]", "password", "password", ""),
            Matchers.`is`(ValidateResult.INVALID_USERNAME)
        )
    }

    @Test
    fun testValidateInputInvalidPassword() {
        MatcherAssert.assertThat(
            validateInput("user", "foo", "password", ""),
            Matchers.`is`(ValidateResult.PASSWORD_TOO_SHORT)
        )
    }

    @Test
    fun testValidateInputPasswordMismatch() {
        MatcherAssert.assertThat(
            validateInput("user", "password", "passw0rd", ""),
            Matchers.`is`(ValidateResult.PASSWORD_MISMATCH)
        )
    }

    @Test
    fun testValidateInputPasswordIsUsername() {
        MatcherAssert.assertThat(
            validateInput("password", "password", "password", ""),
            Matchers.`is`(ValidateResult.PASSWORD_IS_USERNAME)
        )
        MatcherAssert.assertThat(
            validateInput("password", "PassworD", "PassworD", ""),
            Matchers.`is`(ValidateResult.PASSWORD_IS_USERNAME)
        )
    }

    @Test
    fun testValidateInputInvalidEmail() {
        MatcherAssert.assertThat(
            validateInput("user", "password", "password", "foo"),
            Matchers.`is`(ValidateResult.INVALID_EMAIL)
        )
    }
}
