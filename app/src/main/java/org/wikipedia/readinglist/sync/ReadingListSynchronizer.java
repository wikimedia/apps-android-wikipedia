package org.wikipedia.readinglist.sync;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.User;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.ReadingListData;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.settings.Prefs;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.dataclient.UserInfo;
import org.wikipedia.useroption.dataclient.UserOptionDataClientSingleton;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.wikipedia.readinglist.sync.RemoteReadingLists.RemoteReadingList;
import static org.wikipedia.readinglist.sync.RemoteReadingLists.RemoteReadingListPage;
import static org.wikipedia.settings.Prefs.isReadingListSyncEnabled;
import static org.wikipedia.settings.Prefs.isReadingListsRemoteDeletePending;

public class ReadingListSynchronizer {
    private static final String READING_LISTS_SYNC_OPTION = "userjs-reading-lists-v1";
    private static final ReadingListSynchronizer INSTANCE = new ReadingListSynchronizer();

    private final Handler syncHandler = new Handler(WikipediaApp.getInstance().getMainLooper());
    private final SyncRunnable syncRunnable = new SyncRunnable();

    public static ReadingListSynchronizer instance() {
        return INSTANCE;
    }

    public void bumpRevAndSync() {
        bumpRev();

        // Post the sync task with a short delay, so that possible thrashes of
        // this method don't cause a barrage of sync requests.
        syncHandler.removeCallbacks(syncRunnable);
        syncHandler.postDelayed(syncRunnable, TimeUnit.SECONDS.toMillis(1));
    }

    public void sync() {
        if (!ReleaseUtil.isPreBetaRelease()  // TODO: remove when ready for beta/production
                || !User.isLoggedIn()
                || !(isReadingListSyncEnabled() || isReadingListsRemoteDeletePending())) {
            syncSavedPages();
            L.d("Skipped sync of reading lists.");
            return;
        }
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override
            public Void execute() {
                try {
                    UserInfo info = UserOptionDataClientSingleton.instance().get();

                    synchronized (ReadingListSynchronizer.this) {
                        long localRev = Prefs.getReadingListSyncRev();
                        RemoteReadingLists remoteReadingLists = null;

                        for (UserOption option : info.userjsOptions()) {
                            if (READING_LISTS_SYNC_OPTION.equals(option.key())) {
                                remoteReadingLists = GsonUnmarshaller
                                        .unmarshal(RemoteReadingLists.class, option.val());
                            }
                        }

                        if ((remoteReadingLists == null) || (remoteReadingLists.rev() < localRev)) {
                            if (localRev == 0) {
                                // If this is the first time we're syncing, bump the rev explicitly.
                                bumpRev();
                            }
                            L.d("Pushing local reading lists to server.");
                            UserOptionDataClientSingleton.instance()
                                    .post(new UserOption(READING_LISTS_SYNC_OPTION,
                                            GsonMarshaller.marshal(makeRemoteReadingLists())));
                        } else if (localRev < remoteReadingLists.rev()) {
                            L.d("Updating local reading lists from server.");
                            reconcileAsRightJoin(remoteReadingLists);
                            Prefs.setReadingListSyncRev(remoteReadingLists.rev());
                            WikipediaApp.getInstance().getOnboardingStateMachine().setReadingListTutorial();
                        } else {
                            L.d("Local and remote reading lists are in sync.");
                            if (isReadingListsRemoteDeletePending()) {
                                deleteRemoteReadingLists();
                            }
                        }
                    }
                    syncSavedPages();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }, null);
    }

