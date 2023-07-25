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
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.FileUtil

object ReadingListsExportImportHelper : BaseActivity.Callback {

    var lists: List<ReadingList>? = null

    fun exportLists(activity: BaseActivity, readingLists: List<ReadingList>?) {
        lists = readingLists
        activity.callback = this
        readingLists?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                extractListDataToExport(activity, readingLists)
            } else {
                activity.requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun extractListDataToExport(activity: AppCompatActivity, exportLists: List<ReadingList>) {
        try {
            val exportedLists = exportLists.map { list ->
                ExportableReadingList(list.title, list.description, list.pages.map {
                    ExportablePage(it.apiTitle, it.wiki.languageCode, it.namespace.code())
                })
            }
            FileUtil.createFileInDownloadsFolder(activity, activity.getString(if (exportLists.size == 1) R.string.single_list_json_file_name
            else R.string.multiple_lists_json_file_name, exportLists[0].title), "application/json", JsonUtil.encodeToString(ExportableContents(exportedLists)))
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            activity.getSystemService<NotificationManager>()?.notify(0, getNotificationBuilder(activity, intent, exportLists.size).build())
            FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.reading_lists_export_completed_message))
                .setAction(R.string.suggested_edits_article_cta_snackbar_action) { activity.startActivity(intent) }.show()
            ReadingListsAnalyticsHelper.logExportLists(activity, exportLists.size)
        } catch (e: Exception) {
            FeedbackUtil.showMessage(activity, activity.resources.getQuantityString(R.plurals.reading_list_export_failed_message, exportLists.size))
        }
    }

    private fun getNotificationBuilder(context: Context, intent: Intent, numOfLists: Int): NotificationCompat.Builder {
        return NotificationCompat
            .Builder(context, NotificationCategory.MENTION.id)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.reading_list_notification_title))
            .setContentText(context.getString(R.string.reading_list_notification_detailed_text))
            .setContentIntent(PendingIntentCompat.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false))
            .setLargeIcon(NotificationPresenter.drawNotificationBitmap(context, R.color.blue600, R.drawable.ic_download_in_progress, ""))
            .setSmallIcon(R.drawable.ic_wikipedia_w)
            .setColor(ContextCompat.getColor(context, R.color.blue600))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reading_list_notification_text, numOfLists)))
    }

    fun importLists(activity: BaseActivity, jsonString: String) {
        ReadingListsAnalyticsHelper.logImportStart(activity)
        try {
            val contents: ExportableContents = JsonUtil.decodeFromString(jsonString)!!
            val readingLists = contents.readingListsV1
            for (list in readingLists) {
                val allLists = AppDatabase.instance.readingListDao().getAllLists()
                val existingTitles = AppDatabase.instance.readingListDao().getAllLists().map { it.title }
                if (existingTitles.contains(list.name)) {
                    allLists.filter { it.title == list.name }.forEach { addTitlesToList(list, it) }
                    continue
                }
                val readingList = AppDatabase.instance.readingListDao().createList(list.name!!, list.description)
                addTitlesToList(list, readingList)
                ReadingListsAnalyticsHelper.logImportFinished(activity, list.pages.size)
            }
            FeedbackUtil.showMessage(activity, activity.resources.getQuantityString(R.plurals.reading_list_import_success_message, readingLists.size))
        } catch (e: Exception) {
            FeedbackUtil.showMessage(activity, R.string.reading_lists_import_failure_message)
            ReadingListsAnalyticsHelper.logImportCancelled(activity)
        }
    }

    private fun addTitlesToList(exportedList: ExportableReadingList, list: ReadingList) {
        val titles = exportedList.pages.map { page ->
            PageTitle(page.title, WikiSite.forLanguageCode(page.lang)).also {
                if (page.ns != Namespace.MAIN.code()) { it.namespace = Namespace.of(page.ns).name }
            }
        }
        AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(list, titles)
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            lists?.let { extractListDataToExport(activity, it) }
            lists = null
        } else {
            FeedbackUtil.showMessage(activity, R.string.reading_list_export_write_permission_rationale)
        }
    }

    @Suppress("unused")
    @Serializable
    private class ExportableContents(val readingListsV1: List<ExportableReadingList> = emptyList())

    @Suppress("unused")
    @Serializable
    private class ExportableReadingList(val name: String? = null,
                                        val description: String? = null,
                                        val pages: List<ExportablePage> = emptyList())

    @Serializable
    private class ExportablePage(val title: String = "",
                                 val lang: String = "",
                                 val ns: Int = 0)
}
