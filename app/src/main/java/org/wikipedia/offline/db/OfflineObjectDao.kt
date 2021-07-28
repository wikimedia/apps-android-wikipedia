package org.wikipedia.offline.db

import androidx.room.*
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L
import java.io.File

@Dao
interface OfflineObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOfflineObject(obj: OfflineObject)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateOfflineObject(obj: OfflineObject)

    @Query("SELECT * FROM OfflineObject WHERE url = :url AND lang = :lang LIMIT 1")
    fun getOfflineObject(url: String, lang: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE url = :url LIMIT 1")
    fun getOfflineObject(url: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE url LIKE '%/' || :urlFragment || '/%' LIMIT 1")
    fun searchForOfflineObject(urlFragment: String): OfflineObject?

    @Query("SELECT * FROM OfflineObject WHERE usedByStr LIKE '%|' || :id || '|%'")
    fun getFromUsedById(id: Long): List<OfflineObject>

    @Delete
    fun deleteOfflineObject(obj: OfflineObject)

    @Query("DELETE FROM OfflineObject")
    fun deleteAll()

    fun findObject(url: String, lang: String?): OfflineObject? {
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

    fun addObject(url: String, lang: String, path: String, pageTitle: String) {
        // first find this item if it already exists in the db
        var obj = getOfflineObject(url, lang)

        var doInsert = false
        if (obj == null) {
            obj = OfflineObject(url = url, lang = lang, path = path, status = 0)
            doInsert = true
        }

        // try to find the associated title in a reading list, and add its id to the usedBy list.
        val page = AppDatabase.getAppDatabase().readingListPageDao().findPageInAnyList(
            PageTitle(pageTitle, WikiSite.forLanguageCode(lang))
        )
        if (page != null && !obj.usedBy.contains(page.id)) {
            obj.addUsedBy(page.id)
        }
        if (doInsert) {
            insertOfflineObject(obj)
        } else {
            if (path != obj.path) {
                L.w("Existing offline object path is inconsistent.")
            }
            updateOfflineObject(obj)
        }
    }

    fun deleteObjectsForPageId(id: Long) {
        val objects = mutableListOf<OfflineObject>()
        val objUsedBy = getFromUsedById(id)

        objUsedBy.forEach {
            if (it.usedBy.contains(id)) {
                it.removeUsedBy(id)
                objects.add(it)
            }
        }

        for (obj in objects) {
            if (obj.usedBy.isEmpty()) {
                // the object is now an orphan, so remove it!
                deleteOfflineObject(obj)
                deleteFilesForObject(obj)
            } else {
                updateOfflineObject(obj)
            }
        }
    }

    fun getTotalBytesForPageId(id: Long): Long {
        var totalBytes: Long = 0
        try {
            totalBytes = getFromUsedById(id).sumOf { File("${it.path}.1").length() }
        } catch (e: Exception) {
            L.w(e)
        }
        return totalBytes
    }

    fun deleteFilesForObject(obj: OfflineObject) {
        try {
            val metadataFile = File(obj.path + ".0")
            val contentsFile = File(obj.path + ".1")
            metadataFile.delete()
            contentsFile.delete()
        } catch (e: Exception) {
            // ignore
        }
    }
}
