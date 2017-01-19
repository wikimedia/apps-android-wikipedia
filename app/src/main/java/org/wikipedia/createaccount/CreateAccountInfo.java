package org.wikipedia.createaccount;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

class CreateAccountInfo {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Tokens tokens;
    @SuppressWarnings("unused,NullableProblems") @NonNull private AMInfo authmanagerinfo;

    @NonNull String token() {
        return tokens.token();
    }

    @Nullable String captchaId() {
        String captchaId = null;
        for (CreateAccountInfo.Request request : authmanagerinfo.requests()) {
            if ("CaptchaAuthenticationRequest".equals(request.id())) {
                captchaId = request.fields().get("captchaId").value();
            }
        }
        return captchaId;
    }

    static class Tokens {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String createaccounttoken;
        @NonNull String token() {
            return createaccounttoken;
        }
    }

    static class AMInfo {
        @SuppressWarnings("unused,NullableProblems") @NonNull private List<Request> requests;
        @NonNull List<Request> requests() {
            return requests;
        }
    }

    static class Request {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String id;
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, Field> fields;
        @NonNull String id() {
            return id;
        }
        @NonNull Map<String, Field> fields() {
            return fields;
        }
    }

    static class Field {
        @SuppressWarnings("unused") @Nullable private String type;
        @SuppressWarnings("unused") @Nullable private String value;
        @SuppressWarnings("unused") @Nullable private String label;
        @SuppressWarnings("unused") @Nullable private String help;

        @Nullable String value() {
            return value;
        }
    }
}
