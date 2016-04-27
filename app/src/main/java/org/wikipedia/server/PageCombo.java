package org.wikipedia.server;

/**
 * Combines PageLead and PageRemaining Gson POJIs for mobileview API.
 * It's basically the same as PageLead.
 * The class name "Page" was already used, and is very entrenched in this code base.
 */
public interface PageCombo extends PageLead {

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageCombo pageCombo);

        void failure(Throwable throwable);
    }
}
