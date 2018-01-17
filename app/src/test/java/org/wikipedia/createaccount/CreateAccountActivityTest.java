package org.wikipedia.createaccount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.createaccount.CreateAccountActivity.ValidateResult;
import static org.wikipedia.createaccount.CreateAccountActivity.validateInput;

@RunWith(RobolectricTestRunner.class) public class CreateAccountActivityTest {

    @Test public void testValidateInputSuccess() throws Throwable {
        assertThat(validateInput("user", "password", "password",  ""),
                is(ValidateResult.SUCCESS));
    }

    @Test public void testValidateInputSuccessWithEmail() throws Throwable {
        assertThat(validateInput("user", "password", "password", "test@example.com"),
                is(ValidateResult.SUCCESS));
    }

    @Test public void testValidateInputInvalidUser() throws Throwable {
        assertThat(validateInput("user[]", "password", "password", ""),
                is(ValidateResult.INVALID_USERNAME));
    }

    @Test public void testValidateInputInvalidPassword() throws Throwable {
        assertThat(validateInput("user", "foo", "password", ""),
                is(ValidateResult.INVALID_PASSWORD));
    }

    @Test public void testValidateInputPasswordMismatch() throws Throwable {
        assertThat(validateInput("user", "password", "passw0rd", ""),
                is(ValidateResult.PASSWORD_MISMATCH));
    }

    @Test public void testValidateInputInvalidEmail() throws Throwable {
        assertThat(validateInput("user", "password", "password", "foo"),
                is(ValidateResult.INVALID_EMAIL));
    }
}
