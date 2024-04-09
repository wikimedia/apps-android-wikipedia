package org.wikipedia.readinglist

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage

class RemoveFromReadingListsDialog(private val listsContainingPage: List<ReadingList>) {
    fun interface Callback {
        fun onDeleted(lists: List<ReadingList>, page: ReadingListPage)
    }

    init {
        ReadingList.sort(listsContainingPage as MutableList<ReadingList>, ReadingList.SORT_BY_NAME_ASC)
    }

    fun deleteOrShowDialog(context: Context, callback: Callback?) {
        if (listsContainingPage.isNullOrEmpty()) {
            return
        }
        if (listsContainingPage.size == 1 && listsContainingPage[0].pages.isNotEmpty()) {
            AppDatabase.instance.readingListPageDao().markPagesForDeletion(listsContainingPage[0], listOf(listsContainingPage[0].pages[0]))
            callback?.onDeleted(listsContainingPage, listsContainingPage[0].pages[0])
            return
        }
        showDialog(context, callback)
    }

    private fun showDialog(context: Context, callback: Callback?) {
        val selectedLists = BooleanArray(listsContainingPage.size)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.reading_list_remove_from_lists)
            .setPositiveButton(R.string.reading_list_remove_list_dialog_ok_button_text) { _, _ ->
                val newLists = (listsContainingPage zip selectedLists.asIterable())
                    .filter { (_, selected) -> selected }
                    .map { (listContainingPage, _) ->
                        AppDatabase.instance.readingListPageDao().markPagesForDeletion(listContainingPage,
                            listOf(listContainingPage.pages[0]))
                        listContainingPage
                    }
                if (newLists.isNotEmpty()) {
                    callback?.onDeleted(newLists, listsContainingPage[0].pages[0])
                }
            }
            .setNegativeButton(R.string.reading_list_remove_from_list_dialog_cancel_button_text, null)
            .setMultiChoiceItems(listsContainingPage.map { it.title }.toTypedArray(), selectedLists) { _, which, checked ->
                selectedLists[which] = checked
            }
            .show()
    }
}
