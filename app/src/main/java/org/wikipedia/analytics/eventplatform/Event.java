package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import static org.wikipedia.util.DateUtil.iso8601DateFormat;

/**
 * Base class for an Event Platform event.
 *s
 * The only required field is the name of the stream to which the event should be sent.
 *
 * A metadata object may also be provided. The EPC library will inject a timestamp (as
 * meta.client_dt) if it is not already present.
 */
public class Event {
    @SerializedName("$schema") @NonNull private final String schema;
    @SerializedName("meta") @Nullable private final Metadata metadata;
    @SerializedName("client_dt") @Nullable private String timestamp;
    @SerializedName("app_session_id") @Nullable private String sessionId;
    @SerializedName("app_install_id") @Nullable private String appInstallId;

    public Event(@NonNull String schema, @NonNull String stream) {
        this.schema = schema;
        this.metadata = new Metadata(stream);
        this.timestamp = iso8601DateFormat(new Date());
    }

    @NonNull public String getStream() {
        return metadata.getStream();
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAppInstallId(@Nullable String appInstallId) {
        this.appInstallId = appInstallId;
    }

    private static final class Metadata {
        @NonNull private final String stream;

        private Metadata(@NonNull String stream) {
            this.stream = stream;
        }

        @NonNull private String getStream() {
            return stream;
        }
    }
}
