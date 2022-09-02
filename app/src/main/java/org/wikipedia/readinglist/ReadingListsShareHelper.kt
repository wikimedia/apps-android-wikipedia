package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.json.JsonUtil
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.Resource
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import java.io.File

object ReadingListsShareHelper {

    fun shareReadingList(context: Context, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }
        try {


            var shortUrl = ""

            CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
            }) {

                val param = readingListToUrlParam(readingList)
                val url = "https://mediawiki.org/wiki/ReadingList/?list=$param"

                shortUrl = ServiceFactory.get(WikipediaApp.instance.wikiSite).shortenUrl(url).shortenUrl?.shortUrl.orEmpty()


                val intent = Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_SUBJECT, "Reading list: " + readingList.title)
                        .putExtra(Intent.EXTRA_TEXT, "Hi! I'd like to share my reading list with you: $shortUrl")
                        .setType("text/plain")
                context.startActivity(intent)

            }

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

    private fun readingListToUrlParam(readingList: ReadingList): String {
        val str = StringBuilder()
        str.append(UriUtil.encodeURL(readingList.title))
        str.append("|")
        str.append(UriUtil.encodeURL(readingList.description.orEmpty()))
        str.append("|")
        readingList.pages.forEach {
            str.append(it.lang)
            str.append(":")
            str.append(UriUtil.encodeURL(it.apiTitle))
            str.append("|")
        }
        return str.toString()
    }
}
