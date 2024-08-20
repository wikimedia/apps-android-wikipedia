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

    @Query("SELECT * FROM OfflineObject WHERE usedByStr LIKE '%|' || :id || '|%'")
    suspend fun getFromUsedById(id: Long): List<OfflineObject>

    @Delete
    suspend fun deleteOfflineObject(obj: OfflineObject)

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

    suspend fun addObject(url: String, lang: String, path: String, pageTitle: String) {
        // first find this item if it already exists in the db
        var obj = getOfflineObject(url, lang)

        var doInsert = false
        var doModify = false
        if (obj == null) {
            obj = OfflineObject(url = url, lang = lang, path = path, status = 0)
            doInsert = true
        }

        // try to find the associated title in a reading list, and add its id to the usedBy list.
        val pages = AppDatabase.instance.readingListPageDao().getAllPageOccurrences(
            PageTitle(pageTitle, WikiSite.forLanguageCode(lang))
        )

        pages.forEach {
            if (!obj.usedBy.contains(it.id)) {
                obj.addUsedBy(it.id)
                doModify = true
            }
        }
        if (doInsert) {
            insertOfflineObject(obj)
        } else if (doModify) {
            if (path != obj.path) {
                L.w("Existing offline object path is inconsistent.")
            }
            updateOfflineObject(obj)
        }
    }

    suspend fun deleteObjectsForPageId(id: Long) {
        getFromUsedById(id)
            .filter { id in it.usedBy }
            .forEach {
                it.removeUsedBy(id)

                if (it.usedBy.isEmpty()) {
                    // the object is now an orphan, so remove it!
                    deleteOfflineObject(it)
                    deleteFilesForObject(it)
                } else {
                    updateOfflineObject(it)
                }
            }
    }

    suspend fun getTotalBytesForPageId(id: Long): Long {
        return try {
            getFromUsedById(id).sumOf { File("${it.path}.1").length() }
        } catch (e: Exception) {
            L.w(e)
            0
        }
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
