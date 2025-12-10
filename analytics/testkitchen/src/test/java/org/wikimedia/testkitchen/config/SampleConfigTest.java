package org.wikimedia.testkitchen.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wikimedia.testkitchen.config.sampling.SampleConfig.Identifier.DEVICE;

import org.junit.jupiter.api.Test;
import org.wikimedia.testkitchen.config.sampling.SampleConfig;
import org.wikimedia.testkitchen.json.GsonHelper;

import com.google.gson.Gson;

class SampleConfigTest {

    @Test void testSamplingConfigDeserialization() {
        Gson gson = GsonHelper.getGson();
        String samplingConfigJson = "{\"rate\":0.25,\"identifier\":\"device\"}";
        SampleConfig sampleConfig = gson.fromJson(samplingConfigJson, SampleConfig.class);
        assertThat(sampleConfig.getRate()).isEqualTo(0.25);
        assertThat(sampleConfig.getIdentifier()).isEqualTo(DEVICE);
    }

}
