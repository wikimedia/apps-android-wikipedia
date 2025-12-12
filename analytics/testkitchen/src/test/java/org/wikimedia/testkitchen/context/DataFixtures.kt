package org.wikimedia.testkitchen.context

import java.time.Instant

object DataFixtures {
    /*
    fun getTestClientData(expectedEvent: String?): ClientData {
        val dataMap: MutableMap<String?, Any?> = HashMap<String?, Any?>()

        val jsonElement: JsonElement = JsonParser.parseString(expectedEvent)
        val expectedEventJson: JsonObject =
            if (jsonElement.isJsonArray()) jsonElement.getAsJsonArray().get(0)
                .getAsJsonObject() else jsonElement.getAsJsonObject()

        val dataObjectNames: MutableSet<String?> =
            Stream.of<String?>("agent", "page", "mediawiki", "performer")
                .collect(Collectors.toCollection(Supplier { HashSet() }))

        for (dataObjectName in dataObjectNames) {
            val metaData: JsonObject = expectedEventJson.getAsJsonObject(dataObjectName)
            val keys: MutableSet<String?> = metaData.keySet()
            val dataMapEach: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            for (key in keys) {
                dataMapEach.put(key, metaData.get(key))
            }
            dataMap.put(dataObjectName, dataMapEach)
        }

        val domain: String? =
            expectedEventJson.get("meta").getAsJsonObject().get("domain").getAsString()
        dataMap.put("domain", domain)

        val gson: Gson = GsonHelper.getGson()
        val jsonClientData: JsonElement? = gson.toJsonTree(dataMap)
        return gson.fromJson(jsonClientData, ClientData::class.java)
    }
*/

    fun getTestClientData(agentData: AgentData = testAgentData): ClientData {
        return ClientData(
            agentData,
            testPageData,
            testMediawikiData,
            testPerformerData,
            "en.wikipedia.org"
        )
    }

    val testAgentData get() = AgentData(
        appInstallId = "ffffffff-ffff-ffff-ffff-ffffffffffff",
        clientPlatform = "android",
        clientPlatformFamily = "app",
        appFlavor = "devdebug",
        appVersion = 982734,
        appVersionName = "2.7.50470-dev-2024-02-14",
        appTheme = "LIGHT",
        deviceFamily = "Samsung SM-G960F",
        deviceLanguage = "en",
        releaseStatus = "dev"
    )

    val testPageData get() = PageData(
        id = 123,
        title = "Test Page Title",
        namespaceId = 0,
        namespaceName = "Main",
        revisionId = 321L,
        wikidataItemQid = "Q123456",
        contentLanguage = "en"
    )

    val testMediawikiData get() = MediawikiData(
        database = "enwiki"
    )

    val testPerformerData: PerformerData
        get() = PerformerData(
            id = 1,
            name = "TestPerformer",
            isLoggedIn = true,
            isTemp = false,
            sessionId = "eeeeeeeeeeeeeeeeeeee",
            pageviewId = "ffffffffffffffffffff",
            groups = listOf("*"),
            languageGroups = "zh, en",
            languagePrimary = "zh-tw",
            registrationDt = Instant.parse("2023-03-01T01:08:30Z")
        )

    fun getTestInteractionData(action: String?): InteractionData {
        return InteractionData(
            action = action,
            actionSubtype = "TestActionSubtype",
            actionSource = "TestActionSource",
            actionContext = "TestActionContext",
            elementId = "TestElementId",
            elementFriendlyName = "TestElementFriendlyName",
            funnelEntryToken = "TestFunnelEntryToken",
            funnelEventSequencePosition = 8
        )
    }

    fun getTestStream(streamNameFragment: String): String {
        return "mediawiki.metrics_platform.$streamNameFragment"
    }
}
