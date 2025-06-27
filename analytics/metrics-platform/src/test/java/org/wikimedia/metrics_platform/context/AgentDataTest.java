package org.wikimedia.metrics_platform.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;

public class AgentDataTest {
    @Test
    void testAgentData() {
        AgentData agentData = AgentData.builder()
                .appFlavor("flamingo")
                .appInstallId("ffffffff-ffff-ffff-ffff-ffffffffffff")
                .appTheme("giraffe")
                .appVersion(123456789)
                .appVersionName("2.7.50470-dev-2024-02-14")
                .clientPlatform("android")
                .clientPlatformFamily("app")
                .deviceFamily("Samsung SM-G960F")
                .deviceLanguage("en")
                .releaseStatus("beta")
                .build();

        assertThat(agentData.getAppFlavor()).isEqualTo("flamingo");
        assertThat(agentData.getAppInstallId()).isEqualTo("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThat(agentData.getAppTheme()).isEqualTo("giraffe");
        assertThat(agentData.getAppVersion()).isEqualTo(123456789);
        assertThat(agentData.getAppVersionName()).isEqualTo("2.7.50470-dev-2024-02-14");
        assertThat(agentData.getClientPlatform()).isEqualTo("android");
        assertThat(agentData.getClientPlatformFamily()).isEqualTo("app");
        assertThat(agentData.getDeviceFamily()).isEqualTo("Samsung SM-G960F");
        assertThat(agentData.getDeviceLanguage()).isEqualTo("en");
        assertThat(agentData.getReleaseStatus()).isEqualTo("beta");

        Gson gson = GsonHelper.getGson();
        String json = gson.toJson(agentData);
        assertThat(json).isEqualTo("{" +
                "\"app_flavor\":\"flamingo\"," +
                "\"app_install_id\":\"ffffffff-ffff-ffff-ffff-ffffffffffff\"," +
                "\"app_theme\":\"giraffe\"," +
                "\"app_version\":123456789," +
                "\"app_version_name\":\"2.7.50470-dev-2024-02-14\"," +
                "\"client_platform\":\"android\"," +
                "\"client_platform_family\":\"app\"," +
                "\"device_family\":\"Samsung SM-G960F\"," +
                "\"device_language\":\"en\"," +
                "\"release_status\":\"beta\"" +
                "}");
    }
}
