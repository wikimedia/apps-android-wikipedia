package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedConfigureFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppFeedConfigure";
    private static final int REV_ID = 18126175;

    private final int source;

    public FeedConfigureFunnel(WikipediaApp app, WikiSite wiki, int source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.source = source;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void done(List<FeedContentType> orderedContentTypes) {
        List<Integer> enabledList;
        List<Integer> orderedList = new ArrayList<>();
        Map<String, List<Integer>> enabledMap = new HashMap<>();
        for (String language : getApp().language().getAppLanguageCodes()) {
            enabledList = new ArrayList<>();
            for (FeedContentType type : FeedContentType.values()) {
                enabledList.add(type.isEnabled() ? 1 : 0);
            }
            enabledMap.put(language, enabledList);
        }

        for (FeedContentType type : orderedContentTypes) {
            orderedList.add(type.code());
        }
        log(
                "source", source,
                "enabled_list", StringUtil.stringToListMapToJSONString(enabledMap),
                "order_list", StringUtil.listToJSONString(orderedList),
                "languages", StringUtil.listToJsonArrayString(getApp().language().getAppLanguageCodes())
        );
    }
}
