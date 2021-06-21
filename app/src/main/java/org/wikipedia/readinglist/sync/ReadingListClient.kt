package org.wikipedia.readinglist.sync

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.readinglist.sync.SyncedReadingLists.*
import retrofit2.Response
import java.io.IOException
import java.util.*

class ReadingListClient(private val wiki: WikiSite) {
    var lastDateHeader: String? = null
        private set

    /**
     * Sets up reading list syncing on the server, and returns true if the setup was successful,
     * or false if syncing is already set up.
     */
    @Throws(Throwable::class)
    fun setup(csrfToken: String): Boolean {
        return try {
            ServiceFactory.getRest(wiki).setupReadingLists(csrfToken).execute()
            true
        } catch (t: Throwable) {
            if (isErrorType(t, "already-set-up")) {
                return false
            }
            throw t
        }
    }

    @Throws(Throwable::class)
    fun tearDown(csrfToken: String) {
        try {
            ServiceFactory.getRest(wiki).tearDownReadingLists(csrfToken).execute()
        } catch (t: Throwable) {
            if (isErrorType(t, "not-set-up")) {
                return
            }
            throw t
        }
    }

    @get:Throws(Throwable::class)
    val allLists: List<RemoteReadingList>
        get() {
            val totalLists = mutableListOf<RemoteReadingList>()
            var totalCycles = 0
            var continueStr: String? = null
            do {
                val response = ServiceFactory.getRest(wiki).getReadingLists(continueStr).execute()
                val lists = response.body()
                if (lists?.lists == null) {
                    throw IOException("Incorrect response format.")
                }
                totalLists.addAll(lists.lists)
                continueStr = if (lists.continueStr.isNullOrEmpty()) null else lists.continueStr
                saveLastDateHeader(response)
            } while (!continueStr.isNullOrEmpty() && totalCycles++ < MAX_CONTINUE_CYCLES)
            return totalLists
        }

    @Throws(Throwable::class)
    fun getChangesSince(date: String): SyncedReadingLists {
        val totalLists = mutableListOf<RemoteReadingList>()
        val totalEntries = mutableListOf<RemoteReadingListEntry>()
        var totalCycles = 0
        var continueStr: String? = null
        do {
            val response = ServiceFactory.getRest(wiki).getReadingListChangesSince(date, continueStr).execute()
            val body = response.body() ?: throw IOException("Incorrect response format.")
            body.lists?.let {
                totalLists.addAll(it)
            }
            body.entries?.let {
                totalEntries.addAll(it)
            }
            continueStr = if (body.continueStr.isNullOrEmpty()) null else body.continueStr
            saveLastDateHeader(response)
        } while (!continueStr.isNullOrEmpty() && totalCycles++ < MAX_CONTINUE_CYCLES)
        return SyncedReadingLists(totalLists, totalEntries)
    }

    @Throws(Throwable::class)
    @Suppress("unused")
    fun getListsContaining(entry: RemoteReadingListEntry): List<RemoteReadingList> {
        val totalLists: MutableList<RemoteReadingList> = ArrayList()
        var totalCycles = 0
        var continueStr: String? = null
        do {
            val response = ServiceFactory.getRest(wiki)
                    .getReadingListsContaining(entry.project(), entry.title(), continueStr).execute()
            val lists = response.body()
            if (lists?.lists == null) {
                throw IOException("Incorrect response format.")
            }
            totalLists.addAll(lists.lists)
            continueStr = if (lists.continueStr.isNullOrEmpty()) null else lists.continueStr
            saveLastDateHeader(response)
        } while (!continueStr.isNullOrEmpty() && totalCycles++ < MAX_CONTINUE_CYCLES)
        return totalLists
    }

