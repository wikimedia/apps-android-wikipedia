package org.wikipedia.history

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMarginsRelative
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.BackPressedHandler
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentHistoryBinding
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.page.PageAvailableOfflineHandler
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.util.*

class HistoryFragment : Fragment(), BackPressedHandler {
    interface Callback {
        fun onLoadPage(entry: HistoryEntry)
    }

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var currentSearchQuery: String? = null
    private val disposables = CompositeDisposable()
    private val adapter = HistoryEntryItemAdapter()
    private val itemCallback = ItemCallback()
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = HistorySearchCallback()
    private val selectedEntries = mutableSetOf<HistoryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.searchEmptyView.setEmptyText(R.string.search_history_no_results)
        val touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.historyList)
        binding.historyList.layoutManager = LinearLayoutManager(context)
        binding.historyList.adapter = adapter
        binding.historyEmptyContainer.visibility = View.GONE
        setUpScrollListener()
        return binding.root
    }

    private fun setUpScrollListener() {
        binding.historyList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (requireActivity() as MainActivity).updateToolbarElevation(binding.historyList.computeVerticalScrollOffset() != 0)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        reloadHistoryItems()
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.historyList.adapter = null
        binding.historyList.clearOnScrollListeners()
        adapter.clearList()
        _binding = null
        super.onDestroyView()
    }

    override fun onBackPressed(): Boolean {
        actionMode?.run {
            finish()
            return@onBackPressed true
        }
        if (selectedEntries.size > 0) {
            unselectAllPages()
            return true
        }
        return false
    }

    private fun updateEmptyState(searchQuery: String?) {
        if (searchQuery.isNullOrEmpty()) {
            binding.searchEmptyView.visibility = View.GONE
            binding.historyEmptyContainer.visibility = if (adapter.isEmpty) View.VISIBLE else View.GONE
        } else {
            binding.searchEmptyView.visibility = if (adapter.isEmpty) View.VISIBLE else View.GONE
            binding.historyEmptyContainer.visibility = View.GONE
        }
    }

    private fun onPageClick(entry: HistoryEntry) {
        callback()?.onLoadPage(entry)
    }

    private fun onClearHistoryClick() {
        disposables.add(AppDatabase.getAppDatabase().historyEntryDao().deleteAll()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate { reloadHistoryItems() }
                .subscribe())
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun beginMultiSelect() {
        if (SearchActionModeCallback.`is`(actionMode)) {
            finishActionMode()
        }
    }

    private fun toggleSelectPage(entry: HistoryEntry?) {
        if (entry == null) {
            return
        }
        if (selectedEntries.contains(entry)) {
            selectedEntries.remove(entry)
        } else {
            selectedEntries.add(entry)
        }
        val selectedCount = selectedEntries.size
        if (selectedCount == 0) {
            finishActionMode()
        } else {
            actionMode?.title = resources.getQuantityString(R.plurals.multi_items_selected, selectedCount, selectedCount)
        }
        adapter.notifyDataSetChanged()
    }

    fun refresh() {
        adapter.notifyDataSetChanged()
        if (!WikipediaApp.instance.isOnline && Prefs.showHistoryOfflineArticlesToast) {
            Toast.makeText(requireContext(), R.string.history_offline_articles_toast, Toast.LENGTH_SHORT).show()
            Prefs.showHistoryOfflineArticlesToast = false
        }
    }

    private fun unselectAllPages() {
        selectedEntries.clear()
        adapter.notifyDataSetChanged()
    }

    private fun deleteSelectedPages() {
        val selectedEntryList = mutableListOf<HistoryEntry>()
        for (entry in selectedEntries) {
            selectedEntryList.add(entry)
            AppDatabase.getAppDatabase().historyEntryDao().delete(entry)
        }
        selectedEntries.clear()
        if (selectedEntryList.isNotEmpty()) {
            showDeleteItemsUndoSnackbar(selectedEntryList)
            reloadHistoryItems()
        }
    }

    private fun showDeleteItemsUndoSnackbar(entries: List<HistoryEntry>) {
        val message = if (entries.size == 1) getString(R.string.history_item_deleted, entries[0].title.displayText) else getString(R.string.history_items_deleted, entries.size)
        val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), message, FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.history_item_delete_undo) {
            AppDatabase.getAppDatabase().historyEntryDao().insert(entries)
            reloadHistoryItems()
        }
        snackbar.show()
    }

    private fun reloadHistoryItems() {
        disposables.clear()
        disposables.add(Observable.fromCallable { AppDatabase.getAppDatabase().historyEntryWithImageDao().filterHistoryItems(currentSearchQuery.orEmpty()) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items -> onLoadItemsFinished(items) }) { t ->
                    L.e(t)
                    onLoadItemsFinished(emptyList())
                })
    }

    private fun onLoadItemsFinished(items: List<Any>) {
        val list = mutableListOf<Any>()
        if (!SearchActionModeCallback.`is`(actionMode)) {
            list.add(SearchBar())
        }
        list.addAll(items)
        adapter.setList(list)
        updateEmptyState(currentSearchQuery)
        requireActivity().invalidateOptionsMenu()
    }

    private class HeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        var headerText = itemView.findViewById<TextView>(R.id.section_header_text)!!

        fun bindItem(date: String) {
            headerText.text = date
        }
    }

    private inner class SearchCardViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        private val historyFilterButton: ImageView
        private val clearHistoryButton: ImageView

        fun bindItem() {
            clearHistoryButton.visibility = if (adapter.isEmpty) View.GONE else View.VISIBLE
            historyFilterButton.visibility = if (adapter.isEmpty) View.GONE else View.VISIBLE
        }

        private fun adjustSearchCardView(searchCardView: WikiCardView) {
            searchCardView.post {
                if (!isAdded) {
                    return@post
                }
                searchCardView.updateLayoutParams<LinearLayout.LayoutParams> {
                    val horizontalMargin = if (DimenUtil.isLandscape(requireContext())) {
                        searchCardView.width / 6 + DimenUtil.roundedDpToPx(30f)
                    } else {
                        DimenUtil.roundedDpToPx(16f)
                    }
                    updateMarginsRelative(start = horizontalMargin, end = horizontalMargin,
                        top = DimenUtil.roundedDpToPx(3f))
                }
            }
            searchCardView.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_22))
        }

        init {
            val searchCardView = itemView.findViewById<WikiCardView>(R.id.search_card)
            val voiceSearchButton = itemView.findViewById<View>(R.id.voice_search_button)
            historyFilterButton = itemView.findViewById(R.id.history_filter)
            clearHistoryButton = itemView.findViewById(R.id.history_delete)
            searchCardView.setOnClickListener { (requireParentFragment() as MainFragment).openSearchActivity(Constants.InvokeSource.NAV_MENU, null, it) }
            voiceSearchButton.setOnClickListener { (requireParentFragment() as MainFragment).onFeedVoiceSearchRequested() }
            historyFilterButton.setOnClickListener {
                if (actionMode == null) {
                    actionMode = (requireActivity() as AppCompatActivity)
                            .startSupportActionMode(searchActionModeCallback)
                }
            }
            clearHistoryButton.setOnClickListener {
                if (selectedEntries.size == 0) {
                    AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_clear_history)
                            .setMessage(R.string.dialog_message_clear_history)
                            .setPositiveButton(R.string.dialog_message_clear_history_yes) { _, _ -> onClearHistoryClick() }
                            .setNegativeButton(R.string.dialog_message_clear_history_no, null).create().show()
                } else {
                    deleteSelectedPages()
                }
            }
            FeedbackUtil.setButtonLongPressToast(historyFilterButton, clearHistoryButton)
            adjustSearchCardView(searchCardView)
        }
    }

    private inner class HistoryEntryItemHolder constructor(itemView: PageItemView<HistoryEntry>) : DefaultViewHolder<PageItemView<HistoryEntry>>(itemView), SwipeableItemTouchHelperCallback.Callback {
        private lateinit var entry: HistoryEntry

        fun bindItem(entry: HistoryEntry) {
            this.entry = entry
            view.item = entry
            view.setTitle(entry.title.displayText)
            view.setDescription(entry.title.description)
            view.setImageUrl(entry.title.thumbUrl)
            view.isSelected = selectedEntries.contains(entry)
            PageAvailableOfflineHandler.check(entry.title) { available: Boolean -> view.setViewsGreyedOut(!available) }
        }

        override fun onSwipe() {
            selectedEntries.add(entry)
            deleteSelectedPages()
        }
    }

    private inner class HistoryEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var historyEntries = mutableListOf<Any>()
        override fun getItemCount(): Int {
            return historyEntries.size
        }

        val isEmpty
            get() = (itemCount == 0 || itemCount == 1 && historyEntries[0] is SearchBar)

        override fun getItemViewType(position: Int): Int {
            return when {
                historyEntries[position] is SearchBar -> Companion.VIEW_TYPE_SEARCH_CARD
                historyEntries[position] is String -> Companion.VIEW_TYPE_HEADER
                else -> Companion.VIEW_TYPE_ITEM
            }
        }

        fun setList(list: MutableList<Any>) {
            historyEntries = list
            notifyDataSetChanged()
        }

        fun clearList() {
            historyEntries.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                Companion.VIEW_TYPE_SEARCH_CARD -> {
                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_history_header_with_search, parent, false)
                    SearchCardViewHolder(view)
                }
                Companion.VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_section_header, parent, false)
                    HeaderViewHolder(view)
                }
                else -> HistoryEntryItemHolder(PageItemView(requireContext()))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            when (holder) {
                is SearchCardViewHolder -> holder.bindItem()
                is HistoryEntryItemHolder -> holder.bindItem(historyEntries[pos] as HistoryEntry)
                else -> (holder as HeaderViewHolder).bindItem(historyEntries[pos] as String)
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is HistoryEntryItemHolder) {
                holder.view.callback = itemCallback
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is HistoryEntryItemHolder) {
                holder.view.callback = null
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun hideHeader() {
            if (historyEntries.isNotEmpty() && historyEntries[0] is SearchBar) {
                historyEntries.removeAt(0)
                notifyDataSetChanged()
            }
        }
    }

    private inner class ItemCallback : PageItemView.Callback<HistoryEntry?> {
        override fun onClick(item: HistoryEntry?) {
            if (selectedEntries.isNotEmpty()) {
                toggleSelectPage(item)
            } else if (item != null) {
                onPageClick(HistoryEntry(item.title, HistoryEntry.SOURCE_HISTORY))
            }
        }

        override fun onLongClick(item: HistoryEntry?): Boolean {
            beginMultiSelect()
            toggleSelectPage(item)
            return true
        }

        override fun onActionClick(item: HistoryEntry?, view: View) {}
        override fun onListChipClick(readingList: ReadingList) {}
    }

    private inner class HistorySearchCallback : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            (requireParentFragment() as MainFragment).setBottomNavVisible(false)
            (binding.historyList.adapter as HistoryEntryItemAdapter).hideHeader()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            currentSearchQuery = s.trim()
            reloadHistoryItems()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            currentSearchQuery = ""
            reloadHistoryItems()
            actionMode = null
            (requireParentFragment() as MainFragment).setBottomNavVisible(true)
        }

        override fun getSearchHintString(): String {
            return requireContext().resources.getString(R.string.history_filter_list_hint)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private class SearchBar

    companion object {
        private const val VIEW_TYPE_SEARCH_CARD = 0
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_ITEM = 2

        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }
}
