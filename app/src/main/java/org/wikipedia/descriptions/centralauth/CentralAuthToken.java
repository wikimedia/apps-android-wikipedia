package org.wikipedia.descriptions.centralauth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.mwapi.MwResponse;
import org.wikipedia.json.annotations.Required;

/**
 * Represents the Gson response of a CentralAuth token request.
 *
 * Note: a CentralAuthToken is only valid for a single request, and will become invalid after 10
 * seconds.[1]
 *
 * [1] https://www.mediawiki.org/wiki/Extension:CentralAuth/API
 */
public class CentralAuthToken extends MwResponse {
    @SuppressWarnings("unused") @SerializedName("centralauthtoken") @Nullable
    private Token child;

    public boolean success() {
        return child != null && child.centralAuthToken != null;
    }

    /** Only call if #success returns true */
    @NonNull String getToken() {
        return child.centralAuthToken;
    }

    private static class Token {
        @SuppressWarnings("unused") @SerializedName("centralauthtoken") @Required
        private String centralAuthToken;
    }
}
