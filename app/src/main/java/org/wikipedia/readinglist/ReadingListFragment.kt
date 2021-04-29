package org.wikipedia.readinglist

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ReadingListsFunnel
import org.wikipedia.databinding.FragmentReadingListBinding
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageAvailableOfflineHandler
import org.wikipedia.page.PageAvailableOfflineHandler.check
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SiteInfoClient.maxPagesPerReadingList
import org.wikipedia.util.*
import org.wikipedia.views.*
import org.wikipedia.views.MultiSelectActionModeCallback.Companion.isTagType

class ReadingListFragment : Fragment(), ReadingListItemActionsDialog.Callback {

    private var _binding: FragmentReadingListBinding? = null
    private val binding get() = _binding!!

    private lateinit var touchCallback: SwipeableItemTouchHelperCallback
    private lateinit var headerView: ReadingListItemView
    private val disposables = CompositeDisposable()
    private var readingList: ReadingList? = null
    private var readingListId: Long = 0
    private val adapter = ReadingListPageItemAdapter()
    private var actionMode: ActionMode? = null
    private val appBarListener = AppBarListener()
    private var showOverflowMenu = false
    private val funnel = ReadingListsFunnel()
    private val readingListItemCallback = ReadingListItemCallback()
    private val readingListPageItemCallback = ReadingListPageItemCallback()
    private val searchActionModeCallback = SearchCallback()
    private val multiSelectActionModeCallback = MultiSelectCallback()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var toolbarExpanded = true
    private var displayedLists = mutableListOf<Any>()
    private var currentSearchQuery: String? = null
    private var articleLimitMessageShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentReadingListBinding.inflate(inflater, container, false)
        appCompatActivity.setSupportActionBar(binding.readingListToolbar)
        appCompatActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar!!.title = ""
        DeviceUtil.updateStatusBarTheme(requireActivity(), binding.readingListToolbar, true)
        touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        ItemTouchHelper(touchCallback).attachToRecyclerView(binding.readingListRecyclerView)
        readingListId = requireArguments().getLong(ReadingListActivity.EXTRA_READING_LIST_ID)
        setToolbar()
        setHeaderView()
        setRecyclerView()
        setSwipeRefreshView()
        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        updateReadingListData()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.readingListRecyclerView.adapter = null
        binding.readingListAppBar.removeOnOffsetChangedListener(appBarListener)
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reading_list, menu)
        if (showOverflowMenu) {
            inflater.inflate(R.menu.menu_reading_list_item, menu)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val sortByNameItem = menu.findItem(R.id.menu_sort_by_name)
        val sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent)
        val sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC)
        sortByNameItem.setTitle(if (sortMode == ReadingList.SORT_BY_NAME_ASC) R.string.reading_list_sort_by_name_desc else R.string.reading_list_sort_by_name)
        sortByRecentItem.setTitle(if (sortMode == ReadingList.SORT_BY_RECENT_DESC) R.string.reading_list_sort_by_recent_desc else R.string.reading_list_sort_by_recent)
        val searchItem = menu.findItem(R.id.menu_search_lists)
        val sortOptionsItem = menu.findItem(R.id.menu_sort_options)
        val iconColor = if (toolbarExpanded) ContextCompat.getColor(requireContext(), android.R.color.white) else ResourceUtil.getThemedColor(requireContext(), R.attr.toolbar_icon_color)
        searchItem.icon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(iconColor, BlendModeCompat.SRC_IN)
        sortOptionsItem.icon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(iconColor, BlendModeCompat.SRC_IN)
        readingList?.let {
            if (it.isDefault) {
                menu.findItem(R.id.menu_reading_list_rename)?.let { item ->
                    item.isVisible = false
                }
                menu.findItem(R.id.menu_reading_list_delete)?.let { item ->
                    item.isVisible = false
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_lists -> {
                appCompatActivity.startSupportActionMode(searchActionModeCallback)
                true
            }
            R.id.menu_sort_by_name -> {
                setSortMode(ReadingList.SORT_BY_NAME_ASC, ReadingList.SORT_BY_NAME_DESC)
                true
            }
            R.id.menu_sort_by_recent -> {
                setSortMode(ReadingList.SORT_BY_RECENT_DESC, ReadingList.SORT_BY_RECENT_ASC)
                true
            }
            R.id.menu_reading_list_rename -> {
                rename()
                true
            }
            R.id.menu_reading_list_delete -> {
                delete()
                true
            }
            R.id.menu_reading_list_save_all_offline -> {
                readingList?.let {
                    ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), it.pages()) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                }
                true
            }
            R.id.menu_reading_list_remove_all_offline -> {
                readingList?.let {
                    ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), it.pages()) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setToolbar() {
        binding.readingListAppBar.addOnOffsetChangedListener(appBarListener)
        binding.readingListToolbarContainer.setCollapsedTitleTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.toolbar_icon_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.readingListToolbarContainer.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }
    }

    private fun setHeaderView() {
        headerView = ReadingListItemView(requireContext())
        headerView.callback = HeaderCallback()
        headerView.isClickable = false
        headerView.setThumbnailVisible(false)
        headerView.setTitleTextAppearance(R.style.ReadingListTitleTextAppearance)
        headerView.setOverflowViewVisibility(View.VISIBLE)
    }

    private fun setRecyclerView() {
        binding.readingListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.readingListRecyclerView.adapter = adapter
        (binding.readingListRecyclerView.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations = false
        binding.readingListRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, drawStart = true, drawEnd = false))
    }

    private fun setSwipeRefreshView() {
        binding.readingListSwipeRefresh.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.readingListSwipeRefresh.setOnRefreshListener { ReadingListsFragment.refreshSync(this, binding.readingListSwipeRefresh) }
        if (ReadingListSyncAdapter.isDisabledByRemoteConfig) {
            binding.readingListSwipeRefresh.isEnabled = false
        }
    }

    private val appCompatActivity get() = requireActivity() as AppCompatActivity

    private fun update(readingList: ReadingList? = this.readingList) {
        readingList?.let {
            binding.readingListEmptyText.visibility = if (it.pages().isEmpty()) View.VISIBLE else View.GONE
            headerView.setReadingList(it, ReadingListItemView.Description.DETAIL)
            binding.readingListHeader.setReadingList(it)
            ReadingList.sort(readingList, Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC))
            setSearchQuery()
            if (!toolbarExpanded) {
                binding.readingListToolbarContainer.title = it.title()
            }
            if (!articleLimitMessageShown && it.pages().size >= maxPagesPerReadingList) {
                val message = getString(R.string.reading_list_article_limit_message, readingList.title(), maxPagesPerReadingList)
                FeedbackUtil.makeSnackbar(requireActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show()
                articleLimitMessageShown = true
            }
        }
    }

    private fun updateReadingListData() {
        disposables.add(Observable.fromCallable { ReadingListDbHelper.instance().getListById(readingListId, true) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ list ->
                    binding.readingListSwipeRefresh.isRefreshing = false
                    readingList = list
                    readingList?.let {
                        binding.searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results, it.title()))
                    }
                    update()
                }) {
                    // If we failed to retrieve the requested list, it means that the list is no
                    // longer in the database (likely removed due to sync).
                    // In this case, there's nothing for us to do, so just bail from the activity.
                    requireActivity().finish()
                })
    }

    private fun setSearchQuery() {
        setSearchQuery(currentSearchQuery)
    }

    private fun setSearchQuery(query: String?) {
        readingList?.let {
            currentSearchQuery = query
            if (query.isNullOrEmpty()) {
                displayedLists.clear()
                displayedLists.addAll(it.pages())
                adapter.notifyDataSetChanged()
                updateEmptyState(query)
            } else {
                ReadingListBehaviorsUtil.searchListsAndPages(query) { lists ->
                    displayedLists = lists
                    adapter.notifyDataSetChanged()
                    updateEmptyState(query)
                }
            }
            touchCallback.swipeableEnabled = query.isNullOrEmpty()
        }
    }

    private fun updateEmptyState(searchQuery: String?) {
        if (searchQuery.isNullOrEmpty()) {
            binding.searchEmptyView.visibility = View.GONE
            binding.readingListRecyclerView.visibility = View.VISIBLE
            binding.readingListEmptyText.visibility = if (displayedLists.isEmpty()) View.VISIBLE else View.GONE
        } else {
            binding.readingListRecyclerView.visibility = if (displayedLists.isEmpty()) View.GONE else View.VISIBLE
            binding.searchEmptyView.visibility = if (displayedLists.isEmpty()) View.VISIBLE else View.GONE
            binding.readingListEmptyText.visibility = View.GONE
        }
    }

    private fun setSortMode(sortModeAsc: Int, sortModeDesc: Int) {
        var sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC)
        sortMode = if (sortMode != sortModeAsc) {
            sortModeAsc
        } else {
            sortModeDesc
        }
        Prefs.setReadingListPageSortMode(sortMode)
        requireActivity().invalidateOptionsMenu()
        update()
    }

    private fun rename() {
        ReadingListBehaviorsUtil.renameReadingList(requireActivity(), readingList) {
            update()
            funnel.logModifyList(readingList!!, 0)
        }
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun beginMultiSelect() {
        if (SearchActionModeCallback.`is`(actionMode)) {
            finishActionMode()
        }
        if (!isTagType(actionMode)) {
            appCompatActivity.startSupportActionMode(multiSelectActionModeCallback)
        }
    }

    private fun toggleSelectPage(page: ReadingListPage?) {
        page?.let {
            it.selected(!it.selected())
            if (selectedPageCount == 0) {
                finishActionMode()
            } else {
                actionMode?.title = resources.getQuantityString(R.plurals.multi_items_selected, selectedPageCount, selectedPageCount)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private val selectedPageCount: Int
        get() {
            var selectedCount = 0
            displayedLists.forEach {
                if (it is ReadingListPage && it.selected()) {
                    selectedCount++
                }
            }
            return selectedCount
        }

    private fun unselectAllPages() {
        readingList?.let {
            it.pages().forEach { page ->
                page.selected(false)
            }
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * CAUTION: This returns the selected pages AND automatically marks them as unselected.
     * Make sure to call this getter once, and operate only on the returned list.
     */
    private val selectedPages: List<ReadingListPage>
        get() {
            val result = mutableListOf<ReadingListPage>()
            readingList?.let {
                displayedLists.forEach { list ->
                    if (list is ReadingListPage && list.selected()) {
                        result.add(list)
                        list.selected(false)
                    }
                }
            }
            return result
        }

    private fun deleteSelectedPages() {
        readingList?.let {
            val pages = selectedPages
            if (pages.isNotEmpty()) {
                ReadingListDbHelper.instance().markPagesForDeletion(it, pages)
                it.pages().removeAll(pages)
                funnel.logDeleteItem(it, 0)
                ReadingListBehaviorsUtil.showDeletePagesUndoSnackbar(requireActivity(), it, pages) { updateReadingListData() }
                update()
            }
        }
    }

    private fun addSelectedPagesToList() {
        val pages = selectedPages
        if (pages.isNotEmpty()) {
            val titles = pages.map { ReadingListPage.toPageTitle(it) }
            bottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(titles, InvokeSource.READING_LIST_ACTIVITY))
            update()
        }
    }

    private fun moveSelectedPagesToList() {
        val pages = selectedPages
        if (pages.isNotEmpty()) {
            val titles = pages.map { ReadingListPage.toPageTitle(it) }
            bottomSheetPresenter.show(childFragmentManager,
                    MoveToReadingListDialog.newInstance(readingListId, titles, InvokeSource.READING_LIST_ACTIVITY))
            update()
        }
    }

    private fun delete() {
        readingList?.let {
            ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), it, true) {
                startActivity(MainActivity.newIntent(requireActivity()).putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, it.title()))
                requireActivity().finish()
            }
        }
    }

    override fun onToggleItemOffline(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ReadingListBehaviorsUtil.togglePageOffline(requireActivity(), page) {
            adapter.notifyDataSetChanged()
            update()
        }
    }

    override fun onShareItem(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ShareUtil.shareText(requireContext(), ReadingListPage.toPageTitle(page))
    }

    override fun onAddItemToOther(pageId: Long) {
        val page = getPageById(pageId) ?: return
        bottomSheetPresenter.show(childFragmentManager,
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY))
    }

    override fun onMoveItemToOther(pageId: Long) {
        val page = getPageById(pageId) ?: return
        bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(readingListId, ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY))
    }

    override fun onSelectItem(pageId: Long) {
        val page = getPageById(pageId) ?: return
        if (actionMode == null || isTagType(actionMode)) {
            beginMultiSelect()
            toggleSelectPage(page)
        }
    }

    override fun onDeleteItem(pageId: Long) {
        val page = getPageById(pageId) ?: return
        readingList?.let {
            val listsContainPage = if (currentSearchQuery.isNullOrEmpty()) listOf(it) else ReadingListBehaviorsUtil.getListsContainPage(page)
            ReadingListBehaviorsUtil.deletePages(requireActivity(), listsContainPage, page, { updateReadingListData() }, {
                // TODO: need to verify the log of delete item since this action will delete multiple items in the same time.
                funnel.logDeleteItem(it, 0)
                update()
            })
        }
    }

    private fun getPageById(id: Long): ReadingListPage? {
        return readingList?.pages()?.firstOrNull { it.id() == id }
    }

    private inner class AppBarListener : OnOffsetChangedListener {
        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            if (verticalOffset > -appBarLayout.totalScrollRange && showOverflowMenu) {
                showOverflowMenu = false
                binding.readingListToolbarContainer.title = ""
                appCompatActivity.invalidateOptionsMenu()
                toolbarExpanded = true
            } else if (verticalOffset <= -appBarLayout.totalScrollRange && !showOverflowMenu) {
                showOverflowMenu = true
                binding.readingListToolbarContainer.title = readingList?.title()
                appCompatActivity.invalidateOptionsMenu()
                toolbarExpanded = false
            }
            DeviceUtil.updateStatusBarTheme(requireActivity(), binding.readingListToolbar,
                    actionMode == null && appBarLayout.totalScrollRange + verticalOffset > appBarLayout.totalScrollRange / 2)
            (requireActivity() as ReadingListActivity).updateNavigationBarColor()
            // prevent swiping when collapsing the view
            binding.readingListSwipeRefresh.isEnabled = verticalOffset == 0
        }
    }

    private inner class ReadingListItemHolder constructor(itemView: ReadingListItemView) : DefaultViewHolder<View>(itemView) {
        fun bindItem(readingList: ReadingList) {
            view.setReadingList(readingList, ReadingListItemView.Description.SUMMARY)
            view.setSearchQuery(currentSearchQuery)
        }

        override val view get() = itemView as ReadingListItemView
    }

    private inner class ReadingListPageItemHolder constructor(itemView: PageItemView<ReadingListPage>) : DefaultViewHolder<PageItemView<ReadingListPage>>(itemView), SwipeableItemTouchHelperCallback.Callback {
        private lateinit var page: ReadingListPage
        fun bindItem(page: ReadingListPage) {
            this.page = page
            view.item = page
            view.setTitle(page.title())
            view.setDescription(page.description())
            view.setImageUrl(page.thumbUrl())
            view.isSelected = page.selected()
            view.setSecondaryActionIcon(if (page.saving()) R.drawable.ic_download_in_progress else R.drawable.ic_download_circle_gray_24dp,
                    !page.offline() || page.saving())
            view.setCircularProgressVisibility(page.downloadProgress() > 0 && page.downloadProgress() < CircularProgressBar.MAX_PROGRESS)
            view.setProgress(if (page.downloadProgress() == CircularProgressBar.MAX_PROGRESS) 0 else page.downloadProgress())
            view.setActionHint(R.string.reading_list_article_make_offline)
            view.setSearchQuery(currentSearchQuery)
            view.setListItemImageDimensions(imageDimension, imageDimension)
            check(page, PageAvailableOfflineHandler.Callback { available -> view.setViewsGreyedOut(!available) })
            if (!currentSearchQuery.isNullOrEmpty()) {
                view.setTitleMaxLines(2)
                view.setTitleEllipsis()
                view.setDescriptionMaxLines(2)
                view.setDescriptionEllipsis()
                view.setUpChipGroup(ReadingListBehaviorsUtil.getListsContainPage(page))
            } else {
                view.hideChipGroup()
            }
        }

        override fun onSwipe() {
            readingList?.let {
                if (currentSearchQuery.isNullOrEmpty()) {
                    ReadingListBehaviorsUtil.deletePages(requireActivity(), listOf(it), page, { updateReadingListData() }, {
                        funnel.logDeleteItem(it, 0)
                        update()
                    })
                }
            }
        }

        private val imageDimension
            get() = DimenUtil.roundedDpToPx(if (currentSearchQuery.isNullOrEmpty()) DimenUtil.getDimension(R.dimen.view_list_card_item_image) else ReadingListsFragment.ARTICLE_ITEM_IMAGE_DIMENSION.toFloat())
    }

    private inner class ReadingListHeaderHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
    private inner class ReadingListPageItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val headerCount get() = if (currentSearchQuery.isNullOrEmpty()) 1 else 0

        override fun getItemViewType(position: Int): Int {
            return if (headerCount == 1 && position == 0) {
                Companion.TYPE_HEADER
            } else if (displayedLists[position - headerCount] is ReadingList) {
                Companion.TYPE_ITEM
            } else {
                Companion.TYPE_PAGE_ITEM
            }
        }

        override fun getItemCount(): Int {
            return headerCount + displayedLists.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return when (type) {
                Companion.TYPE_ITEM -> {
                    val view = ReadingListItemView(requireContext())
                    ReadingListItemHolder(view)
                }
                Companion.TYPE_HEADER -> {
                    ReadingListHeaderHolder(headerView)
                }
                else -> {
                    ReadingListPageItemHolder(PageItemView(requireContext()))
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            readingList?.let {
                if (holder is ReadingListItemHolder) {
                    holder.bindItem(displayedLists[pos - headerCount] as ReadingList)
                } else if (holder is ReadingListPageItemHolder) {
                    holder.bindItem(displayedLists[pos - headerCount] as ReadingListPage)
                }
            }
        }

        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder is ReadingListItemHolder) {
                holder.view.callback = readingListItemCallback
            } else if (holder is ReadingListPageItemHolder) {
                holder.view.callback = readingListPageItemCallback
            }
        }

        override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
            if (holder is ReadingListItemHolder) {
                holder.view.callback = null
            } else if (holder is ReadingListPageItemHolder) {
                holder.view.callback = null
            }
            super.onViewDetachedFromWindow(holder)
        }
    }

    private inner class HeaderCallback : ReadingListItemView.Callback {
        override fun onClick(readingList: ReadingList) {}
        override fun onRename(readingList: ReadingList) {
            rename()
        }

        override fun onDelete(readingList: ReadingList) {
            delete()
        }

        override fun onSaveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), readingList.pages()) {
                adapter.notifyDataSetChanged()
                update()
            }
        }

        override fun onRemoveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), readingList.pages()) {
                adapter.notifyDataSetChanged()
                update()
            }
        }
    }

    private inner class ReadingListItemCallback : ReadingListItemView.Callback {
        override fun onClick(readingList: ReadingList) {
            actionMode?.finish()
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList))
        }

        override fun onRename(readingList: ReadingList) {
            ReadingListBehaviorsUtil.renameReadingList(requireActivity(), readingList) { update(readingList) }
        }

        override fun onDelete(readingList: ReadingList) {
            ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), readingList, true) {
                ReadingListBehaviorsUtil.showDeleteListUndoSnackbar(requireActivity(), readingList) { setSearchQuery() }
                setSearchQuery()
            }
        }

        override fun onSaveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), readingList.pages()) { setSearchQuery() }
        }

        override fun onRemoveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), readingList.pages()) { setSearchQuery() }
        }
    }

    private inner class ReadingListPageItemCallback : PageItemView.Callback<ReadingListPage?> {
        override fun onClick(item: ReadingListPage?) {
            if (isTagType(actionMode)) {
                toggleSelectPage(item)
            } else if (item != null) {
                val title = ReadingListPage.toPageTitle(item)
                val entry = HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST)
                item.touch()
                Completable.fromAction {
                    ReadingListDbHelper.instance().updateLists(ReadingListBehaviorsUtil.getListsContainPage(item), false)
                    ReadingListDbHelper.instance().updatePage(item)
                }.subscribeOn(Schedulers.io()).subscribe()
                startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
            }
        }

        override fun onLongClick(item: ReadingListPage?): Boolean {
            item?.let {
                bottomSheetPresenter.show(childFragmentManager,
                        ReadingListItemActionsDialog.newInstance(if (currentSearchQuery.isNullOrEmpty()) listOf(readingList!!)
                        else ReadingListBehaviorsUtil.getListsContainPage(it), it.id(), actionMode != null))
                return true
            }
            return false
        }

        override fun onThumbClick(item: ReadingListPage?) {
            onClick(item)
        }

        override fun onActionClick(item: ReadingListPage?, view: View) {
            item?.let {
                if (Prefs.isDownloadOnlyOverWiFiEnabled() && !DeviceUtil.isOnWiFi() && it.status() == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                    it.offline(false)
                }
                if (it.saving()) {
                    Toast.makeText(context, R.string.reading_list_article_save_in_progress, Toast.LENGTH_LONG).show()
                } else {
                    ReadingListBehaviorsUtil.toggleOffline(requireActivity(), item) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                }
            }
        }

        override fun onListChipClick(readingList: ReadingList) {
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList))
        }
    }

    private fun setStatusBarActionMode(inActionMode: Boolean) {
        DeviceUtil.updateStatusBarTheme(requireActivity(), binding.readingListToolbar, toolbarExpanded && !inActionMode)
        requireActivity().window.statusBarColor = if (!inActionMode) Color.TRANSPARENT else ResourceUtil.getThemedColor(requireActivity(), R.attr.paper_color)
    }

    private inner class SearchCallback : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            binding.readingListRecyclerView.stopScroll()
            binding.readingListAppBar.setExpanded(false, false)
            setStatusBarActionMode(true)
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            setSearchQuery(s.trim())
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            currentSearchQuery = null
            setStatusBarActionMode(false)
            updateReadingListData()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.filter_hint_filter_my_lists_and_articles)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.menuInflater.inflate(R.menu.menu_action_mode_reading_list, menu)
            actionMode = mode
            setStatusBarActionMode(true)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_delete_selected -> {
                    onDeleteSelected()
                    finishActionMode()
                    return true
                }
                R.id.menu_remove_from_offline -> {
                    ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), selectedPages) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                    finishActionMode()
                    return true
                }
                R.id.menu_save_for_offline -> {
                    ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), selectedPages) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                    finishActionMode()
                    return true
                }
                R.id.menu_add_to_another_list -> {
                    addSelectedPagesToList()
                    finishActionMode()
                    return true
                }
                R.id.menu_move_to_another_list -> {
                    moveSelectedPagesToList()
                    finishActionMode()
                    return true
                }
                else -> return false
            }
        }

        override fun onDeleteSelected() {
            deleteSelectedPages()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            unselectAllPages()
            actionMode = null
            setStatusBarActionMode(false)
            super.onDestroyActionMode(mode)
        }
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is ReadingListSyncEvent) {
                updateReadingListData()
            } else if (event is PageDownloadEvent) {
                val pagePosition = getPagePositionInList(event.page)
                if (pagePosition != -1 && displayedLists[pagePosition] is ReadingListPage) {
                    (displayedLists[pagePosition] as ReadingListPage).downloadProgress(event.page.downloadProgress())
                    adapter.notifyItemChanged(pagePosition + 1)
                }
            }
        }
    }

    private fun getPagePositionInList(page: ReadingListPage): Int {
        displayedLists.forEach {
            if (it is ReadingListPage && it.id() == page.id()) {
                return displayedLists.indexOf(it)
            }
        }
        return -1
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_PAGE_ITEM = 2

        fun newInstance(listId: Long): ReadingListFragment {
            return ReadingListFragment().apply {
                arguments = bundleOf(ReadingListActivity.EXTRA_READING_LIST_ID to listId)
            }
        }
    }
}
