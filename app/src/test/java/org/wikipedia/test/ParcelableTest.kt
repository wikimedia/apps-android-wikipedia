package org.wikipedia.test

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
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
    fun testPageImage() {
        val wiki = WikiSite.forLanguageCode("en")
        val title = PageTitle("Talk", "India", wiki)
        val pageImage = PageImage(title, "Testing image", "Test description", 1.2, 3.4)
        TestParcelUtil.test(pageImage)
    }
}
