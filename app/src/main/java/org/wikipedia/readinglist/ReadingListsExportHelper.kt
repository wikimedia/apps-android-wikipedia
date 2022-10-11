package org.wikipedia.readinglist

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.FileUtil

object ReadingListsExportHelper : BaseActivity.Callback {
    var lists: List<ReadingList>? = null

    fun exportLists(activity: MainActivity, readingLists: List<ReadingList>?) {
        lists = readingLists
        (activity as BaseActivity).callback = this
        readingLists?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                extractListDataToExport(activity, readingLists)
            } else {
                activity.requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun extractListDataToExport(activity: AppCompatActivity, readingLists: List<ReadingList>?) {
        val exportedLists = mutableListOf<ExportableReadingList>()

        readingLists?.forEach {
            val wikiPageTitlesMap = mutableMapOf<String, String>()

            it.pages.forEach { page ->
                wikiPageTitlesMap[page.apiTitle] = page.lang
            }
            val exportedList = ExportableReadingList(it.title, it.description, wikiPageTitlesMap)
            exportedLists.add(exportedList)
        }
        FileUtil.createFileInDownloadsFolder(activity, "reading_lists_export.json", JsonUtil.encodeToString(exportedLists))
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        activity.getSystemService<NotificationManager>()?.notify(0, getNotificationBuilder(activity, intent, "reading_lists_export.json").build())
        FeedbackUtil.showMessage(activity, R.string.reading_list_export_completed_message)
    }

    private fun getNotificationBuilder(context: Context, intent: Intent, listName: String): NotificationCompat.Builder {
        return NotificationCompat
            .Builder(context, NotificationCategory.MENTION.id)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentTitle(context.getString(R.string.reading_list_notification_title))
            .setContentText(context.getString(R.string.reading_list_notification_text, listName))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or DeviceUtil.pendingIntentFlags))
            .setLargeIcon(NotificationPresenter.drawNotificationBitmap(context, R.color.accent50, R.drawable.ic_download_in_progress, ""))
            .setSmallIcon(R.drawable.ic_wikipedia_w)
            .setColor(ContextCompat.getColor(context, R.color.accent50))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reading_list_notification_detailed_text, listName)))
    }

    @Suppress("unused")
    @Serializable
    private class ExportableReadingList
        (val name: String? = null, val description: String? = null, val pages: Map<String, String>)

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            extractListDataToExport(activity, lists)
        } else {
            FeedbackUtil.showMessage(activity, R.string.reading_list_export_write_permission_rationale)
        }
    }

    fun importLists(jsonString: String) {
        val readingLists: List<ExportableReadingList> = JsonUtil.decodeFromString(jsonString)!!
        readingLists.forEach {
            val readingList = AppDatabase.instance.readingListDao().createList(it.name!!, it.description)
            val titles = mutableListOf<PageTitle>()
            it.pages.keys.forEach { apiTitle ->
                titles.add(PageTitle(apiTitle, WikiSite.forLanguageCode(it.pages[apiTitle].orEmpty())))
            }
            AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(readingList, titles)
        }
    }
}
