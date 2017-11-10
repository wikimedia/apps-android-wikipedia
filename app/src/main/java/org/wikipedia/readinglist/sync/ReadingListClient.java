package org.wikipedia.readinglist.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.retrofit.RbCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingList;
import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntry;

public class ReadingListClient {
    @NonNull private final WikiCachedService<Service> cachedService = new RbCachedService<>(Service.class);
    @NonNull private final WikiSite wiki;
    @Nullable private String lastDateHeader;

    // Artificial upper limit on the number of continuation cycles we can do, to prevent
    // getting stuck in an infinite loop.
    private static final int MAX_CONTINUE_CYCLES = 256;

    public ReadingListClient(@NonNull WikiSite wiki) {
        this.wiki = wiki;
    }

    @Nullable public String getLastDateHeader() {
        return lastDateHeader;
    }

    public void setup(@NonNull String csrfToken) throws Throwable {
        try {
            cachedService.service(wiki).setup(csrfToken).execute();
        } catch (Throwable t) {
            if (isErrorType(t, "already-set-up")) {
                return;
            }
            throw t;
        }
    }

    public void tearDown(@NonNull String csrfToken) throws Throwable {
        try {
            cachedService.service(wiki).tearDown(csrfToken).execute();
        } catch (Throwable t) {
            if (isErrorType(t, "not-set-up")) {
                return;
            }
            throw t;
        }
    }

    @NonNull
    public List<RemoteReadingList> getAllLists() throws Throwable {
        List<RemoteReadingList> totalLists = new ArrayList<>();
        int totalCycles = 0;
        String continueStr = null;
        do {
            Response<SyncedReadingLists> response = cachedService.service(wiki).getLists(continueStr).execute();
            SyncedReadingLists lists = response.body();
            if (lists == null || lists.getLists() == null) {
                throw new IOException("Incorrect response format.");
            }
            totalLists.addAll(lists.getLists());
            continueStr = TextUtils.isEmpty(lists.getContinueStr()) ? null : lists.getContinueStr();
            saveLastDateHeader(response);
        } while (!TextUtils.isEmpty(continueStr) && (totalCycles++ < MAX_CONTINUE_CYCLES));
        return totalLists;
    }

    @NonNull
    public SyncedReadingLists getChangesSince(@NonNull String date) throws Throwable {
        List<RemoteReadingList> totalLists = new ArrayList<>();
        List<RemoteReadingListEntry> totalEntries = new ArrayList<>();
        int totalCycles = 0;
        String continueStr = null;
        do {
            Response<SyncedReadingLists> response = cachedService.service(wiki).getChangesSince(date, continueStr).execute();
            SyncedReadingLists body = response.body();
            if (body == null) {
                throw new IOException("Incorrect response format.");
            }
            if (body.getLists() != null) {
                totalLists.addAll(body.getLists());
            }
            if (body.getEntries() != null) {
                totalEntries.addAll(body.getEntries());
            }
            continueStr = TextUtils.isEmpty(body.getContinueStr()) ? null : body.getContinueStr();
            saveLastDateHeader(response);
        } while (!TextUtils.isEmpty(continueStr) && (totalCycles++ < MAX_CONTINUE_CYCLES));
        return new SyncedReadingLists(totalLists, totalEntries);
    }

    @NonNull
    public List<RemoteReadingList> getListsContaining(@NonNull RemoteReadingListEntry entry) throws Throwable {
        List<RemoteReadingList> totalLists = new ArrayList<>();
        int totalCycles = 0;
        String continueStr = null;
        do {
            Response<SyncedReadingLists> response = cachedService.service(wiki)
                    .getListsContaining(entry.project(), entry.title(), continueStr).execute();
            SyncedReadingLists lists = response.body();
            if (lists == null || lists.getLists() == null) {
                throw new IOException("Incorrect response format.");
            }
            totalLists.addAll(lists.getLists());
            continueStr = TextUtils.isEmpty(lists.getContinueStr()) ? null : lists.getContinueStr();
            saveLastDateHeader(response);
        } while (!TextUtils.isEmpty(continueStr) && (totalCycles++ < MAX_CONTINUE_CYCLES));
        return totalLists;
    }

    @NonNull
    public List<RemoteReadingListEntry> getListEntries(long listId) throws Throwable {
        List<RemoteReadingListEntry> totalEntries = new ArrayList<>();
        int totalCycles = 0;
        String continueStr = null;
        do {
            Response<SyncedReadingLists> response
                    = cachedService.service(wiki).getListEntries(listId, continueStr).execute();
            SyncedReadingLists body = response.body();
            if (body == null || body.getEntries() == null) {
                throw new IOException("Incorrect response format.");
            }
            totalEntries.addAll(body.getEntries());
            continueStr = TextUtils.isEmpty(body.getContinueStr()) ? null : body.getContinueStr();
            saveLastDateHeader(response);
        } while (!TextUtils.isEmpty(continueStr) && (totalCycles++ < MAX_CONTINUE_CYCLES));
        return totalEntries;
    }

