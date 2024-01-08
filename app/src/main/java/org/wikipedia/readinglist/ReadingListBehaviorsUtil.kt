package org.wikipedia.readinglist

import android.app.Activity
import android.content.DialogInterface
import android.icu.text.ListFormatter
import android.os.Build
import android.text.Spanned
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar.Companion.MIN_PROGRESS
import java.util.Locale

object ReadingListBehaviorsUtil {

    fun interface SearchCallback {
        fun onCompleted(lists: MutableList<Any>)
    }

    fun interface SnackbarCallback {
        fun onUndoDeleteClicked()
    }

    fun interface Callback {
        fun onCompleted()
    }

    private var allReadingLists = listOf<ReadingList>()

    // Kotlin coroutine
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(Dispatchers.Main)
    private val exceptionHandler = CoroutineExceptionHandler { _, exception -> L.w(exception) }

    fun getListsContainPage(readingListPage: ReadingListPage) =
            allReadingLists.filter { list -> list.pages.any { it.apiTitle == readingListPage.apiTitle } }

    fun savePagesForOffline(activity: Activity, selectedPages: List<ReadingListPage>, callback: Callback) {
        if (Prefs.isDownloadOnlyOverWiFiEnabled && !DeviceUtil.isOnWiFi) {
            showMobileDataWarningDialog(activity) { _, _ ->
                savePagesForOffline(activity, selectedPages, true)
                callback.onCompleted()
            }
        } else {
            savePagesForOffline(activity, selectedPages, !Prefs.isDownloadingReadingListArticlesEnabled)
            callback.onCompleted()
        }
    }

    private fun savePagesForOffline(activity: Activity, selectedPages: List<ReadingListPage>, forcedSave: Boolean) {
        if (selectedPages.isNotEmpty()) {
            for (page in selectedPages) {
                resetPageProgress(page)
            }
            AppDatabase.instance.readingListPageDao().markPagesForOffline(selectedPages, true, forcedSave)
            showMultiSelectOfflineStateChangeSnackbar(activity, selectedPages, true)
        }
    }

    fun removePagesFromOffline(activity: Activity, selectedPages: List<ReadingListPage>, callback: Callback) {
        if (selectedPages.isNotEmpty()) {
            AppDatabase.instance.readingListPageDao().markPagesForOffline(selectedPages, offline = false, forcedSave = false)
            showMultiSelectOfflineStateChangeSnackbar(activity, selectedPages, false)
            callback.onCompleted()
        }
    }

    fun deleteReadingList(activity: Activity, readingList: ReadingList?, showDialog: Boolean, callback: Callback) {
        if (readingList == null) {
            return
        }
        if (showDialog) {
            MaterialAlertDialogBuilder(activity)
                    .setMessage(activity.getString(R.string.reading_list_delete_confirm, readingList.title))
                    .setPositiveButton(R.string.reading_list_delete_dialog_ok_button_text) { _, _ ->
                        AppDatabase.instance.readingListDao().deleteList(readingList)
                        AppDatabase.instance.readingListPageDao().markPagesForDeletion(readingList, readingList.pages, false)
                        callback.onCompleted() }
                    .setNegativeButton(R.string.reading_list_delete_dialog_cancel_button_text, null)
                    .show()
        } else {
            AppDatabase.instance.readingListDao().deleteList(readingList)
            AppDatabase.instance.readingListPageDao().markPagesForDeletion(readingList, readingList.pages, false)
            callback.onCompleted()
        }
    }

