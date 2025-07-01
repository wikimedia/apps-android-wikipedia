package org.wikimedia.metricsplatform.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.google.gson.Gson;

public class MediawikiDataTest {
    @Test
    void testMediawikiData() {
        MediawikiData mediawikiData = MediawikiData.builder()
                .database("enwiki")
                .build();

        assertThat(mediawikiData.getDatabase()).isEqualTo("enwiki");

        Gson gson = GsonHelper.getGson();
        String json = gson.toJson(mediawikiData);
        assertThat(json).isEqualTo("{" +
                "\"database\":\"enwiki\"" +
                "}");
    }
}
