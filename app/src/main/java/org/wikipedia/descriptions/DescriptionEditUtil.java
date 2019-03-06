package org.wikipedia.descriptions;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.util.ReleaseUtil;

import java.util.Arrays;

public final class DescriptionEditUtil {
    static final String ABUSEFILTER_DISALLOWED = "abusefilter-disallowed";
    static final String ABUSEFILTER_WARNING = "abusefilter-warning";
    private static final String DESCRIPTION_SOURCE_LOCAL = "local";
    private static final String DESCRIPTION_SOURCE_WIKIDATA = "central";

    public static boolean isEditAllowed(@NonNull Page page) {
        PageProperties props = page.getPageProperties();
        return !TextUtils.isEmpty(props.getWikiBaseItem())
                && !DESCRIPTION_SOURCE_LOCAL.equals(props.getDescriptionSource())
                && (!isLanguageBlacklisted(page.getTitle().getWikiSite().languageCode())
                || ReleaseUtil.isPreBetaRelease());
    }

    private static boolean isLanguageBlacklisted(@NonNull String lang) {
        JSONArray blacklist = WikipediaApp.getInstance().getRemoteConfig().getConfig()
                .optJSONArray("descriptionEditLangBlacklist");
        if (blacklist != null) {
            for (int i = 0; i < blacklist.length(); i++) {
                if (lang.equals(blacklist.optString(i))) {
                    return true;
                }
            }
            return false;
        } else {
            return Arrays.asList("en")
                    .contains(lang);
        }
    }

    private DescriptionEditUtil() {
    }
}
