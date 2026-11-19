package org.wikipedia.offline.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.wikipedia.database.AppDatabase
import org.wikipedia.offline.OfflineCacheWrite
import org.wikipedia.page.Namespace
import java.io.File

@Dao
interface OfflineObjectDao {
    @Query("SELECT * FROM OfflineObject")
    suspend fun getAllObjects(): List<OfflineObject>

    @Query("SELECT * FROM OfflineObject")
    fun observeAllObjects(): Flow<List<OfflineObject>>

    suspend fun attachObjectsToPages(objects: List<OfflineObject>, sourceIds: Set<Long>, targetIds: Set<Long>) {
        objects.filter { obj -> obj.usedBy.any { it in sourceIds } }.forEach { obj ->
            targetIds.forEach { obj.addUsedBy(it) }
            updateOfflineObject(obj)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineObject(obj: OfflineObject)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateOfflineObject(obj: OfflineObject)

    @Query("SELECT * FROM OfflineObject WHERE url = :url AND lang = :lang LIMIT 1")
    suspend fun getOfflineObject(url: String, lang: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE url = :url LIMIT 1")
    suspend fun getOfflineObject(url: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE url LIKE '%/' || :urlFragment || '/%' LIMIT 1")
    suspend fun searchForOfflineObject(urlFragment: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE url LIKE '%' || :urlFragment || '%'")
    suspend fun searchForOfflineObjects(urlFragment: String): List<OfflineObject>

    @Delete
    suspend fun deleteOfflineObject(obj: OfflineObject)

    @Transaction
    suspend fun findObject(url: String, lang: String?): OfflineObject? {
        var obj = if (lang.isNullOrEmpty()) getOfflineObject(url) else getOfflineObject(url, lang)

        // Couldn't find an exact match, so...
        // If we're trying to load an image from Commons, try to look for any other resolution.
        if (obj == null && url.contains("/commons/thumb/")) {
            val parts = url.split("/").toTypedArray()
            if (parts.size > 2) {
                val fileName = parts[parts.size - 2].replace("'".toRegex(), "%27")
                obj = searchForOfflineObject(fileName)
            }
        }
        return obj
    }

    @Query("SELECT * FROM OfflineObject WHERE url = :url")
    suspend fun getObjectsByUrl(url: String): List<OfflineObject>

    @Query("SELECT * FROM OfflineObject WHERE lang = :lang AND url IN (:urls)")
    suspend fun getObjectsByUrls(lang: String, urls: Set<String>): List<OfflineObject>

    @Query("SELECT DISTINCT path FROM OfflineObject WHERE path IN (:paths)")
    suspend fun getReferencedPaths(paths: List<String>): List<String>

    @Query("SELECT * FROM OfflineObject WHERE EXISTS (SELECT 1 FROM ReadingListPage WHERE wiki = :wiki AND lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle AND offline = 1 AND status != 2 AND OfflineObject.usedByStr LIKE '%|' || ReadingListPage.id || '|%')")
    suspend fun getObjectsForArticle(wiki: String, lang: String, namespace: Namespace, apiTitle: String): List<OfflineObject>

    @Transaction
    suspend fun publishResponse(staged: OfflineObject, path: String, write: OfflineCacheWrite?, sharedImage: Boolean): Set<String>? {
        // Untagged responses are from Talk, which has its own cache lifecycle.
        val owners = if (write == null) emptyList() else {
            if (!write.isCurrent()) return null
            AppDatabase.instance.readingListPageDao().getAllPageOccurrences(write.key).filter { it.offline }
                .also { if (it.isEmpty()) return null }
        }
        val previous = if (sharedImage) getObjectsByUrl(staged.url) else listOfNotNull(getOfflineObject(staged.url, staged.lang))
        if (write?.isCurrent() == false || File(staged.path + ".1").length() == 0L) return null

        if (!File(staged.path + ".0").renameTo(File(path + ".0")) ||
            !File(staged.path + ".1").renameTo(File(path + ".1"))) return null

        val existing = previous.firstOrNull { it.lang == staged.lang }
        val published = (existing ?: staged).copy(path = path)
        owners.forEach { published.addUsedBy(it.id) }
        if (existing == null) insertOfflineObject(published) else updateOfflineObject(published)
        previous.filter { it.id != existing?.id }.forEach { updateOfflineObject(it.copy(path = path)) }
        return previous.mapTo(mutableSetOf()) { it.path }
    }

    @Transaction
    suspend fun deleteUnreferencedFiles(paths: Set<String>) {
        paths.chunked(900).forEach { batch ->
            val referenced = getReferencedPaths(batch).toSet()
            batch.filterNot { it in referenced }.forEach { deleteFilesAtPath(it) }
        }
    }

    @Transaction
    suspend fun detachObjectsFromPages(ids: List<Long>): Set<String> {
        if (ids.isEmpty()) return emptySet()
        val removedIds = ids.toSet()
        val detachedPaths = mutableSetOf<String>()
        getAllObjects().forEach { obj ->
            val ownersToRemove = obj.usedBy.filter { it in removedIds }
            if (ownersToRemove.isNotEmpty()) {
                ownersToRemove.forEach { obj.removeUsedBy(it) }
                if (obj.usedBy.isEmpty()) {
                    deleteOfflineObject(obj)
                    detachedPaths.add(obj.path)
                } else {
                    updateOfflineObject(obj)
                }
            }
        }
        return detachedPaths
    }

    fun deleteFilesForObject(obj: OfflineObject) = deleteFilesAtPath(obj.path)

    private fun deleteFilesAtPath(path: String) {
        File(path + ".0").delete()
        File(path + ".1").delete()
    }
}
