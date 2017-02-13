package org.wikipedia.edit;

import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwPostResponse;

class Edit extends MwPostResponse {
    @SuppressWarnings("unused,") @Nullable private Result edit;

    @Nullable Result edit() {
        return edit;
    }

    boolean hasEditResult() {
        return edit != null;
    }

    class Result {
        @SuppressWarnings("unused") @Nullable private String result;
        @SuppressWarnings("unused") private int newrevid;
        @SuppressWarnings("unused") @Nullable private Captcha captcha;
        @SuppressWarnings("unused") @Nullable private String code;
        @SuppressWarnings("unused") @Nullable private String info;
        @SuppressWarnings("unused") @Nullable private String warning;
        @SuppressWarnings("unused") @Nullable private String spamblacklist;

        @Nullable String status() {
            return result;
        }

        int newRevId() {
            return newrevid;
        }

        boolean editSucceeded() {
            return "Success".equals(result);
        }

        @Nullable String captchaId() {
            return captcha == null ? null : captcha.id();
        }

        boolean hasEditErrorCode() {
            return code != null;
        }

        boolean hasCaptchaResponse() {
            return captcha != null;
        }

        @Nullable String code() {
            return code;
        }

        @Nullable String info() {
            return info;
        }

        @Nullable String warning() {
            return warning;
        }

        @Nullable String spamblacklist() {
            return spamblacklist;
        }

        boolean hasSpamBlacklistResponse() {
            return spamblacklist != null;
        }
    }

    private static class Captcha {
        @SuppressWarnings("unused") @Nullable private String id;

        @Nullable String id() {
            return id;
        }
    }
}
