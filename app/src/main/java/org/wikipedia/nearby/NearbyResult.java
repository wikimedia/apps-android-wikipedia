package org.wikipedia.nearby;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.mwapi.NearbyPage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class NearbyResult {
    @NonNull private final List<NearbyPage> pages = new ArrayList<>();

    @NonNull public List<NearbyPage> getList() {
        return pages;
    }

    public void clear() {
        pages.clear();
    }

    public synchronized void add(@NonNull List<NearbyPage> newPages) {
        final double epsilon = 0.00000001;
        String primaryLang = WikipediaApp.getInstance().language().getAppLanguageCode();

        for (NearbyPage newPage : newPages) {
            boolean exists = false;
            Iterator<NearbyPage> i = pages.iterator();
            while (i.hasNext()) {
                NearbyPage page = i.next();
                if (Math.abs(page.getLocation().getLatitude() - newPage.getLocation().getLatitude()) < epsilon
                        && Math.abs(page.getLocation().getLongitude() - newPage.getLocation().getLongitude()) < epsilon) {
                    if (newPage.getTitle().getWikiSite().languageCode().equals(primaryLang)) {
                        i.remove();
                    } else {
                        exists = true;
                    }
                    break;
                }
            }
            if (!exists) {
                pages.add(newPage);
            }
        }
    }
}
