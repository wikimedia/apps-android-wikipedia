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

    /**
     * Removes all the data in the Wikidata descriptions cache.
     */
    public void clear() {
        descriptionCache.evictAll();
    }

    /**
     * Adds the description for a Wikidata item directly to the Wikidata description cache.
     *
     * This method should be used sparingly. Instead of putting descriptions directly into the
     * cache consider using the get method instead, which will (if necessary) retrieve descriptions
     * that the cache currently does not contain and cache them itself if appropriate.
     *
     * @param key Wikidata ID of the page for which the description is to be cached.
     * @param value Wikidata description of the page associated with the Wikidata ID.
     */
    public void put(String key, String value) {
        descriptionCache.put(key, value);
    }

    /**
     * Retrieves the description for a Wikidata item directly from the Wikidata description cache.
     *
     * This method should be used sparingly. Instead of getting descriptions directly from the
     * cache consider using the get method instead, which will (if necessary) retrieve descriptions
     * that the cache currently does not contain and cache them itself if appropriate.
     *
     * @param id Wikidata ID of the page for which the description is required.
     * @return The Wikidata description that was cached, or null if the description is not cached.
     */
    public String get(String id) {
        return descriptionCache.get(id);
    }

    /**
     * Retrieves Wikidata description for a page in the app's current primary language.
     *
     * @param id Wikidata ID of the page for which the description is required.
     * @param listener Listener that will receive the description retrieved from the cache.
     */
    public void get(String id, OnWikidataReceiveListener listener) {
        get(id, app.getPrimaryLanguage(), listener);
    }

    /**
     * Retrieves Wikidata description for a page in the specified language.
     *
     * @param id Wikidata ID of the page for which the description is required.
     * @param language The language in which the description is required.
     * @param listener Listener that will receive the description retrieved from the cache.
     */
    public void get(String id, String language, OnWikidataReceiveListener listener) {
        List<String> idList = new ArrayList<String>();
        idList.add(id);
        get(idList, language, listener);
    }

    /**
     * Retrieves Wikidata descriptions for a list of pages in the app's current primary language.
     *
     * @param ids Wikidata IDs of the pages for which the descriptions are required.
     * @param listener Listener that will receive the descriptions retrieved from the cache.
     */
    public void get(List<String> ids, final OnWikidataReceiveListener listener) {
        get(ids, app.getPrimaryLanguage(), listener);
    }

    /**
     * Retrieves Wikidata descriptions for a list of pages in the specified language.
     *
     * @param ids Wikidata IDs of the pages for which the descriptions are required.
     * @param language The language in which the descriptions are required.
     * @param listener Listener that will receive the descriptions retrieved from the cache.
     */
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
                        // currently using; that way if the user chose "Read in another language"
                        // we're not caching and displaying results from the incorrect language
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
