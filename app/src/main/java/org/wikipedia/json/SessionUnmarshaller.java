package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.analytics.SessionData;
import org.wikipedia.util.log.L;

public final class SessionUnmarshaller {
    @NonNull public static SessionData unmarshal(@Nullable String json) {
        SessionData sessionData = null;
        try {
            sessionData = GsonUnmarshaller.unmarshal(SessionData.class, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            L.e(e);
        }
        if (sessionData == null) {
            sessionData = new SessionData();
        }
        return sessionData;
    }

    private SessionUnmarshaller() { }
}
