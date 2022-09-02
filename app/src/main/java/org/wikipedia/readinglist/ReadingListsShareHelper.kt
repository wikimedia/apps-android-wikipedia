package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import java.io.File

object ReadingListsShareHelper {

    fun shareReadingList(context: Context, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }

        try {


            /*
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
                    .setType("text/plain")

            context.startActivity(intent)
            */



        } catch (e: Exception) {
            L.e(e)
        }
    }
}
