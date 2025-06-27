package org.wikimedia.metrics_platform.config;

import java.util.Map;

import javax.annotation.ParametersAreNullableByDefault;
import javax.annotation.concurrent.ThreadSafe;

import com.google.gson.annotations.SerializedName;

import lombok.Value;

@Value @ThreadSafe
@ParametersAreNullableByDefault
public class StreamConfigCollection {
    @SerializedName("streams") public Map<String, StreamConfig> streamConfigs;
}
