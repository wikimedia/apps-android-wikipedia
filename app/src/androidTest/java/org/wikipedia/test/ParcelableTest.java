package org.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;
import junit.framework.TestCase;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.Site;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageProperties;
import org.wikipedia.pageimages.PageImage;

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
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        parcelAndTestObjects(title);
    }

    public void testPageTitleTalk() throws Exception {
        Site site = new Site("en.wikipedia.org");
        PageTitle origTitle = new PageTitle("Talk", "India", site);
        parcelAndTestObjects(origTitle);
    }

    public void testSite() throws Exception {
        Site site = new Site("en.wikipedia.org");
        parcelAndTestObjects(site);
    }

    public void testPageProperties() throws Exception {
        PageProperties props = new PageProperties(new JSONObject("{\"protection\":{\"edit\":[\"autoconfirmed\"],\"move\":[\"sysop\"]},\"id\":15580374,\"displaytitle\":\"Something\",\"revision\":615503846,\"lastmodified\":\"\",\"editable\":false,\"mainpage\":false}"));
        parcelAndTestObjects(props);
    }

    public void testLruCache() throws Exception {
        ParcelableLruCache<Site> oldCache = new ParcelableLruCache<>(2, Site.class);
        oldCache.put("english", new Site("en.wikipedia.org"));
        oldCache.put("tamil", new Site("ta.wikipedia.org"));

        Parcel parcel = Parcel.obtain();
        oldCache.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) oldCache.getClass().getField("CREATOR").get(null);
        //noinspection unchecked
        ParcelableLruCache<Site> newCache = (ParcelableLruCache<Site>) creator.createFromParcel(parcel);

        assertEquals(newCache.maxSize(), oldCache.maxSize());
        assertEquals(newCache.size(), oldCache.size());
        assertEquals(newCache.get("english"), oldCache.get("english"));
        assertEquals(newCache.get("tamil"), oldCache.get("tamil"));
    }

    public void testHistoryEntry() throws Exception {
        Site site = new Site("en.wikipedia.org");
        PageTitle title = new PageTitle("Talk", "India", site);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);

        parcelAndTestObjects(historyEntry);
    }

    public void testPageImage() throws Exception {
        Site site = new Site("en.wikipedia.org");
        PageTitle title = new PageTitle("Talk", "India", site);
        PageImage pageImage = new PageImage(title, "Testing image");

        parcelAndTestObjects(pageImage);
    }
}
