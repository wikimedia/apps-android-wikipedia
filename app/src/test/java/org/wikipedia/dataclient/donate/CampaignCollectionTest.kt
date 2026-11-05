package org.wikipedia.dataclient.donate

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSettings
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.util.GeoUtil
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class CampaignCollectionTest {

    @get: Rule
    val errorCollector = ErrorCollector()

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
    fun testReplaceParamsForDateTimeVariables() {
        ShadowSettings.set24HourTimeFormat(true)
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.of(2025, 3, 19, 12, 19)

        val template = "%weekday% | %date% | %HH:MM%"
        val expected = mapOf(
            "en" to "Wednesday | Wednesday, March 19 | 12:19",
            "fr" to "mercredi | mercredi 19 mars | 12:19",
            "de" to "Mittwoch | Mittwoch, 19. März | 12:19",
            "nl" to "woensdag | woensdag 19 maart | 12:19",
            "ja" to "水曜日 | 3月19日水曜日 | 12:19",
            "zh-hans" to "星期三 | 3月19日星期三 | 12:19",
            "ar" to "الأربعاء | الأربعاء، 19 مارس | 12:19",
        )

        val campaignAsset: (String) -> Campaign.Assets = { textString ->
            Campaign.Assets(
                id = "123",
                weight = 1f,
                text = textString,
                footer = "",
                actions = arrayOf(
                    Campaign.Action(
                        title = "",
                        url = ""
                    )
                )
            )
        }
        expected.forEach {
            WikipediaApp.instance.languageState.setAppLanguageCodes(listOf(it.key))
            val replacedAssets = CampaignCollection.replaceAssetsParams(campaignAsset(template), "US_2025")
            errorCollector.checkThat(
                replacedAssets.text,
                equalTo(it.value)
            )
        }
    }
}
