package org.wikipedia.settings;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.log.L;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public final class SiteInfoClient {
    private static Map<String, SiteInfo> SITE_INFO_MAP = new HashMap<>();

    @NonNull public static String getMainPageForLang(@NonNull String lang) {
        SiteInfo info = getSiteInfoForLang(lang);
        if (info != null && !TextUtils.isEmpty(info.mainPage())) {
            return info.mainPage();
        }
        return MainPageNameData.valueFor(lang);
    }

    public static int getMaxPagesPerReadingList() {
        SiteInfo info = getSiteInfoForLang(WikipediaApp.getInstance().getWikiSite().languageCode());
        if (info != null && info.readingListsConfig() != null
                && info.readingListsConfig().maxEntriesPerList() > 0) {
            return info.readingListsConfig().maxEntriesPerList();
        }
        return Constants.MAX_READING_LIST_ARTICLE_LIMIT;
    }


    @Nullable private static SiteInfo getSiteInfoForLang(@NonNull String lang) {
        if (SITE_INFO_MAP.containsKey(lang)) {
            return SITE_INFO_MAP.get(lang);
        }
        return null;
    }

    @SuppressLint("CheckResult")
    public static void updateFor(@NonNull WikiSite wiki) {
        if (SITE_INFO_MAP.containsKey(wiki.languageCode())) {
            return;
        }

        ServiceFactory.get(wiki).getSiteInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> SITE_INFO_MAP.put(wiki.languageCode(), response.query().siteInfo()),
                        L::e);
    }

    private SiteInfoClient() { }
}
