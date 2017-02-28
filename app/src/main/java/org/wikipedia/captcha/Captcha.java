package org.wikipedia.captcha;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwResponse;

public class Captcha extends MwResponse {
    @SuppressWarnings("unused,NullableProblems") @NonNull private FancyCaptchaReload fancycaptchareload;
    @NonNull String captchaId() {
        return fancycaptchareload.index();
    }

    private static class FancyCaptchaReload {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String index;
        @NonNull String index() {
            return index;
        }
    }
}
