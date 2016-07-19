package org.wikipedia.createaccount;

import android.support.annotation.Nullable;

public class CreateAccountInfoResult {
    @Nullable private String token;
    @Nullable private String captchaId;

    public CreateAccountInfoResult(@Nullable String token, @Nullable String captchaId) {
        this.token = token;
        this.captchaId = captchaId;
    }

    @Nullable
    public String token() {
        return token;
    }

    @Nullable
    public String captchaId() {
        return captchaId;
    }

    public boolean hasCaptcha() {
        return captchaId != null;
    }
}
