package org.wikipedia.wikidata;

import org.wikipedia.ParcelableLruCache;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikidataCache {
    private static final int MAX_CACHE_SIZE_DESCRIPTIONS = 96;

    private WikipediaApp app;
    private ParcelableLruCache<String> descriptionCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_DESCRIPTIONS, String.class);

    public WikidataCache(WikipediaApp app) {
        this.app = app;
    }

    public void clear() {
        descriptionCache.evictAll();
    }

    public void put(String key, String value) {
        descriptionCache.put(key, value);
    }

    public String get(String id) {
        return descriptionCache.get(id);
    }

    public void get(String id, OnWikidataReceiveListener listener) {
        get(id, app.getPrimaryLanguage(), listener);
    }

    public void get(String id, String language, OnWikidataReceiveListener listener) {
        List<String> idList = new ArrayList<String>();
        idList.add(id);
        get(idList, language, listener);
    }
    public void get(List<String> ids, final OnWikidataReceiveListener listener) {
        get(ids, app.getPrimaryLanguage(), listener);
    }

    public void get(List<String> ids, final String language, final OnWikidataReceiveListener listener) {
        final Map<String, String> results = new HashMap<String, String>();
        List<String> idsToFetch = new ArrayList<String>();
        for (String id : ids) {
            if (descriptionCache.get(id) == null || !language.equals(app.getPrimaryLanguage())) {
                // not in our cache yet, or we want a result in a different language from the one
                // the cache is currently storing descriptions in
                idsToFetch.add(id);
            } else {
                results.put(id, descriptionCache.get(id));
            }
        }
        if (idsToFetch.size() > 0) {
            (new WikidataDescriptionsTask(
                    app.getAPIForSite(new WikidataSite()),
                    language,
                    idsToFetch) {
                @Override
                public void onFinish(Map<String, String> result) {
                    for (Map.Entry<String, String> entry : result.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        // Only store results in cache if they correspond to the language we're
                        // currently using; that way if you choose "Read in another language" you
                        // won't cache and display results from the incorrect language
                        if (language.equals(app.getPrimaryLanguage())) {
                            descriptionCache.put(entry.getKey(), entry.getValue());
                        }
                        results.put(entry.getKey(), entry.getValue());
                    }
                    listener.onWikidataReceived(results);
                }
                @Override
                public void onCatch(Throwable caught) {
                    listener.onWikidataFailed(caught);
                }
            }).execute();
        } else {
            listener.onWikidataReceived(results);
        }
    }
    public interface OnWikidataReceiveListener {
        void onWikidataReceived(Map<String, String> result);
        void onWikidataFailed(Throwable caught);
    }
}
