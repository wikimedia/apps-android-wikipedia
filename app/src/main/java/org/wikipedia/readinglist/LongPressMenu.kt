package org.wikipedia.readinglist

import android.content.ContextWrapper
import android.location.Location
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
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil

class LongPressMenu(
    private val anchorView: View,
    private val openPageInPlaces: Boolean = false,
    private val menuRes: Int = R.menu.menu_long_press,
    private val location: Location? = null,
    private val callback: Callback? = null
) {
    interface Callback {
        fun onOpenLink(entry: HistoryEntry) {}
        fun onOpenInNewTab(entry: HistoryEntry) {}
        fun onOpenInPlaces(entry: HistoryEntry, location: Location) {}
        fun onSaveRequest(entry: HistoryEntry)
        fun onShareRequest() {}
    }

    private var isArticleSaved = false
    private var entry: HistoryEntry? = null

    fun show(entry: HistoryEntry?) {
        entry?.let {
            anchorView.coroutineScope().launch {
                isArticleSaved = AppDatabase.instance.readingListPageDao().findPageInAnyList(it.title) != null
                this@LongPressMenu.entry = it
                showMenu()
            }
        }
    }

    private fun showMenu() {
        PopupMenu(getActivity(), anchorView).let { menu ->
            menu.menuInflater.inflate(menuRes, menu.menu)
            menu.setOnMenuItemClickListener(PageSaveMenuClickListener())
            menu.gravity = Gravity.END
            // A single entry either way: both states open the save sheet, which is where adding to
            // and removing from collections now happens.
            menu.menu.findItem(R.id.menu_long_press_add_to_default_list)?.setTitle(
                if (isArticleSaved) R.string.link_preview_dialog_saved_button
                else R.string.feed_card_add_to_default_list
            )
            val showOpenPageInPlaces = openPageInPlaces && location != null
            menu.menu.findItem(R.id.menu_long_press_open_in_places)?.isVisible = showOpenPageInPlaces
            menu.menu.findItem(R.id.menu_long_press_open_page)?.isVisible = !showOpenPageInPlaces
            menu.menu.findItem(R.id.menu_long_press_get_directions)?.isVisible = location != null
            menu.show()
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
                    entry?.let { callback?.onSaveRequest(it) }
                    true
                }
                R.id.menu_long_press_share_page -> {
                    sendPlacesEvent("share_click")
                    entry?.let {
                        callback?.onShareRequest()
                        ShareUtil.shareText(getActivity(), it.title)
                    }
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
