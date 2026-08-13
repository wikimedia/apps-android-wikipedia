package org.wikipedia.createaccount

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.createaccount.CreateAccountActivity.Companion.validateInput
import org.wikipedia.createaccount.CreateAccountActivity.ValidateResult

@RunWith(RobolectricTestRunner::class)
class CreateAccountActivityTest {
    @Test
    fun testValidateInputSuccessWithEmail() {
        assertEquals(
            ValidateResult.SUCCESS,
            validateInput("user", "password", "password", "test@example.com")
        )
    }

    @Test
    fun testValidateInvalidEmail() {
        assertEquals(
            ValidateResult.NO_EMAIL,
            validateInput("user", "password", "password", "")
        )
    }

    @Test
    fun testValidateInputInvalidUser() {
        assertEquals(
            ValidateResult.INVALID_USERNAME,
            validateInput("user[]", "password", "password", "")
        )
    }

    @Test
    fun testValidateInputInvalidPassword() {
        assertEquals(
            ValidateResult.PASSWORD_TOO_SHORT,
            validateInput("user", "foo", "password", "")
        )
    }

    @Test
    fun testValidateInputPasswordMismatch() {
        assertEquals(
            ValidateResult.PASSWORD_MISMATCH,
            validateInput("user", "password", "passw0rd", "")
        )
    }

    @Test
    fun testValidateInputPasswordIsUsername() {
        assertEquals(
            ValidateResult.PASSWORD_IS_USERNAME,
            validateInput("password", "password", "password", "")
        )
        assertEquals(
            ValidateResult.PASSWORD_IS_USERNAME,
            validateInput("password", "PassworD", "PassworD", "")
        )
    }

    @Test
    fun testValidateInputInvalidEmail() {
        assertEquals(
            ValidateResult.INVALID_EMAIL,
            validateInput("user", "password", "password", "foo")
        )
    }
}
