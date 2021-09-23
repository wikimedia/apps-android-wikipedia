package org.wikipedia.test

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.PageProperties
import org.wikipedia.page.PageTitle
import org.wikipedia.pageimages.db.PageImage

@RunWith(RobolectricTestRunner::class)
class ParcelableTest {
    @Test
    @Throws(Throwable::class)
    fun testPageTitle() {
        val title = PageTitle(null, "Test", WikiSite.forLanguageCode("en"))
        TestParcelUtil.test(title)
    }

    @Test
    @Throws(Throwable::class)
    fun testPageTitleTalk() {
        val wiki = WikiSite.forLanguageCode("en")
        val origTitle = PageTitle("Talk", "India", wiki)
        TestParcelUtil.test(origTitle)
    }

    @Test
    @Throws(Throwable::class)
    fun testPageProperties() {
        val wiki = WikiSite.forLanguageCode("en")
        val title = PageTitle("Talk", "India", wiki)
        val props = PageProperties(title, false)
        TestParcelUtil.test(props)
    }

    @Test
    @Throws(Throwable::class)
    fun testPagePropertiesFromSummary() {
        val json = TestFileUtil.readRawFile("rb_page_summary_geo.json")
        val summary = GsonUnmarshaller.unmarshal(PageSummary::class.java, json)
        val props = PageProperties(summary)
        TestParcelUtil.test(props)
    }

    @Test
    @Throws(Throwable::class)
    fun testPageImage() {
        val wiki = WikiSite.forLanguageCode("en")
        val title = PageTitle("Talk", "India", wiki)
        val pageImage = PageImage(title, "Testing image")
        TestParcelUtil.test(pageImage)
    }
}
