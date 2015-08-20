package org.wikipedia.server;

import org.wikipedia.page.Page;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Gson POJI for loading remaining page content.
 */
public interface PageRemaining {
    void mergeInto(Page page);

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageRemaining pageRemaining, Response response);

        void failure(RetrofitError error);
    }
}
