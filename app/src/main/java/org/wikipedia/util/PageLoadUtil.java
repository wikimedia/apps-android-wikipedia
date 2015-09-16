package org.wikipedia.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageServiceFactory;

public final class PageLoadUtil {

    @NonNull
    public static PageService getApiService(Site site) {
        return PageServiceFactory.create(site);
    }

    // TODO: use getResources().getDimensionPixelSize()?  Define leadImageWidth with px, not dp?
    public static int calculateLeadImageWidth() {
        Resources res = WikipediaApp.getInstance().getResources();
        return (int) (res.getDimension(R.dimen.leadImageWidth) / res.getDisplayMetrics().density);
    }

    private PageLoadUtil() { }
}