    public void syncSavedPages() {
        WikipediaApp.getInstance().startService(new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));
    }

    private void deleteRemoteReadingLists() {
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() throws Throwable {
                UserOptionDataClientSingleton.instance().post(new UserOption(READING_LISTS_SYNC_OPTION, null));
                return null;
            }
        }, new CallbackTask.Callback<Void>() {
            @Override public void success(Void result) {
                Prefs.setReadingListsRemoteDeletePending(false);
            }
            @Override public void failure(Throwable caught) {
                L.e("Failed to delete remote reading lists", caught);
            }
        });
    }

    private class SyncRunnable implements Runnable {
        @Override
        public void run() {
            sync();
        }
    }

    private void bumpRev() {
        Prefs.setReadingListSyncRev(Prefs.getReadingListSyncRev() + 1);
    }

    private void reconcileAsRightJoin(@NonNull RemoteReadingLists remoteReadingLists) {
        List<ReadingList> localLists = ReadingListData.instance().queryMruLists(null);
        List<RemoteReadingList> remoteLists = remoteReadingLists.lists();

        // Remove any pages that already exist in local lists from remote lists.
        // At the end of this loop, whatever is left in remoteLists will be added.
        for (ReadingList localList : localLists) {
            for (RemoteReadingList remoteList : remoteLists) {

                if (!localList.getTitle().equals(remoteList.title())) {
                    continue;
                }

                for (int localPageIndex = 0; localPageIndex < localList.getPages().size(); localPageIndex++) {
                    ReadingListPage localPage = localList.getPages().get(localPageIndex);

                    boolean deleteLocalPage = true;
                    for (int remotePageIndex = 0; remotePageIndex < remoteList.pages().size(); remotePageIndex++) {
                        RemoteReadingListPage remotePage = remoteList.pages().get(remotePageIndex);
                        if (localPage.title().equals(remotePage.title())
                                && localPage.namespace().code() == remotePage.namespace()
                                && localPage.wikiSite().languageCode().equals(remotePage.lang())) {
                            remoteList.pages().remove(remotePageIndex--);
                            deleteLocalPage = false;
                        }
                    }

                    if (deleteLocalPage) {
                        ReadingList.DAO.removeTitleFromList(localList, localPage);
                        localPageIndex--;
                    }
                }
            }
        }

        // Delete local list(s) if they're not present in remote lists,
        // and/or update list properties.
        for (ReadingList localList : localLists) {
            boolean deleteList = true;
            for (RemoteReadingList remoteList : remoteLists) {
                if (remoteList.title().equals(localList.getTitle())) {
                    // if this list title still matches one of the remote lists,
                    // then rescue it from deletion, and update its metadata.
                    deleteList = false;
                    localList.setDescription(remoteList.desc());
                    ReadingList.DAO.saveListInfo(localList);
                }
            }
            if (deleteList) {
                while (localList.getPages().size() > 0) {
                    ReadingList.DAO.removeTitleFromList(localList, localList.getPages().get(0));
                }
                ReadingList.DAO.removeList(localList);
            }
        }

        createPagesFromRemoteLists(localLists, remoteLists);
    }

    private void createPagesFromRemoteLists(@NonNull List<ReadingList> localLists,
                                            @NonNull List<RemoteReadingList> remoteLists) {
        for (RemoteReadingList remoteList : remoteLists) {

            ReadingList localList = null;
            // do we need to create a new list?
            for (ReadingList list : localLists) {
                if (remoteList.title().equals(list.getTitle())) {
                    localList = list;
                    break;
                }
            }
            if (localList == null) {
                long now = System.currentTimeMillis();
                localList = ReadingList.builder()
                        .key(ReadingListDaoProxy.listKey(remoteList.title()))
                        .title(remoteList.title())
                        .mtime(now)
                        .atime(now)
                        .description(remoteList.desc())
                        .pages(new ArrayList<ReadingListPage>())
                        .build();
                ReadingList.DAO.addList(localList);
                localLists.add(localList);
            }

            for (RemoteReadingListPage remotePage : remoteList.pages()) {
                createPage(localLists, localList, remotePage);
            }
        }
    }

    private void createPage(@NonNull List<ReadingList> allLists, @NonNull ReadingList listForPage,
                            @NonNull RemoteReadingListPage remotePage) {
        ReadingListPage localPage = null;
        // does this page already exist in another list?
        for (ReadingList list : allLists) {
            for (ReadingListPage page : list.getPages()) {
                if (page.title().equals(remotePage.title())
                        && page.namespace().code() == remotePage.namespace()
                        && page.wikiSite().languageCode().equals(remotePage.lang())) {
                    localPage = page;
                    break;
                }
            }
        }
        if (localPage == null) {
            localPage = ReadingListDaoProxy.page(listForPage,
                    new PageTitle(Namespace.of(remotePage.namespace()).toLegacyString(),
                            remotePage.title(),
                            WikiSite.forLanguageCode(remotePage.lang())));
        }
        ReadingList.DAO.addTitleToList(listForPage, localPage, false);
    }

    @NonNull
    private static RemoteReadingLists makeRemoteReadingLists() {
        List<ReadingList> lists = ReadingListData.instance().queryMruLists(null);
        return new RemoteReadingLists(Prefs.getReadingListSyncRev(), lists);
    }
}
