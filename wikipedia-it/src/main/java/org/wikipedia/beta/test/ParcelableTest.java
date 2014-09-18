package org.wikipedia.beta.test;

import android.os.Parcel;
import android.os.Parcelable;
import junit.framework.TestCase;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.ParcelableLruCache;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.history.HistoryEntry;
import org.wikipedia.beta.page.Page;
import org.wikipedia.beta.page.PageProperties;
import org.wikipedia.beta.page.Section;
import org.wikipedia.beta.pageimages.PageImage;

import java.util.ArrayList;

public class ParcelableTest extends TestCase {

    private static final int NUM_SECTIONS = 10;

    private void parcelAndTestObjects(Parcelable p) throws Exception {
        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator creator = (Parcelable.Creator) p.getClass().getField("CREATOR").get(null);
        Parcelable newObject = (Parcelable) creator.createFromParcel(parcel);
        assertEquals(p, newObject);
    }

    public void testPageTitle() throws Exception {
        Site site = new Site("en.wikipedia.org");
        PageTitle origTitle = new PageTitle("Talk", "India", site);

        parcelAndTestObjects(origTitle);
    }

    public void testSite() throws Exception {
        Site site = new Site("en.wikipedia.org");
        parcelAndTestObjects(site);
    }

    public void testSection() throws Exception {
        Section parentSection = new Section(1, 1, null, null, "Hi there!");
        parcelAndTestObjects(parentSection);
    }

    public void testPage() throws Exception {
        ArrayList<Section> sections = new ArrayList<Section>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        for (int i = 1; i <= NUM_SECTIONS; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        PageProperties props = new PageProperties("", "Something", "autoconfirmed", false, false);
        Page page = new Page(title, sections, props);
        parcelAndTestObjects(page);
    }

    public void testLruCache() throws Exception {
        ParcelableLruCache<Site> oldCache = new ParcelableLruCache<Site>(2, Site.class);
        oldCache.put("english", new Site("en.wikipedia.org"));
        oldCache.put("tamil", new Site("ta.wikipedia.org"));

        Parcel parcel = Parcel.obtain();
        oldCache.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator creator = (Parcelable.Creator) oldCache.getClass().getField("CREATOR").get(null);
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
