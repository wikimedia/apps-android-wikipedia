package org.wikipedia.readinglist

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.DialogAddToReadingListBinding
import org.wikipedia.extensions.parcelableArrayList
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.ReadingListTitleDialog.readingListTitleDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.DimenUtil.getDimension
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.FeedbackUtil.makeSnackbar
import org.wikipedia.util.log.L

open class AddToReadingListDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogAddToReadingListBinding? = null
    private val binding get() = _binding!!

    private lateinit var titles: List<PageTitle>
    private lateinit var adapter: ReadingListAdapter
    private val createClickListener = CreateButtonClickListener()
    private var showDefaultList = false
    private val displayedLists = mutableListOf<ReadingList>()
    private val listItemCallback = ReadingListItemCallback()
    lateinit var invokeSource: InvokeSource
    var readingLists = listOf<ReadingList>()
    var dismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        titles = requireArguments().parcelableArrayList(PAGE_TITLE_LIST)!!
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        showDefaultList = requireArguments().getBoolean(SHOW_DEFAULT_LIST)
        adapter = ReadingListAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogAddToReadingListBinding.inflate(inflater, container, false)
        binding.listOfLists.layoutManager = LinearLayoutManager(requireActivity())
        binding.listOfLists.adapter = adapter
        binding.createButton.setOnClickListener(createClickListener)
        updateLists()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.root.parent as View).peekHeight = roundedDpToPx(getDimension(R.dimen.readingListSheetPeekHeight))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun dismiss() {
        super.dismiss()
        dismissListener?.onDismiss(null)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateLists() {
        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            readingLists = withContext(Dispatchers.IO) {
                AppDatabase.instance.readingListDao().getAllLists()
            }
            displayedLists.clear()
            displayedLists.addAll(readingLists)
            if (!showDefaultList && displayedLists.isNotEmpty()) {
                displayedLists.removeAt(0)
            }
            ReadingList.sort(displayedLists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC))
            adapter.notifyDataSetChanged()
            if (displayedLists.isEmpty()) {
                showCreateListDialog()
            }
        }
    }

    private inner class CreateButtonClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            if (readingLists.size >= Constants.MAX_READING_LISTS_LIMIT) {
                val message = getString(R.string.reading_lists_limit_message)
                dismiss()
                makeSnackbar(requireActivity(), message).show()
            } else {
                showCreateListDialog()
            }
        }
    }

    private fun showCreateListDialog() {
        readingListTitleDialog(requireActivity(), "", "", readingLists.map { it.title }, callback = object : ReadingListTitleDialog.Callback {
            override fun onSuccess(text: String, description: String) {
                addAndDismiss(AppDatabase.instance.readingListDao().createList(text, description), titles)
            }
            override fun onCancel() { }
        }).show()
    }

    private fun addAndDismiss(readingList: ReadingList, titles: List<PageTitle>?) {
        if (readingList.pages.size + titles!!.size > SiteInfoClient.maxPagesPerReadingList) {
            val message = getString(R.string.reading_list_article_limit_message, readingList.title, SiteInfoClient.maxPagesPerReadingList)
            makeSnackbar(requireActivity(), message).show()
            dismiss()
            return
        }
        commitChanges(readingList, titles)
    }

    open fun commitChanges(readingList: ReadingList, titles: List<PageTitle>) {
        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val addedTitlesList = withContext(Dispatchers.IO) {
                AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(readingList, titles)
            }
            val message = if (addedTitlesList.isEmpty()) {
                if (titles.size == 1) getString(R.string.reading_list_article_already_exists_message, readingList.title, titles[0].displayText) else getString(R.string.reading_list_articles_already_exist_message, readingList.title)
            } else {
                if (addedTitlesList.size == 1) getString(R.string.reading_list_article_added_to_named, addedTitlesList[0], readingList.title) else getString(R.string.reading_list_articles_added_to_named, addedTitlesList.size, readingList.title)
            }
            showViewListSnackBar(readingList, message)
            dismiss()
        }
    }

    fun showViewListSnackBar(list: ReadingList, message: String) {
        makeSnackbar(requireActivity(), message)
                .setAction(R.string.reading_list_added_view_button) { v -> v.context.startActivity(ReadingListActivity.newIntent(v.context, list)) }.show()
    }

    private inner class ReadingListItemCallback : ReadingListItemView.Callback {
        override fun onClick(readingList: ReadingList) {
            addAndDismiss(readingList, titles)
        }

        override fun onRename(readingList: ReadingList) {}
        override fun onDelete(readingList: ReadingList) {}
        override fun onSaveAllOffline(readingList: ReadingList) {}
        override fun onRemoveAllOffline(readingList: ReadingList) {}
        override fun onSelectList(readingList: ReadingList) {}
        override fun onChecked(readingList: ReadingList) {}
        override fun onShare(readingList: ReadingList) {}
    }

    private class ReadingListItemHolder constructor(itemView: ReadingListItemView) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(readingList: ReadingList) {
            (itemView as ReadingListItemView).setReadingList(readingList, ReadingListItemView.Description.SUMMARY)
        }

        val view get() = itemView as ReadingListItemView

        init {
            itemView.isLongClickable = false
        }
    }

    private inner class ReadingListAdapter : RecyclerView.Adapter<ReadingListItemHolder>() {
        override fun getItemCount(): Int {
            return displayedLists.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ReadingListItemHolder {
            val view = ReadingListItemView(requireContext())
            return ReadingListItemHolder(view)
        }

        override fun onBindViewHolder(holder: ReadingListItemHolder, pos: Int) {
            holder.bindItem(displayedLists[pos])
        }

        override fun onViewAttachedToWindow(holder: ReadingListItemHolder) {
            super.onViewAttachedToWindow(holder)
            holder.view.callback = listItemCallback
        }

        override fun onViewDetachedFromWindow(holder: ReadingListItemHolder) {
            holder.view.callback = null
            super.onViewDetachedFromWindow(holder)
        }
    }

    companion object {
        const val PAGE_TITLE_LIST = "pageTitleList"
        const val SHOW_DEFAULT_LIST = "showDefaultList"

        fun newInstance(title: PageTitle,
                        source: InvokeSource,
                        listener: DialogInterface.OnDismissListener? = null): AddToReadingListDialog {
            return newInstance(listOf(title), source, listener)
        }

        fun newInstance(titles: List<PageTitle>,
                        source: InvokeSource,
                        listener: DialogInterface.OnDismissListener? = null): AddToReadingListDialog {
            return AddToReadingListDialog().apply {
                arguments = bundleOf(PAGE_TITLE_LIST to ArrayList<Parcelable>(titles),
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source,
                        SHOW_DEFAULT_LIST to true)
                dismissListener = listener
            }
        }
    }
}
