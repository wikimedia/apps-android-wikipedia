package org.wikipedia.edit;

import androidx.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwPostResponse;

public class Edit extends MwPostResponse {
    @SuppressWarnings("unused,") @Nullable private Result edit;

    @Nullable public Result edit() {
        return edit;
    }

    public boolean hasEditResult() {
        return edit != null;
    }

    public static class Result {
        @SuppressWarnings("unused") @Nullable private String result;
        @SuppressWarnings("unused") private long newrevid;
        @SuppressWarnings("unused") @Nullable private Captcha captcha;
        @SuppressWarnings("unused") @Nullable private String code;
        @SuppressWarnings("unused") @Nullable private String info;
        @SuppressWarnings("unused") @Nullable private String warning;
        @SuppressWarnings("unused") @Nullable private String spamblacklist;

        @Nullable public String status() {
            return result;
        }

        public long newRevId() {
            return newrevid;
        }

        public boolean editSucceeded() {
            return "Success".equals(result);
        }

        @Nullable public String captchaId() {
            return captcha == null ? null : captcha.id();
        }

        public boolean hasEditErrorCode() {
            return code != null;
        }

        public boolean hasCaptchaResponse() {
            return captcha != null;
        }

        @Nullable public String code() {
            return code;
        }

        @Nullable public String info() {
            return info;
        }

        @Nullable public String warning() {
            return warning;
        }

        @Nullable public String spamblacklist() {
            return spamblacklist;
        }

        public boolean hasSpamBlacklistResponse() {
            return spamblacklist != null;
        }
    }

    private static class Captcha {
        @SuppressWarnings("unused") @Nullable private String id;

        @Nullable public String id() {
            return id;
        }
    }
}
