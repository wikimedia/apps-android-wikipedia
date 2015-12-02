package org.wikipedia.util;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.server.PageService;
import org.wikipedia.server.ContentServiceFactory;

public final class PageLoadUtil {

    @NonNull
    public static PageService getApiService(Site site) {
        return ContentServiceFactory.create(site);
    }

    private PageLoadUtil() { }
}
