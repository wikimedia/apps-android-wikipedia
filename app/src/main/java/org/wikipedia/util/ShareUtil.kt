package org.wikipedia.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.widget.Toast
import androidx.core.content.FileProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil.getFeedCardDateString
import org.wikipedia.util.log.L.e
import java.io.File
import java.util.*

object ShareUtil {
    private const val APP_PACKAGE_REGEX = "org\\.wikipedia.*"
    private const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"
    private const val FILE_PREFIX = "file://"

    private fun shareText(context: Context, subject: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        shareIntent.type = "text/plain"
        val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_via))
        if (chooserIntent == null) {
            showUnresolvableIntentMessage(context)
        } else {
            context.startActivity(chooserIntent)
        }
    }

    @JvmStatic
    fun shareText(context: Context, title: PageTitle) {
        shareText(context, StringUtil.fromHtml(title.displayText).toString(),
                UriUtil.getUrlWithProvenance(context, title, R.string.prov_share_link))
    }

    fun shareText(context: Context, title: PageTitle, newId: Long, oldId: Long) {
        shareText(context, StringUtil.fromHtml(title.displayText).toString(),
                title.getWebApiUrl("diff=" + newId + "&oldid=" +
                        oldId + "&variant=" + title.wikiSite.languageCode()))
    }

    @JvmStatic
    @SuppressLint("CheckResult")
    fun shareImage(context: Context, bmp: Bitmap,
                   imageFileName: String, subject: String, text: String) {
        Observable.fromCallable {
            getUriFromFile(context,
                    processBitmapForSharing(context, bmp, imageFileName))
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ uri: Uri? ->
                    if (uri == null) {
                        displayShareErrorMessage(context)
                        return@subscribe
                    }
                    val chooserIntent = buildImageShareChooserIntent(context, subject, text, uri)
                    context.startActivity(chooserIntent)
                }) { caught: Throwable -> displayOnCatchMessage(caught, context) }
    }

    @JvmStatic
    fun getFeaturedImageShareSubject(context: Context, age: Int): String {
        return context.getString(R.string.feed_featured_image_share_subject) + " | " + getFeedCardDateString(age)
    }

    private fun buildImageShareChooserIntent(context: Context, subject: String,
                                             text: String, uri: Uri): Intent {
        val shareIntent = createImageShareIntent(subject, text, uri)
        return Intent.createChooser(shareIntent,
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
        return FileUtil.writeToFile(bytes, File(shareFolder, cleanFileName(imageFileName)))
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

    @JvmStatic
    fun showUnresolvableIntentMessage(context: Context?) {
        Toast.makeText(context, R.string.error_can_not_process_link, Toast.LENGTH_LONG).show()
    }

    private fun getClearShareFolder(context: Context): File? {
        try {
            val dir = File(getShareFolder(context), "share")
            FileUtil.deleteRecursively(dir)
            return dir
        } catch (caught: Throwable) {
            e("Caught " + caught.message, caught)
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
        // ensure file name ends with .jpg
        if (!fileNameStr.endsWith(".jpg")) {
            fileNameStr = "$fileNameStr.jpg"
        }
        return fileNameStr
    }

    @JvmStatic
    fun createChooserIntent(targetIntent: Intent, context: Context): Intent {
        val chooser = Intent.createChooser(targetIntent, null)
        var intents = queryIntents(context, targetIntent, false)
        if (intents.isEmpty()) {
            // This implies that the Wikipedia app itself has been chosen as the default handler
            // for our links, so we need to explicitly build a chooser that contains other activities.
            intents = queryIntents(context, targetIntent, true)
            if (intents.isNotEmpty()) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray<Parcelable?>())
            }
        }
        return chooser
    }

    private fun queryIntents(context: Context, targetIntent: Intent, replaceUri: Boolean): List<Intent> {
        val intents: MutableList<Intent> = ArrayList()
        var queryIntent = targetIntent
        if (replaceUri) {
            queryIntent = Intent(targetIntent)
            queryIntent.data = Uri.parse("https://example.com/")
        }
        for (intentActivity in context.packageManager.queryIntentActivities(queryIntent, 0)) {
            if (!isIntentActivityBlacklisted(intentActivity, APP_PACKAGE_REGEX)) {
                intents.add(buildLabeledIntent(targetIntent, intentActivity))
            }
        }
        return intents
    }

    @JvmStatic
    fun canOpenUrlInApp(context: Context, url: String): Boolean {
        var canOpen = false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        for (intentActivity in context.packageManager.queryIntentActivities(intent, 0)) {
            if (getPackageName(intentActivity).matches(APP_PACKAGE_REGEX.toRegex())) {
                canOpen = true
                break
            }
        }
        return canOpen
    }

    private fun isIntentActivityBlacklisted(intentActivity: ResolveInfo?,
                                            packageNameBlacklistRegex: String?): Boolean {
        return (intentActivity != null && getPackageName(intentActivity)
                .matches(StringUtils.defaultString(packageNameBlacklistRegex).toRegex()))
    }

    private fun buildLabeledIntent(intent: Intent, intentActivity: ResolveInfo): LabeledIntent {
        val labeledIntent = LabeledIntent(intent, intentActivity.resolvePackageName,
                intentActivity.labelRes, intentActivity.iconResource)
        labeledIntent.setPackage(getPackageName(intentActivity))
        labeledIntent.setClassName(getPackageName(intentActivity), intentActivity.activityInfo.name)
        return labeledIntent
    }

    private fun getPackageName(intentActivity: ResolveInfo): String {
        return intentActivity.activityInfo.packageName
    }
}
