package org.wikipedia.readinglist

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.ReadingListsFunnel
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.NotificationCategory
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.FileUtil

object ReadingListsExportImportHelper : BaseActivity.Callback {
    var lists: List<ReadingList>? = null
    val funnel = ReadingListsFunnel()

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

    private fun extractListDataToExport(activity: AppCompatActivity, readingLists: List<ReadingList>?) {
        val exportedLists = mutableListOf<ExportableReadingList>()
        try {
            readingLists?.let { lists ->
                lists.forEach {
                    val wikiPageTitlesMap = mutableMapOf<String, String>()
                    it.pages.forEach { page ->
                        wikiPageTitlesMap[page.apiTitle] = page.lang
                    }
                    val exportedList = ExportableReadingList(it.title, it.description, wikiPageTitlesMap)
                    exportedLists.add(exportedList)
                }
                FileUtil.createFileInDownloadsFolder(activity, activity.getString(if (lists.size == 1) R.string.single_list_json_file_name
                else R.string.multiple_lists_json_file_name, lists[0].title), JsonUtil.encodeToString(exportedLists))
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                activity.getSystemService<NotificationManager>()?.notify(0, getNotificationBuilder(activity, intent, lists.size).build())
                FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.reading_lists_export_completed_message))
                    .setAction(R.string.suggested_edits_article_cta_snackbar_action) { activity.startActivity(intent) }.show()
                funnel.logExportLists(lists.size)
            }
        } catch (e: Exception) {
            FeedbackUtil.showMessage(activity, activity.resources.getQuantityString(R.plurals.reading_list_export_failed_message, exportedLists.size))
        }
    }

    private fun getNotificationBuilder(context: Context, intent: Intent, numOfLists: Int): NotificationCompat.Builder {
        return NotificationCompat
            .Builder(context, NotificationCategory.MENTION.id)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.reading_list_notification_title))
            .setContentText(context.getString(R.string.reading_list_notification_text, numOfLists))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or DeviceUtil.pendingIntentFlags))
            .setLargeIcon(NotificationPresenter.drawNotificationBitmap(context, R.color.accent50, R.drawable.ic_download_in_progress, ""))
            .setSmallIcon(R.drawable.ic_wikipedia_w)
            .setColor(ContextCompat.getColor(context, R.color.accent50))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reading_list_notification_detailed_text)))
    }

    fun importLists(activity: BaseActivity, jsonString: String) {
        funnel.logImportStart()
        var readingLists: List<ExportableReadingList>? = null
        try {
            readingLists = JsonUtil.decodeFromString(jsonString)
        } catch (e: Exception) {
            funnel.logImportCancel()
            FeedbackUtil.showMessage(activity, R.string.reading_lists_import_failure_message)
        }
        readingLists?.let {
            for (list in readingLists) {
                val allLists = AppDatabase.instance.readingListDao().getAllLists()
                val existingTitles = AppDatabase.instance.readingListDao().getAllLists().map { it.title }
                if (existingTitles.contains(list.name)) {
                    val dialog = AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.reading_lists_import_conflict_dialog_title, list.name))
                        .setPositiveButton(R.string.reading_lists_import_conflict_dialog_primary_action_text) { _, _ -> allLists.filter { it.title == list.name }.forEach { replaceList(list, it) } }
                        .setNegativeButton(R.string.reading_lists_import_conflict_dialog_secondary_action_text) { _, _ -> keepBothLists(activity, list) }
                        .setNeutralButton(R.string.reading_lists_import_conflict_dialog_tertiary_action_text, null)
                        .create()
                    dialog.show()
                    continue
                }
                val readingList = AppDatabase.instance.readingListDao().createList(list.name!!, list.description)
                addTitlesToList(list, readingList)
            }
            funnel.logImportFinish(readingLists.size)
            FeedbackUtil.showMessage(activity, activity.resources.getQuantityString(R.plurals.reading_list_import_success_message, readingLists.size))
        }
    }

    private fun keepBothLists(activity: AppCompatActivity, importedList: ExportableReadingList) {
        val readingList = AppDatabase.instance.readingListDao().createList(activity.getString(R.string.copy_of_imported_list_name,
            importedList.name!!, System.currentTimeMillis().toString()), importedList.description)
        addTitlesToList(importedList, readingList)
    }

    private fun replaceList(importedList: ExportableReadingList, userList: ReadingList) {
        AppDatabase.instance.readingListDao().deleteReadingList(userList)
        AppDatabase.instance.readingListPageDao().markPagesForDeletion(userList, userList.pages, false)
        val readingList = AppDatabase.instance.readingListDao().createList(importedList.name!!, importedList.description)
        addTitlesToList(importedList, readingList)
    }

    private fun addTitlesToList(exportedList: ExportableReadingList, list: ReadingList) {
        val titles = mutableListOf<PageTitle>()
        exportedList.pages.keys.forEach { apiTitle ->
            titles.add(PageTitle(apiTitle, WikiSite.forLanguageCode(exportedList.pages[apiTitle].orEmpty())))
        }
        AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(list, titles)
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            extractListDataToExport(activity, lists)
        } else {
            FeedbackUtil.showMessage(activity, R.string.reading_list_export_write_permission_rationale)
        }
    }

    @Suppress("unused")
    @Serializable
    private class ExportableReadingList(
            val name: String? = null,
            val description: String? = null,
            val pages: Map<String, String>
        )
}
