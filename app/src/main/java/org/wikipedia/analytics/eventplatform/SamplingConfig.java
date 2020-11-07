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

    @NonNull private final Double rate;
    @NonNull private final Identifier identifier;

    // This constructor is needed for correct Gson deserialization. Do not remove!
    SamplingConfig() {
        this(null);
    }

    @VisibleForTesting SamplingConfig(@Nullable Double rate) {
        this(rate, null);
    }

    @VisibleForTesting SamplingConfig(@Nullable Double rate, @Nullable Identifier identifier) {
        if (rate == null) {
            rate = 1.0;
        }
        if (identifier == null) {
            identifier = Identifier.SESSION;
        }
        this.rate = rate;
        this.identifier = identifier;
    }

    public double getRate() {
        return rate;
    }

    @NonNull public Identifier getIdentifier() {
        return identifier;
    }

}
