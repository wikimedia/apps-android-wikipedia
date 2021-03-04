package org.wikipedia.readinglist

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.analytics.ReadingListsFunnel
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.util.log.L
import java.util.*

class MoveToReadingListDialog : AddToReadingListDialog() {
    private var sourceReadingList: ReadingList? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding.dialogTitle.setText(R.string.reading_list_move_to)
        val sourceReadingListId = requireArguments().getLong(SOURCE_READING_LIST_ID)
        sourceReadingList = ReadingListDbHelper.instance().getListById(sourceReadingListId, false)
        if (sourceReadingList == null) {
            dismiss()
        }
        return binding.root
    }

    override fun logClick(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            ReadingListsFunnel().logMoveClick(invokeSource)
        }
    }

    override fun commitChanges(readingList: ReadingList, titles: List<PageTitle>) {
        disposables.add(Observable.fromCallable { ReadingListDbHelper.instance().movePagesToListAndDeleteSourcePages(sourceReadingList!!, readingList, titles) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ movedTitlesList: List<String> ->
                    ReadingListsFunnel().logMoveToList(readingList, readingLists.size, invokeSource)
                    showViewListSnackBar(readingList, if (movedTitlesList.size == 1) getString(R.string.reading_list_article_moved_to_named, movedTitlesList[0], readingList.title()) else getString(R.string.reading_list_articles_moved_to_named, movedTitlesList.size, readingList.title()))
                    dismiss()
                }) { obj: Throwable -> L.w(obj) })
    }

    companion object {
        private const val SOURCE_READING_LIST_ID = "sourceReadingListId"

        @JvmStatic
        fun newInstance(sourceReadingListId: Long,
                        title: PageTitle,
                        source: InvokeSource): MoveToReadingListDialog {
            return newInstance(sourceReadingListId, listOf(title), source, true, null)
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(sourceReadingListId: Long,
                        titles: List<PageTitle>,
                        source: InvokeSource,
                        showDefaultList: Boolean = true,
                        listener: DialogInterface.OnDismissListener? = null): MoveToReadingListDialog {
            val dialog = MoveToReadingListDialog()
            val args = Bundle()
            args.putParcelableArrayList(PAGE_TITLE_LIST, ArrayList<Parcelable>(titles))
            args.putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
            args.putLong(SOURCE_READING_LIST_ID, sourceReadingListId)
            args.putBoolean(SHOW_DEFAULT_LIST, showDefaultList)
            dialog.arguments = args
            dialog.dismissListener = listener
            return dialog
        }
    }
}
