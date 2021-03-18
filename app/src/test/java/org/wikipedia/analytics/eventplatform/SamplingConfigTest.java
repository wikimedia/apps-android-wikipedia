package org.wikipedia.analytics.eventplatform;

import com.google.gson.Gson;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;
import static org.wikipedia.json.GsonUtil.getDefaultGson;

@SuppressWarnings("checkstyle:magicnumber")
public class SamplingConfigTest {

    private static final Gson GSON = getDefaultGson();

    @Test
    public void testSamplingConfigDeserializationDefaults() {
        String json = "{}";
        assertDeserializedValues(json, 1.0, SESSION);
    }

    @Test
    public void testSamplingConfigDeserializationDefaultIdentifierOnly() {
        String json = "{\"rate\": 0.5}";
        assertDeserializedValues(json, 0.5, SESSION);
    }

    @Test
    public void testSamplingConfigDeserializationDefaultRateOnly() {
        String json = "{\"identifier\": \"device\"}";
        assertDeserializedValues(json, 1.0, DEVICE);
    }

    @Test
    public void testSamplingConfigDeserializationNoDefaults() {
        String json = "{\"rate\": 0.325, \"identifier\": \"pageview\"}";
        assertDeserializedValues(json, 0.325, PAGEVIEW);
    }

    private void assertDeserializedValues(String json, double rate, SamplingConfig.Identifier identifier) {
        SamplingConfig config = GSON.fromJson(json, SamplingConfig.class);
        assertThat(config.getRate(), equalTo(rate));
        assertThat(config.getIdentifier(), equalTo(identifier));
    }

}
