package org.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;


import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.pageimages.PageImage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(TestRunner.class) public class ParcelableTest {
    @Test public void testPageTitle() throws Throwable {
        PageTitle title = new PageTitle(null, "Test", WikiSite.forLanguageCode("en"));
        parcelAndTestObjects(title);
    }

    @Test public void testPageTitleTalk() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle origTitle = new PageTitle("Talk", "India", wiki);
        parcelAndTestObjects(origTitle);
    }

    @Test public void testWikiSite() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        parcelAndTestObjects(wiki);
    }

    @Test public void testPageProperties() throws Throwable {
        PageProperties props = new PageProperties(new JSONObject("{\"protection\":{\"edit\":[\"autoconfirmed\"],\"move\":[\"sysop\"]},\"id\":15580374,\"displaytitle\":\"Something\",\"revision\":615503846,\"lastmodified\":\"\",\"editable\":false,\"mainpage\":false}"));
        parcelAndTestObjects(props);
    }

    @Test public void testLruCache() throws Throwable {
        ParcelableLruCache<WikiSite> oldCache = new ParcelableLruCache<>(2, WikiSite.class);
        oldCache.put("english", WikiSite.forLanguageCode("en"));
        oldCache.put("tamil", WikiSite.forLanguageCode("ta"));

        Parcel parcel = Parcel.obtain();
        oldCache.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) oldCache.getClass().getField("CREATOR").get(null);
        //noinspection unchecked
        ParcelableLruCache<WikiSite> newCache = (ParcelableLruCache<WikiSite>) creator.createFromParcel(parcel);

        assertThat(newCache.maxSize(), is(oldCache.maxSize()));
        assertThat(newCache.size(), is(oldCache.size()));
        assertThat(newCache.get("english"), is(oldCache.get("english")));
        assertThat(newCache.get("tamil"), is(oldCache.get("tamil")));
    }

    @Test public void testHistoryEntry() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);

        parcelAndTestObjects(historyEntry);
    }

    @Test public void testPageImage() throws Throwable {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle("Talk", "India", wiki);
        PageImage pageImage = new PageImage(title, "Testing image");

        parcelAndTestObjects(pageImage);
    }

    private void parcelAndTestObjects(Parcelable p) throws Throwable {
        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) p.getClass().getField("CREATOR").get(null);
        Parcelable newObject = (Parcelable) creator.createFromParcel(parcel);
        assertThat(p, is(newObject));
    }
}
