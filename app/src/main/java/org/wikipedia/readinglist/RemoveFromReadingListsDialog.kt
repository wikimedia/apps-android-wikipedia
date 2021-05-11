package org.wikipedia.readinglist

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import java.util.*

class RemoveFromReadingListsDialog(private val listsContainingPage: List<ReadingList>?) {
    fun interface Callback {
        fun onDeleted(lists: List<ReadingList>, page: ReadingListPage)
    }

    init {
        listsContainingPage?.let {
            ReadingList.sort(it as MutableList<ReadingList>, ReadingList.SORT_BY_NAME_ASC)
        }
    }

    fun deleteOrShowDialog(context: Context, callback: Callback?) {
        if (listsContainingPage.isNullOrEmpty()) {
            return
        }
        if (listsContainingPage.size == 1 && listsContainingPage[0].pages.isNotEmpty()) {
            ReadingListDbHelper.markPagesForDeletion(listsContainingPage[0], listOf(listsContainingPage[0].pages[0]))
            callback?.onDeleted(listsContainingPage, listsContainingPage[0].pages[0])
            return
        }
        showDialog(context, callback)
    }

    private fun showDialog(context: Context, callback: Callback?) {
        listsContainingPage?.let {
            val listNames = arrayOfNulls<String>(it.size)
            val selected = BooleanArray(listNames.size)
            it.forEachIndexed { index, readingList ->
                listNames[index] = readingList.title
            }
            AlertDialog.Builder(context)
                    .setTitle(R.string.reading_list_remove_from_lists)
                    .setPositiveButton(R.string.reading_list_remove_list_dialog_ok_button_text) { _: DialogInterface, _: Int ->
                        var atLeastOneSelected = false
                        val newLists = mutableListOf<ReadingList>()
                        for (i in listNames.indices) {
                            if (selected[i]) {
                                atLeastOneSelected = true
                                ReadingListDbHelper.markPagesForDeletion(it[i], listOf(it[i].pages[0]))
                                newLists.add(it[i])
                            }
                        }
                        if (atLeastOneSelected) {
                            callback?.onDeleted(newLists, it[0].pages[0])
                        }
                    }
                    .setNegativeButton(R.string.reading_list_remove_from_list_dialog_cancel_button_text, null)
                    .setMultiChoiceItems(listNames, selected) { _: DialogInterface, which: Int, checked: Boolean -> selected[which] = checked }
                    .create()
                    .show()
        }
    }
}
