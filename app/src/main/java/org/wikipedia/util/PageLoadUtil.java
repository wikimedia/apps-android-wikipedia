package org.wikipedia.util;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.server.PageService;
import org.wikipedia.server.PageServiceFactory;

public final class PageLoadUtil {

    @NonNull
    public static PageService getApiService(Site site) {
        return PageServiceFactory.create(site);
    }

    private PageLoadUtil() { }
}
