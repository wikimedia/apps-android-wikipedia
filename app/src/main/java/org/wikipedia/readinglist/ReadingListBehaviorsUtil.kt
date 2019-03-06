package org.wikipedia.readinglist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.text.Spanned
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar.MIN_PROGRESS
import java.util.*


object ReadingListBehaviorsUtil {

    interface SearchCallback {
        fun onCompleted(lists: MutableList<Any>)
    }

    interface SnackbarCallback {
        fun onUndoDeleteClicked()
    }

    interface Callback {
        fun onCompleted()
    }

    private var allReadingLists = listOf<ReadingList>()

    fun getListsContainPage(readingListPage: ReadingListPage): List<ReadingList> {
        val lists = mutableListOf<ReadingList>()
        for (list in allReadingLists) {
            for (page in list.pages()) {
                if (page.title() == readingListPage.title()) {
                    lists.add(list)
                    break
                }
            }
        }

        L.d("getListsContainPage match " + lists.size)
        return lists
    }

    fun savePagesForOffline(activity: Activity,
                            selectedPages: List<ReadingListPage>,
                            callback: Callback) {
        if (Prefs.isDownloadOnlyOverWiFiEnabled() && !DeviceUtil.isOnWiFi()) {
            showMobileDataWarningDialog(activity, DialogInterface.OnClickListener { _, _ -> savePagesForOffline(activity, selectedPages, true, callback) })
        } else {
            savePagesForOffline(activity, selectedPages, !Prefs.isDownloadingReadingListArticlesEnabled(), callback)
        }
    }

