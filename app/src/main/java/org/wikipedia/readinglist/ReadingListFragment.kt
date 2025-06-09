package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentReadingListBinding
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageAvailableOfflineHandler
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListNotificationManager
import org.wikipedia.readinglist.recommended.RecommendedReadingListSettingsActivity
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.CircularProgressBar
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.MultiSelectActionModeCallback.Companion.isTagType
import org.wikipedia.views.PageItemView
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import java.util.Date
import java.util.Locale

class ReadingListFragment : Fragment(), MenuProvider, ReadingListItemActionsDialog.Callback {

    private var _binding: FragmentReadingListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReadingListFragmentViewModel by viewModels()

    private lateinit var touchCallback: SwipeableItemTouchHelperCallback
    private lateinit var headerView: ReadingListItemView
    private var previewSaveDialog: AlertDialog? = null
    private var readingListMode: ReadingListMode = ReadingListMode.DEFAULT
    private var readingListId: Long = 0
    private val adapter = ReadingListPageItemAdapter()
    private var actionMode: ActionMode? = null
    private val appBarListener = AppBarListener()
    private var showOverflowMenu = false
    private val readingListItemCallback = ReadingListItemCallback()
    private val readingListPageItemCallback = ReadingListPageItemCallback()
    private val searchActionModeCallback = SearchCallback()
    private val multiSelectActionModeCallback = MultiSelectCallback()
    private var toolbarExpanded = true
    private var displayedLists = mutableListOf<Any>()
    private var currentSearchQuery: String? = null
    private var articleLimitMessageShown = false
    private var exclusiveTooltipRunnable: Runnable? = null
    private val isPreview get() = readingListMode == ReadingListMode.PREVIEW
    private val isRecommendedList get() = readingListMode == ReadingListMode.RECOMMENDED
    var readingList: ReadingList? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentReadingListBinding.inflate(inflater, container, false)
        appCompatActivity.setSupportActionBar(binding.readingListToolbar)
        appCompatActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appCompatActivity.supportActionBar!!.title = ""
        DeviceUtil.updateStatusBarTheme(requireActivity(), binding.readingListToolbar, true)

        readingListMode = (requireArguments().getSerializable(ReadingListActivity.EXTRA_READING_LIST_MODE) as ReadingListMode?) ?: ReadingListMode.DEFAULT
        readingListId = requireArguments().getLong(ReadingListActivity.EXTRA_READING_LIST_ID, -1)

        touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        ItemTouchHelper(touchCallback).attachToRecyclerView(binding.readingListRecyclerView)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        setToolbar()
        setHeaderView()
        setRecyclerView()
        setSwipeRefreshView()

