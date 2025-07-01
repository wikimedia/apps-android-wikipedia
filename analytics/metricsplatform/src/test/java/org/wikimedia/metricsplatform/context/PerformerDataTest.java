package org.wikimedia.metricsplatform.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.google.gson.Gson;

class PerformerDataTest {

    @Test void testPerformerData() {
        PerformerData performerData = PerformerData.builder()
                .id(1)
                .name("TestPerformer")
                .isLoggedIn(true)
                .isTemp(false)
                .sessionId("eeeeeeeeeeeeeeeeeeee")
                .pageviewId("eeeeeeeeeeeeeeeeeeee")
                .groups(Collections.singletonList("*"))
                .languageGroups("zh, en")
                .languagePrimary("zh-tw")
                .registrationDt(Instant.parse("2023-03-01T01:08:30Z"))
                .build();

        assertThat(performerData.getId()).isEqualTo(1);
        assertThat(performerData.getName()).isEqualTo("TestPerformer");
        assertThat(performerData.getIsLoggedIn()).isTrue();
        assertThat(performerData.getIsTemp()).isFalse();
        assertThat(performerData.getGroups()).isEqualTo(Collections.singletonList("*"));
        assertThat(performerData.getLanguageGroups()).isEqualTo("zh, en");
        assertThat(performerData.getLanguagePrimary()).isEqualTo("zh-tw");
        assertThat(performerData.getRegistrationDt()).isEqualTo("2023-03-01T01:08:30Z");

        Gson gson = GsonHelper.getGson();

        String json = gson.toJson(performerData);
        assertThat(json).isEqualTo("{" +
                "\"id\":1," +
                "\"name\":\"TestPerformer\"," +
                "\"is_logged_in\":true," +
                "\"is_temp\":false," +
                "\"session_id\":\"eeeeeeeeeeeeeeeeeeee\"," +
                "\"pageview_id\":\"eeeeeeeeeeeeeeeeeeee\"," +
                "\"groups\":[\"*\"]," +
                "\"language_groups\":\"zh, en\"," +
                "\"language_primary\":\"zh-tw\"," +
                "\"registration_dt\":\"2023-03-01T01:08:30Z\"" +
                "}");
    }

}
