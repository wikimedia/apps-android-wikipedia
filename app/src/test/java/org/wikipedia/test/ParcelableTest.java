package org.wikipedia.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.pageimages.PageImage;

@RunWith(RobolectricTestRunner.class) public class ParcelableTest {
    @Test public void testPageTitle() throws Throwable {
        PageTitle title = new PageTitle(null, "Test", WikiSite.forLanguageCode("en"));
        TestParcelUtil.test(title);
    }

    @Test public void testPageTitleTalk() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle origTitle = new PageTitle("Talk", "India", wiki);
        TestParcelUtil.test(origTitle);
    }

    @Test public void testPageProperties() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        PageProperties props = new PageProperties(title, false);
        TestParcelUtil.test(props);
    }

    @Test public void testPagePropertiesFromSummary() throws Throwable {
        String json = TestFileUtil.readRawFile("rb_page_summary_geo.json");
        PageSummary summary = GsonUnmarshaller.unmarshal(PageSummary.class, json);
        PageProperties props = new PageProperties(summary);
        TestParcelUtil.test(props);
    }

    @Test public void testHistoryEntry() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);

        TestParcelUtil.test(historyEntry);
    }

    @Test
    public void testPageImage() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        PageImage pageImage = new PageImage(title, "Testing image");

        TestParcelUtil.test(pageImage);
    }
}
