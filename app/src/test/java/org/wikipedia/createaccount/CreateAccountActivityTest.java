package org.wikipedia.createaccount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.createaccount.CreateAccountActivity.ValidateResult;
import static org.wikipedia.createaccount.CreateAccountActivity.validateInput;

@RunWith(RobolectricTestRunner.class) public class CreateAccountActivityTest {

    @Test public void testValidateInputSuccessWithEmail() {
        assertThat(validateInput("user", "password", "password", "test@example.com"),
                is(ValidateResult.SUCCESS));
    }
    @Test public void testValidateInvalidEmail() {
        assertThat(validateInput("user", "password", "password", ""),
                is(ValidateResult.NO_EMAIL));
    }
    @Test public void testValidateInputInvalidUser() {
        assertThat(validateInput("user[]", "password", "password", ""),
                is(ValidateResult.INVALID_USERNAME));
    }

    @Test public void testValidateInputInvalidPassword() {
        assertThat(validateInput("user", "foo", "password", ""),
                is(ValidateResult.PASSWORD_TOO_SHORT));
    }

    @Test public void testValidateInputPasswordMismatch() {
        assertThat(validateInput("user", "password", "passw0rd", ""),
                is(ValidateResult.PASSWORD_MISMATCH));
    }

    @Test public void testValidateInputInvalidEmail() {
        assertThat(validateInput("user", "password", "password", "foo"),
                is(ValidateResult.INVALID_EMAIL));
    }
}
