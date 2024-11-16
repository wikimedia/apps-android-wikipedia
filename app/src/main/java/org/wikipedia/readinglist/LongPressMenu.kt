package org.wikipedia.readinglist

import android.content.ContextWrapper
import android.icu.text.ListFormatter
import android.location.Location
import android.os.Build
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.extensions.coroutineScope
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil

class LongPressMenu(
    private val anchorView: View,
    private val existsInAnyList: Boolean = true,
    private val openPageInPlaces: Boolean = false,
    private var menuRes: Int = R.menu.menu_long_press,
    private val location: Location? = null,
    private val callback: Callback? = null
) {
    interface Callback {
        fun onOpenLink(entry: HistoryEntry) { /* ignore by default */ }
        fun onOpenInNewTab(entry: HistoryEntry) { /* ignore by default */ }
        fun onOpenInPlaces(entry: HistoryEntry, location: Location) { /* ignore by default */ }
        fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean)
        fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry)
        fun onRemoveRequest() { /* ignore by default */ }
    }

    private var listsContainingPage: List<ReadingList>? = null
    private var entry: HistoryEntry? = null

    fun show(entry: HistoryEntry?) {
        entry?.let {
            anchorView.coroutineScope().launch {
                listsContainingPage = AppDatabase.instance.readingListDao().getListsFromPageOccurrences(
                        AppDatabase.instance.readingListPageDao().getAllPageOccurrences(it.title)
                    )
                this@LongPressMenu.entry = it
                if (!existsInAnyList) {
                    this@LongPressMenu.menuRes = R.menu.menu_reading_list_page_toggle
                }
                showMenu()
            }
        }
    }

    private fun showMenu() {
        if (!existsInAnyList && listsContainingPage.isNullOrEmpty()) {
            return
        }
        listsContainingPage?.let {
            PopupMenu(getActivity(), anchorView).let { menu ->
                menu.menuInflater.inflate(menuRes, menu.menu)
                menu.setOnMenuItemClickListener(PageSaveMenuClickListener())
                if (it.size == 1) {
                    val removeItem = menu.menu.findItem(R.id.menu_long_press_remove_from_lists)
                    removeItem.title = getActivity().getString(R.string.reading_list_remove_from_list, it[0].title)
                    val moveItem = menu.menu.findItem(R.id.menu_long_press_move_from_list_to_another_list)
                    moveItem.title = getActivity().getString(R.string.reading_list_move_from_to_other_list, it[0].title)
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
                val showOpenPageInPlaces = openPageInPlaces && location != null
                menu.menu.findItem(R.id.menu_long_press_open_in_places)?.isVisible = showOpenPageInPlaces
                menu.menu.findItem(R.id.menu_long_press_open_page)?.isVisible = !showOpenPageInPlaces
                menu.menu.findItem(R.id.menu_long_press_get_directions)?.isVisible = location != null
                menu.show()
            }
        }
    }

    private fun deleteOrShowDialog() {
        listsContainingPage?.let { list ->
            RemoveFromReadingListsDialog(list).deleteOrShowDialog(getActivity()) { readingLists, _ ->
                entry?.let {
                    if (!getActivity().isDestroyed) {
                        val readingListNames = readingLists.map { readingList -> readingList.title }.run {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ListFormatter.getInstance().format(this)
                            } else {
                                joinToString(separator = ", ")
                            }
                        }
                        FeedbackUtil.showMessage(getActivity(), getActivity().getString(R.string.reading_list_item_deleted_from_list,
                                        it.title.displayText, readingListNames))
                    }
                }
            }
        }
    }

    private fun getActivity(): AppCompatActivity {
        return (if (anchorView.context !is AppCompatActivity && anchorView.context is ContextWrapper) {
            (anchorView.context as ContextWrapper).baseContext
        } else {
            anchorView.context
        }) as AppCompatActivity
    }

    private inner class PageSaveMenuClickListener : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            BreadCrumbLogEvent.logClick(anchorView.context, item)
            return when (item.itemId) {
                R.id.menu_long_press_open_page -> {
                    entry?.let { callback?.onOpenLink(it) }
                    true
                }
                R.id.menu_long_press_open_in_places -> {
                    location?.let { location ->
                        entry?.let { callback?.onOpenInPlaces(it, location) }
                    }
                    true
                }
                R.id.menu_long_press_open_in_new_tab -> {
                    sendPlacesEvent("new_tab_click")
                    entry?.let { callback?.onOpenInNewTab(it) }
                    true
                }
                R.id.menu_long_press_add_to_default_list -> {
                    sendPlacesEvent("save_click")
                    entry?.let { callback?.onAddRequest(it, true) }
                    true
                }
                R.id.menu_long_press_add_to_another_list -> {
                    sendPlacesEvent("add_to_another_list_click")
                    listsContainingPage?.let { entry?.let { callback?.onAddRequest(it, false) } }
                    true
                }
                R.id.menu_long_press_move_from_list_to_another_list -> {
                    sendPlacesEvent("move_from_list_to_another_list_click")
                    listsContainingPage?.let { list -> entry?.let { callback?.onMoveRequest(list[0].pages[0], it) } }
                    true
                }
                R.id.menu_long_press_remove_from_lists -> {
                    sendPlacesEvent("remove_from_list_click")
                    deleteOrShowDialog()
                    callback?.onRemoveRequest()
                    true
                }
                R.id.menu_long_press_share_page -> {
                    sendPlacesEvent("share_click")
                    entry?.let { ShareUtil.shareText(getActivity(), it.title) }
                    true
                }
                R.id.menu_long_press_copy_page -> {
                    sendPlacesEvent("copy_link_click")
                    entry?.let {
                        ClipboardUtil.setPlainText(getActivity(), text = it.title.uri)
                        FeedbackUtil.showMessage((getActivity()), R.string.address_copied)
                    }
                    true
                }
                R.id.menu_long_press_get_directions -> {
                    sendPlacesEvent("directions_click")
                    location?.let {
                        entry?.let {
                            GeoUtil.sendGeoIntent(getActivity(), location, StringUtil.fromHtml(it.title.displayText).toString())
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun sendPlacesEvent(action: String) {
        entry?.let {
            if (it.source == HistoryEntry.SOURCE_PLACES) {
                PlacesEvent.logAction(action, "list_view_menu")
            }
        }
    }
}
