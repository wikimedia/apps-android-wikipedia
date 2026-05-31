package org.wikimedia.testkitchen.context

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import java.time.Instant

object DataFixtures {
    fun getTestInstrument(): InstrumentImpl {
        return InstrumentImpl("test-instrument")
    }

    fun getTestClientData(agentData: AgentData = testAgentData): ClientData {
        return ClientData(
            agentData,
            testPageData,
            testMediawikiData,
            testPerformerData
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
            elementFriendlyName = "TestElementFriendlyName"
        )
    }

    fun getTestStream(streamNameFragment: String): String {
        return "mediawiki.metrics_platform.$streamNameFragment"
    }
}
