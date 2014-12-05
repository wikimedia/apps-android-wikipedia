package org.wikipedia.wikidata;

import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikidataCache {
    private static final int MAX_CACHE_SIZE_DESCRIPTIONS = 48;
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
        if (value == null) {
            value = "";
        }
        descriptionCache.put(key, value);
    }

    /**
     * Retrieves the description for a Wikidata item directly from the Wikidata description cache.
     *
     * This method should be used sparingly. Instead of getting descriptions directly from the
     * cache consider using the get method instead, which will (if necessary) retrieve descriptions
     * that the cache currently does not contain and cache them itself if appropriate.
     *
     * @param title Page title for which the description is required.
     * @return The Wikidata description that was cached, or null if the description is not cached.
     */
    public String get(PageTitle title) {
        return descriptionCache.get(title.toString());
    }

    /**
     * Retrieves Wikidata description for a page in the app's current primary language.
     *
     * @param title Page title for which the description is required.
     * @param listener Listener that will receive the description retrieved from the cache.
     */
    public void get(PageTitle title, OnWikidataReceiveListener listener) {
        get(title, app.getPrimaryLanguage(), listener);
    }

    /**
     * Retrieves Wikidata description for a page in the specified language.
     *
     * @param title Page title for which the description is required.
     * @param language The language in which the description is required.
     * @param listener Listener that will receive the description retrieved from the cache.
     */
    public void get(PageTitle title, String language, OnWikidataReceiveListener listener) {
        List<PageTitle> idList = new ArrayList<PageTitle>();
        idList.add(title);
        get(idList, language, listener);
    }

    /**
     * Retrieves Wikidata descriptions for a list of pages in the app's current primary language.
     *
     * @param titles Page titles for which the descriptions are required.
     * @param listener Listener that will receive the descriptions retrieved from the cache.
     */
    public void get(List<PageTitle> titles, final OnWikidataReceiveListener listener) {
        get(titles, app.getPrimaryLanguage(), listener);
    }

    /**
     * Retrieves Wikidata descriptions for a list of pages in the specified language.
     *
     * @param titles Page titles for which the descriptions are required.
     * @param language The language in which the descriptions are required.
     * @param listener Listener that will receive the descriptions retrieved from the cache.
     */
    public void get(List<PageTitle> titles, final String language, final OnWikidataReceiveListener listener) {
        final Map<PageTitle, String> results = new HashMap<PageTitle, String>();
        List<PageTitle> titlesToFetch = new ArrayList<PageTitle>();
        for (PageTitle title : titles) {
            if (descriptionCache.get(title.toString()) == null || !language.equals(app.getPrimaryLanguage())) {
                // not in our cache yet, or we want a result in a different language from the one
                // the cache is currently storing descriptions in
                titlesToFetch.add(title);
            } else {
                results.put(title, descriptionCache.get(title.toString()));
            }
        }
        if (titlesToFetch.size() > 0) {
            final Site site = Site.forLang(language);
            (new GetDescriptionsTask(app.getAPIForSite(site), site, titlesToFetch) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    for (Map.Entry<PageTitle, String> entry : result.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        // Only store results in cache if they correspond to the language we're
                        // currently using; that way if the user chose "Read in another language"
                        // we're not caching and displaying results from the incorrect language
                        if (language.equals(app.getPrimaryLanguage())) {
                            descriptionCache.put(entry.getKey().toString(), entry.getValue());
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
        void onWikidataReceived(Map<PageTitle, String> result);
        void onWikidataFailed(Throwable caught);
    }
}
