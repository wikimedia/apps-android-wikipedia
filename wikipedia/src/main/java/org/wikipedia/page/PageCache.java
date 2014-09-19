package org.wikipedia.page;

import org.wikipedia.PageTitle;

import java.util.*;

/**
 * Implements a cache of Page objects, to be readily retrieved from memory.
 * TODO: make this Parcelable? (or save/restore it from physical storage?)
 */
public class PageCache {

    private static final int MAX_CACHE_ITEMS = 10;
    private static final float CACHE_LOAD_FACTOR = 0.75f;

    private LinkedHashMap<PageTitle, Page> items;

    public PageCache() {
        items = new LinkedHashMap<PageTitle, Page>(MAX_CACHE_ITEMS, CACHE_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_ITEMS;
            }
        };
    }

    public boolean has(PageTitle title) {
        return items.containsKey(title);
    }

    public Page get(PageTitle title) {
        return items.get(title);
    }

    public void put(PageTitle title, Page page) {
        items.put(title, page);
    }
}
