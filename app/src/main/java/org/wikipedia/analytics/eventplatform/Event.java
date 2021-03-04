package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import static org.wikipedia.util.DateUtil.iso8601DateFormat;

/** Base class for an Event Platform event. */
public class Event {
    @SerializedName("$schema") @NonNull private final String schema;
    @Nullable private final Meta meta;
    @Nullable private String dt;
    @SerializedName("app_session_id") @Nullable private String sessionId;
    @SerializedName("app_install_id") @Nullable private String appInstallId;

    public Event(@NonNull String schema, @NonNull String stream) {
        this.schema = schema;
        this.meta = new Meta(stream);
        this.dt = iso8601DateFormat(new Date());
    }

    @NonNull public String getStream() {
        return meta.getStream();
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAppInstallId(@Nullable String appInstallId) {
        this.appInstallId = appInstallId;
    }

    private static final class Meta {
        @NonNull private final String stream;

        private Meta(@NonNull String stream) {
            this.stream = stream;
        }

        @NonNull private String getStream() {
            return stream;
        }
    }
}