        if (isRecommendedList) {
            RecommendedReadingListEvent.submit("impression", "rrl_discover")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.updateListByIdFlow.collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                binding.readingListSwipeRefresh.isRefreshing = false
                                readingList = resource.data
                                readingList?.let {
                                    binding.searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results, it.title))
                                }
                                update()
                            }
                            is Resource.Error -> {
                                // If we failed to retrieve the requested list, it means that the list is no
                                // longer in the database (likely removed due to sync).
                                // In this case, there's nothing for us to do, so just bail from the activity.
                                requireActivity().finish()
                            }
                        }
                    }
                }
                launch {
                    viewModel.updateListFlow.collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                readingList = resource.data
                                readingList?.let {
                                    ReadingListsAnalyticsHelper.logReceivePreview(requireContext(), it)
                                    binding.searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results, it.title))
                                }
                                update()
                            }
                            is Resource.Error -> {
                                L.e(resource.throwable)
                                FeedbackUtil.showError(requireActivity(), resource.throwable)
                                requireActivity().finish()
                            }
                        }
                    }
                }
                launch {
                    viewModel.recommendedListFlow.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.errorView.isVisible = false
                                binding.readingListHeader.isVisible = false
                                binding.readingListSwipeRefresh.isVisible = false
                            }
                            is Resource.Success -> {
                                readingList = it.data
                                binding.progressBar.isVisible = false
                                binding.errorView.isVisible = false
                                binding.readingListHeader.isVisible = true
                                binding.readingListSwipeRefresh.isVisible = true
                                binding.readingListSwipeRefresh.isRefreshing = false
                                update()
                                maybeShowCustomizeSnackbar()
                            }
                            is Resource.Error -> {
                                L.e(it.throwable)
                                binding.progressBar.isVisible = false
                                binding.errorView.isVisible = true
                                binding.readingListHeader.isVisible = false
                                binding.readingListSwipeRefresh.isVisible = false
                                binding.errorView.backClickListener = View.OnClickListener {
                                    (requireActivity() as ReadingListActivity).onBackPressed()
                                }
                                binding.errorView.setError(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    FlowEventBus.events.collectLatest { event ->
                        when (event) {
                            is ReadingListSyncEvent -> {
                                updateReadingListData()
                            }
                            is PageDownloadEvent -> {
                                val pagePosition = getPagePositionInList(event.page)
                                if (pagePosition < 0) {
                                    return@collectLatest
                                }
                                val readingLisPage = displayedLists[pagePosition]
                                if (readingLisPage is ReadingListPage) {
                                    readingLisPage.downloadProgress = event.page.downloadProgress
                                    adapter.notifyItemChanged(pagePosition + 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateReadingListData()
        ReadingListsAnalyticsHelper.logListShown(requireContext(), readingList?.pages?.size ?: 0)
    }

    override fun onDestroyView() {
        previewSaveDialog?.dismiss()
        binding.readingListRecyclerView.adapter = null
        binding.readingListAppBar.removeOnOffsetChangedListener(appBarListener)
        _binding = null

        super.onDestroyView()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        if (readingListMode != ReadingListMode.DEFAULT) {
            return
        }
        inflater.inflate(R.menu.menu_reading_list, menu)
        if (showOverflowMenu) {
            inflater.inflate(R.menu.menu_reading_list_item, menu)
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        if (readingListMode != ReadingListMode.DEFAULT) {
            return
        }
        val sortByNameItem = menu.findItem(R.id.menu_sort_by_name)
        val sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent)
        val sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC)
        sortByNameItem.setTitle(if (sortMode == ReadingList.SORT_BY_NAME_ASC) R.string.reading_list_sort_by_name_desc else R.string.reading_list_sort_by_name)
        sortByRecentItem.setTitle(if (sortMode == ReadingList.SORT_BY_RECENT_DESC) R.string.reading_list_sort_by_recent_desc else R.string.reading_list_sort_by_recent)
        val searchItem = menu.findItem(R.id.menu_search_lists)
        val sortOptionsItem = menu.findItem(R.id.menu_sort_options)
        val iconColor = if (toolbarExpanded) AppCompatResources.getColorStateList(requireContext(), android.R.color.white)
        else ResourceUtil.getThemedColorStateList(requireContext(), R.attr.primary_color)
        MenuItemCompat.setIconTintList(searchItem, iconColor)
        MenuItemCompat.setIconTintList(sortOptionsItem, iconColor)
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

    override fun onMenuItemSelected(item: MenuItem): Boolean {
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
                    ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), it.pages) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                }
                true
            }
            R.id.menu_reading_list_remove_all_offline -> {
                readingList?.let {
                    ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), it.pages) {
                        adapter.notifyDataSetChanged()
                        update()
                    }
                }
                true
            }
            R.id.menu_reading_list_share -> {
                readingList?.let {
                    ReadingListsShareHelper.shareReadingList(requireActivity() as AppCompatActivity, it)
                }
                true
            }
            R.id.menu_reading_list_export -> {
                readingList?.let {
                    ReadingListsExportImportHelper.exportLists(requireActivity() as BaseActivity, listOf(it))
                }
                true
            }
            R.id.menu_reading_list_select -> {
                beginMultiSelect()
                true
            }
            else -> false
        }
    }

    private fun setToolbar() {
        binding.readingListAppBar.addOnOffsetChangedListener(appBarListener)
        binding.readingListToolbarContainer.setCollapsedTitleTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.readingListToolbarContainer.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }
    }

    private fun setHeaderView() {
        headerView = ReadingListItemView(requireContext())
        headerView.callback = HeaderCallback()
        headerView.isClickable = false
        headerView.setThumbnailVisible(false)
        headerView.setTitleTextAppearance(R.style.H2)
        headerView.setOverflowViewVisibility(true)
        headerView.setMode(readingListMode)

        if (isPreview) {
            headerView.saveClickListener = View.OnClickListener {
                previewSaveDialog()
            }
            return
        }

        if (!Prefs.readingListShareTooltipShown) {
            enqueueTooltip {
                FeedbackUtil.showTooltip(
                    requireActivity(),
                    headerView.shareButton,
                    getString(R.string.reading_list_share_menu_tooltip),
                    aboveOrBelow = false,
                    autoDismiss = true,
                    showDismissButton = true
                )
                Prefs.readingListShareTooltipShown = true
            }
        }
    }

    private fun setRecyclerView() {
        binding.readingListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.readingListRecyclerView.adapter = adapter
        (binding.readingListRecyclerView.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations = false
        binding.readingListRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, drawStart = true, drawEnd = false))
    }

    private fun setSwipeRefreshView() {
        if (isRecommendedList) {
            return
        }
        binding.readingListSwipeRefresh.setOnRefreshListener { ReadingListsFragment.refreshSync(this, binding.readingListSwipeRefresh) }
        if (RemoteConfig.config.disableReadingListSync) {
            binding.readingListSwipeRefresh.isEnabled = false
        }
    }

    private val appCompatActivity get() = requireActivity() as AppCompatActivity

    private fun update(readingList: ReadingList? = this.readingList) {
        readingList?.let {
            binding.readingListEmptyText.visibility = if (it.pages.isEmpty()) View.VISIBLE else View.GONE
            headerView.setReadingList(it, ReadingListItemView.Description.DETAIL)
            headerView.setMode(readingListMode)
            binding.readingListHeader.setReadingList(it)
            ReadingList.sort(readingList, Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC))
            setSearchQuery()
            if (!toolbarExpanded) {
                binding.readingListToolbarContainer.title = it.title
            }
            if (!articleLimitMessageShown && it.pages.size >= Constants.MAX_READING_LIST_ARTICLE_LIMIT) {
                val message = getString(R.string.reading_list_article_limit_message, readingList.title, Constants.MAX_READING_LIST_ARTICLE_LIMIT)
                FeedbackUtil.makeSnackbar(requireActivity(), message).show()
                articleLimitMessageShown = true
            }
        }
    }

    private fun updateReadingListData() {
        when (readingListMode) {
            ReadingListMode.DEFAULT -> viewModel.updateListById(readingListId)
            ReadingListMode.PREVIEW -> {
                if (readingList == null) {
                    val emptyTitle = requireContext().getString(R.string.reading_lists_preview_header_title)
                    val emptyDescription = DateUtil.getTimeAndDateString(requireContext(), Date())
                    viewModel.updateList(emptyTitle, emptyDescription, encoded = true)
                } else {
                    update()
                }
            }
            ReadingListMode.RECOMMENDED -> {
                // Make sure the feature is enabled
                Prefs.isRecommendedReadingListEnabled = true
                if (readingList == null) {
                    viewModel.generateRecommendedReadingList()
                } else {
                    update()
                }
            }
        }
    }

    private fun enqueueTooltip(runnable: Runnable) {
        if (exclusiveTooltipRunnable != null) {
            return
        }
        exclusiveTooltipRunnable = runnable
        binding.readingListSwipeRefresh.postDelayed({
            exclusiveTooltipRunnable = null
            if (!isAdded) {
                return@postDelayed
            }
            runnable.run()
        }, 500)
    }

    private fun setSearchQuery() {
        setSearchQuery(currentSearchQuery)
    }

    private fun setSearchQuery(query: String?) {
        readingList?.let {
            currentSearchQuery = query
            if (query.isNullOrEmpty()) {
                displayedLists.clear()
                displayedLists.addAll(it.pages)
                adapter.notifyDataSetChanged()
                updateEmptyState(query)
            } else {
                ReadingListBehaviorsUtil.searchListsAndPages(lifecycleScope, query) { lists ->
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
        }
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun beginMultiSelect() {
        if (SearchActionModeCallback.matches(actionMode)) {
            finishActionMode()
        }
        if (!isTagType(actionMode)) {
            appCompatActivity.startSupportActionMode(multiSelectActionModeCallback)
        }
    }

    private fun toggleSelectPage(page: ReadingListPage?) {
        page?.let {
            it.selected = !it.selected
            if (selectedPageCount == 0) {
                finishActionMode()
            } else {
                actionMode?.title = resources.getQuantityString(R.plurals.multi_items_selected, selectedPageCount, selectedPageCount)
            }
            adapter.notifyDataSetChanged()
        }
    }

    private val selectedPageCount get() = displayedLists.count { it is ReadingListPage && it.selected }

    private fun unselectAllPages() {
        readingList?.let {
            it.pages.forEach { page ->
                page.selected = false
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun previewSaveDialog() {
        readingList?.let {
            val view = ReadingListPreviewSaveDialogView(requireContext())
            view.readingListMode = readingListMode
            val savedPages = it.pages.toMutableList()
            var readingListTitle = getString(R.string.reading_list_name_sample)

            view.setContentType(it, savedPages, object : ReadingListPreviewSaveDialogView.Callback {
                override fun onError() {
                    previewSaveDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                }

                override fun onSuccess(listTitle: String) {
                    previewSaveDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    readingListTitle = listTitle
                }
            })

            previewSaveDialog = MaterialAlertDialogBuilder(requireContext())
                .setPositiveButton(R.string.reading_lists_preview_save_dialog_save) { _, _ ->
                    it.pages.clear()
                    it.pages.addAll(savedPages)
                    it.listTitle = readingListTitle
                    if (readingListMode == ReadingListMode.RECOMMENDED) {
                        it.description = null
                    }
                    // Save reading list to database
                    it.id = AppDatabase.instance.readingListDao().insertReadingList(it)
                    AppDatabase.instance.readingListPageDao().addPagesToList(it, it.pages, true)
                    Prefs.readingListRecentReceivedId = it.id

                    if (isRecommendedList) {
                        RecommendedReadingListEvent.submit("add_list_new", "rrl_discover", countSaved = it.pages.size)
                    }

                    requireActivity().startActivity(MainActivity.newIntent(requireContext())
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(Constants.INTENT_EXTRA_PREVIEW_SAVED_READING_LISTS, true))
                    requireActivity().finish()
                }
                .setNegativeButton(R.string.reading_lists_preview_save_dialog_cancel, null)
                .create()

            previewSaveDialog?.setView(view)
            previewSaveDialog?.show()
        }
    }

    fun updateNotificationIcon() {
        update()
    }

    /**
     * CAUTION: This returns the selected pages AND automatically marks them as unselected.
     * Make sure to call this getter once, and operate only on the returned list.
     */
    private val selectedPages: List<ReadingListPage>
        get() {
            return readingList?.let {
                displayedLists.filterIsInstance<ReadingListPage>()
                    .filter { it.selected }
                    .onEach { it.selected = false }
            } ?: emptyList()
        }

    private fun deleteSelectedPages() {
        readingList?.let {
            val pages = selectedPages
            if (pages.isNotEmpty()) {
                AppDatabase.instance.readingListPageDao().markPagesForDeletion(it, pages)
                it.pages.removeAll(pages)
                ReadingListBehaviorsUtil.showDeletePagesUndoSnackbar(requireActivity(), it, pages) { updateReadingListData() }
                update()
            }
        }
    }

    private fun addSelectedPagesToList() {
        val pages = selectedPages
        if (pages.isNotEmpty()) {
            val titles = pages.map { ReadingListPage.toPageTitle(it) }
            ExclusiveBottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(titles, InvokeSource.READING_LIST_ACTIVITY))
            update()
        }
    }

    private fun moveSelectedPagesToList() {
        val pages = selectedPages
        if (pages.isNotEmpty()) {
            val titles = pages.map { ReadingListPage.toPageTitle(it) }
            ExclusiveBottomSheetPresenter.show(childFragmentManager,
                    MoveToReadingListDialog.newInstance(readingListId, titles, InvokeSource.READING_LIST_ACTIVITY))
            update()
        }
    }

    private fun delete() {
        readingList?.let {
            ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), it, true) {
                startActivity(MainActivity.newIntent(requireActivity()).putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, it.title))
                requireActivity().finish()
            }
        }
    }

    override fun onToggleItemOffline(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ReadingListBehaviorsUtil.togglePageOffline(requireActivity() as AppCompatActivity, page) {
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
        ExclusiveBottomSheetPresenter.show(childFragmentManager,
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY))
    }

    override fun onMoveItemToOther(pageId: Long) {
        val page = getPageById(pageId) ?: return
        ExclusiveBottomSheetPresenter.show(childFragmentManager,
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
            ReadingListBehaviorsUtil.deletePages(requireActivity() as AppCompatActivity, listsContainPage, page, { updateReadingListData() }, {
                update()
            })
        }
    }

    private fun getPageById(id: Long): ReadingListPage? {
        return readingList?.pages?.firstOrNull { it.id == id }
    }

    private fun maybeShowCustomizeSnackbar() {
        if (isRecommendedList && !Prefs.isRecommendedReadingListOnboardingShown) {
            // Register the notification permission and schedule the notification for the first time.
            requestPermissionAndScheduleRecommendedReadingNotification()
            val message = getString(
                R.string.recommended_reading_list_page_snackbar,
                Prefs.recommendedReadingListArticlesNumber,
                getString(Prefs.recommendedReadingListUpdateFrequency.snackbarStringRes).lowercase(Locale.getDefault())
            )
            FeedbackUtil.makeSnackbar(requireActivity(), message)
                .setAction(R.string.recommended_reading_list_page_snackbar_action) {
                    RecommendedReadingListEvent.submit("customize_click", "rrl_discover")
                   startActivity(RecommendedReadingListSettingsActivity.newIntent(requireContext()))
                }
                .show()

            Prefs.isRecommendedReadingListOnboardingShown = true
        }
    }

    private fun showRecommendedReadingListNotificationOffDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recommended_reading_list_settings_notifications_dialog_title)
            .setMessage(R.string.recommended_reading_list_settings_notifications_dialog_message)
            .setPositiveButton(R.string.recommended_reading_list_settings_notifications_dialog_negative_button) { _, _ ->
                if (Prefs.isRecommendedReadingListNotificationEnabled) {
                    return@setPositiveButton
                }
                Prefs.isRecommendedReadingListNotificationEnabled = true
                requestPermissionAndScheduleRecommendedReadingNotification()
                update()
            }
            .setNegativeButton(R.string.recommended_reading_list_settings_notifications_dialog_positive_button) { _, _ ->
                Prefs.isRecommendedReadingListNotificationEnabled = false
                RecommendedReadingListNotificationManager.cancelRecommendedReadingListNotification(requireContext())
                update()
            }
            .show()
    }

    private fun requestPermissionAndScheduleRecommendedReadingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(requireActivity(), permission) == PackageManager.PERMISSION_GRANTED -> {
                    RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(requireActivity())
                    Prefs.isRecommendedReadingListNotificationEnabled = true
                    update()
                }
                else -> (requireActivity() as ReadingListActivity).requestPermissionLauncher.launch(permission)
            }
        } else {
            RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(requireActivity())
            Prefs.isRecommendedReadingListNotificationEnabled = true
            update()
        }
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
                binding.readingListToolbarContainer.title = readingList?.title
                appCompatActivity.invalidateOptionsMenu()
                toolbarExpanded = false
            }
            DeviceUtil.updateStatusBarTheme(requireActivity(), binding.readingListToolbar,
                    actionMode == null && appBarLayout.totalScrollRange + verticalOffset > appBarLayout.totalScrollRange / 2)
            (requireActivity() as ReadingListActivity).updateNavigationBarColor()
            // prevent swiping when collapsing the view
            binding.readingListSwipeRefresh.isEnabled = verticalOffset == 0 && !isRecommendedList
        }
    }

    private inner class ReadingListItemHolder(itemView: ReadingListItemView) : DefaultViewHolder<View>(itemView) {
        fun bindItem(readingList: ReadingList) {
            view.setReadingList(readingList, ReadingListItemView.Description.SUMMARY)
            view.setMode(readingListMode)
            view.setSearchQuery(currentSearchQuery)
        }

        override val view get() = itemView as ReadingListItemView
    }

    private inner class ReadingListPageItemHolder(itemView: PageItemView<ReadingListPage>) : DefaultViewHolder<PageItemView<ReadingListPage>>(itemView), SwipeableItemTouchHelperCallback.Callback {
        private lateinit var page: ReadingListPage
        private lateinit var pageTitle: PageTitle
        fun bindItem(page: ReadingListPage) {
            this.page = page
            this.pageTitle = ReadingListPage.toPageTitle(page)
            view.item = page
            view.setTitle(page.displayTitle)
            view.setDescription(page.description)
            view.setImageUrl(page.thumbUrl)
            view.isSelected = page.selected
            view.setSecondaryActionIcon(if (page.saving) R.drawable.ic_download_in_progress else R.drawable.ic_download_circle_gray_24dp,
                    if (readingListMode != ReadingListMode.DEFAULT) false else !page.offline || page.saving)
            view.setCircularProgressVisibility(page.downloadProgress > 0 && page.downloadProgress < CircularProgressBar.MAX_PROGRESS)
            view.setProgress(if (page.downloadProgress == CircularProgressBar.MAX_PROGRESS) 0 else page.downloadProgress)
            view.setActionHint(R.string.reading_list_article_make_offline)
            view.setSearchQuery(currentSearchQuery)
            PageAvailableOfflineHandler.check(page) { view.setViewsGreyedOut(!it) }
            if (isRecommendedList) {
                PageAvailableOfflineHandler.checkHistory(viewLifecycleOwner.lifecycleScope, pageTitle) { view.setViewsRead(it) }
            }
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
            when (readingListMode) {
                ReadingListMode.DEFAULT -> {
                    readingList?.let {
                        if (currentSearchQuery.isNullOrEmpty()) {
                            ReadingListBehaviorsUtil.deletePages(requireActivity() as AppCompatActivity, listOf(it), page, { updateReadingListData() }, {
                                update()
                            })
                        }
                    }
                }
                ReadingListMode.RECOMMENDED, ReadingListMode.PREVIEW -> { }
            }
        }

        override fun isSwipeable(): Boolean { return readingListMode == ReadingListMode.DEFAULT }
    }

    private inner class ReadingListHeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private inner class ReadingListPageItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val headerCount get() = if (currentSearchQuery.isNullOrEmpty()) 1 else 0

        override fun getItemViewType(position: Int): Int {
            return if (headerCount == 1 && position == 0) {
                TYPE_HEADER
            } else if (displayedLists[position - headerCount] is ReadingList) {
                TYPE_ITEM
            } else {
                TYPE_PAGE_ITEM
            }
        }

        override fun getItemCount(): Int {
            return headerCount + displayedLists.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return when (type) {
                TYPE_ITEM -> {
                    val view = ReadingListItemView(requireContext())
                    ReadingListItemHolder(view)
                }
                TYPE_HEADER -> {
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
        override fun onRename(readingList: ReadingList) {
            rename()
        }

        override fun onDelete(readingList: ReadingList) {
            delete()
        }

        override fun onSaveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), readingList.pages) {
                adapter.notifyDataSetChanged()
                update()
            }
        }

        override fun onRemoveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), readingList.pages) {
                adapter.notifyDataSetChanged()
                update()
            }
        }

        override fun onShare(readingList: ReadingList) {
            if (isRecommendedList) {
                RecommendedReadingListEvent.submit("share_click", "rrl_discover_menu")
            }
            ReadingListsShareHelper.shareReadingList(requireActivity() as AppCompatActivity, readingList)
        }

        override fun onCustomize() {
            RecommendedReadingListEvent.submit("customize_click", "rrl_discover_menu")
            startActivity(RecommendedReadingListSettingsActivity.newIntent(requireContext()))
        }

        override fun onAbout() {
            if (isRecommendedList) {
                RecommendedReadingListEvent.submit("about_click", "rrl_discover_menu")
            }
            UriUtil.visitInExternalBrowser(requireContext(), getString(R.string.recommended_reading_list_url).toUri())
        }

        override fun onNotification() {
            RecommendedReadingListEvent.submit("notifications_click", "rrl_discover")
            if (Prefs.isRecommendedReadingListNotificationEnabled) {
                showRecommendedReadingListNotificationOffDialog()
            } else {
                Prefs.isRecommendedReadingListNotificationEnabled = true
                requestPermissionAndScheduleRecommendedReadingNotification()
                update()
            }
        }

        override fun onSaveToList(readingList: ReadingList) {
            if (isRecommendedList) {
                RecommendedReadingListEvent.submit("save_click", "rrl_discover")
            }
            previewSaveDialog()
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
            ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), readingList.pages) { setSearchQuery() }
        }

        override fun onRemoveAllOffline(readingList: ReadingList) {
            ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), readingList.pages) { setSearchQuery() }
        }

        override fun onShare(readingList: ReadingList) {
            ReadingListsShareHelper.shareReadingList(requireActivity() as AppCompatActivity, readingList)
        }
    }

    private inner class ReadingListPageItemCallback : PageItemView.Callback<ReadingListPage?> {
        override fun onClick(item: ReadingListPage?) {
            if (isTagType(actionMode)) {
                toggleSelectPage(item)
            } else if (item != null) {
                val title = ReadingListPage.toPageTitle(item)
                val entry = HistoryEntry(title, if (isRecommendedList) HistoryEntry.SOURCE_RECOMMENDED_READING_LIST else HistoryEntry.SOURCE_READING_LIST)
                item.touch()
                ReadingListBehaviorsUtil.updateReadingListPage(item)
                if (isRecommendedList) {
                    RecommendedReadingListEvent.submit("reading_list_click", "rrl_discover")
                }
                startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
            }
        }

        override fun onLongClick(item: ReadingListPage?): Boolean {
            if (readingListMode != ReadingListMode.DEFAULT) {
                return false
            }
            item?.let {
                ExclusiveBottomSheetPresenter.show(childFragmentManager,
                        ReadingListItemActionsDialog.newInstance(if (currentSearchQuery.isNullOrEmpty()) listOf(readingList!!)
                        else ReadingListBehaviorsUtil.getListsContainPage(it), it.id, actionMode != null))
                return true
            }
            return false
        }

        override fun onActionClick(item: ReadingListPage?, view: View) {
            item?.let {
                if (Prefs.isDownloadOnlyOverWiFiEnabled && !DeviceUtil.isOnWiFi && it.status == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                    it.offline = false
                }
                if (it.saving) {
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
        (requireActivity() as ReadingListActivity).updateStatusBarColor(inActionMode)
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

    private fun getPagePositionInList(page: ReadingListPage): Int {
        return displayedLists.indexOfFirst { it is ReadingListPage && it.id == page.id }
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

        fun newInstance(readingListMode: ReadingListMode): ReadingListFragment {
            return ReadingListFragment().apply {
                arguments = bundleOf(ReadingListActivity.EXTRA_READING_LIST_MODE to readingListMode)
            }
        }
    }
}
