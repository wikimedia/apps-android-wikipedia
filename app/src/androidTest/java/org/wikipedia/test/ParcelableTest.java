package org.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.filters.SmallTest;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageProperties;
import org.wikipedia.pageimages.PageImage;

@SmallTest
public class ParcelableTest extends TestCase {

    private void parcelAndTestObjects(Parcelable p) throws Exception {
        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) p.getClass().getField("CREATOR").get(null);
        Parcelable newObject = (Parcelable) creator.createFromParcel(parcel);
        assertEquals(p, newObject);
    }

    public void testPageTitle() throws Exception {
        PageTitle title = new PageTitle(null, "Test", WikiSite.forLanguageCode("en"));
        parcelAndTestObjects(title);
    }

    public void testPageTitleTalk() throws Exception {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle origTitle = new PageTitle("Talk", "India", wiki);
        parcelAndTestObjects(origTitle);
    }

    public void testWikiSite() throws Exception {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        parcelAndTestObjects(wiki);
    }

    public void testPageProperties() throws Exception {
        PageProperties props = new PageProperties(new JSONObject("{\"protection\":{\"edit\":[\"autoconfirmed\"],\"move\":[\"sysop\"]},\"id\":15580374,\"displaytitle\":\"Something\",\"revision\":615503846,\"lastmodified\":\"\",\"editable\":false,\"mainpage\":false}"));
        parcelAndTestObjects(props);
    }

    public void testLruCache() throws Exception {
        ParcelableLruCache<WikiSite> oldCache = new ParcelableLruCache<>(2, WikiSite.class);
        oldCache.put("english", WikiSite.forLanguageCode("en"));
        oldCache.put("tamil", WikiSite.forLanguageCode("ta"));

        Parcel parcel = Parcel.obtain();
        oldCache.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) oldCache.getClass().getField("CREATOR").get(null);
        //noinspection unchecked
        ParcelableLruCache<WikiSite> newCache = (ParcelableLruCache<WikiSite>) creator.createFromParcel(parcel);

        assertEquals(newCache.maxSize(), oldCache.maxSize());
        assertEquals(newCache.size(), oldCache.size());
        assertEquals(newCache.get("english"), oldCache.get("english"));
        assertEquals(newCache.get("tamil"), oldCache.get("tamil"));
    }

    public void testHistoryEntry() throws Exception {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);

        parcelAndTestObjects(historyEntry);
    }

    public void testPageImage() throws Exception {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        PageImage pageImage = new PageImage(title, "Testing image");

        parcelAndTestObjects(pageImage);
    }
}
