package org.wikipedia.server;

import org.wikipedia.page.Page;

/**
 * Gson POJI for loading remaining page content.
 */
public interface PageRemaining {
    void mergeInto(Page page);

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageRemaining pageRemaining);

        void failure(Throwable throwable);
    }
}
