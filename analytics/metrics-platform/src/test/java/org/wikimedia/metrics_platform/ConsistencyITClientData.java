package org.wikimedia.metrics_platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.wikimedia.metrics_platform.context.AgentData;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.MediawikiData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConsistencyITClientData extends ClientData {
    public JsonObject agentJson;
    public JsonObject pageJson;
    public JsonObject mediawikiJson;
    public JsonObject performerJson;

    public ConsistencyITClientData(
            JsonObject agent,
            JsonObject page,
            JsonObject mediawiki,
            JsonObject performer,
            String domain
    ) {
        this.agentJson = agent;
        this.pageJson = page;
        this.mediawikiJson = mediawiki;
        this.performerJson = performer;

        AgentData agentData = AgentData.builder()
                .appInstallId(this.agentJson.get("app_install_id").getAsString())
                .clientPlatform(this.agentJson.get("client_platform").getAsString())
                .clientPlatformFamily(this.agentJson.get("client_platform_family").getAsString())
                .deviceFamily(this.agentJson.get("device_family").getAsString())
                .build();

        PageData pageData = PageData.builder()
                .id(this.pageJson.get("id").getAsInt())
                .title(this.pageJson.get("title").getAsString())
                .namespaceId(this.pageJson.get("namespace_id").getAsInt())
                .namespaceName(this.pageJson.get("namespace_name").getAsString())
                .revisionId(this.pageJson.get("revision_id").getAsLong())
                .wikidataItemQid(this.pageJson.get("wikidata_qid").getAsString())
                .contentLanguage(this.pageJson.get("content_language").getAsString())
                .build();
        MediawikiData mediawikiData = MediawikiData.builder()
                .database(this.mediawikiJson.get("database").getAsString())
                .build();
        PerformerData performerData = PerformerData.builder()
                .id(this.performerJson.get("id").getAsInt())
                .isLoggedIn(this.performerJson.get("is_logged_in").getAsBoolean())
                .sessionId(this.performerJson.get("session_id").getAsString())
                .pageviewId(this.performerJson.get("pageview_id").getAsString())
                .groups(Collections.singleton(this.performerJson.get("groups").getAsString()))
                .languagePrimary(this.performerJson.get("language_primary").getAsString())
                .languageGroups(this.performerJson.get("language_groups").getAsString())
                .build();

        this.setAgentData(agentData);
        this.setPageData(pageData);
        this.setMediawikiData(mediawikiData);
        this.setPerformerData(performerData);
        this.setDomain(domain);
    }

    public static ConsistencyITClientData createConsistencyTestClientData() {
        try {
            JsonObject data = getIntegrationData();
            JsonObject agent = data.getAsJsonObject("agent");
            JsonObject page = data.getAsJsonObject("page");
            JsonObject mediawiki = data.getAsJsonObject("mediawiki");
            JsonObject performer = data.getAsJsonObject("performer");
            String domain = data.get("hostname").getAsString();

            return new ConsistencyITClientData(
                    agent,
                    page,
                    mediawiki,
                    performer,
                    domain
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject getIntegrationData() throws IOException {
        Path pathIntegration = Paths.get("../tests/consistency/integration_data_apps.json");
        try (BufferedReader reader = Files.newBufferedReader(pathIntegration)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            return jsonElement.getAsJsonObject();
        }
    }
}
