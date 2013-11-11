package org.wikimedia.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;
import junit.framework.TestCase;
import org.wikimedia.wikipedia.Page;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.Section;
import org.wikimedia.wikipedia.Site;

import java.util.ArrayList;

public class ParcelableTest extends TestCase {
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
        for (int i = 2; i <= 10; i++) {
            parentSection.insertSection(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        parcelAndTestObjects(parentSection);
    }

    public void testPage() throws Exception {
        ArrayList<Section> sections = new ArrayList<Section>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        for (int i = 1; i <= 10; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i ));
        }
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        Page page = new Page(title, sections);
        parcelAndTestObjects(page);
    }

}
