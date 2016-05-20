package org.wikipedia.createaccount.authmanager;

import android.support.annotation.Nullable;

public class AMCreateAccountInfoResult {

    private boolean enabled;
    @Nullable private String captchaId;

    public AMCreateAccountInfoResult(boolean enabled, @Nullable String captchaId) {
        this.enabled = enabled;
        this.captchaId = captchaId;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Nullable
    public String getCaptchaId() {
        return captchaId;
    }
}
