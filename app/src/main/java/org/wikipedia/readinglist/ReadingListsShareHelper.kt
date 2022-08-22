package org.wikipedia.readinglist

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import java.io.File
import java.io.OutputStreamWriter

object ReadingListsShareHelper {

    const val EXPORT_FILE_EXTENSION = "wikipedia"

    fun shareReadingList(context: Context, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }
        try {
            val payload = JsonUtil.encodeToString(ReadingListToExportedData(readingList))
            val shareFolder = ShareUtil.getClearShareFolder(context)
            shareFolder!!.mkdirs()
            val f = File(shareFolder, ShareUtil.cleanFileName(readingList.title) + "." + EXPORT_FILE_EXTENSION)
            val fo = f.outputStream()
            fo.write(payload!!.encodeToByteArray())
            fo.flush()
            fo.close()

            val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, "Reading list: " + readingList.title)
                    .putExtra(Intent.EXTRA_TEXT, "Hi! I'd like to share my reading list with you. Please tap on the attached file to open it in the Wikipedia app.")
                    .putExtra(Intent.EXTRA_STREAM, ShareUtil.getUriFromFile(context, f))
                    .setType("application/x-wikipedia")

            context.startActivity(intent)
        } catch (e: Exception) {
            L.e(e)
        }
    }

    fun exportReadingListCsv(context: Context, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }
        try {
            val fileName = ShareUtil.cleanFileName(readingList.title) + ".csv"

            /*
            val shareFolder = getClearShareFolder(context)
            shareFolder!!.mkdirs()
            val f = File(shareFolder, fileName)
            val writer = OutputStreamWriter(f.outputStream())

            readingList.pages.forEach {
                writer.appendLine(it.displayTitle + ", " + it.apiTitle)
            }
            writer.flush()
            writer.close()
            */

            val contentResolver = context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            val uri = contentResolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            contentResolver.openOutputStream(uri!!).use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    readingList.pages.forEach {
                        writer.appendLine(it.displayTitle + ", " + it.apiTitle)
                    }
                    writer.flush()
                }
            }

            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)

            val builder = NotificationCompat.Builder(context, NotificationCategory.MENTION.id)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

            val notificationText = "Your reading list was exported successfully to your Downloads."

            NotificationPresenter.showNotification(context, builder, 0,
                    "Exported \"" + readingList.title + "\"",
                    notificationText,
                    notificationText,
                    null,
                    R.drawable.ic_icon_list, R.color.accent50, intent)
        } catch (e: Exception) {
            L.e(e)
        }
    }

    private fun ReadingListToExportedData(readingList: ReadingList): ExportedData {
        return ExportedData(listOf(ExportedReadingList(readingList.title, readingList.description,
                readingList.pages.map { ExportedReadingListPage(it.lang, it.apiTitle) })))
    }

    @Serializable
    class ExportedData(
            val readinglists: List<ExportedReadingList>
    )

    @Serializable
    class ExportedReadingList(
            val title: String,
            val description: String?,
            val pages: List<ExportedReadingListPage>
    )

    @Serializable
    class ExportedReadingListPage(
            val lang: String,
            val title: String
    )
}
