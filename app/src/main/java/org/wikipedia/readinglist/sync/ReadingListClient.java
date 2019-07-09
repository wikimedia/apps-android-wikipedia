package org.wikipedia.readinglist.sync;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingList;
import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntry;
import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntryBatch;

public class ReadingListClient {
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

    /**
     * Sets up reading list syncing on the server, and returns true if the setup was successful,
     * or false if syncing is already set up.
     */
    public boolean setup(@NonNull String csrfToken) throws Throwable {
        try {
            ServiceFactory.getRest(wiki).setupReadingLists(csrfToken).execute();
            return true;
        } catch (Throwable t) {
            if (isErrorType(t, "already-set-up")) {
                return false;
            }
            throw t;
        }
    }

    public void tearDown(@NonNull String csrfToken) throws Throwable {
        try {
            ServiceFactory.getRest(wiki).tearDownReadingLists(csrfToken).execute();
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
            Response<SyncedReadingLists> response = ServiceFactory.getRest(wiki).getReadingLists(continueStr).execute();
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
            Response<SyncedReadingLists> response = ServiceFactory.getRest(wiki).getReadingListChangesSince(date, continueStr).execute();
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
            Response<SyncedReadingLists> response = ServiceFactory.getRest(wiki)
                    .getReadingListsContaining(entry.project(), entry.title(), continueStr).execute();
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
                    = ServiceFactory.getRest(wiki).getReadingListEntries(listId, continueStr).execute();
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
                = ServiceFactory.getRest(wiki).createReadingList(csrfToken, list).execute();
        SyncedReadingLists.RemoteIdResponse idResponse = response.body();
        if (idResponse == null) {
            throw new IOException("Incorrect response format.");
        }
        saveLastDateHeader(response);
        return idResponse.id();
    }

    public void updateList(@NonNull String csrfToken, long listId, @NonNull RemoteReadingList list) throws Throwable {
        Response response = ServiceFactory.getRest(wiki).updateReadingList(listId, csrfToken, list).execute();
        saveLastDateHeader(response);
    }

    public void deleteList(@NonNull String csrfToken, long listId) throws Throwable {
        Response response = ServiceFactory.getRest(wiki).deleteReadingList(listId, csrfToken).execute();
        saveLastDateHeader(response);
    }

    public long addPageToList(@NonNull String csrfToken, long listId, @NonNull RemoteReadingListEntry entry) throws Throwable {
        Response<SyncedReadingLists.RemoteIdResponse> response
                = ServiceFactory.getRest(wiki).addEntryToReadingList(listId, csrfToken, entry).execute();
        SyncedReadingLists.RemoteIdResponse idResponse = response.body();
        if (idResponse == null) {
            throw new IOException("Incorrect response format.");
        }
        saveLastDateHeader(response);
        return idResponse.id();
    }

    public List<Long> addPagesToList(@NonNull String csrfToken, long listId, @NonNull List<RemoteReadingListEntry> entries) throws Throwable {
        final int maxBatchSize = 50;
        int batchIndex = 0;
        List<Long> ids = new ArrayList<>();
        List<RemoteReadingListEntry> currentBatch = new ArrayList<>();
        while (true) {
            currentBatch.clear();
            while (batchIndex < entries.size() && currentBatch.size() < maxBatchSize) {
                currentBatch.add(entries.get(batchIndex++));
            }
            if (currentBatch.isEmpty()) {
                break;
            }

            try {
                Response<SyncedReadingLists.RemoteIdResponseBatch> response
                        = ServiceFactory.getRest(wiki).addEntriesToReadingList(listId, csrfToken, new RemoteReadingListEntryBatch(currentBatch)).execute();
                SyncedReadingLists.RemoteIdResponseBatch idResponse = response.body();
                if (idResponse == null) {
                    throw new IOException("Incorrect response format.");
                }
                saveLastDateHeader(response);

                for (SyncedReadingLists.RemoteIdResponse id : idResponse.batch()) {
                    ids.add(id.id());
                }
            } catch (Throwable t) {
                if (isErrorType(t, "entry-limit")) {
                    // TODO: handle more meaningfully than ignoring, for now.
                    break;
                }
                throw t;
            }
        }
        return ids;
    }

    public void deletePageFromList(@NonNull String csrfToken, long listId, long entryId) throws Throwable {
        Response response = ServiceFactory.getRest(wiki).deleteEntryFromReadingList(listId, entryId, csrfToken).execute();
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
}
