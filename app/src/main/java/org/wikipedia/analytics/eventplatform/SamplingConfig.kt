package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the sampling config component of a stream configuration.
 *
 * The boxed Double type is used instead of the double primitive because its value may be null,
 * which denotes that the stream should always be *included*.
 */
class SamplingConfig {

    enum Identifier {
        @SerializedName("pageview") PAGEVIEW,
        @SerializedName("session") SESSION,
        @SerializedName("device") DEVICE
    }

    private double rate = 1.0;
    @Nullable private Identifier identifier;

    // This constructor is needed for correct Gson deserialization. Do not remove!
    SamplingConfig() { }

    @VisibleForTesting SamplingConfig(double rate, @Nullable Identifier identifier) {
        this.rate = rate;
        this.identifier = identifier;
    }

    public double getRate() {
        return rate;
    }

    @NonNull public Identifier getIdentifier() {
        return identifier != null ? identifier : Identifier.SESSION;
    }

}
