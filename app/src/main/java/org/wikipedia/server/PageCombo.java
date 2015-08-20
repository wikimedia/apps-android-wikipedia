package org.wikipedia.server;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Combines PageLead and PageRemaining Gson POJIs for mobileview API.
 * It's basically the same as PageLead.
 * The class name "Page" was already used, and is very entrenched in this code base.
 */
public interface PageCombo extends PageLead {

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageCombo pageCombo, Response response);

        void failure(RetrofitError error);
    }
}
