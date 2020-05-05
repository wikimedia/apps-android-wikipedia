package org.wikipedia.readinglist

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage

class ReadingListBookmarkMenu(private val anchorView: View, private val existsInAnyList: Boolean, private val callback: Callback?) {
    interface Callback {
        fun onAddRequest(page: ReadingListPage?)
        fun onDeleted(page: ReadingListPage?)
        fun onShare()
    }

    @MenuRes
    private val menuRes = if (existsInAnyList) R.menu.menu_feed_card_item else R.menu.menu_reading_list_page_toggle
    private var listsContainingPage: List<ReadingList>? = null

    constructor(anchorView: View, callback: Callback?) : this(anchorView, false, callback)

    @SuppressLint("CheckResult")
    fun show(title: PageTitle) {
        Completable.fromAction {
            val pageOccurrences = ReadingListDbHelper.instance().getAllPageOccurrences(title)
            listsContainingPage = ReadingListDbHelper.instance().getListsFromPageOccurrences(pageOccurrences)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (!ViewCompat.isAttachedToWindow(anchorView)) {
                        return@subscribe
                    }
                    showMenu()
                }
    }

    private fun showMenu() {
        if (!existsInAnyList && isListsContainingPageEmpty) {
            return
        }
        val context = anchorView.context
        val menu = PopupMenu(context, anchorView)
        menu.menuInflater.inflate(menuRes, menu.menu)
        menu.setOnMenuItemClickListener(PageSaveMenuClickListener())
        if (listsContainingPage?.size == 1) {
            val removeItem = menu.menu.findItem(R.id.menu_remove_from_lists)
            removeItem.title = context.getString(R.string.reading_list_remove_from_list, listsContainingPage!![0].title())
        }
        if (existsInAnyList) {
            menu.gravity = Gravity.END
            val addToOtherItem = menu.menu.findItem(R.id.menu_add_to_other_list)
            addToOtherItem.isVisible = !listsContainingPage.isNullOrEmpty()
            addToOtherItem.isEnabled = !listsContainingPage.isNullOrEmpty()
            val removeItem = menu.menu.findItem(R.id.menu_remove_from_lists)
            removeItem.isVisible = !listsContainingPage.isNullOrEmpty()
            removeItem.isEnabled = !listsContainingPage.isNullOrEmpty()
            val saveItem = menu.menu.findItem(R.id.menu_feed_card_item_save)
            saveItem.isVisible = listsContainingPage.isNullOrEmpty()
            saveItem.isEnabled = listsContainingPage.isNullOrEmpty()
        }
        menu.show()
    }

    private fun deleteOrShowDialog(context: Context) {
        if (isListsContainingPageEmpty) {
            return
        }
        RemoveFromReadingListsDialog(listsContainingPage!!).deleteOrShowDialog(context) { _: List<ReadingList?>?, page: ReadingListPage? ->
            callback?.onDeleted(page)
        }
    }

    private val isListsContainingPageEmpty: Boolean
        get() = listsContainingPage.isNullOrEmpty()

    private inner class PageSaveMenuClickListener : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_feed_card_item_save -> {
                    callback?.onAddRequest(null)
                    true
                }
                R.id.menu_feed_card_item_share -> {
                    callback?.onShare()
                    true
                }
                R.id.menu_add_to_other_list -> {
                    if (callback != null && !isListsContainingPageEmpty) {
                        callback.onAddRequest(listsContainingPage!![0].pages()[0])
                    }
                    true
                }
                R.id.menu_remove_from_lists -> {
                    deleteOrShowDialog(anchorView.context)
                    true
                }
                else -> false
            }
        }
    }
}
