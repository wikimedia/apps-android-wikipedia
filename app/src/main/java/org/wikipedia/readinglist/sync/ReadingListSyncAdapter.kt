package org.wikipedia.readinglist.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.text.TextUtils
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ReadingListsEnableDialogEvent
import org.wikipedia.events.ReadingListsEnabledStatusEvent
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingList
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntry
import org.wikipedia.savedpages.SavedPageSyncService
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.text.ParseException

class ReadingListSyncAdapter : AbstractThreadedSyncAdapter {
    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize)
    constructor(context: Context, autoInitialize: Boolean, allowParallelSyncs: Boolean) : super(context, autoInitialize, allowParallelSyncs)

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        if (isDisabledByRemoteConfig || !AccountUtil.isLoggedIn ||
                !(Prefs.isReadingListSyncEnabled() || Prefs.isReadingListsRemoteDeletePending())) {
            L.d("Skipping sync of reading lists.")
            if (extras.containsKey(SYNC_EXTRAS_REFRESHING)) {
                SavedPageSyncService.sendSyncEvent()
            }
            return
        }
        L.d("Begin sync of reading lists...")
        val csrfToken = mutableListOf<String>()
        val listIdsDeleted = Prefs.getReadingListsDeletedIds()
        val pageIdsDeleted = Prefs.getReadingListPagesDeletedIds()
        var allLocalLists: MutableList<ReadingList>? = null
        val wiki = WikipediaApp.getInstance().wikiSite
        val client = ReadingListClient(wiki)
        val readingListSyncNotification = ReadingListSyncNotification.instance
        var lastSyncTime = Prefs.getReadingListsLastSyncTime()
        var shouldSendSyncEvent = extras.containsKey(SYNC_EXTRAS_REFRESHING)
        var shouldRetry = false
        var shouldRetryWithForce = false
        try {
            IN_PROGRESS = true
            var syncEverything = false
            if (extras.containsKey(SYNC_EXTRAS_FORCE_FULL_SYNC) ||
                    Prefs.isReadingListsRemoteDeletePending() ||
                    Prefs.isReadingListsRemoteSetupPending()) {
                // reset the remote ID on all lists, since they will need to be recreated next time.
                L.d("Resetting all lists to un-synced.")
                syncEverything = true
                ReadingListDbHelper.instance().markEverythingUnsynced()
                allLocalLists = ReadingListDbHelper.instance().allLists
            }
            if (Prefs.isReadingListsRemoteDeletePending()) {
                // Are we scheduled for a teardown? If so, delete everything and bail.
                L.d("Tearing down remote lists...")
                client.tearDown(getCsrfToken(wiki, csrfToken))
                Prefs.setReadingListsRemoteDeletePending(false)
                return
            } else if (Prefs.isReadingListsRemoteSetupPending()) {
                // ...Or are we scheduled for setup?
                client.setup(getCsrfToken(wiki, csrfToken))
                Prefs.setReadingListsRemoteSetupPending(false)
            }

            // -----------------------------------------------
            // PHASE 1: Sync from remote to local.
            // -----------------------------------------------
            var remoteListsModified = mutableListOf<RemoteReadingList>()
            var remoteEntriesModified = mutableListOf<RemoteReadingListEntry>()
            if (TextUtils.isEmpty(lastSyncTime)) {
                syncEverything = true
            }
            if (syncEverything) {
                if (allLocalLists == null) {
                    allLocalLists = ReadingListDbHelper.instance().allLists
                }
            } else {
                if (allLocalLists == null) {
                    allLocalLists = ReadingListDbHelper.instance().allListsWithUnsyncedPages
                }
                L.d("Fetching changes from server, since $lastSyncTime")
                val allChanges = client.getChangesSince(lastSyncTime)
                allChanges.lists?.let {
                    remoteListsModified = it as MutableList<RemoteReadingList>
                }
                allChanges.entries?.let {
                    remoteEntriesModified = it as MutableList<RemoteReadingListEntry>
                }
            }

            // Perform a quick check for whether we'll need to sync all lists
            for (remoteEntry in remoteEntriesModified) {
                // find the list to which this entry belongs...
                val eigenLocalList = allLocalLists.find { it.remoteId == remoteEntry.listId }
                val eigenRemoteList = remoteListsModified.find { it.id == remoteEntry.listId }

                if (eigenLocalList == null && eigenRemoteList == null) {
                    L.w("Remote entry belongs to an unknown local list. Falling back to full sync.")
                    syncEverything = true
                    break
                }
            }
            if (syncEverything) {
                allLocalLists = ReadingListDbHelper.instance().allLists
                L.d("Fetching all lists from server...")
                remoteListsModified = client.allLists as MutableList<RemoteReadingList>
            }

            // Notify any event consumers that reading lists are, in fact, enabled.
            WikipediaApp.getInstance().bus.post(ReadingListsEnabledStatusEvent())

            // setup syncing indicator for remote to local
            val remoteItemsTotal = remoteListsModified.size

            // First, update our list hierarchy to match the remote hierarchy.
            for ((remoteItemsSynced, remoteList) in remoteListsModified.withIndex()) {
                readingListSyncNotification.setNotificationProgress(context, remoteItemsTotal, remoteItemsSynced)
                // Find the remote list in our local lists...
                var localList: ReadingList? = null
                var upsertNeeded = false
                for (list in allLocalLists!!) {
                    if (list.isDefault && remoteList.isDefault) {
                        localList = list
                        if (list.remoteId != remoteList.id) {
                            list.remoteId = remoteList.id
                            upsertNeeded = true
                        }
                        break
                    }
                    if (list.remoteId == remoteList.id) {
                        localList = list
                        break
                    } else if (StringUtil.normalizedEquals(list.title, remoteList.name())) {
                        localList = list
                        localList.remoteId = remoteList.id
                        upsertNeeded = true
                        break
                    }
                }
                if (remoteList.isDefault && localList != null && !localList.isDefault) {
                    L.logRemoteError(RuntimeException("Unexpected: remote default list corresponds to local non-default list."))
                    localList = ReadingListDbHelper.instance().defaultList
                }
                if (remoteList.isDeleted) {
                    if (localList != null && !localList.isDefault) {
                        L.d("Deleting local list " + localList.title)
                        ReadingListDbHelper.instance().deleteList(localList, false)
                        ReadingListDbHelper.instance().markPagesForDeletion(localList, localList.pages, false)
                        allLocalLists.remove(localList)
                        shouldSendSyncEvent = true
                    }
                    continue
                }
                if (localList == null) {
                    // A new list needs to be created locally.
                    L.d("Creating local list " + remoteList.name())
                    localList = if (remoteList.isDefault) {
                        L.logRemoteError(RuntimeException("Unexpected: local default list no longer matches remote."))
                        ReadingListDbHelper.instance().defaultList
                    } else {
                        ReadingListDbHelper.instance().createList(remoteList.name(), remoteList.description())
                    }
                    localList.remoteId = remoteList.id
                    allLocalLists.add(localList)
                    upsertNeeded = true
                } else {
                    if (!localList.isDefault && !StringUtil.normalizedEquals(localList.title, remoteList.name())) {
                        localList.title = remoteList.name()
                        upsertNeeded = true
                    }
                    if (!localList.isDefault && !StringUtil.normalizedEquals(localList.description, remoteList.description())) {
                        localList.description = remoteList.description()
                        upsertNeeded = true
                    }
                }
                if (upsertNeeded) {
                    L.d("Updating info for local list " + localList.title)
                    localList.dirty = false
                    ReadingListDbHelper.instance().updateList(localList, false)
                    shouldSendSyncEvent = true
                }
                if (syncEverything) {
                    L.d("Fetching all pages in remote list " + remoteList.name())
                    client.getListEntries(remoteList.id).forEach {
                        // TODO: optimization opportunity -- create/update local pages in bulk.
                        createOrUpdatePage(localList, it)
                    }
                    shouldSendSyncEvent = true
                }
            }
            if (!syncEverything) {
                for (remoteEntry in remoteEntriesModified) {
                    // find the list to which this entry belongs...
                    val eigenList = allLocalLists.find { it.remoteId == remoteEntry.listId }
                    if (eigenList == null) {
                        L.w("Remote entry belongs to an unknown local list.")
                        continue
                    }
                    shouldSendSyncEvent = true
                    if (remoteEntry.isDeleted) {
                        deletePageByTitle(eigenList, pageTitleFromRemoteEntry(remoteEntry))
                    } else {
                        createOrUpdatePage(eigenList, remoteEntry)
                    }
                }
            }

            // -----------------------------------------------
            // PHASE 2: Sync from local to remote.
            // -----------------------------------------------

            // Do any remote lists need to be deleted?
            val listIdsToDelete = mutableListOf<Long>()
            listIdsToDelete.addAll(listIdsDeleted)
            for (id in listIdsToDelete) {
                L.d("Deleting remote list id $id")
                try {
                    client.deleteList(getCsrfToken(wiki, csrfToken), id)
                } catch (t: Throwable) {
                    L.w(t)
                    if (!client.isServiceError(t) && !client.isUnavailableError(t)) {
                        throw t
                    }
                }
                listIdsDeleted.remove(id)
            }

            // Do any remote pages need to be deleted?
            val pageIdsToDelete = mutableListOf<String>()
            pageIdsToDelete.addAll(pageIdsDeleted)
            for (id in pageIdsToDelete) {
                L.d("Deleting remote page id $id")
                val listAndPageId = id.split(":").toTypedArray()
                try {
                    // TODO: optimization opportunity once server starts supporting batch deletes.
                    client.deletePageFromList(getCsrfToken(wiki, csrfToken), listAndPageId[0].toLong(), listAndPageId[1].toLong())
                } catch (t: Throwable) {
                    L.w(t)
                    if (!client.isServiceError(t) && !client.isUnavailableError(t)) {
                        throw t
                    }
                }
                pageIdsDeleted.remove(id)
            }

            // setup syncing indicator for local to remote
            val localItemsTotal = allLocalLists!!.size

            // Determine whether any remote lists need to be created or updated
            for ((localItemsSynced, localList) in allLocalLists.withIndex()) {
                readingListSyncNotification.setNotificationProgress(context, localItemsTotal, localItemsSynced)
                val remoteList = RemoteReadingList(localList.title, localList.description)
                var upsertNeeded = false
                if (localList.remoteId > 0) {
                    if (!localList.isDefault && localList.dirty) {
                        // Update remote metadata for this list.
                        L.d("Updating info for remote list " + remoteList.name())
                        client.updateList(getCsrfToken(wiki, csrfToken), localList.remoteId, remoteList)
                        upsertNeeded = true
                    }
                } else if (!localList.isDefault) {
                    // This list needs to be created remotely.
                    L.d("Creating remote list " + remoteList.name())
                    val id = client.createList(getCsrfToken(wiki, csrfToken), remoteList)
                    localList.remoteId = id
                    upsertNeeded = true
                }
                if (upsertNeeded) {
                    localList.dirty = false
                    ReadingListDbHelper.instance().updateList(localList, false)
                }
            }
            for (localList in allLocalLists) {
                val localPages = mutableListOf<ReadingListPage>()
                val newEntries = mutableListOf<RemoteReadingListEntry>()
                localList.pages.forEach {
                    if (it.remoteId < 1) {
                        localPages.add(it)
                        newEntries.add(remoteEntryFromLocalPage(it))
                    }
                }
                // Note: newEntries.size() is guaranteed to be equal to localPages.size()
                if (newEntries.isEmpty()) {
                    continue
                }
                var tryOneAtATime = false
                try {
                    if (localPages.size == 1) {
                        L.d("Creating new remote page " + localPages[0].displayTitle)
                        localPages[0].remoteId = client.addPageToList(getCsrfToken(wiki, csrfToken), localList.remoteId, newEntries[0])
                        ReadingListDbHelper.instance().updatePage(localPages[0])
                    } else {
                        L.d("Creating " + newEntries.size + " new remote pages")
                        val ids = client.addPagesToList(getCsrfToken(wiki, csrfToken), localList.remoteId, newEntries)
                        for (i in ids.indices) {
                            localPages[i].remoteId = ids[i]
                        }
                        ReadingListDbHelper.instance().updatePages(localPages)
                    }
                } catch (t: Throwable) {
                    // TODO: optimization opportunity -- if the server can return the ID
                    // of the existing page(s), then we wouldn't need to do a hard sync.

                    // If the page already exists in the remote list, this means that
                    // the contents of this list have diverged from the remote list,
                    // so let's force a full sync.
                    if (client.isErrorType(t, "duplicate-page")) {
                        shouldRetryWithForce = true
                        break
                    } else if (client.isErrorType(t, "entry-limit")) {
                        // TODO: handle more meaningfully than ignoring, for now.
                    } else if (client.isErrorType(t, "no-such-project")) {
                        // Something is malformed in the page domain, but we don't know which page
                        // in the batch caused the error. Therefore, let's retry uploading the pages
                        // one at a time, and single out the one that fails.
                        tryOneAtATime = true
                    } else {
                        throw t
                    }
                }
                if (tryOneAtATime) {
                    for (i in localPages.indices) {
                        val localPage = localPages[i]
                        try {
                            L.d("Creating new remote page " + localPage.displayTitle)
                            localPage.remoteId = client.addPageToList(getCsrfToken(wiki, csrfToken), localList.remoteId, newEntries[i])
                        } catch (t: Throwable) {
                            if (client.isErrorType(t, "duplicate-page")) {
                                shouldRetryWithForce = true
                                break
                            } else if (client.isErrorType(t, "entry-limit")) {
                                // TODO: handle more meaningfully than ignoring, for now.
                            } else if (client.isErrorType(t, "no-such-project")) {
                                // Ignore the error, and give this malformed page a bogus remoteID,
                                // so that we won't try syncing it again.
                                localPage.remoteId = Int.MAX_VALUE.toLong()
                                // ...and also log it:
                                L.logRemoteError(RuntimeException("Attempted to sync malformed page: ${localPage.wiki}, ${localPage.displayTitle}"))
                            } else {
                                throw t
                            }
                        }
                    }
                    ReadingListDbHelper.instance().updatePages(localPages)
                }
            }
        } catch (t: Throwable) {
            var errorMsg = t
            if (client.isErrorType(t, "not-set-up")) {
                Prefs.setReadingListSyncEnabled(false)
                if (lastSyncTime.isNullOrEmpty()) {
                    // This means that it's our first time attempting to sync, and we see that
                    // syncing isn't enabled on the server. So, let's prompt the user to enable it:
                    WikipediaApp.getInstance().bus.post(ReadingListsEnableDialogEvent())
                } else {
                    // This can only mean that our reading lists have been torn down (disabled) by
                    // another client, so we need to notify the user of this development.
                    WikipediaApp.getInstance().bus.post(ReadingListsNoLongerSyncedEvent())
                }
            }
            if (client.isErrorType(t, "notloggedin")) {
                try {
                    L.d("Server doesn't believe we're logged in, so logging in...")
                    getCsrfToken(wiki, csrfToken)
                    shouldRetry = true
                } catch (caught: Throwable) {
                    errorMsg = caught
                }
            }
            L.w(errorMsg)
        } finally {
            lastSyncTime = getLastDateFromHeader(lastSyncTime, client)
            Prefs.setReadingListsLastSyncTime(lastSyncTime)
            Prefs.setReadingListsDeletedIds(listIdsDeleted)
            Prefs.setReadingListPagesDeletedIds(pageIdsDeleted)
            readingListSyncNotification.cancelNotification(context)
            if (shouldSendSyncEvent) {
                SavedPageSyncService.sendSyncEvent(extras.containsKey(SYNC_EXTRAS_REFRESHING))
            }
            if ((shouldRetry || shouldRetryWithForce) && !extras.containsKey(SYNC_EXTRAS_RETRYING)) {
                val b = Bundle()
                b.putAll(extras)
                b.putBoolean(SYNC_EXTRAS_RETRYING, true)
                if (shouldRetryWithForce) {
                    b.putBoolean(SYNC_EXTRAS_FORCE_FULL_SYNC, true)
                }
                manualSync(b)
            }
            IN_PROGRESS = false
            SavedPageSyncService.enqueue()
            L.d("Finished sync of reading lists.")
        }
    }

    @Throws(Throwable::class)
    private fun getCsrfToken(wiki: WikiSite, tokenList: MutableList<String>): String {
        if (tokenList.size == 0) {
            tokenList.add(CsrfTokenClient(wiki).token.blockingSingle())
        }
        return tokenList[0]
    }

    private fun getLastDateFromHeader(lastSyncTime: String, client: ReadingListClient): String {
        val lastDateHeader = client.lastDateHeader
        return if (lastDateHeader.isNullOrEmpty()) {
            lastSyncTime
        } else try {
            val date = DateUtil.getHttpLastModifiedDate(lastDateHeader)
            DateUtil.iso8601DateFormat(date)
        } catch (e: ParseException) {
            lastSyncTime
        }
    }

    private fun createOrUpdatePage(listForPage: ReadingList,
                                   remotePage: RemoteReadingListEntry) {
        val remoteTitle = pageTitleFromRemoteEntry(remotePage)
        var localPage = listForPage.pages.find { ReadingListPage.toPageTitle(it) == remoteTitle }
        var updateOnly = localPage != null

        if (localPage == null) {
            localPage = ReadingListPage(pageTitleFromRemoteEntry(remotePage))
            localPage.listId = listForPage.id
            if (ReadingListDbHelper.instance().pageExistsInList(listForPage, remoteTitle)) {
                updateOnly = true
            }
        }
        localPage.remoteId = remotePage.id
        if (remotePage.summary != null) {
            localPage.description = remotePage.summary.description
            localPage.thumbUrl = remotePage.summary.thumbnailUrl
        }
        if (updateOnly) {
            L.d("Updating local page " + localPage.displayTitle)
            ReadingListDbHelper.instance().updatePage(localPage)
        } else {
            L.d("Creating local page " + localPage.displayTitle)
            ReadingListDbHelper.instance().addPagesToList(listForPage, listOf(localPage), false)
        }
    }

    private fun deletePageByTitle(listForPage: ReadingList, title: PageTitle) {
        var localPage = listForPage.pages.find { ReadingListPage.toPageTitle(it) == title }
        if (localPage == null) {
            localPage = ReadingListDbHelper.instance().getPageByTitle(listForPage, title)
            if (localPage == null) {
                return
            }
        }
        L.d("Deleting local page " + localPage.displayTitle)
        ReadingListDbHelper.instance().markPagesForDeletion(listForPage, listOf(localPage), false)
    }

    private fun pageTitleFromRemoteEntry(remoteEntry: RemoteReadingListEntry): PageTitle {
        return PageTitle(remoteEntry.title(), WikiSite(remoteEntry.project()))
    }

    private fun remoteEntryFromLocalPage(localPage: ReadingListPage): RemoteReadingListEntry {
        val title = ReadingListPage.toPageTitle(localPage)
        return RemoteReadingListEntry(title.wikiSite.scheme() + "://" + title.wikiSite.authority(), title.prefixedText)
    }

    companion object {
        private const val SYNC_EXTRAS_FORCE_FULL_SYNC = "forceFullSync"
        private const val SYNC_EXTRAS_REFRESHING = "refreshing"
        private const val SYNC_EXTRAS_RETRYING = "retrying"
        private var IN_PROGRESS = false
        fun inProgress(): Boolean {
            return IN_PROGRESS
        }

        fun setSyncEnabledWithSetup() {
            Prefs.setReadingListSyncEnabled(true)
            Prefs.setReadingListsRemoteSetupPending(true)
            Prefs.setReadingListsRemoteDeletePending(false)
            manualSync()
        }

        val isDisabledByRemoteConfig get() = WikipediaApp.getInstance().remoteConfig.config.optBoolean("disableReadingListSync", false)

        @JvmStatic
        fun manualSyncWithDeleteList(list: ReadingList) {
            if (list.remoteId <= 0) {
                return
            }
            Prefs.addReadingListsDeletedIds(setOf(list.remoteId))
            manualSync()
        }

        @JvmStatic
        fun manualSyncWithDeletePages(list: ReadingList, pages: List<ReadingListPage>) {
            if (list.remoteId <= 0) {
                return
            }
            val ids = mutableSetOf<String>()
            pages.forEach {
                if (it.remoteId > 0) {
                    ids.add(list.remoteId.toString() + ":" + it.remoteId)
                }
            }
            if (ids.isNotEmpty()) {
                Prefs.addReadingListPagesDeletedIds(ids)
                manualSync()
            }
        }

        @JvmStatic
        fun manualSyncWithForce() {
            val extras = Bundle()
            extras.putBoolean(SYNC_EXTRAS_FORCE_FULL_SYNC, true)
            manualSync(extras)
        }

        fun manualSyncWithRefresh() {
            Prefs.setSuggestedEditsHighestPriorityEnabled(false)
            val extras = Bundle()
            extras.putBoolean(SYNC_EXTRAS_REFRESHING, true)
            manualSync(extras)
        }

        @JvmStatic
        fun manualSync() {
            manualSync(Bundle())
        }

        private fun manualSync(extras: Bundle) {
            if (AccountUtil.account() == null || !WikipediaApp.getInstance().isOnline) {
                if (extras.containsKey(SYNC_EXTRAS_REFRESHING)) {
                    SavedPageSyncService.sendSyncEvent()
                }
                return
            }
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            ContentResolver.requestSync(AccountUtil.account(), BuildConfig.READING_LISTS_AUTHORITY, extras)
        }
    }
}
