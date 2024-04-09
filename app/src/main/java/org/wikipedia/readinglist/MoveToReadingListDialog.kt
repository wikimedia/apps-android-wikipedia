package org.wikipedia.readinglist

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.log.L

class MoveToReadingListDialog : AddToReadingListDialog() {
    private var sourceReadingList: ReadingList? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val parentView = super.onCreateView(inflater, container, savedInstanceState)
        parentView.findViewById<TextView>(R.id.dialog_title).setText(R.string.reading_list_move_to)
        val sourceReadingListId = requireArguments().getLong(SOURCE_READING_LIST_ID)

        CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { _, exception ->
            L.w(exception)
        }) {
            sourceReadingList = AppDatabase.instance.readingListDao().getListById(sourceReadingListId, false)
            if (sourceReadingList == null) {
                dismiss()
            }
        }

        return parentView
    }

    override fun commitChanges(readingList: ReadingList, titles: List<PageTitle>) {
        sourceReadingList?.let {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
            }) {
                val movedTitlesList = withContext(Dispatchers.IO) {
                    AppDatabase.instance.readingListPageDao().movePagesToListAndDeleteSourcePages(it, readingList, titles)
                }
                showViewListSnackBar(readingList, if (movedTitlesList.size == 1) getString(R.string.reading_list_article_moved_to_named, movedTitlesList[0], readingList.title) else getString(R.string.reading_list_articles_moved_to_named, movedTitlesList.size, readingList.title))
                dismiss()
            }
        }
    }

    companion object {
        private const val SOURCE_READING_LIST_ID = "sourceReadingListId"

        fun newInstance(sourceReadingListId: Long,
                        title: PageTitle,
                        source: InvokeSource): MoveToReadingListDialog {
            return newInstance(sourceReadingListId, listOf(title), source, true, null)
        }

        fun newInstance(sourceReadingListId: Long,
                        title: PageTitle,
                        source: InvokeSource,
                        showDefaultList: Boolean = true,
                        listener: DialogInterface.OnDismissListener? = null): MoveToReadingListDialog {
            return newInstance(sourceReadingListId, listOf(title), source, showDefaultList, listener)
        }

        fun newInstance(sourceReadingListId: Long,
                        titles: List<PageTitle>,
                        source: InvokeSource,
                        showDefaultList: Boolean = true,
                        listener: DialogInterface.OnDismissListener? = null): MoveToReadingListDialog {
            return MoveToReadingListDialog().apply {
                arguments = bundleOf(PAGE_TITLE_LIST to ArrayList<Parcelable>(titles),
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source,
                        SOURCE_READING_LIST_ID to sourceReadingListId,
                        SHOW_DEFAULT_LIST to showDefaultList)
                dismissListener = listener
            }
        }
    }
}
