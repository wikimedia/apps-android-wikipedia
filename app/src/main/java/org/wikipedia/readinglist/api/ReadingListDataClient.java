package org.wikipedia.readinglist.api;

import org.wikipedia.Site;
import org.wikipedia.readinglist.api.legacy.LegacyReadingListPageTitlesResponse;
import org.wikipedia.readinglist.api.legacy.LegacyReadingListsResponse;
import org.wikipedia.dataclient.retrofit.RetrofitFactory;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.io.IOException;

/**
 * Gets and posts collection related data from and to the server.
 */
public class ReadingListDataClient {
    private static final Site SITE = new Site("en.wikipedia.org");

    @NonNull private final Api client;

    public ReadingListDataClient() {
        client = RetrofitFactory.newInstance(SITE).create(Api.class);
    }

    @VisibleForTesting
    public ReadingListDataClient(String baseUrl) {
        client = RetrofitFactory.newInstance(SITE, baseUrl).create(Api.class);
    }

    /**
     * Gets the Collections of the current user.
     */
    public LegacyReadingListsResponse getReadingLists() throws IOException {
        return client.getReadingLists().execute().body();
    }

    /**
     * Gets the list of page titles of a single ReadingList of the current user.
     *
     * @param listId ID of the reading list to be retrieved
     */
    public LegacyReadingListPageTitlesResponse getMemberPages(int listId) throws IOException {
        return client.getMemberPages(listId).execute().body();
    }

    private interface Api {
        String ACTION_QUERY_LIST = "w/api.php?format=json&formatversion=2&action=query&list=";

        @GET(ACTION_QUERY_LIST + "lists"
                + "&lstprop=label%7Cdescription%7Cpublic%7Creview%7Cimage%7Ccount%7Cupdated%7Cowner")
        Call<LegacyReadingListsResponse> getReadingLists();

        @GET(ACTION_QUERY_LIST + "listpages")
        Call<LegacyReadingListPageTitlesResponse> getMemberPages(@Query("lspid") int collectionId);
    }
}
