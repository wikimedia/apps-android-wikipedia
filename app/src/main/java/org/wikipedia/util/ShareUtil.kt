package org.wikipedia.util

import android.app.DownloadManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.os.TransactionTooLargeException
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import org.wikipedia.BuildConfig
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.gallery.MediaDownloadReceiver
import org.wikipedia.json.JsonUtil
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DateUtil.getFeedCardDateString
import org.wikipedia.util.log.L
import java.io.File
import java.io.OutputStreamWriter

object ShareUtil {
    private const val APP_PACKAGE_REGEX = "org\\.wikipedia.*"
    private const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"
    private const val FILE_PREFIX = "file://"

    fun shareText(context: Context, subject: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        shareIntent.type = "text/plain"

        try {
            val chooserIntent = getIntentChooser(context, shareIntent, context.getString(R.string.share_via))
            if (chooserIntent == null) {
                showUnresolvableIntentMessage(context)
            } else {
                context.startActivity(chooserIntent)
            }
        } catch (e: TransactionTooLargeException) {
            L.logRemoteErrorIfProd(RuntimeException("Transaction too large for share intent."))
        }
    }

    fun shareText(context: Context, title: PageTitle, withProvenance: Boolean = true) {
        shareText(context, StringUtil.fromHtml(title.displayText).toString(),
                if (withProvenance) UriUtil.getUrlWithProvenance(context, title, R.string.prov_share_link) else title.uri)
    }

    fun shareText(context: Context, title: PageTitle, newId: Long, oldId: Long) {
        shareText(context, StringUtil.fromHtml(title.displayText).toString(),
                title.getWebApiUrl("diff=$newId&oldid=$oldId&variant=${title.wikiSite.languageCode}"))
    }

