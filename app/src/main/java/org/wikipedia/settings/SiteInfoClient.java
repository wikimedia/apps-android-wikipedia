package org.wikipedia.settings;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.staticdata.MainPageNameData;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class SiteInfoClient {
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

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull MwQueryResponse results);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);
    @Nullable private Call<MwQueryResponse> call;

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, @Nullable Callback cb) {
        return request(cachedService.service(wiki), wiki, cb);
    }

    @VisibleForTesting
    Call<MwQueryResponse> request(@NonNull Service service, @NonNull WikiSite site, @Nullable final Callback cb) {
        call = service.request();
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                if (response.body() != null && response.body().success() && response.body().query().siteInfo() != null) {
                    // noinspection ConstantConditions
                    SITE_INFO_MAP.put(site.languageCode(), response.body().query().siteInfo());
                    if (cb != null) {
                        cb.success(call, response.body());
                    }
                } else {
                    if (cb != null) {
                        cb.failure(call, new RuntimeException("Incorrect response format."));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                if (call.isCanceled()) {
                    return;
                }
                if (cb != null) {
                    cb.failure(call, caught);
                }
            }
        });
        return call;
    }

    void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&&format=json&formatversion=2&meta=siteinfo")
        @NonNull Call<MwQueryResponse> request();
    }
}
