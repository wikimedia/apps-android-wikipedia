package org.wikipedia.compose.components.menu

import android.content.Context
import android.icu.text.ListFormatter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.RemoveFromReadingListsDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil

class PageOverflowMenuViewModel : ViewModel() {
    data class PageOverflowMenuState(
        val entry: HistoryEntry,
        val items: List<Pair<String, () -> Unit>>
    )

    var pageOverflowMenuState by mutableStateOf<PageOverflowMenuState?>(null)

    fun onPageOverflowClick(
        pageSummary: PageSummary,
        wikiSite: WikiSite,
        context: Context,
        onOpenPage: (HistoryEntry) -> Unit,
        onOpenInNewTab: (HistoryEntry) -> Unit,
        onAddRequest: (HistoryEntry, Boolean) -> Unit,
        onMoveRequest: (Long, HistoryEntry) -> Unit
    ) {
        viewModelScope.launch {
            val entry = pageSummary.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_MOST_READ)
            val lists = AppDatabase.instance.readingListDao().getListsFromPageOccurrences(
                AppDatabase.instance.readingListPageDao().getAllPageOccurrences(entry.title)
            )
            pageOverflowMenuState = PageOverflowMenuState(
                entry = entry,
                items = buildOverflowMenuItems(context, entry, lists, onOpenPage, onOpenInNewTab, onAddRequest, onMoveRequest)
            )
        }
    }

    fun dismissPageOverflowMenu() {
        pageOverflowMenuState = null
    }

    private fun buildOverflowMenuItems(
        context: Context,
        entry: HistoryEntry,
        lists: List<ReadingList>,
        onOpenPage: (HistoryEntry) -> Unit = {},
        onOpenInNewTab: (HistoryEntry) -> Unit = {},
        onAddRequest: (HistoryEntry, addToDefault: Boolean) -> Unit,
        onMoveRequest: (Long, HistoryEntry) -> Unit
    ): List<Pair<String, () -> Unit>> = buildList {

        // Open page
        add(context.getString(R.string.menu_long_press_open_page) to {
            onOpenPage(entry)
        })

        // Open in new tab
        add(context.getString(R.string.menu_long_press_open_in_new_tab) to {
            onOpenInNewTab(entry)
        })

        // Reading lists actions
        if (lists.isEmpty()) {
            add(context.getString(R.string.feed_card_add_to_default_list) to {
                onAddRequest(entry, true)
            })
        } else {
            add(context.getString(R.string.reading_list_add_to_other_list) to {
                onAddRequest(entry, false)
            })

            val removeLabel = if (lists.size == 1) {
                context.getString(R.string.reading_list_remove_from_list, lists[0].title)
            } else {
                context.getString(R.string.reading_list_remove_from_lists)
            }
            val activity = context as? AppCompatActivity
            activity?.let {
                add(removeLabel to {
                    RemoveFromReadingListsDialog(lists).deleteOrShowDialog(activity) { readingLists, _ ->
                        if (!activity.isDestroyed) {
                            val names = readingLists.map { it.title }.run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    ListFormatter.getInstance().format(this)
                                } else {
                                    joinToString(separator = ", ")
                                }
                            }
                            FeedbackUtil.showMessage(
                                activity,
                                activity.getString(
                                    R.string.reading_list_item_deleted_from_list,
                                    entry.title.displayText,
                                    names
                                )
                            )
                        }
                    }
                })
            }

            if (lists.size == 1) {
                add(context.getString(R.string.reading_list_move_from_to_other_list, lists[0].title) to {
                    onMoveRequest(lists[0].id, entry)
                })
            }
        }

        // Share
        add(context.getString(R.string.menu_page_share) to {
            ShareUtil.shareText(context as AppCompatActivity, entry.title)
        })

        // Copy link
        add(context.getString(R.string.menu_long_press_copy_page) to {
            ClipboardUtil.setPlainText(context, text = entry.title.uri)
            FeedbackUtil.showMessage(context as AppCompatActivity, R.string.address_copied)
        })
    }
}