    public long createList(@NonNull String csrfToken, @NonNull RemoteReadingList list) throws Throwable {
        Response<SyncedReadingLists.RemoteIdResponse> response
                = cachedService.service(wiki).createList(csrfToken, list).execute();
        SyncedReadingLists.RemoteIdResponse idResponse = response.body();
        if (idResponse == null) {
            throw new IOException("Incorrect response format.");
        }
        saveLastDateHeader(response);
        return idResponse.id();
    }

    public void updateList(@NonNull String csrfToken, long listId, @NonNull RemoteReadingList list) throws Throwable {
        Response response = cachedService.service(wiki).updateList(listId, csrfToken, list).execute();
        saveLastDateHeader(response);
    }

    public void deleteList(@NonNull String csrfToken, long listId) throws Throwable {
        Response response = cachedService.service(wiki).deleteList(listId, csrfToken).execute();
        saveLastDateHeader(response);
    }

    public long addPageToList(@NonNull String csrfToken, long listId, @NonNull RemoteReadingListEntry entry) throws Throwable {
        Response<SyncedReadingLists.RemoteIdResponse> response
                = cachedService.service(wiki).addEntryToList(listId, csrfToken, entry).execute();
        SyncedReadingLists.RemoteIdResponse idResponse = response.body();
        if (idResponse == null) {
            throw new IOException("Incorrect response format.");
        }
        saveLastDateHeader(response);
        return idResponse.id();
    }

    public void deletePageFromList(@NonNull String csrfToken, long listId, long entryId) throws Throwable {
        Response response = cachedService.service(wiki).deleteEntryFromList(listId, entryId, csrfToken).execute();
        saveLastDateHeader(response);
    }

    public boolean isErrorType(Throwable t, @NonNull String errorType) {
        return (t instanceof HttpStatusException
                && ((HttpStatusException) t).serviceError() != null
                && ((HttpStatusException) t).serviceError().getTitle().contains(errorType));
    }

    public boolean isServiceError(Throwable t) {
        final int code = 400;
        return (t instanceof HttpStatusException && ((HttpStatusException) t).code() == code);
    }

    public boolean isUnavailableError(Throwable t) {
        final int code = 405;
        return (t instanceof HttpStatusException && ((HttpStatusException) t).code() == code);
    }

    private void saveLastDateHeader(@NonNull Response response) {
        lastDateHeader = response.headers().get("date");
    }

    // Documentation: https://en.wikipedia.org/api/rest_v1/#/Reading_lists
    private interface Service {

        @POST("data/lists/setup")
        @NonNull
        Call<Void> setup(@Query("csrf_token") String token);

        @POST("data/lists/teardown")
        @NonNull
        Call<Void> tearDown(@Query("csrf_token") String token);

        @GET("data/lists/")
        @NonNull
        Call<SyncedReadingLists> getLists(@Query("next") String next);

        @POST("data/lists/")
        @NonNull
        Call<SyncedReadingLists.RemoteIdResponse> createList(@Query("csrf_token") String token,
                                                             @Body RemoteReadingList list);

        @PUT("data/lists/{id}")
        @NonNull
        Call<Void> updateList(@Path("id") long listId, @Query("csrf_token") String token,
                              @Body RemoteReadingList list);

        @DELETE("data/lists/{id}")
        @NonNull
        Call<Void> deleteList(@Path("id") long listId, @Query("csrf_token") String token);

        @GET("data/lists/changes/since/{date}")
        @NonNull
        Call<SyncedReadingLists> getChangesSince(@Path("date") String iso8601Date,
                                                 @Query("next") String next);

        @GET("data/lists/pages/{project}/{title}")
        @NonNull
        Call<SyncedReadingLists> getListsContaining(@Path("project") String project,
                                                    @Path("title") String title,
                                                    @Query("next") String next);

        @GET("data/lists/{id}/entries/")
        @NonNull
        Call<SyncedReadingLists> getListEntries(@Path("id") long listId,
                                                @Query("next") String next);

        @POST("data/lists/{id}/entries/")
        @NonNull
        Call<SyncedReadingLists.RemoteIdResponse> addEntryToList(@Path("id") long listId,
                                                                 @Query("csrf_token") String token,
                                                                 @Body RemoteReadingListEntry entry);

        @DELETE("data/lists/{id}/entries/{entry_id}")
        @NonNull
        Call<Void> deleteEntryFromList(@Path("id") long listId, @Path("entry_id") long entryId,
                                       @Query("csrf_token") String token);

    }
}
