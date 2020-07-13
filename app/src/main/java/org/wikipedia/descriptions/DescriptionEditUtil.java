package org.wikipedia.descriptions;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;

public final class DescriptionEditUtil {
    static final String ABUSEFILTER_DISALLOWED = "abusefilter-disallowed";
    static final String ABUSEFILTER_WARNING = "abusefilter-warning";
    private static final String DESCRIPTION_SOURCE_LOCAL = "local";
    private static final String DESCRIPTION_SOURCE_WIKIDATA = "central";

    public static boolean isEditAllowed(@NonNull Page page) {
        PageProperties props = page.getPageProperties();
        return !TextUtils.isEmpty(props.getWikiBaseItem())
                && !DESCRIPTION_SOURCE_LOCAL.equals(props.getDescriptionSource());
    }

    public static boolean usesLocalDescriptions(@NonNull WikiSite wiki) {
        return wiki.languageCode().equals("en");
    }

    public static boolean usesLocalDescriptions(@NonNull PageSummary summary) {
        return summary.getLang().equals("en");
    }

    private DescriptionEditUtil() {
    }
}