    fun deleteReadingLists(activity: Activity, readingLists: List<ReadingList>, callback: Callback) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.reading_list_delete_lists_confirm_dialog_title)
            .setMessage(activity.resources.getQuantityString(R.plurals.reading_list_delete_lists_confirm_dialog_message, readingLists.size, readingLists.size))
            .setPositiveButton(R.string.reading_list_delete_lists_dialog_delete_button_text) { _, _ ->
                readingLists.filterNot { it.isDefault }.forEach {
                    AppDatabase.instance.readingListDao().deleteList(it)
                    AppDatabase.instance.readingListPageDao().markPagesForDeletion(it, it.pages, false)
                }
                callback.onCompleted()
            }
            .setNegativeButton(R.string.reading_list_delete_dialog_cancel_button_text, null)
            .show()
    }

    fun deletePages(activity: Activity, listsContainPage: List<ReadingList>, readingListPage: ReadingListPage, snackbarCallback: SnackbarCallback, callback: Callback) {
        if (listsContainPage.size > 1) {
            scope.launch(exceptionHandler) {
                val pages = withContext(dispatcher) { AppDatabase.instance.readingListPageDao().getAllPageOccurrences(ReadingListPage.toPageTitle(readingListPage)) }
                val lists = withContext(dispatcher) { AppDatabase.instance.readingListDao().getListsFromPageOccurrences(pages) }
                RemoveFromReadingListsDialog(lists).deleteOrShowDialog(activity) { list, page ->
                    showDeletePageFromListsUndoSnackbar(activity, list, page, snackbarCallback)
                    callback.onCompleted()
                }
            }
        } else {
            AppDatabase.instance.readingListPageDao().markPagesForDeletion(listsContainPage[0], listOf(readingListPage))
            listsContainPage[0].pages.remove(readingListPage)
            showDeletePagesUndoSnackbar(activity, listsContainPage[0], listOf(readingListPage), snackbarCallback)
            callback.onCompleted()
        }
    }

    fun renameReadingList(activity: Activity, readingList: ReadingList?, callback: Callback) {
        if (readingList == null) {
            return
        } else if (readingList.isDefault) {
            L.w("Attempted to rename default list.")
            return
        }

        val tempLists = AppDatabase.instance.readingListDao().getListsWithoutContents()
        val existingTitles = ArrayList<String>()
        for (list in tempLists) {
            existingTitles.add(list.title)
        }
        existingTitles.remove(readingList.title)

        ReadingListTitleDialog.readingListTitleDialog(activity, readingList.title, readingList.description, existingTitles,
            callback = object : ReadingListTitleDialog.Callback {
                override fun onSuccess(text: String, description: String) {
                    readingList.title = text
                    readingList.description = description
                    readingList.dirty = true
                    AppDatabase.instance.readingListDao().updateList(readingList, true)
                    callback.onCompleted()
                }

                override fun onCancel() { }
            }).show()
    }

    private fun showDeletePageFromListsUndoSnackbar(activity: Activity, lists: List<ReadingList>?, page: ReadingListPage, callback: SnackbarCallback) {
        if (lists == null) {
            return
        }
        val readingListNames = lists.map { it.title }.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ListFormatter.getInstance().format(this)
            } else {
                joinToString(separator = ", ")
            }
        }
        FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.reading_list_item_deleted_from_list,
                page.displayTitle, readingListNames))
                .setAction(R.string.reading_list_item_delete_undo) {
                    AppDatabase.instance.readingListPageDao().addPageToLists(lists, page, true)
                    callback.onUndoDeleteClicked()
                }
                .show()
    }

    fun showDeletePagesUndoSnackbar(activity: Activity, readingList: ReadingList?, pages: List<ReadingListPage>, callback: SnackbarCallback) {
        if (readingList == null) {
            return
        }
        FeedbackUtil
                .makeSnackbar(activity, if (pages.size == 1) activity.getString(R.string.reading_list_item_deleted_from_list,
                        pages[0].displayTitle, readingList.title) else activity.resources.getQuantityString(R.plurals.reading_list_articles_deleted_from_list,
                        pages.size, pages.size, readingList.title))
                .setAction(R.string.reading_list_item_delete_undo) {
                    val newPages = ArrayList<ReadingListPage>()
                    for (page in pages) {
                        newPages.add(ReadingListPage(ReadingListPage.toPageTitle(page)))
                    }
                    AppDatabase.instance.readingListPageDao().addPagesToList(readingList, newPages, true)
                    readingList.pages.addAll(newPages)
                    callback.onUndoDeleteClicked() }
                .show()
    }

    fun showDeleteListUndoSnackbar(activity: Activity, readingList: ReadingList?, callback: SnackbarCallback) {
        if (readingList == null) {
            return
        }
        FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.reading_list_deleted, readingList.title))
            .setAction(R.string.reading_list_item_delete_undo) {
                val newList = AppDatabase.instance.readingListDao().createList(readingList.title, readingList.description)
                val newPages = ArrayList<ReadingListPage>()
                for (page in readingList.pages) {
                    newPages.add(ReadingListPage(ReadingListPage.toPageTitle(page)))
                }
                AppDatabase.instance.readingListPageDao().addPagesToList(newList, newPages, true)
                callback.onUndoDeleteClicked()
            }
            .show()
    }

    fun showDeleteListsUndoSnackbar(activity: Activity, readingLists: List<ReadingList>?, callback: SnackbarCallback) {
        if (readingLists == null) {
            return
        }
        val snackBar = FeedbackUtil.makeSnackbar(activity, getDeleteListMessage(activity, readingLists))
        if (!(readingLists.size == 1 && readingLists[0].isDefault)) {
            snackBar.setAction(R.string.reading_list_item_delete_undo) {
                readingLists.filterNot { it.isDefault }.forEach {
                    val newList = AppDatabase.instance.readingListDao().createList(it.title, it.description)
                    val newPages = ArrayList<ReadingListPage>()
                    for (page in it.pages) {
                        newPages.add(ReadingListPage(ReadingListPage.toPageTitle(page)))
                    }
                    AppDatabase.instance.readingListPageDao().addPagesToList(newList, newPages, true)
                }
                callback.onUndoDeleteClicked()
            }
        }
        snackBar.show()
    }

    private fun getDeleteListMessage(activity: Activity, readingLists: List<ReadingList>): String {
        return if (readingLists.any { it.isDefault }) {
            when (readingLists.size) {
                1 -> activity.getString(R.string.reading_lists_default_list_delete_message, activity.getString(R.string.default_reading_list_name))
                2 -> activity.getString(R.string.reading_lists_default_plus_one_list_delete_message, readingLists.first { !it.isDefault }.title, activity.getString(R.string.default_reading_list_name))
                else -> activity.getString(R.string.reading_lists_default_plus_many_lists_delete_message, activity.getString(R.string.default_reading_list_name))
            }
        } else {
            activity.resources.getQuantityString(R.plurals.reading_lists_deleted_message, readingLists.size, readingLists.size)
        }
    }

    fun togglePageOffline(activity: Activity, page: ReadingListPage?, callback: Callback) {
        if (page == null) {
            return
        }
        if (page.offline) {
            scope.launch(exceptionHandler) {
                val pages = withContext(dispatcher) { AppDatabase.instance.readingListPageDao().getAllPageOccurrences(ReadingListPage.toPageTitle(page)) }
                val lists = withContext(dispatcher) { AppDatabase.instance.readingListDao().getListsFromPageOccurrences(pages) }
                if (lists.size > 1) {
                    MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.reading_list_confirm_remove_article_from_offline_title)
                            .setMessage(getConfirmToggleOfflineMessage(activity, page, lists))
                            .setPositiveButton(R.string.reading_list_confirm_remove_article_from_offline) { _, _ -> toggleOffline(activity, page, callback) }
                            .setNegativeButton(R.string.reading_list_remove_from_offline_cancel_button_text, null)
                            .show()
                } else {
                    toggleOffline(activity, page, callback)
                }
            }
        } else {
            toggleOffline(activity, page, callback)
        }
    }

    fun toggleOffline(activity: Activity, page: ReadingListPage, callback: Callback) {
        resetPageProgress(page)
        if (Prefs.isDownloadOnlyOverWiFiEnabled && !DeviceUtil.isOnWiFi && !page.offline) {
            showMobileDataWarningDialog(activity) { _, _ ->
                toggleOffline(activity, page, true)
                callback.onCompleted()
            }
        } else {
            toggleOffline(activity, page, !Prefs.isDownloadingReadingListArticlesEnabled)
            callback.onCompleted()
        }
    }

    fun addToDefaultList(activity: Activity, title: PageTitle, addToDefault: Boolean, invokeSource: InvokeSource, listener: DialogInterface.OnDismissListener? = null) {
        activity as AppCompatActivity
        if (addToDefault) {
            // If the title is a redirect, resolve it before saving to the reading list.
            activity.lifecycleScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
                var finalPageTitle = title
                try {
                    ServiceFactory.get(title.wikiSite).getInfoByPageIdsOrTitles(null, title.prefixedText)
                        .query?.firstPage()?.let {
                            finalPageTitle = PageTitle(it.title, title.wikiSite, it.thumbUrl(), it.description, it.displayTitle(title.wikiSite.languageCode), null)
                        }
                } finally {
                    val defaultList = AppDatabase.instance.readingListDao().getDefaultList()
                    val addedTitles = AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(defaultList, listOf(finalPageTitle))
                    if (addedTitles.isNotEmpty()) {
                        FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.reading_list_article_added_to_default_list, StringUtil.fromHtml(finalPageTitle.displayText)))
                            .setAction(R.string.reading_list_add_to_list_button) {
                                moveToList(activity, defaultList.id, listOf(finalPageTitle), invokeSource, false, listener)
                            }.show()
                    }
                }
            }
        } else {
            ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager,
                AddToReadingListDialog.newInstance(title, invokeSource, listener))
        }
    }

    fun moveToList(activity: Activity, sourceReadingListId: Long, titles: List<PageTitle>, source: InvokeSource,
                   showDefaultList: Boolean = true, listener: DialogInterface.OnDismissListener? = null) {
        activity as AppCompatActivity
        ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager,
            MoveToReadingListDialog.newInstance(sourceReadingListId, titles, source, showDefaultList, listener))
    }

    private fun toggleOffline(activity: Activity, page: ReadingListPage, forcedSave: Boolean) {
        AppDatabase.instance.readingListPageDao().markPageForOffline(page, !page.offline, forcedSave)
        FeedbackUtil.showMessage(activity,
                activity.resources.getQuantityString(
                        if (page.offline) R.plurals.reading_list_article_offline_message else R.plurals.reading_list_article_not_offline_message, 1))
    }

    private fun showMobileDataWarningDialog(activity: Activity, listener: DialogInterface.OnClickListener) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_title_download_only_over_wifi)
                .setMessage(R.string.dialog_text_download_only_over_wifi)
                .setPositiveButton(R.string.dialog_title_download_only_over_wifi_allow, listener)
                .setNegativeButton(R.string.reading_list_download_using_mobile_data_cancel_button_text, null)
                .show()
    }

    private fun showMultiSelectOfflineStateChangeSnackbar(activity: Activity, pages: List<ReadingListPage>, offline: Boolean) {
        FeedbackUtil.showMessage(activity,
                activity.resources.getQuantityString(
                        if (offline) R.plurals.reading_list_article_offline_message else R.plurals.reading_list_article_not_offline_message, pages.size
                ))
    }

    private fun resetPageProgress(page: ReadingListPage) {
        if (!page.offline) {
            page.downloadProgress = MIN_PROGRESS
        }
    }

    private fun getConfirmToggleOfflineMessage(activity: Activity, page: ReadingListPage, lists: List<ReadingList>): Spanned {
        var result = activity.getString(R.string.reading_list_confirm_remove_article_from_offline_message,
                "<b>${page.displayTitle}</b>")
        lists.forEach {
            result += "<br>&nbsp;&nbsp;<b>&#8226; ${it.title}</b>"
        }
        return StringUtil.fromHtml(result)
    }

    fun searchListsAndPages(searchQuery: String?, callback: SearchCallback) {
        scope.launch(exceptionHandler) {
            allReadingLists = withContext(dispatcher) { AppDatabase.instance.readingListDao().getAllLists() }
            val list = withContext(dispatcher) { applySearchQuery(searchQuery, allReadingLists) }
            if (searchQuery.isNullOrEmpty()) {
                ReadingList.sortGenericList(list, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC))
            }
            callback.onCompleted(list)
        }
    }

    private fun applySearchQuery(searchQuery: String?, lists: List<ReadingList>): MutableList<Any> {
        val result = mutableListOf<Any>()

        if (searchQuery.isNullOrEmpty()) {
            result.addAll(lists)
            return result
        }

        val normalizedQuery = StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
        var lastListItemIndex = 0
        lists.forEach { list ->
            if (StringUtils.stripAccents(list.title).lowercase(Locale.getDefault()).contains(normalizedQuery)) {
                result.add(lastListItemIndex++, list)
            }
            list.pages.forEach { page ->
                if (page.accentAndCaseInvariantTitle().contains(normalizedQuery)) {
                    if (result.none { it is ReadingListPage && it.lang == page.lang && it.apiTitle == page.apiTitle }) {
                        result.add(page)
                    }
                }
            }
        }
        return result
    }
}
