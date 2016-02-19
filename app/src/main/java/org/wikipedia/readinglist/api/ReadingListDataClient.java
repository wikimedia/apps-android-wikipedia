package org.wikipedia.readinglist.api;

import org.wikipedia.Site;
import org.wikipedia.readinglist.api.legacy.LegacyReadingListPageTitlesResponse;
import org.wikipedia.readinglist.api.legacy.LegacyReadingListsResponse;
import org.wikipedia.dataclient.RestAdapterFactory;

import retrofit.http.GET;
import retrofit.http.Query;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

/**
 * Gets and posts collection related data from and to the server.
 */
public class ReadingListDataClient {
    private static final Site SITE = new Site("en.wikipedia.org");

    @NonNull private final Api client;

    public ReadingListDataClient() {
        client = RestAdapterFactory.newInstance(SITE).create(Api.class);
    }

    @VisibleForTesting
    public ReadingListDataClient(String baseUrl) {
        client = RestAdapterFactory.newInstance(SITE, baseUrl).create(Api.class);
    }

    /**
     * Gets the Collections of the current user.
     */
    public LegacyReadingListsResponse getReadingLists() {
        return client.getReadingLists();
    }

    /**
     * Gets the list of page titles of a single ReadingList of the current user.
     *
     * @param listId ID of the reading list to be retrieved
     */
    public LegacyReadingListPageTitlesResponse getMemberPages(int listId) {
        return client.getMemberPages(listId);
    }

    private interface Api {
        String ACTION_QUERY_LIST = "/w/api.php?format=json&formatversion=2&action=query&list=";

        @GET(ACTION_QUERY_LIST + "lists"
                + "&lstprop=label%7Cdescription%7Cpublic%7Creview%7Cimage%7Ccount%7Cupdated%7Cowner")
        LegacyReadingListsResponse getReadingLists();

        @GET(ACTION_QUERY_LIST + "listpages")
        LegacyReadingListPageTitlesResponse getMemberPages(@Query("lspid") int collectionId);
    }
}
