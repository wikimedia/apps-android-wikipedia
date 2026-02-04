package org.wikipedia.dataclient.donate

import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.util.GeoUtil

@RunWith(RobolectricTestRunner::class)
class CampaignCollectionTest {
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
                    url = "http://example.com?wmf_source=\$formattedId;&country=\$country;&lang=\$language;"
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
        assertEquals(replacedAssets.actions[0].url, "http://example.com?wmf_source=enUS_US_2025_Android&country=US&lang=en")
    }
}
