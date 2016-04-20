package org.wikipedia.createaccount.authmanager;

import android.support.annotation.Nullable;

import java.util.List;

public class AMCreateAccountInfoResult {

    private boolean enabled;
    @Nullable private String captchaId;
    @Nullable private List<String> sessionCookie;

    public AMCreateAccountInfoResult(boolean enabled, @Nullable String captchaId,
                                     List<String> sessionCookie) {
        this.enabled = enabled;
        this.captchaId = captchaId;
        this.sessionCookie = sessionCookie;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Nullable
    public String getCaptchaId() {
        return captchaId;
    }

    @Nullable
    public List<String> getSessionCookie() {
        return sessionCookie;
    }
}
