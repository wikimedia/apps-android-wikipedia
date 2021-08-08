package org.wikipedia.analytics.eventplatform;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.Test;
import org.wikipedia.json.MoshiUtil;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;

@SuppressWarnings("checkstyle:magicnumber")
public class SamplingConfigTest {
    private static final Moshi MOSHI = MoshiUtil.getDefaultMoshi();

    @Test
    public void testSamplingConfigDeserializationDefaults() throws IOException {
        String json = "{}";
        assertDeserializedValues(json, 1.0, SESSION);
    }

    @Test
    public void testSamplingConfigDeserializationDefaultIdentifierOnly() throws IOException {
        String json = "{\"rate\": 0.5}";
        assertDeserializedValues(json, 0.5, SESSION);
    }

    @Test
    public void testSamplingConfigDeserializationDefaultRateOnly() throws IOException {
        String json = "{\"identifier\": \"device\"}";
        assertDeserializedValues(json, 1.0, DEVICE);
    }

    @Test
    public void testSamplingConfigDeserializationNoDefaults() throws IOException {
        String json = "{\"rate\": 0.325, \"identifier\": \"pageview\"}";
        assertDeserializedValues(json, 0.325, PAGEVIEW);
    }

    private void assertDeserializedValues(String json, double rate, SamplingConfig.Identifier identifier) throws IOException {
        final JsonAdapter<SamplingConfig> adapter = MOSHI.adapter(SamplingConfig.class);
        final SamplingConfig config = adapter.fromJson(json);
        assert config != null;
        assertThat(config.getRate(), equalTo(rate));
        assertThat(config.getIdentifier(), equalTo(identifier));
    }
}
