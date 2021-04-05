package org.wikipedia.readinglist

import android.icu.text.ListFormatter
import android.os.Build
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.FeedbackUtil.showMessage
import org.wikipedia.util.ShareUtil.shareText

class LongPressMenu(private val anchorView: View, private val existsInAnyList: Boolean, private val callback: Callback?) {
    interface Callback {
        fun onOpenLink(entry: HistoryEntry)
        fun onOpenInNewTab(entry: HistoryEntry)
        fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean)
        fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry)
    }

    @MenuRes
    private val menuRes: Int = if (existsInAnyList) R.menu.menu_long_press else R.menu.menu_reading_list_page_toggle
    private var listsContainingPage: List<ReadingList>? = null
    private var entry: HistoryEntry? = null

    constructor(anchorView: View, callback: Callback?) : this(anchorView, false, callback)

    fun show(entry: HistoryEntry?) {
        entry?.let {
            Completable.fromAction { listsContainingPage = ReadingListDbHelper.instance().let { helper ->
                helper.getListsFromPageOccurrences(helper.getAllPageOccurrences(it.title))
            } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (!anchorView.isAttachedToWindow) {
                    return@subscribe
                }
                this.entry = it
                showMenu()
            }
        }
    }

    private fun showMenu() {
        if (!existsInAnyList && listsContainingPage.isNullOrEmpty()) {
            return
        }
        listsContainingPage?.let {
            PopupMenu(anchorView.context, anchorView).let { menu ->
                menu.menuInflater.inflate(menuRes, menu.menu)
                menu.setOnMenuItemClickListener(PageSaveMenuClickListener())
                if (it.size == 1) {
                    val removeItem = menu.menu.findItem(R.id.menu_long_press_remove_from_lists)
                    removeItem.title = anchorView.context.getString(R.string.reading_list_remove_from_list, it[0].title())
                    val moveItem = menu.menu.findItem(R.id.menu_long_press_move_from_list_to_another_list)
                    moveItem.title = anchorView.context.getString(R.string.reading_list_move_from_to_other_list, it[0].title())
                    moveItem.isVisible = true
                    moveItem.isEnabled = true
                }
                if (existsInAnyList) {
                    menu.gravity = Gravity.END
                    val addToOtherItem = menu.menu.findItem(R.id.menu_long_press_add_to_another_list)
                    addToOtherItem.isVisible = it.isNotEmpty()
                    addToOtherItem.isEnabled = it.isNotEmpty()
                    val removeItem = menu.menu.findItem(R.id.menu_long_press_remove_from_lists)
                    removeItem.isVisible = it.isNotEmpty()
                    removeItem.isEnabled = it.isNotEmpty()
                    val saveItem = menu.menu.findItem(R.id.menu_long_press_add_to_default_list)
                    saveItem.isVisible = it.isEmpty()
                    saveItem.isEnabled = it.isEmpty()
                }
                menu.show()
            }
        }
    }

    private fun deleteOrShowDialog() {
        listsContainingPage?.let { list ->
            RemoveFromReadingListsDialog(list).deleteOrShowDialog(anchorView.context) { readingLists, _ ->
                entry?.let {
                    if (anchorView.isAttachedToWindow) {
                        val readingListNames = mutableListOf<String>()
                        readingLists.forEach { readingList ->
                            readingListNames.add(readingList.title())
                        }
                        showMessage((anchorView.context as AppCompatActivity), anchorView.context.getString(R.string.reading_list_item_deleted_from_list,
                                it.title.displayText, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ListFormatter.getInstance().format(readingListNames)
                        else readingListNames.joinToString(separator = ", ")))
                    }
                }
            }
        }
    }

    private inner class PageSaveMenuClickListener : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_long_press_open_page -> {
                    entry?.let { callback?.onOpenLink(it) }
                    true
                }
                R.id.menu_long_press_open_in_new_tab -> {
                    entry?.let { callback?.onOpenInNewTab(it) }
                    true
                }
                R.id.menu_long_press_add_to_default_list -> {
                    entry?.let { callback?.onAddRequest(it, true) }
                    true
                }
                R.id.menu_long_press_add_to_another_list -> {
                    listsContainingPage?.let { entry?.let { callback?.onAddRequest(it, false) } }
                    true
                }
                R.id.menu_long_press_move_from_list_to_another_list -> {
                    listsContainingPage?.let { list -> entry?.let { callback?.onMoveRequest(list[0].pages()[0], it) } }
                    true
                }
                R.id.menu_long_press_remove_from_lists -> {
                    deleteOrShowDialog()
                    true
                }
                R.id.menu_long_press_share_page -> {
                    entry?.let { shareText(anchorView.context, it.title) }
                    true
                }
                R.id.menu_long_press_copy_page -> {
                    entry?.let {
                        setPlainText(anchorView.context, null, it.title.uri)
                        showMessage((anchorView.context as AppCompatActivity), R.string.address_copied)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
