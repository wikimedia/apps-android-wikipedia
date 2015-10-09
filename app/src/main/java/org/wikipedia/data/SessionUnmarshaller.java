package org.wikipedia.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.acra.ACRA;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SessionData;
import org.wikipedia.util.log.L;

public final class SessionUnmarshaller {
    @NonNull public static SessionData unmarshal(@Nullable String json) {
        SessionData sessionData = null;
        try {
            sessionData = GsonUnmarshaller.unmarshal(SessionData.class, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            // TODO: replace this block with silent exception reporting.
            if (WikipediaApp.getInstance().isProdRelease()) {
                L.e(e);
            } else {
                ACRA.getErrorReporter().putCustomData("json", json);
                ACRA.getErrorReporter().handleException(e, false);
            }
        }
        if (sessionData == null) {
            sessionData = new SessionData();
        }
        return sessionData;
    }

    private SessionUnmarshaller() { }
}