    private fun savePagesForOffline(activity: Activity,
                                    selectedPages: List<ReadingListPage>,
                                    forcedSave: Boolean,
                                    callback: Callback) {
        if (!selectedPages.isEmpty()) {
            for (page in selectedPages) {
                resetPageProgress(page)
            }
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, true, forcedSave)
            showMultiSelectOfflineStateChangeSnackbar(activity, selectedPages, true)
            callback.onCompleted()
        }
    }

    fun removePagesFromOffline(activity: Activity,
                               selectedPages: List<ReadingListPage>,
                               callback: Callback) {
        if (!selectedPages.isEmpty()) {
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, false, false)
            showMultiSelectOfflineStateChangeSnackbar(activity, selectedPages, false)
            callback.onCompleted()
        }
    }

    fun deleteReadingList(activity: Activity,
                          readingList: ReadingList?,
                          showDialog: Boolean,
                          callback: Callback) {
        if (readingList == null) {
            return
        }
        if (showDialog) {
            val alert = AlertDialog.Builder(activity)
            alert.setMessage(activity.getString(R.string.reading_list_delete_confirm, readingList.title()))
            alert.setPositiveButton(android.R.string.yes) { _, _ ->
                ReadingListDbHelper.instance().deleteList(readingList)
                ReadingListDbHelper.instance().markPagesForDeletion(readingList, readingList.pages(), false)
                callback.onCompleted()
            }
            alert.setNegativeButton(android.R.string.no, null)
            alert.create().show()
        } else {
            ReadingListDbHelper.instance().deleteList(readingList)
            ReadingListDbHelper.instance().markPagesForDeletion(readingList, readingList.pages(), false)
            callback.onCompleted()
        }
    }

    @SuppressLint("CheckResult")
    fun deletePages(activity: Activity,
                    listsContainPage: List<ReadingList>,
                    readingListPage: ReadingListPage,
                    snackbarCallback: SnackbarCallback,
                    callback: Callback) {
        if (listsContainPage.size > 1) {
            Observable.fromCallable { ReadingListDbHelper.instance().getAllPageOccurrences(ReadingListPage.toPageTitle(readingListPage)) }
                    .map {ReadingListDbHelper.instance().getListsFromPageOccurrences(it)}
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        RemoveFromReadingListsDialog(it).deleteOrShowDialog(activity) { lists, page ->
                            showDeletePageFromListsUndoSnackbar(activity, lists, page, snackbarCallback)
                            callback.onCompleted()
                        }
                    }, { L.w(it) })
        } else {
            ReadingListDbHelper.instance().markPagesForDeletion(listsContainPage[0], listOf(readingListPage))
            listsContainPage[0].pages().remove(readingListPage)
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

        val tempLists = ReadingListDbHelper.instance().allListsWithoutContents
        val existingTitles = ArrayList<String>()
        for (list in tempLists) {
            existingTitles.add(list.title())
        }
        existingTitles.remove(readingList.title())

        ReadingListTitleDialog.readingListTitleDialog(activity, readingList.title(), readingList.description(), existingTitles
        ) { text, description ->
            readingList.title(text)
            readingList.description(description)
            readingList.dirty(true)
            ReadingListDbHelper.instance().updateList(readingList, true)
            callback.onCompleted()
        }.show()
    }

    private fun showDeletePageFromListsUndoSnackbar(activity: Activity,
                                                    lists: List<ReadingList>?,
                                                    page: ReadingListPage,
                                                    callback: SnackbarCallback) {
        if (lists == null) {
            return
        }
        val message = if (lists.size == 1)
            String.format(activity.getString(R.string.reading_list_item_deleted), page.title())
        else
            String.format(activity.getString(R.string.reading_lists_item_deleted), page.title())
        val snackbar = FeedbackUtil.makeSnackbar(activity, message,
                FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.reading_list_item_delete_undo) {
            ReadingListDbHelper.instance().addPageToLists(lists, page, true)
            callback.onUndoDeleteClicked()
        }
        snackbar.show()
    }

    fun showDeletePagesUndoSnackbar(activity: Activity,
                                    readingList: ReadingList?,
                                    pages: List<ReadingListPage>,
                                    callback: SnackbarCallback) {
        if (readingList == null) {
            return
        }
        val message = if (pages.size == 1)
            String.format(activity.getString(R.string.reading_list_item_deleted), pages[0].title())
        else
            String.format(activity.getString(R.string.reading_list_items_deleted), pages.size)
        val snackbar = FeedbackUtil.makeSnackbar(activity, message,
                FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.reading_list_item_delete_undo) {
            val newPages = ArrayList<ReadingListPage>()
            for (page in pages) {
                newPages.add(ReadingListPage(ReadingListPage.toPageTitle(page)))
            }
            ReadingListDbHelper.instance().addPagesToList(readingList, newPages, true)
            readingList.pages().addAll(newPages)
            callback.onUndoDeleteClicked()
        }
        snackbar.show()
    }

    fun showDeleteListUndoSnackbar(activity: Activity,
                                   readingList: ReadingList?,
                                   callback: SnackbarCallback) {
        if (readingList == null) {
            return
        }
        val snackbar = FeedbackUtil.makeSnackbar(activity,
                String.format(activity.getString(R.string.reading_list_deleted), readingList.title()),
                FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.reading_list_item_delete_undo) {
            val newList = ReadingListDbHelper.instance().createList(readingList.title(), readingList.description())
            val newPages = ArrayList<ReadingListPage>()
            for (page in readingList.pages()) {
                newPages.add(ReadingListPage(ReadingListPage.toPageTitle(page)))
            }
            ReadingListDbHelper.instance().addPagesToList(newList, newPages, true)
            callback.onUndoDeleteClicked()
        }
        snackbar.show()
    }

    @SuppressLint("CheckResult")
    fun togglePageOffline(activity: Activity, page: ReadingListPage?, callback: Callback) {
        if (page == null) {
            return
        }
        if (page.offline()) {
            Observable.fromCallable { ReadingListDbHelper.instance().getAllPageOccurrences(ReadingListPage.toPageTitle(page))}
                    .map { ReadingListDbHelper.instance().getListsFromPageOccurrences(it)}
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ lists ->
                        if (lists.size > 1) {
                            val dialog = AlertDialog.Builder(activity)
                                    .setTitle(R.string.reading_list_confirm_remove_article_from_offline_title)
                                    .setMessage(getConfirmToggleOfflineMessage(activity, page, lists))
                                    .setPositiveButton(R.string.reading_list_confirm_remove_article_from_offline) { _, _ -> toggleOffline(activity, page, callback) }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create()
                            dialog.show()
                        } else {
                            toggleOffline(activity, page, callback)
                        }
                    }, { L.w(it) })
        } else {
            toggleOffline(activity, page, callback)
        }
    }

    fun toggleOffline(activity: Activity,
                      page: ReadingListPage,
                      callback: Callback) {
        resetPageProgress(page)
        if (Prefs.isDownloadOnlyOverWiFiEnabled() && !DeviceUtil.isOnWiFi()) {
            showMobileDataWarningDialog(activity, DialogInterface.OnClickListener { _, _ -> toggleOffline(activity, page, true, callback) })
        } else {
            toggleOffline(activity, page, !Prefs.isDownloadingReadingListArticlesEnabled(), callback)
        }
    }

    private fun toggleOffline(activity: Activity,
                              page: ReadingListPage,
                              forcedSave: Boolean,
                              callback: Callback) {
        ReadingListDbHelper.instance().markPageForOffline(page, !page.offline(), forcedSave)
        FeedbackUtil.showMessage(activity, if (page.offline())
            activity.resources.getQuantityString(R.plurals.reading_list_article_offline_message, 1)
        else
            activity.resources.getQuantityString(R.plurals.reading_list_article_not_offline_message, 1))
        callback.onCompleted()
    }

    private fun showMobileDataWarningDialog(activity: Activity, listener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_title_download_only_over_wifi)
                .setMessage(R.string.dialog_text_download_only_over_wifi)
                .setPositiveButton(R.string.dialog_title_download_only_over_wifi_allow, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun showMultiSelectOfflineStateChangeSnackbar(activity: Activity,
                                                          pages: List<ReadingListPage>,
                                                          offline: Boolean) {
        val message = if (offline)
            activity.resources.getQuantityString(R.plurals.reading_list_article_offline_message, pages.size)
        else
            activity.resources.getQuantityString(R.plurals.reading_list_article_not_offline_message, pages.size)
        FeedbackUtil.showMessage(activity, message)
    }

    private fun resetPageProgress(page: ReadingListPage) {
        if (!page.offline()) {
            page.downloadProgress(MIN_PROGRESS)
        }
    }

    private fun getConfirmToggleOfflineMessage(activity: Activity,
                                               page: ReadingListPage,
                                               lists: List<ReadingList>): Spanned {
        var result = activity.getString(R.string.reading_list_confirm_remove_article_from_offline_message,
                "<b>" + page.title() + "</b>")
        for (list in lists) {
            result += "<br>&nbsp;&nbsp;<b>&#8226; " + list.title() + "</b>"
        }
        return StringUtil.fromHtml(result)
    }

    @SuppressLint("CheckResult")
    fun searchListsAndPages(searchQuery: String?, callback: SearchCallback) {
        Observable.fromCallable { ReadingListDbHelper.instance().allLists }
                .map {
                    allReadingLists = it
                    val list = applySearchQuery(searchQuery, it)
                    if (TextUtils.isEmpty(searchQuery)) {
                        ReadingList.sortGenericList(list, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC))
                    }
                    list
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ callback.onCompleted(it) }, { L.w(it) })
    }

    private fun applySearchQuery(searchQuery: String?, lists: List<ReadingList>): MutableList<Any> {
        val result = mutableListOf<Any>()

        if (TextUtils.isEmpty(searchQuery)) {
            result.addAll(lists)
            return result
        }

        val normalizedQuery = StringUtils.stripAccents(searchQuery)?.toLowerCase()
        var lastListItemIndex = 0
        for (list in lists) {
            if (StringUtils.stripAccents(list.title()).toLowerCase().contains(normalizedQuery!!)) {
                result.add(lastListItemIndex++, list)
            }
            for (page in list.pages()) {
                if (page.title().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    var noMatch = true
                    for (item in result) {
                        if (item is ReadingListPage && item.title() == page.title()) {
                            noMatch = false
                            break
                        }
                    }
                    if (noMatch) {
                        result.add(page)
                    }
                }
            }
        }
        return result
    }
}
