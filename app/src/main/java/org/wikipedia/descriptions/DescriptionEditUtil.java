package org.wikipedia.descriptions;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;

public final class DescriptionEditUtil {
    static final String ABUSEFILTER_DISALLOWED = "abusefilter-disallowed";
    static final String ABUSEFILTER_WARNING = "abusefilter-warning";
    private static final String DESCRIPTION_SOURCE_LOCAL = "local";
    private static final String DESCRIPTION_SOURCE_WIKIDATA = "central";

    public static boolean isEditAllowed(@NonNull Page page) {
        PageProperties props = page.getPageProperties();
        if (page.getTitle().getWikiSite().languageCode().equals("en")) {
            // For English Wikipedia, allow editing the description for all articles, since the
            // edit will go directly into the article instead of Wikidata.
            return true;
        }
        return !TextUtils.isEmpty(props.getWikiBaseItem())
                && !DESCRIPTION_SOURCE_LOCAL.equals(props.getDescriptionSource());
    }

    private DescriptionEditUtil() {
    }
}