    fun shareImage(context: Context, bmp: Bitmap,
                   imageFileName: String, subject: String, text: String) {
        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, msg ->
            run {
                displayOnCatchMessage(msg, context)
            }
        }) {
            val uri = withContext(Dispatchers.IO) { getUriFromFile(context, processBitmapForSharing(context, bmp, imageFileName)) }
            if (uri == null) {
                displayShareErrorMessage(context)
            } else {
                val chooserIntent = buildImageShareChooserIntent(context, subject, text, uri)
                context.startActivity(chooserIntent)
            }
        }
    }

    fun getFeaturedImageShareSubject(context: Context, age: Int): String {
        return context.getString(R.string.feed_featured_image_share_subject) + " | " + getFeedCardDateString(age)
    }

    private fun buildImageShareChooserIntent(context: Context, subject: String,
                                             text: String, uri: Uri): Intent? {
        val shareIntent = createImageShareIntent(subject, text, uri)
        return getIntentChooser(context, shareIntent,
                context.resources.getString(R.string.image_share_via))
    }

    private fun getUriFromFile(context: Context, file: File?): Uri? {
        if (file == null) {
            return null
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        else Uri.parse(FILE_PREFIX + file.absolutePath)
    }

    private fun processBitmapForSharing(context: Context, bmp: Bitmap,
                                        imageFileName: String): File? {
        val shareFolder = getClearShareFolder(context) ?: return null
        shareFolder.mkdirs()
        val bytes = FileUtil.compressBmpToJpg(bmp)
        var fileName = cleanFileName(imageFileName)
        // ensure file name ends with .jpg
        if (!fileName.endsWith(".jpg")) {
            fileName = "$fileName.jpg"
        }
        return FileUtil.writeToFile(bytes, File(shareFolder, fileName))
    }

    private fun createImageShareIntent(subject: String, text: String, uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, subject)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("image/jpeg")
    }

    private fun displayOnCatchMessage(caught: Throwable, context: Context) {
        Toast.makeText(context, context.getString(R.string.gallery_share_error,
                caught.localizedMessage),
                Toast.LENGTH_SHORT).show()
    }

    private fun displayShareErrorMessage(context: Context) {
        Toast.makeText(context, context.getString(R.string.gallery_share_error,
                context.getString(R.string.err_cannot_save_file)),
                Toast.LENGTH_SHORT).show()
    }

    fun showUnresolvableIntentMessage(context: Context?) {
        Toast.makeText(context, R.string.error_can_not_process_link, Toast.LENGTH_LONG).show()
    }

    private fun getClearShareFolder(context: Context): File? {
        try {
            val dir = File(getShareFolder(context), "share")
            FileUtil.deleteRecursively(dir)
            return dir
        } catch (caught: Throwable) {
            L.e("Caught " + caught.message, caught)
        }
        return null
    }

    private fun getShareFolder(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.cacheDir
        else context.getExternalFilesDir(null)!!
    }

    private fun cleanFileName(fileName: String): String {
        // Google+ doesn't like file names that have characters %28, %29, %2C
        var fileNameStr = fileName
        fileNameStr = fileNameStr.replace("%2[0-9A-F]".toRegex(), "_")
                .replace("[^0-9a-zA-Z-_.]".toRegex(), "_")
                .replace("_+".toRegex(), "_")
        return fileNameStr
    }

    fun getIntentChooser(context: Context, intent: Intent, chooserTitle: CharSequence? = null): Intent? {
        val infoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val excludedComponents = HashSet<ComponentName>()
        infoList.forEach {
            val activityInfo = it.activityInfo
            val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
            if (isSelfComponentName(componentName))
                excludedComponents.add(componentName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return if (excludedComponents.size >= infoList.size) null
            else Intent.createChooser(intent, chooserTitle)
                .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
        }
        if (infoList.isEmpty()) {
            return null
        }
        val targetIntents = ArrayList<Intent>()
        for (info in infoList) {
            val activityInfo = info.activityInfo
            if (excludedComponents.contains(ComponentName(activityInfo.packageName, activityInfo.name)))
                continue
            val targetIntent = Intent(intent)
                    .setPackage(activityInfo.packageName)
                    .setComponent(ComponentName(activityInfo.packageName, activityInfo.name))
            targetIntents.add(LabeledIntent(targetIntent, activityInfo.packageName, info.labelRes, info.icon))
        }
        val chooserIntent = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent.createChooser(Intent(), chooserTitle)
        } else {
            Intent.createChooser(targetIntents.removeAt(0), chooserTitle)
        }) ?: return null

        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray<Parcelable>())
        return chooserIntent
    }

    fun canOpenUrlInApp(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .any { it.activityInfo.packageName.matches(APP_PACKAGE_REGEX.toRegex()) }
    }

    private fun isSelfComponentName(componentName: ComponentName): Boolean {
        return componentName.packageName.matches(APP_PACKAGE_REGEX.toRegex())
    }




    fun shareReadingList(context: Context, readingList: ReadingList?) {
        if (readingList == null) {
            return
        }
        try {
            val payload = JsonUtil.encodeToString(readingList)
            val shareFolder = getClearShareFolder(context)
            shareFolder!!.mkdirs()
            val f = File(shareFolder, cleanFileName(readingList.title) + ".wikilist")
            val fo = f.outputStream()
            fo.write(payload!!.encodeToByteArray())
            fo.flush()
            fo.close()

            val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, "Reading list: " + readingList.title)
                    .putExtra(Intent.EXTRA_TEXT, "Hi! I'd like to share my reading list with you. Please tap on the attached file to open it in the Wikipedia app.")
                    .putExtra(Intent.EXTRA_STREAM, getUriFromFile(context, f))
                    .setType("application/json")

            context.startActivity(intent)
        } catch (e: Exception) {
            L.e(e)
        }
    }

    fun exportReadingListCsv(context: Context, readingList: ReadingList?, downloadReceiver: MediaDownloadReceiver) {
        if (readingList == null) {
            return
        }
        try {
            val fileName = cleanFileName(readingList.title) + ".csv"

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
}
