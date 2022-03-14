package org.wikipedia.readinglist

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ReadingListsFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentReadingListsBinding
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.feed.FeedFragment
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageAvailableOfflineHandler
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.util.*

class ReadingListsFragment : Fragment(), SortReadingListsDialog.Callback, ReadingListItemActionsDialog.Callback {
    private var _binding: FragmentReadingListsBinding? = null
    private val binding get() = _binding!!
    private var displayedLists = listOf<Any>()
    private val funnel = ReadingListsFunnel()
    private val disposables = CompositeDisposable()
    private val adapter = ReadingListAdapter()
    private val readingListItemCallback = ReadingListItemCallback()
    private val readingListPageItemCallback = ReadingListPageItemCallback()
    private val searchActionModeCallback = ReadingListsSearchCallback()
    private var actionMode: ActionMode? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val overflowCallback = OverflowCallback()
    private var currentSearchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReadingListsBinding.inflate(inflater, container, false)
        binding.searchEmptyView.setEmptyText(R.string.search_reading_lists_no_results)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable))
        setUpScrollListener()
        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))
        binding.swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.swipeRefreshLayout.setOnRefreshListener { refreshSync(this, binding.swipeRefreshLayout) }
        if (ReadingListSyncAdapter.isDisabledByRemoteConfig) {
            binding.swipeRefreshLayout.isEnabled = false
        }
        binding.searchEmptyView.visibility = View.GONE
        enableLayoutTransition(true)
        return binding.root
    }

    private fun setUpScrollListener() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (requireActivity() as MainActivity).updateToolbarElevation(binding.recyclerView.computeVerticalScrollOffset() != 0)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.recyclerView.adapter = null
        binding.recyclerView.clearOnScrollListeners()
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        updateLists()
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_lists -> {
                (requireActivity() as AppCompatActivity)
                        .startSupportActionMode(searchActionModeCallback)
                true
            }
            R.id.menu_overflow_button -> {
                ReadingListsOverflowView(requireContext()).show((requireActivity() as MainActivity).getToolbar().findViewById(R.id.menu_overflow_button), overflowCallback)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onToggleItemOffline(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ReadingListBehaviorsUtil.togglePageOffline(requireActivity(), page) { this.updateLists() }
    }

    override fun onShareItem(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ShareUtil.shareText(requireActivity(), ReadingListPage.toPageTitle(page))
    }

    override fun onAddItemToOther(pageId: Long) {
        val page = getPageById(pageId) ?: return
        bottomSheetPresenter.show(childFragmentManager,
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY))
    }

    override fun onMoveItemToOther(pageId: Long) {
        val page = getPageById(pageId) ?: return
        bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(page.listId, ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY))
    }

    override fun onSelectItem(pageId: Long) {
        // ignore
    }

    override fun onDeleteItem(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ReadingListBehaviorsUtil.deletePages(requireActivity(), ReadingListBehaviorsUtil.getListsContainPage(page), page, { this.updateLists() }) { this.updateLists() }
    }

    private fun getPageById(id: Long): ReadingListPage? {
        return displayedLists.firstOrNull { it is ReadingListPage && it.id == id } as ReadingListPage?
    }

    private inner class OverflowCallback : ReadingListsOverflowView.Callback {
        override fun sortByClick() {
            bottomSheetPresenter.show(childFragmentManager,
                    SortReadingListsDialog.newInstance(Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)))
        }

        override fun createNewListClick() {
            val existingTitles = displayedLists.filterIsInstance<ReadingList>().map { it.title }
            ReadingListTitleDialog.readingListTitleDialog(requireActivity(), getString(R.string.reading_list_name_sample), "",
                    existingTitles) { text, description ->
                AppDatabase.instance.readingListDao().createList(text, description)
                updateLists()
            }.show()
        }

        override fun refreshClick() {
            binding.swipeRefreshLayout.isRefreshing = true
            refreshSync(this@ReadingListsFragment, binding.swipeRefreshLayout)
        }
    }

    private fun sortListsBy(option: Int) {
        when (option) {
            ReadingList.SORT_BY_NAME_DESC -> Prefs.setReadingListSortMode(ReadingList.SORT_BY_NAME_DESC)
            ReadingList.SORT_BY_RECENT_DESC -> Prefs.setReadingListSortMode(ReadingList.SORT_BY_RECENT_DESC)
            ReadingList.SORT_BY_RECENT_ASC -> Prefs.setReadingListSortMode(ReadingList.SORT_BY_RECENT_ASC)
            ReadingList.SORT_BY_NAME_ASC -> Prefs.setReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)
            else -> Prefs.setReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)
        }
        updateLists()
    }

    private fun enableLayoutTransition(enable: Boolean) {
        if (enable) {
            binding.contentContainer.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            binding.emptyContainer.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        } else {
            binding.contentContainer.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
            binding.emptyContainer.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
        }
    }

    fun updateLists() {
        updateLists(currentSearchQuery, !currentSearchQuery.isNullOrEmpty())
    }

    private fun updateLists(searchQuery: String?, forcedRefresh: Boolean) {
        maybeShowOnboarding(searchQuery)
        ReadingListBehaviorsUtil.searchListsAndPages(searchQuery) { lists ->
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return lists.size
                }

                override fun getNewListSize(): Int {
                    return lists.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    if (displayedLists.size <= oldItemPosition || lists.size <= newItemPosition) {
                        return false
                    }
                    return (displayedLists[oldItemPosition] is ReadingList && lists[newItemPosition] is ReadingList &&
                            (displayedLists[oldItemPosition] as ReadingList).id == (lists[newItemPosition] as ReadingList).id)
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    if (displayedLists.size <= oldItemPosition || lists.size <= newItemPosition) {
                        return false
                    }
                    return (displayedLists[oldItemPosition] is ReadingList && lists[newItemPosition] is ReadingList &&
                            (displayedLists[oldItemPosition] as ReadingList).id == (lists[newItemPosition] as ReadingList).id &&
                            (displayedLists[oldItemPosition] as ReadingList).pages.size == (lists[newItemPosition] as ReadingList).pages.size &&
                            (displayedLists[oldItemPosition] as ReadingList).numPagesOffline == (lists[newItemPosition] as ReadingList).numPagesOffline)
                }
            })
            // If the number of lists has changed, just invalidate everything, as a
            // simple way to get the bottom item margin to apply to the correct item.
            val invalidateAll = (forcedRefresh || displayedLists.size != lists.size ||
                    (!currentSearchQuery.isNullOrEmpty() && !searchQuery.isNullOrEmpty() && currentSearchQuery != searchQuery))

            // if the default list is empty, then removes it.
            if (lists.size == 1 && lists[0] is ReadingList &&
                    (lists[0] as ReadingList).isDefault &&
                    (lists[0] as ReadingList).pages.isEmpty()) {
                lists.removeAt(0)
            }
            displayedLists = lists
            if (invalidateAll) {
                adapter.notifyDataSetChanged()
            } else {
                result.dispatchUpdatesTo(adapter)
            }
            binding.swipeRefreshLayout.isRefreshing = false
            maybeShowListLimitMessage()
            updateEmptyState(searchQuery)
            maybeDeleteListFromIntent()
            currentSearchQuery = searchQuery
        }
    }

    private fun maybeShowListLimitMessage() {
        if (actionMode == null && displayedLists.size >= Constants.MAX_READING_LISTS_LIMIT) {
            val message = getString(R.string.reading_lists_limit_message)
            FeedbackUtil.makeSnackbar(requireActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show()
        }
    }

    private fun updateEmptyState(searchQuery: String?) {
        if (searchQuery.isNullOrEmpty()) {
            binding.searchEmptyView.visibility = View.GONE
            setUpEmptyContainer()
            setEmptyContainerVisibility(displayedLists.isEmpty() && binding.onboardingView.visibility == View.GONE)
        } else {
            binding.searchEmptyView.visibility = if (displayedLists.isEmpty()) View.VISIBLE else View.GONE
            setEmptyContainerVisibility(false)
        }
    }

    private fun setEmptyContainerVisibility(visible: Boolean) {
        if (visible) {
            binding.emptyContainer.visibility = View.VISIBLE
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        } else {
            binding.emptyContainer.visibility = View.GONE
        }
    }

    private fun setUpEmptyContainer() {
        if (displayedLists.size == 1 && displayedLists[0] is ReadingList &&
                (displayedLists[0] as ReadingList).isDefault &&
                (displayedLists[0] as ReadingList).pages.isNotEmpty()) {
            binding.emptyTitle.text = getString(R.string.no_user_lists_title)
            binding.emptyMessage.text = getString(R.string.no_user_lists_msg)
        } else {
            binding.emptyTitle.text = getString(R.string.saved_list_empty_title)
            binding.emptyMessage.text = getString(R.string.reading_lists_empty_message)
        }
    }

    override fun onSortOptionClick(position: Int) {
        sortListsBy(position)
    }

    private inner class ReadingListItemHolder constructor(itemView: ReadingListItemView) : DefaultViewHolder<View>(itemView) {
        fun bindItem(readingList: ReadingList) {
            view.setReadingList(readingList, ReadingListItemView.Description.SUMMARY)
            view.setSearchQuery(currentSearchQuery)
        }

        override val view get() = itemView as ReadingListItemView
    }

    private inner class ReadingListPageItemHolder constructor(itemView: PageItemView<ReadingListPage>) : DefaultViewHolder<PageItemView<ReadingListPage>>(itemView) {
        fun bindItem(page: ReadingListPage) {
            view.item = page
            view.setTitle(page.displayTitle)
            view.setTitleMaxLines(2)
            view.setTitleEllipsis()
            view.setDescription(page.description)
            view.setDescriptionMaxLines(2)
            view.setDescriptionEllipsis()
            view.setListItemImageDimensions(DimenUtil.roundedDpToPx(ARTICLE_ITEM_IMAGE_DIMENSION.toFloat()), DimenUtil.roundedDpToPx(ARTICLE_ITEM_IMAGE_DIMENSION.toFloat()))
            view.setImageUrl(page.thumbUrl)
            view.isSelected = page.selected
            view.setSecondaryActionIcon(if (page.saving) R.drawable.ic_download_in_progress else R.drawable.ic_download_circle_gray_24dp, !page.offline || page.saving)
            view.setCircularProgressVisibility(page.downloadProgress > 0 && page.downloadProgress < CircularProgressBar.MAX_PROGRESS)
            view.setProgress(if (page.downloadProgress == CircularProgressBar.MAX_PROGRESS) 0 else page.downloadProgress)
            view.setActionHint(R.string.reading_list_article_make_offline)
            view.setSearchQuery(currentSearchQuery)
            view.setUpChipGroup(ReadingListBehaviorsUtil.getListsContainPage(page))
            PageAvailableOfflineHandler.check(page) { available -> view.setViewsGreyedOut(!available) }
        }
    }

    private inner class ReadingListAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        override fun getItemViewType(position: Int): Int {
            return if (displayedLists[position] is ReadingList) {
                Companion.VIEW_TYPE_ITEM
            } else {
                Companion.VIEW_TYPE_PAGE_ITEM
            }
        }

        override fun getItemCount(): Int {
            return displayedLists.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return if (viewType == Companion.VIEW_TYPE_ITEM) {
                ReadingListItemHolder(ReadingListItemView(requireContext()))
            } else {
                ReadingListPageItemHolder(PageItemView(requireContext()))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            if (holder is ReadingListItemHolder) {
                holder.bindItem(displayedLists[pos] as ReadingList)
            } else {
                (holder as ReadingListPageItemHolder).bindItem(displayedLists[pos] as ReadingListPage)
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is ReadingListItemHolder) {
                holder.view.callback = readingListItemCallback
            } else {
                (holder as ReadingListPageItemHolder).view.callback = readingListPageItemCallback
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ReadingListItemHolder) {
                holder.view.callback = null
            } else {
                (holder as ReadingListPageItemHolder).view.callback = null
            }
            super.onViewDetachedFromWindow(holder)
        }
    }

    private inner class ReadingListItemCallback : ReadingListItemView.Callback {
        override fun onClick(readingList: ReadingList) {
            actionMode?.finish()
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList))
        }

        override fun onRename(readingList: ReadingList) {
            if (readingList.isDefault) {
                L.w("Attempted to rename default list.")
                return
            }
            ReadingListBehaviorsUtil.renameReadingList(requireActivity(), readingList) {
                ReadingListSyncAdapter.manualSync()
                updateLists(currentSearchQuery, true)
                funnel.logModifyList(readingList, displayedLists.size)
            }
        }

        override fun onDelete(readingList: ReadingList) {
            ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), readingList, true) {
                ReadingListBehaviorsUtil.showDeleteListUndoSnackbar(requireActivity(), readingList) { updateLists() }
                funnel.logDeleteList(readingList, displayedLists.size)
                updateLists()
            }
        }

        override fun onSaveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), readingList.pages) { updateLists(currentSearchQuery, true) }
        }

        override fun onRemoveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), readingList.pages) { updateLists(currentSearchQuery, true) }
        }
    }

    private inner class ReadingListPageItemCallback : PageItemView.Callback<ReadingListPage?> {
        override fun onClick(item: ReadingListPage?) {
            item?.let {
                val title = ReadingListPage.toPageTitle(it)
                val entry = HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST)
                it.touch()
                Completable.fromAction {
                    AppDatabase.instance.readingListDao().updateLists(ReadingListBehaviorsUtil.getListsContainPage(it), false)
                    AppDatabase.instance.readingListPageDao().updateReadingListPage(it)
                }.subscribeOn(Schedulers.io()).subscribe()
                startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
            }
        }

        override fun onLongClick(item: ReadingListPage?): Boolean {
            item?.let {
                bottomSheetPresenter.show(childFragmentManager,
                        ReadingListItemActionsDialog.newInstance(ReadingListBehaviorsUtil.getListsContainPage(it), it.id, actionMode != null))
                return true
            }
            return false
        }

        override fun onActionClick(item: ReadingListPage?, view: View) {
            item?.let {
                if (Prefs.isDownloadOnlyOverWiFiEnabled && !DeviceUtil.isOnWiFi &&
                        it.status == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                    it.offline = false
                }
                if (it.saving) {
                    Toast.makeText(context, R.string.reading_list_article_save_in_progress, Toast.LENGTH_LONG).show()
                } else {
                    ReadingListBehaviorsUtil.toggleOffline(requireActivity(), it) { adapter.notifyDataSetChanged() }
                }
            }
        }

        override fun onListChipClick(readingList: ReadingList) {
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList))
        }
    }

    private fun maybeDeleteListFromIntent() {
        if (requireActivity().intent.hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            val titleToDelete = requireActivity().intent
                    .getStringExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)
            requireActivity().intent.removeExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)
            displayedLists.forEach {
                if (it is ReadingList && it.title == titleToDelete) {
                    ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), it, false) {
                        ReadingListBehaviorsUtil.showDeleteListUndoSnackbar(requireActivity(), it) { updateLists() }
                        funnel.logDeleteList(it, displayedLists.size)
                        updateLists()
                    }
                }
            }
        }
    }

    private inner class ReadingListsSearchCallback : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            // searching delay will let the animation cannot catch the update of list items, and will cause crashes
            enableLayoutTransition(false)
            binding.onboardingView.visibility = View.GONE
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(false)
            }
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            updateLists(s.trim(), false)
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            enableLayoutTransition(true)
            actionMode = null
            currentSearchQuery = null
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(true)
            }
            updateLists()
        }

        override fun getSearchHintString(): String {
            return requireContext().resources.getString(R.string.filter_hint_filter_my_lists_and_articles)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is ReadingListSyncEvent) {
                binding.recyclerView.post {
                    if (isAdded) {
                        updateLists()
                    }
                }
            } else if (event is ArticleSavedOrDeletedEvent) {
                if (event.isAdded) {
                    if (Prefs.readingListsPageSaveCount < SAVE_COUNT_LIMIT) {
                        showReadingListsSyncDialog()
                        Prefs.readingListsPageSaveCount = Prefs.readingListsPageSaveCount + 1
                    }
                }
            }
        }
    }

    private fun showReadingListsSyncDialog() {
        if (!Prefs.isReadingListSyncEnabled) {
            if (AccountUtil.isLoggedIn) {
                ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(requireActivity())
            } else {
                ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(requireActivity())
            }
        }
    }

    private fun maybeShowOnboarding(searchQuery: String?) {
        binding.onboardingView.visibility = View.GONE
        if (!searchQuery.isNullOrEmpty()) {
            return
        }
        if (AccountUtil.isLoggedIn && !Prefs.isReadingListSyncEnabled &&
                Prefs.isReadingListSyncReminderEnabled && !ReadingListSyncAdapter.isDisabledByRemoteConfig) {
            binding.onboardingView.setMessageTitle(getString(R.string.reading_lists_sync_reminder_title))
            binding.onboardingView.setMessageText(StringUtil.fromHtml(getString(R.string.reading_lists_sync_reminder_text)).toString())
            binding.onboardingView.setImageResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.sync_reading_list_prompt_drawable), true)
            binding.onboardingView.setPositiveButton(R.string.reading_lists_sync_reminder_action, { ReadingListSyncAdapter.setSyncEnabledWithSetup() }, true)
            binding.onboardingView.setNegativeButton(R.string.reading_lists_ignore_button, {
                binding.onboardingView.visibility = View.GONE
                Prefs.isReadingListSyncReminderEnabled = false
            }, false)
            binding.onboardingView.visibility = View.VISIBLE
        } else if (!AccountUtil.isLoggedIn && Prefs.isReadingListLoginReminderEnabled && !ReadingListSyncAdapter.isDisabledByRemoteConfig) {
            binding.onboardingView.setMessageTitle(getString(R.string.reading_list_login_reminder_title))
            binding.onboardingView.setMessageText(getString(R.string.reading_lists_login_reminder_text))
            binding.onboardingView.setImageResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.sync_reading_list_prompt_drawable), true)
            binding.onboardingView.setPositiveButton(R.string.reading_lists_login_button, {
                if (isAdded && requireParentFragment() is FeedFragment.Callback) {
                    (requireParentFragment() as FeedFragment.Callback).onLoginRequested()
                }
            }, true)
            binding.onboardingView.setNegativeButton(R.string.reading_lists_ignore_button, {
                binding.onboardingView.visibility = View.GONE
                Prefs.isReadingListLoginReminderEnabled = false
                updateEmptyState(null)
            }, false)
            binding.onboardingView.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_PAGE_ITEM = 1
        private const val SAVE_COUNT_LIMIT = 3
        const val ARTICLE_ITEM_IMAGE_DIMENSION = 57

        fun newInstance(): ReadingListsFragment {
            return ReadingListsFragment()
        }

        fun refreshSync(fragment: Fragment, swipeRefreshLayout: SwipeRefreshLayout) {
            if (!AccountUtil.isLoggedIn) {
                ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(fragment.requireActivity())
                swipeRefreshLayout.isRefreshing = false
            } else {
                Prefs.isReadingListSyncEnabled = true
                ReadingListSyncAdapter.manualSyncWithRefresh()
            }
        }
    }
}
