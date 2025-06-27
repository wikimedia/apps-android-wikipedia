package org.wikimedia.metrics_platform.config.sampling;

import com.google.gson.annotations.SerializedName;

public class SampleConfig {

    public enum Identifier {
        @SerializedName("session") SESSION,
        @SerializedName("device") DEVICE,
        @SerializedName("pageview") PAGEVIEW
    }

    /** Sampling rate. **/
    double rate;

    /** ID type to use for sampling. */
    Identifier identifier;
}
