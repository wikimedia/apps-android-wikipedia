package org.wikipedia.editing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Edit {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Result edit;
    @NonNull protected Result edit() {
        return edit;
    }

    protected class Result {
        @Nullable private String result;
        @Nullable protected String status() {
            return result;
        }

        private int newrevid;
        protected int newRevId() {
            return newrevid;
        }

        @Nullable private Captcha captcha;
        @Nullable protected Captcha captcha() {
            return captcha;
        }

        @Nullable private String code;
        @Nullable protected String code() {
            return code;
        }

        protected boolean hasErrorCode() {
            return code != null;
        }

        protected boolean hasCaptchaResponse() {
            return captcha != null;
        }

        @Nullable private String spamblacklist;
        @Nullable protected String spamblacklist() {
            return spamblacklist;
        }

        protected boolean hasSpamBlacklistResponse() {
            return spamblacklist != null;
        }
    }

    protected class Captcha {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @NonNull protected String id() {
            return id;
        }
    }
}
