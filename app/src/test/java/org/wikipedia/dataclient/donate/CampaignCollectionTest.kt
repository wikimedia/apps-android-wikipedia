package org.wikipedia.dataclient.donate

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil
import org.wikipedia.util.GeoUtil
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class CampaignCollectionTest {

    @get:Rule
    val errorCollector = ErrorCollector()

    @Before
    fun setUp() {
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.of(2025, 3, 19, 12, 19)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createCampaignAssets(text: String): Campaign.Assets {
        return Campaign.Assets(
            id = "123",
            weight = 1f,
            text = text,
            footer = "",
            actions = arrayOf(
                Campaign.Action(
                    title = "",
                    url = ""
                )
            )
        )
    }

    private fun loadCampaignsFromJson(version: Int): List<Campaign> {
        val json = TestFileUtil.readRawFile("campaign_config_multi_version.json")
        return JsonUtil.decodeFromString<List<JsonElement>>(json).orEmpty()
            .filter { JsonUtil.json.decodeFromJsonElement<CampaignCollection.CampaignProto>(it).version == version }
            .map { JsonUtil.json.decodeFromJsonElement<Campaign>(it) }
    }

    @Test
    fun testReplaceAssetsNoParams() {
        val assets = Campaign.Assets(
            id = "123",
            weight = 1f,
            text = "Lorem ipsum",
            footer = "dolor sit amet",
            actions = arrayOf(
                Campaign.Action(
                    title = "Foo",
                    url = "http://example.com"
                )
            )
        )
        val replacedAssets = CampaignCollection.replaceAssetsParams(assets, "US_2025")

        assertEquals(replacedAssets.id, "123")
        assertEquals(replacedAssets.weight, 1f)
        assertEquals(replacedAssets.text, "Lorem ipsum")
        assertEquals(replacedAssets.footer, "dolor sit amet")
        assertEquals(replacedAssets.actions.size, 1)
        assertEquals(replacedAssets.actions[0].title, "Foo")
        assertEquals(replacedAssets.actions[0].url, "http://example.com")
    }

    @Test
    fun testReplaceAssetsWithParams() {

        mockkObject(GeoUtil)
        every { GeoUtil.geoIPCountry } returns "US"

        val assets = Campaign.Assets(
            id = "123",
            weight = 1f,
            text = "Lorem ipsum \$platform;",
            footer = "dolor sit \$language; amet",
            actions = arrayOf(
                Campaign.Action(
                    title = "Foo",
                    url = "http://example.com?wmf_source=\$formattedId;&country=\$country;&lang=\$language;&appVersion=\$appVersion;&appInstallId=\$appInstallId;"
                )
            )
        )
        val replacedAssets = CampaignCollection.replaceAssetsParams(assets, "US_2025")

        assertEquals(replacedAssets.id, "123")
        assertEquals(replacedAssets.weight, 1f)
        assertEquals(replacedAssets.text, "Lorem ipsum Android")
        assertEquals(replacedAssets.footer, "dolor sit en amet")
        assertEquals(replacedAssets.actions.size, 1)
        assertEquals(replacedAssets.actions[0].title, "Foo")
        assertEquals(replacedAssets.actions[0].url, "http://example.com?wmf_source=enUS_US_2025_Android&country=US&lang=en&appVersion=${BuildConfig.VERSION_NAME}&appInstallId=${WikipediaApp.instance.appInstallID}")
    }

    @Test
    fun testReplaceWeekdayParam() {
        val cases = mapOf(
            "en" to Pair("This %weekday%, we ask you", "This Wednesday, we ask you"),
            "fr" to Pair("Ce %weekday%, nous vous demandons", "Ce mercredi, nous vous demandons"),
            "de" to Pair("Diesen %weekday% bitten wir Sie", "Diesen Mittwoch bitten wir Sie"),
            "nl" to Pair("Deze %weekday% vragen we u", "Deze woensdag vragen we u"),
            "ja" to Pair("この%weekday%、皆様にお願いします", "この水曜日、皆様にお願いします"),
            "zh-hans" to Pair("这个%weekday%，我们请求您", "这个星期三，我们请求您"),
            "ar" to Pair("هذا %weekday%، نطلب منكم", "هذا الأربعاء، نطلب منكم"),
        )

        cases.forEach { (wikiLanguageCode, case) ->
            val (testString, expected) = case
            WikipediaApp.instance.languageState.setAppLanguageCodes(listOf(wikiLanguageCode))
            val replacedAssets = CampaignCollection.replaceAssetsParams(createCampaignAssets(testString), "US_2025")
            errorCollector.checkThat(
                "lang=$wikiLanguageCode",
                replacedAssets.text,
                equalTo(expected)
            )
        }
    }

    @Test
    fun testReplaceDateParam() {
        val assets = createCampaignAssets("It's %date%, and our fundraiser will soon be over.")

        val replacedAssets = CampaignCollection.replaceAssetsParams(assets, "US_2025")
        assertEquals("It's Wednesday, March 19, and our fundraiser will soon be over.", replacedAssets.text)
    }

    @Test
    fun testReplaceTimeParam24Hour() {
        ShadowSettings.set24HourTimeFormat(true)
        val assets = createCampaignAssets("It's %HH:MM%: Please don't scroll away.")

        val replacedAssets = CampaignCollection.replaceAssetsParams(assets, "US_2025")
        assertEquals("It's 12:19: Please don't scroll away.", replacedAssets.text)
    }

    @Test
    fun testReplaceTimeParam12Hour() {
        ShadowSettings.set24HourTimeFormat(false)
        val assets = createCampaignAssets("It's %HH:MM%: Please don't scroll away.")

        val text = CampaignCollection.replaceAssetsParams(assets, "US_2025").text.orEmpty()
        assertTrue("Missing time in: $text", text.contains("12:19"))
        assertTrue("Missing PM marker in: $text", text.contains("PM"))
    }

    @Test
    fun testReplaceParamsFromMultiVersionConfig() {
        ShadowSettings.set24HourTimeFormat(true)

        val campaigns = loadCampaignsFromJson(version = 4)
        assertEquals(2, campaigns.size)

        val campaign = campaigns.first()
        val assets = campaign.getAssetsForLang("en")!!
        val text = CampaignCollection.replaceAssetsParams(assets, campaign.getIdForLang("en")).text.orEmpty()

        assertEquals(
            "<b>The internet we were promised.</b><br><i>An important update for readers</i><br><br>It's 12:19: Please don't skip this 1-minute read. It's Wednesday, March 19, and our fundraiser will soon be over, but we're short of our goal. We hope that Wikipedia has given you at least €2.50 of knowledge this year. If just 2% of our most loyal readers gave €2.50, we'd hit our goal in a few hours.<br><br>The internet we were promised—a place of free, collaborative, accessible knowledge—is under constant threat. Wikipedia volunteers create and verify the pages you rely on, supported by tools that undo vandalism within minutes, ensuring the information you seek is trustworthy.<br><br>Most readers don’t donate, so your gift matters. This Wednesday, we ask you to donate, whether it's €2.50 one time or monthly. Thanks.",
            text
        )
    }

    @Test
    fun testReplaceWeekdayParamWithNonBcp47WikiLanguageCodes() {
        val cases = mapOf(
            "als" to "This Mittwuch, we ask you",
            "simple" to "This Wednesday, we ask you",
            "sr-el" to "This sreda, we ask you",
            "map-bms" to "This Rabu, we ask you"
        )

        cases.forEach { (wikiLanguageCode, expected) ->
            WikipediaApp.instance.languageState.setAppLanguageCodes(listOf(wikiLanguageCode))
            val replacedAssets = CampaignCollection.replaceAssetsParams(createCampaignAssets("This %weekday%, we ask you"), "US_2025")
            errorCollector.checkThat("lang=$wikiLanguageCode", replacedAssets.text, equalTo(expected))
        }
    }
}
