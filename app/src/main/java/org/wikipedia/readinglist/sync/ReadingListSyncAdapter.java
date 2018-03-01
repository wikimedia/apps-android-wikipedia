package org.wikipedia.readinglist.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.BuildConfig;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.events.ReadingListsEnableDialogEvent;
import org.wikipedia.events.ReadingListsEnabledStatusEvent;
import org.wikipedia.events.ReadingListsMergeLocalDialogEvent;
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingList;
import static org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntry;

public class ReadingListSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String SYNC_EXTRAS_FORCE_FULL_SYNC = "forceFullSync";
    private static final String SYNC_EXTRAS_REFRESHING = "refreshing";
    private static final String SYNC_EXTRAS_RETRYING = "retrying";
    private static boolean IN_PROGRESS;

    public ReadingListSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public ReadingListSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    public static boolean inProgress() {
        return IN_PROGRESS;
    }

    public static void setSyncEnabledWithSetup() {
        Prefs.setReadingListSyncEnabled(true);
        Prefs.setReadingListsRemoteSetupPending(true);
        Prefs.setReadingListsRemoteDeletePending(false);
        ReadingListSyncAdapter.manualSync();
    }

    public static boolean isDisabledByRemoteConfig() {
        return WikipediaApp.getInstance().getRemoteConfig().getConfig().optBoolean("disableReadingListSync", false);
    }

    public static void manualSyncWithDeleteList(@NonNull ReadingList list) {
        if (list.remoteId() <= 0) {
            return;
        }
        Prefs.addReadingListsDeletedIds(Collections.singleton(list.remoteId()));
        manualSync();
    }

    public static void manualSyncWithDeletePages(@NonNull ReadingList list, @NonNull List<ReadingListPage> pages) {
        if (list.remoteId() <= 0) {
            return;
        }
        Set<String> ids = new HashSet<>();
        for (ReadingListPage page : pages) {
            if (page.remoteId() > 0) {
                ids.add(Long.toString(list.remoteId()) + ":" + Long.toString(page.remoteId()));
            }
        }
        if (!ids.isEmpty()) {
            Prefs.addReadingListPagesDeletedIds(ids);
            manualSync();
        }
    }

    public static void manualSyncWithForce() {
        Bundle extras = new Bundle();
        extras.putBoolean(SYNC_EXTRAS_FORCE_FULL_SYNC, true);
        manualSync(extras);
    }

    public static void manualSyncWithRefresh() {
        Bundle extras = new Bundle();
        extras.putBoolean(SYNC_EXTRAS_REFRESHING, true);
        manualSync(extras);
    }

    public static void manualSync() {
        manualSync(new Bundle());
    }

    private static void manualSync(@NonNull Bundle extras) {
        if (AccountUtil.account() == null) {
            if (extras.containsKey(SYNC_EXTRAS_REFRESHING)) {
                SavedPageSyncService.sendSyncEvent();
            }
            return;
        }
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(AccountUtil.account(), BuildConfig.READING_LISTS_AUTHORITY, extras);
    }

    @SuppressWarnings("methodlength")
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (isDisabledByRemoteConfig()
                || !AccountUtil.isLoggedIn()
                || !(Prefs.isReadingListSyncEnabled()
                || Prefs.shouldShowReadingListSyncMergePrompt()
                || Prefs.isReadingListsRemoteDeletePending())) {
            L.d("Skipping sync of reading lists.");

            if (extras.containsKey(SYNC_EXTRAS_REFRESHING)) {
                SavedPageSyncService.sendSyncEvent();
            }
            return;
        }

        L.d("Begin sync of reading lists...");

        List<String> csrfToken = new ArrayList<>();
        Set<Long> listIdsDeleted = Prefs.getReadingListsDeletedIds();
        Set<String> pageIdsDeleted = Prefs.getReadingListPagesDeletedIds();

        List<ReadingList> allLocalLists = null;

        WikiSite wiki = WikipediaApp.getInstance().getWikiSite();
        ReadingListClient client = new ReadingListClient(wiki);

        String lastSyncTime = Prefs.getReadingListsLastSyncTime();
        boolean shouldSendSyncEvent = extras.containsKey(SYNC_EXTRAS_REFRESHING);
        boolean shouldRetry = false;
        boolean shouldRetryWithForce = false;

        try {
            IN_PROGRESS = true;
            boolean syncEverything = false;

            if (extras.containsKey(SYNC_EXTRAS_FORCE_FULL_SYNC)
                    || Prefs.isReadingListsRemoteDeletePending()
                    || Prefs.isReadingListsRemoteSetupPending()) {
                // reset the remote ID on all lists, since they will need to be recreated next time.
                L.d("Resetting all lists to un-synced.");
                syncEverything = true;
                ReadingListDbHelper.instance().markEverythingUnsynced();
                allLocalLists = ReadingListDbHelper.instance().getAllLists();
            }

            if (Prefs.isReadingListsRemoteDeletePending()) {
                // Are we scheduled for a teardown? If so, delete everything and bail.
                L.d("Tearing down remote lists...");
                client.tearDown(getCsrfToken(wiki, csrfToken));
                Prefs.setReadingListsRemoteDeletePending(false);
                return;
            } else if (Prefs.isReadingListsRemoteSetupPending()) {
                // ...Or are we scheduled for setup?
                if (client.setup(getCsrfToken(wiki, csrfToken))) {
                    // Set up for the first time, which means that we don't need to ask whether
                    // the user wants to merge local lists.
                    Prefs.shouldShowReadingListSyncMergePrompt(false);
                }
                Prefs.setReadingListsRemoteSetupPending(false);
            }

            if (Prefs.shouldShowReadingListSyncMergePrompt()) {
                Prefs.shouldShowReadingListSyncMergePrompt(false);
                for (ReadingList list : allLocalLists) {
                    for (ReadingListPage page : list.pages()) {
                        if (page.remoteId() <= 0) {
                            // At least one page in our collection is not synced.
                            // We therefore need to ask the user if we want to merge unsynced pages
                            // with the remote collection, or delete them.
                            // However, let's issue a request to the changes endpoint, so that
                            // it can throw an exception if lists are not set up for the user.
                            client.getChangesSince(DateUtil.getIso8601DateFormat().format(new Date()));
                            // Exception wasn't thrown, so post the bus event.
                            WikipediaApp.getInstance().getBus().post(new ReadingListsMergeLocalDialogEvent());
                            return;
                        }
                    }
                }
            }

            //-----------------------------------------------
            // PHASE 1: Sync from remote to local.
            //-----------------------------------------------

            List<RemoteReadingList> remoteListsModified = Collections.emptyList();
            List<RemoteReadingListEntry> remoteEntriesModified = Collections.emptyList();

            if (TextUtils.isEmpty(lastSyncTime)) {
                syncEverything = true;
            }

            if (syncEverything) {
                if (allLocalLists == null) {
                    allLocalLists = ReadingListDbHelper.instance().getAllLists();
                }
            } else {
                if (allLocalLists == null) {
                    allLocalLists = ReadingListDbHelper.instance().getAllListsWithUnsyncedPages();
                }
                L.d("Fetching changes from server, since " + lastSyncTime);
                SyncedReadingLists allChanges = client.getChangesSince(lastSyncTime);
                if (allChanges.getLists() != null) {
                    remoteListsModified = allChanges.getLists();
                }
                if (allChanges.getEntries() != null) {
                    remoteEntriesModified = allChanges.getEntries();
                }
            }

            // Perform a quick check for whether we'll need to sync all lists
            for (RemoteReadingListEntry remoteEntry : remoteEntriesModified) {
                // find the list to which this entry belongs...
                ReadingList eigenLocalList = null;
                RemoteReadingList eigenRemoteList = null;
                for (ReadingList localList : allLocalLists) {
                    if (localList.remoteId() == remoteEntry.listId()) {
                        eigenLocalList = localList;
                        break;
                    }
                }
                for (RemoteReadingList remoteList : remoteListsModified) {
                    if (remoteList.id() == remoteEntry.listId()) {
                        eigenRemoteList = remoteList;
                        break;
                    }
                }
                if (eigenLocalList == null && eigenRemoteList == null) {
                    L.w("Remote entry belongs to an unknown local list. Falling back to full sync.");
                    syncEverything = true;
                    break;
                }
            }

            if (syncEverything) {
                allLocalLists = ReadingListDbHelper.instance().getAllLists();
                L.d("Fetching all lists from server...");
                remoteListsModified = client.getAllLists();
            }

            // Notify any event consumers that reading lists are, in fact, enabled.
            WikipediaApp.getInstance().getBus().post(new ReadingListsEnabledStatusEvent());

            // First, update our list hierarchy to match the remote hierarchy.
            for (RemoteReadingList remoteList : remoteListsModified) {
                // Find the remote list in our local lists...
                ReadingList localList = null;
                boolean upsertNeeded = false;

                for (ReadingList list : allLocalLists) {
                    if (list.isDefault() && remoteList.isDefault()) {
                        localList = list;
                        if (list.remoteId() != remoteList.id()) {
                            list.remoteId(remoteList.id());
                            upsertNeeded = true;
                        }
                        break;
                    }
                    if (list.remoteId() == remoteList.id()) {
                        localList = list;
                        break;
                    } else if (StringUtil.normalizedEquals(list.title(), remoteList.name())) {
                        list.remoteId(remoteList.id());
                        upsertNeeded = true;
                        localList = list;
                    }
                }

                if (remoteList.isDeleted()) {
                    if (localList != null && !localList.isDefault()) {
                        L.d("Deleting local list " + localList.title());
                        ReadingListDbHelper.instance().deleteList(localList, false);
                        ReadingListDbHelper.instance().markPagesForDeletion(localList, localList.pages(), false);
                        allLocalLists.remove(localList);
                        shouldSendSyncEvent = true;
                    }
                    continue;
                }

                if (localList == null) {
                    // A new list needs to be created locally.
                    L.d("Creating local list " + remoteList.name());
                    localList = ReadingListDbHelper.instance().createList(remoteList.name(), remoteList.description());
                    localList.remoteId(remoteList.id());
                    allLocalLists.add(localList);
                    upsertNeeded = true;
                } else {
                    if (!localList.isDefault() && !StringUtil.normalizedEquals(localList.title(), remoteList.name())) {
                        localList.title(remoteList.name());
                        upsertNeeded = true;
                    }
                    if (!localList.isDefault() && !StringUtil.normalizedEquals(localList.description(), remoteList.description())) {
                        localList.description(remoteList.description());
                        upsertNeeded = true;
                    }
                }
                if (upsertNeeded) {
                    L.d("Updating info for local list " + localList.title());
                    localList.dirty(false);
                    ReadingListDbHelper.instance().updateList(localList, false);
                    shouldSendSyncEvent = true;
                }

                if (syncEverything) {
                    L.d("Fetching all pages in remote list " + remoteList.name());
                    List<RemoteReadingListEntry> remoteEntries = client.getListEntries(remoteList.id());
                    for (RemoteReadingListEntry remoteEntry : remoteEntries) {
                        // TODO: optimization opportunity -- create/update local pages in bulk.
                        createOrUpdatePage(localList, remoteEntry);
                    }
                    shouldSendSyncEvent = true;
                }
            }

            if (!syncEverything) {
                for (RemoteReadingListEntry remoteEntry : remoteEntriesModified) {
                    // find the list to which this entry belongs...
                    ReadingList eigenList = null;
                    for (ReadingList localList : allLocalLists) {
                        if (localList.remoteId() == remoteEntry.listId()) {
                            eigenList = localList;
                            break;
                        }
                    }
                    if (eigenList == null) {
                        L.w("Remote entry belongs to an unknown local list.");
                        continue;
                    }
                    shouldSendSyncEvent = true;
                    if (remoteEntry.isDeleted()) {
                        deletePageByTitle(eigenList, pageTitleFromRemoteEntry(remoteEntry));
                    } else {
                        createOrUpdatePage(eigenList, remoteEntry);
                    }
                }
            }

            //-----------------------------------------------
            // PHASE 2: Sync from local to remote.
            //-----------------------------------------------

            // Do any remote lists need to be deleted?
            List<Long> listIdsToDelete = new ArrayList<>();
            listIdsToDelete.addAll(listIdsDeleted);
            for (Long id : listIdsToDelete) {
                L.d("Deleting remote list id " + id);
                try {
                    client.deleteList(getCsrfToken(wiki, csrfToken), id);
                } catch (Throwable t) {
                    L.w(t);
                    if (!client.isServiceError(t) && !client.isUnavailableError(t)) {
                        throw t;
                    }
                }
                listIdsDeleted.remove(id);
            }

            // Do any remote pages need to be deleted?
            List<String> pageIdsToDelete = new ArrayList<>();
            pageIdsToDelete.addAll(pageIdsDeleted);
            for (String id : pageIdsToDelete) {
                L.d("Deleting remote page id " + id);
                String[] listAndPageId = id.split(":");
                try {
                    // TODO: optimization opportunity once server starts supporting batch deletes.
                    client.deletePageFromList(getCsrfToken(wiki, csrfToken), Long.parseLong(listAndPageId[0]), Long.parseLong(listAndPageId[1]));
                } catch (Throwable t) {
                    L.w(t);
                    if (!client.isServiceError(t) && !client.isUnavailableError(t)) {
                        throw t;
                    }
                }
                pageIdsDeleted.remove(id);
            }

            // Determine whether any remote lists need to be created or updated
            for (ReadingList localList : allLocalLists) {
                RemoteReadingList remoteList =
                        new RemoteReadingList(localList.title(), localList.description());

                boolean upsertNeeded = false;
                if (localList.remoteId() > 0) {
                    if (!localList.isDefault() && localList.dirty()) {
                        // Update remote metadata for this list.
                        L.d("Updating info for remote list " + remoteList.name());
                        client.updateList(getCsrfToken(wiki, csrfToken), localList.remoteId(), remoteList);
                        upsertNeeded = true;
                    }
                } else if (!localList.isDefault()) {
                    // This list needs to be created remotely.
                    L.d("Creating remote list " + remoteList.name());
                    long id = client.createList(getCsrfToken(wiki, csrfToken), remoteList);
                    localList.remoteId(id);
                    upsertNeeded = true;
                }
                if (upsertNeeded) {
                    localList.dirty(false);
                    ReadingListDbHelper.instance().updateList(localList, false);
                }
            }

            for (ReadingList localList : allLocalLists) {
                List<ReadingListPage> localPages = new ArrayList<>();
                List<RemoteReadingListEntry> newEntries = new ArrayList<>();
                for (ReadingListPage localPage : localList.pages()) {
                    if (localPage.remoteId() < 1) {
                        localPages.add(localPage);
                        newEntries.add(remoteEntryFromLocalPage(localPage));
                    }
                }
                if (newEntries.isEmpty()) {
                    continue;
                }
                try {
                    if (localPages.size() == 1) {
                        L.d("Creating new remote page " + localPages.get(0).title());
                        localPages.get(0).remoteId(client.addPageToList(getCsrfToken(wiki, csrfToken), localList.remoteId(), newEntries.get(0)));
                        ReadingListDbHelper.instance().updatePage(localPages.get(0));
                    } else {
                        L.d("Creating " + newEntries.size() + " new remote pages");
                        List<Long> ids = client.addPagesToList(getCsrfToken(wiki, csrfToken), localList.remoteId(), newEntries);
                        for (int i = 0; i < ids.size(); i++) {
                            localPages.get(i).remoteId(ids.get(i));
                        }
                        ReadingListDbHelper.instance().updatePages(localPages);
                    }
                } catch (Throwable t) {
                    // TODO: optimization opportunity -- if the server can return the ID
                    // of the existing page(s), then we wouldn't need to do a hard sync.

                    // If the page already exists in the remote list, this means that
                    // the contents of this list have diverged from the remote list,
                    // so let's force a full sync.
                    if (client.isErrorType(t, "duplicate-page")) {
                        shouldRetryWithForce = true;
                        break;
                    } else if (client.isErrorType(t, "entry-limit")) {
                        // TODO: handle more meaningfully than ignoring, for now.
                    } else {
                        throw t;
                    }
                }
            }

        } catch (Throwable t) {
            if (client.isErrorType(t, "not-set-up")) {
                Prefs.setReadingListSyncEnabled(false);
                if (TextUtils.isEmpty(lastSyncTime)) {
                    // This means that it's our first time attempting to sync, and we see that
                    // syncing isn't enabled on the server. So, let's prompt the user to enable it:
                    WikipediaApp.getInstance().getBus().post(new ReadingListsEnableDialogEvent());
                } else {
                    // This can only mean that our reading lists have been torn down (disabled) by
                    // another client, so we need to notify the user of this development.
                    WikipediaApp.getInstance().getBus().post(new ReadingListsNoLongerSyncedEvent());
                }
            }

            if (client.isErrorType(t, "notloggedin")) {
                try {
                    L.d("Server doesn't believe we're logged in, so logging in...");
                    getCsrfToken(wiki, csrfToken);
                    shouldRetry = true;
                } catch (Throwable caught) {
                    t = caught;
                }
            }
            L.w(t);
        } finally {
            lastSyncTime = getLastDateFromHeader(lastSyncTime, client);

            Prefs.setReadingListsLastSyncTime(lastSyncTime);
            Prefs.setReadingListsDeletedIds(listIdsDeleted);
            Prefs.setReadingListPagesDeletedIds(pageIdsDeleted);

            if (shouldSendSyncEvent) {
                SavedPageSyncService.sendSyncEvent();
            }
            if ((shouldRetry || shouldRetryWithForce) && !extras.containsKey(SYNC_EXTRAS_RETRYING)) {
                Bundle b = new Bundle();
                b.putAll(extras);
                b.putBoolean(SYNC_EXTRAS_RETRYING, true);
                if (shouldRetryWithForce) {
                    b.putBoolean(SYNC_EXTRAS_FORCE_FULL_SYNC, true);
                }
                manualSync(b);
            }
            IN_PROGRESS = false;
            SavedPageSyncService.enqueue();
            L.d("Finished sync of reading lists.");
        }
    }


    private String getCsrfToken(@NonNull WikiSite wiki, @NonNull List<String> tokenList) throws Throwable {
        if (tokenList.size() == 0) {
            tokenList.add(new CsrfTokenClient(wiki, wiki).getTokenBlocking());
        }
        return tokenList.get(0);
    }

    @NonNull
    private String getLastDateFromHeader(@NonNull String lastSyncTime, @NonNull ReadingListClient client) {
        String lastDateHeader = client.getLastDateHeader();
        if (TextUtils.isEmpty(lastDateHeader)) {
            return lastSyncTime;
        }
        try {
            Date date = DateUtil.getHttpLastModifiedDate(lastDateHeader);
            return DateUtil.getIso8601DateFormat().format(date);
        } catch (ParseException e) {
            return lastSyncTime;
        }
    }

    private void createOrUpdatePage(@NonNull ReadingList listForPage,
                                    @NonNull RemoteReadingListEntry remotePage) {
        PageTitle remoteTitle = pageTitleFromRemoteEntry(remotePage);
        ReadingListPage localPage = null;
        boolean updateOnly = false;

        for (ReadingListPage page : listForPage.pages()) {
            if (ReadingListPage.toPageTitle(page).equals(remoteTitle)) {
                localPage = page;
                updateOnly = true;
                break;
            }
        }
        if (localPage == null) {
            localPage = new ReadingListPage(pageTitleFromRemoteEntry(remotePage));
            localPage.listId(listForPage.id());
            if (ReadingListDbHelper.instance().pageExistsInList(listForPage, remoteTitle)) {
                updateOnly = true;
            }
        }
        localPage.remoteId(remotePage.id());
        if (remotePage.summary() != null) {
            localPage.description(remotePage.summary().getDescription());
            localPage.thumbUrl(remotePage.summary().getThumbnailUrl());
        }
        if (updateOnly) {
            L.d("Updating local page " + localPage.title());
            ReadingListDbHelper.instance().updatePage(localPage);
        } else {
            L.d("Creating local page " + localPage.title());
            ReadingListDbHelper.instance().addPagesToList(listForPage, Collections.singletonList(localPage), false);
        }
    }

    private void deletePageByTitle(@NonNull ReadingList listForPage, @NonNull PageTitle title) {
        ReadingListPage localPage = null;
        for (ReadingListPage page : listForPage.pages()) {
            if (ReadingListPage.toPageTitle(page).equals(title)) {
                localPage = page;
                break;
            }
        }
        if (localPage == null) {
            localPage = ReadingListDbHelper.instance().getPageByTitle(listForPage, title);
            if (localPage == null) {
                return;
            }
        }
        L.d("Deleting local page " + localPage.title());
        ReadingListDbHelper.instance().markPagesForDeletion(listForPage,
                Collections.singletonList(localPage), false);
    }

    private PageTitle pageTitleFromRemoteEntry(@NonNull RemoteReadingListEntry remoteEntry) {
        WikiSite wiki = new WikiSite(remoteEntry.project());
        return new PageTitle(remoteEntry.title(), wiki);
    }

    private RemoteReadingListEntry remoteEntryFromLocalPage(@NonNull ReadingListPage localPage) {
        PageTitle title = ReadingListPage.toPageTitle(localPage);
        return new RemoteReadingListEntry(title.getWikiSite().scheme() + "://" + title.getWikiSite().authority(), title.getPrefixedText());
    }
}
