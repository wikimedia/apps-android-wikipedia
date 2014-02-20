package org.wikipedia.createaccount;

import org.wikipedia.editing.*;

public class CreateAccountTokenResult extends CreateAccountResult {
    private final CaptchaResult captchaResult;
    private final String token;

    public CreateAccountTokenResult(CaptchaResult captchaResult, String token) {
        super("needtoken");
        this.captchaResult = captchaResult;
        this.token = token;
    }

    public CaptchaResult getCaptchaResult() {
        return captchaResult;
    }

    public String getToken() {
        return token;
    }
}
