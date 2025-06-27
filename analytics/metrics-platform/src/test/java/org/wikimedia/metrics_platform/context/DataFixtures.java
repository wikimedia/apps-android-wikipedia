package org.wikimedia.metrics_platform.context;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.wikimedia.metrics_platform.json.GsonHelper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class DataFixtures {

    private DataFixtures() {
        // Utility class, should never be instantiated
    }

    public static ClientData getTestClientData() {
        return new ClientData(
                getTestAgentData(),
                getTestPageData(),
                getTestMediawikiData(),
                getTestPerformerData(),
                "en.wikipedia.org"
        );
    }

    public static ClientData getTestClientData(String expectedEvent) {
        Map<String, Object> dataMap = new HashMap<>();

        JsonElement jsonElement = JsonParser.parseString(expectedEvent);
        JsonObject expectedEventJson = jsonElement.isJsonArray() ? jsonElement.getAsJsonArray().get(0).getAsJsonObject() : jsonElement.getAsJsonObject();

        Set<String> dataObjectNames = Stream.of("agent", "page", "mediawiki", "performer")
                .collect(Collectors.toCollection(HashSet::new));

        for (String dataObjectName : dataObjectNames) {
            JsonObject metaData = expectedEventJson.getAsJsonObject(dataObjectName);
            Set<String> keys = metaData.keySet();
            Map<String, Object> dataMapEach = new HashMap<>();
            for (String key : keys) {
                dataMapEach.put(key, metaData.get(key));
            }
            dataMap.put(dataObjectName, dataMapEach);
        }

        String domain = expectedEventJson.get("meta").getAsJsonObject().get("domain").getAsString();
        dataMap.put("domain", domain);

        Gson gson = GsonHelper.getGson();
        JsonElement jsonClientData = gson.toJsonTree(dataMap);
        return gson.fromJson(jsonClientData, ClientData.class);
    }

    public static AgentData getTestAgentData() {
        return AgentData.builder()
                .appInstallId("ffffffff-ffff-ffff-ffff-ffffffffffff")
                .clientPlatform("android")
                .clientPlatformFamily("app")
                .appFlavor("devdebug")
                .appVersion(982734)
                .appVersionName("2.7.50470-dev-2024-02-14")
                .appTheme("LIGHT")
                .deviceFamily("Samsung SM-G960F")
                .deviceLanguage("en")
                .releaseStatus("dev")
                .build();
    }

    public static PageData getTestPageData() {
        return PageData.builder()
                .id(1)
                .title("Test Page Title")
                .namespaceId(0)
                .namespaceName("Main")
                .revisionId(1L)
                .wikidataItemQid("Q123456")
                .contentLanguage("en")
                .build();
    }

    public static MediawikiData getTestMediawikiData() {
        return MediawikiData.builder()
                .database("enwiki")
                .build();
    }

    public static PerformerData getTestPerformerData() {
        return PerformerData.builder()
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
    }

    public static InteractionData getTestInteractionData(String action) {
        return InteractionData.builder()
                .action(action)
                .actionSubtype("TestActionSubtype")
                .actionSource("TestActionSource")
                .actionContext("TestActionContext")
                .elementId("TestElementId")
                .elementFriendlyName("TestElementFriendlyName")
                .funnelEntryToken("TestFunnelEntryToken")
                .funnelEventSequencePosition(8)
                .build();
    }

    public static Map<String, Object> getTestCustomData() {
        Map<String, Object> customData = new HashMap<String, Object>();
        customData.put("font_size", "small");
        customData.put("is_full_width", true);
        customData.put("screen_size", 1080);
        return customData;
    }

    public static String getTestStream(String streamNameFragment) {
        return "mediawiki.metrics_platform." + streamNameFragment;
    }
}