    @Throws(Throwable::class)
    fun getListEntries(listId: Long): List<RemoteReadingListEntry> {
        val totalEntries = mutableListOf<RemoteReadingListEntry>()
        var totalCycles = 0
        var continueStr: String? = null
        do {
            val response = ServiceFactory.getRest(wiki).getReadingListEntries(listId, continueStr).execute()
            val body = response.body()
            if (body?.entries == null) {
                throw IOException("Incorrect response format.")
            }
            totalEntries.addAll(body.entries)
            continueStr = if (body.continueStr.isNullOrEmpty()) null else body.continueStr
            saveLastDateHeader(response)
        } while (!continueStr.isNullOrEmpty() && totalCycles++ < MAX_CONTINUE_CYCLES)
        return totalEntries
    }

    @Throws(Throwable::class)
    fun createList(csrfToken: String, list: RemoteReadingList): Long {
        val response = ServiceFactory.getRest(wiki).createReadingList(csrfToken, list).execute()
        val idResponse = response.body() ?: throw IOException("Incorrect response format.")
        saveLastDateHeader(response)
        return idResponse.id
    }

    @Throws(Throwable::class)
    fun updateList(csrfToken: String, listId: Long, list: RemoteReadingList) {
        saveLastDateHeader(ServiceFactory.getRest(wiki).updateReadingList(listId, csrfToken, list).execute())
    }

    @Throws(Throwable::class)
    fun deleteList(csrfToken: String, listId: Long) {
        saveLastDateHeader(ServiceFactory.getRest(wiki).deleteReadingList(listId, csrfToken).execute())
    }

    @Throws(Throwable::class)
    fun addPageToList(csrfToken: String, listId: Long, entry: RemoteReadingListEntry): Long {
        val response = ServiceFactory.getRest(wiki).addEntryToReadingList(listId, csrfToken, entry).execute()
        val idResponse = response.body() ?: throw IOException("Incorrect response format.")
        saveLastDateHeader(response)
        return idResponse.id
    }

    @Throws(Throwable::class)
    fun addPagesToList(csrfToken: String, listId: Long, entries: List<RemoteReadingListEntry>): List<Long> {
        val maxBatchSize = 50
        var batchIndex = 0
        val ids = mutableListOf<Long>()
        val currentBatch = mutableListOf<RemoteReadingListEntry>()
        while (true) {
            currentBatch.clear()
            while (batchIndex < entries.size && currentBatch.size < maxBatchSize) {
                currentBatch.add(entries[batchIndex++])
            }
            if (currentBatch.isEmpty()) {
                break
            }
            try {
                val response = ServiceFactory.getRest(wiki).addEntriesToReadingList(listId, csrfToken, RemoteReadingListEntryBatch(currentBatch)).execute()
                val idResponse = response.body() ?: throw IOException("Incorrect response format.")
                saveLastDateHeader(response)
                for (id in idResponse.batch) {
                    ids.add(id.id)
                }
            } catch (t: Throwable) {
                if (isErrorType(t, "entry-limit")) {
                    // TODO: handle more meaningfully than ignoring, for now.
                    break
                }
                throw t
            }
        }
        return ids
    }

    @Throws(Throwable::class)
    fun deletePageFromList(csrfToken: String, listId: Long, entryId: Long) {
        saveLastDateHeader(ServiceFactory.getRest(wiki).deleteEntryFromReadingList(listId, entryId, csrfToken).execute())
    }

    fun isErrorType(t: Throwable?, errorType: String): Boolean {
        return t is HttpStatusException && t.serviceError?.title?.contains(errorType) == true
    }

    fun isServiceError(t: Throwable?): Boolean {
        return t is HttpStatusException && t.code == 400
    }

    fun isUnavailableError(t: Throwable?): Boolean {
        return t is HttpStatusException && t.code == 405
    }

    private fun saveLastDateHeader(response: Response<*>) {
        lastDateHeader = response.headers()["date"]
    }

    companion object {
        // Artificial upper limit on the number of continuation cycles we can do, to prevent
        // getting stuck in an infinite loop.
        private const val MAX_CONTINUE_CYCLES = 256
    }
}
