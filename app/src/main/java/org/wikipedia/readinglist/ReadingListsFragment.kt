package org.wikipedia.readinglist

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.events.LoggedInEvent
import org.wikipedia.events.LoggedOutEvent
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.events.PageDownloadEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.compose.OnboardingAction
import org.wikipedia.readinglist.compose.ReadingListMenuAction
import org.wikipedia.readinglist.compose.ReadingListsScreen
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.MultiSelectActionModeCallback.Companion.isTagType
import org.wikipedia.views.ReadingListsOverflowView

class ReadingListsFragment : Fragment(), SortReadingListsDialog.Callback, ReadingListItemActionsDialog.Callback {

    private val viewModel: ReadingListsViewModel by viewModels()
    private var actionMode: ActionMode? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let(::importReadingLists)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecommendedReadingListEvent.submit("impression", "rrl_saved")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()
                    ReadingListsScreen(
                        uiState = uiState,
                        isRefreshing = isRefreshing,
                        pullToRefreshEnabled = !RemoteConfig.config.disableReadingListSync,
                        isSelectionMode = selectionState.enabled,
                        selectedListIds = selectionState.selectedListIds,
                        selectedPageIds = selectionState.selectedPageIds,
                        onSelectTab = ::onSelectTab,
                        onOnboardingAction = ::onOnboardingAction,
                        onRefresh = ::onRefresh,
                        onListClick = ::onListClick,
                        onListMenuAction = ::onListMenuAction,
                        onListSelectionChange = viewModel::toggleListSelection,
                        onPageSelectionChange = viewModel::togglePageSelection,
                        onPageClick = ::onPageClick,
                        onPageLongClick = ::onPageLongClick,
                        onPageChipClick = ::onPageChipClick,
                        onPageToggleOfflineClick = ::onToggleOfflineClick,
                        onDiscoverCardClick = ::onDiscoverCardClick
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        // The list itself updates reactively via the DB flow; this just stops the spinner.
                        is ReadingListSyncEvent -> viewModel.setRefreshing(false)
                        is LoggedInEvent,
                        is LoggedOutEvent,
                        is LoggedOutInBackgroundEvent -> viewModel.refreshAccountState()
                        is PageDownloadEvent -> viewModel.updatePageDownloadProgress(event.page)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.uiState, viewModel.selectionState) { _, _ -> }.collect {
                    actionMode?.invalidate()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    val initialState = viewModel.uiState.first { !it.isLoading }
                    ReadingListsAnalyticsHelper.logListsShown(requireContext(), initialState.listCount)
                    viewModel.uiState
                        .map { it.listCount >= Constants.MAX_READING_LISTS_LIMIT }
                        .distinctUntilChanged()
                        .collect(::maybeShowListLimitMessage)
                }
                launch {
                    viewModel.uiState
                        .map { state ->
                            state.pendingPreviewSavedListId?.takeIf { id ->
                                state.rows.any {
                                    it is ReadingListRow.ListRow && it.list.id == id
                                }
                            }
                        }
                        .distinctUntilChanged()
                        .collect { listId ->
                            listId?.let { maybeShowPreviewSavedReadingListsSnackbar(it) }
                        }
                }
            }
        }
    }

    private fun onRefresh() {
        viewModel.setRefreshing(true)
        if (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount) {
            ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(requireActivity())
            viewModel.setRefreshing(false)
        } else {
            Prefs.isReadingListSyncEnabled = true
            // TODO: Change this back to the less forceful manualSyncWithRefresh() when the
            // service-side endpoint is fixed.
            // https://phabricator.wikimedia.org/T351149
            ReadingListSyncAdapter.manualSyncWithForce(fromRefresh = true)
        }
    }

    private fun maybeShowListLimitMessage(atLimit: Boolean) {
        if (atLimit && actionMode == null) {
            FeedbackUtil.makeSnackbar(
                requireActivity(),
                getString(R.string.reading_lists_limit_message)
            ).show()
        }
    }

    private suspend fun maybeShowPreviewSavedReadingListsSnackbar(listId: Long) {
        val list = AppDatabase.instance.readingListDao().getListWithPagesById(listId)?.toReadingList() ?: return
        ReadingListsAnalyticsHelper.logReceiveFinish(requireContext(), list)
        FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.reading_lists_preview_saved_snackbar))
            .setAction(R.string.suggested_edits_article_cta_snackbar_action) {
                viewModel.clearRecentPreviewSavedList()
                startActivity(ReadingListActivity.newIntent(requireContext(), list))
            }
            .show()
        viewModel.consumePreviewSavedSnackbar(listId)
    }

    private fun maybeDeleteListFromIntent() {
        val intent = requireActivity().intent
        val titleToDelete = intent.getStringExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST) ?: return
        intent.removeExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.instance.readingListDao()
            val listId = dao.getListsWithoutContents().firstOrNull { it.title == titleToDelete }?.id ?: return@launch
            val list = dao.getListWithPagesById(listId)?.toReadingList() ?: return@launch
            ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), list, false) {
                ReadingListBehaviorsUtil.showDeleteListUndoSnackbar(requireActivity(), list) {}
            }
        }
    }

    private fun onOnboardingAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.RecommendedShown -> {
                RecommendedReadingListEvent.submit("impression", "rrl_saved_prompt")
            }
            OnboardingAction.RecommendedAccept -> {
                startActivity(RecommendedReadingListOnboardingActivity.newIntent(requireContext()))
                RecommendedReadingListEvent.submit("enter_click", "rrl_saved_prompt")
            }
            OnboardingAction.RecommendedDismiss -> {
                Prefs.isRecommendedReadingListOnboardingShown = true
                FeedbackUtil.showMessage(this, getString(R.string.recommended_reading_list_onboarding_card_negative_snackbar))
                RecommendedReadingListEvent.submit("nothanks_click", "rrl_saved_prompt")
            }
            OnboardingAction.SyncEnable -> {
                ReadingListSyncAdapter.setSyncEnabledWithSetup()
            }
            OnboardingAction.SyncDismiss -> {
                Prefs.isReadingListSyncReminderEnabled = false
            }
            OnboardingAction.LoginRequest -> {
                if (isAdded && requireParentFragment() is MainFragment) {
                    (requireParentFragment() as MainFragment).onLoginRequested()
                }
            }
            OnboardingAction.LoginDismiss -> {
                Prefs.isReadingListLoginReminderEnabled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccountState()
        viewModel.refreshRecentPreviewSavedList()
        maybeDeleteListFromIntent()
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        actionMode?.finish()
    }

    // Search action mode
    fun startSearchActionMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(searchActionModeCallback)
    }

    private fun onSelectTab(tab: SavedTab) {
        actionMode?.takeIf(::isTagType)?.finish()
        viewModel.setSelectedTab(tab)
        searchActionModeCallback.updateSearchHint(getSearchHint(tab))
    }

    private fun getSearchHint(tab: SavedTab): String {
        return getString(
            when (tab) {
                SavedTab.ALL_ARTICLES -> R.string.reading_lists_search_saved_articles
                SavedTab.COLLECTIONS -> R.string.reading_lists_search_collections
            }
        )
    }

    private val searchActionModeCallback = object : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            viewModel.setSearchActive(true)
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(false)
            }
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.setSearchQuery(s.trim())
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.setSearchActive(false)
            viewModel.setSearchQuery(null)
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(true)
            }
        }

        override fun getSearchHintString(): String {
            return getSearchHint(viewModel.uiState.value.selectedTab)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    // Overflow menu
    fun showReadingListsOverflowMenu() {
        ReadingListsOverflowView(requireContext()).show(
            anchorView = (requireActivity() as MainActivity).getToolbar()
                .findViewById(R.id.menu_overflow_button),
            callback = overflowCallback,
            showCollectionActions = viewModel.uiState.value.selectedTab == SavedTab.COLLECTIONS
        )
    }

    private val overflowCallback = object : ReadingListsOverflowView.Callback {
        override fun sortByClick() {
            ExclusiveBottomSheetPresenter.show(
                childFragmentManager,
                SortReadingListsDialog.newInstance(
                    sortOption = viewModel.uiState.value.sortMode,
                    sortArticles = viewModel.uiState.value.selectedTab == SavedTab.ALL_ARTICLES
                )
            )
        }

        override fun createNewListClick() {
            val existingTitles = viewModel.uiState.value.rows
                .filterIsInstance<ReadingListRow.ListRow>()
                .map { it.list.title }
            ReadingListTitleDialog.readingListTitleDialog(
                activity = requireActivity(),
                title = getString(R.string.reading_list_name_sample),
                description = "",
                otherTitles = existingTitles,
                callback = object : ReadingListTitleDialog.Callback {
                    override fun onSuccess(text: String, description: String) {
                        viewLifecycleOwner.lifecycleScope.launch(
                            CoroutineExceptionHandler { _, throwable -> L.w(throwable) }
                        ) {
                            AppDatabase.instance.readingListDao().createList(text, description)
                        }
                    }
                }
            ).show()
        }

        override fun importNewList() {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/json" }
            val filePickerIntent = Intent.createChooser(intent, getString(R.string.reading_lists_import_file_picker_title))
            filePickerLauncher.launch(filePickerIntent)
        }

        override fun selectListClick() {
            beginMultiSelect()
        }

        override fun refreshClick() {
            onRefresh()
        }
    }

    override fun onSortOptionClick(position: Int) {
        viewModel.setSortMode(position)
    }

    private fun importReadingLists(uri: android.net.Uri) {
        val contentResolver = requireContext().contentResolver
        viewLifecycleOwner.lifecycleScope.launch {
            val input = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } ?: return@launch
            ReadingListsExportImportHelper.importLists(requireActivity() as AppCompatActivity, input)
        }
    }

    private fun onListClick(listId: Long) {
        if (isTagType(actionMode)) {
            viewModel.toggleListSelection(listId)
            return
        }
        viewModel.clearRecentPreviewSavedList()
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.instance.readingListDao().getListById(listId, true)?.let { list ->
                RecommendedReadingListEvent.submit("open_list_click", "rrl_saved")
                startActivity(ReadingListActivity.newIntent(requireContext(), list))
            }
        }
    }

    // ListRow onLongClick actions
    private fun onListMenuAction(listId: Long, action: ReadingListMenuAction) {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = AppDatabase.instance.readingListDao().getListWithPagesById(listId)?.toReadingList() ?: return@launch
            // List refresh happens reactively via the DB flow, so these callbacks don't call updateLists.
            when (action) {
                ReadingListMenuAction.Rename -> {
                    if (list.isDefault) return@launch
                    ReadingListBehaviorsUtil.renameReadingList(requireActivity(), list) {
                        ReadingListSyncAdapter.manualSync()
                    }
                }
                ReadingListMenuAction.Delete -> {
                    ReadingListBehaviorsUtil.deleteReadingList(requireActivity(), list, true) {
                        ReadingListBehaviorsUtil.showDeleteListUndoSnackbar(requireActivity(), list) {}
                    }
                }
                ReadingListMenuAction.SaveAllOffline -> {
                    ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), list.pages) {}
                }
                ReadingListMenuAction.RemoveAllOffline -> {
                    ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), list.pages) {}
                }
                ReadingListMenuAction.Export -> {
                    ReadingListsExportImportHelper.exportLists(requireActivity() as BaseActivity, listOf(list))
                }
                ReadingListMenuAction.Select -> {
                    beginMultiSelect()
                    viewModel.toggleListSelection(listId)
                }
                ReadingListMenuAction.Share -> {
                    ReadingListsShareHelper.shareReadingList(requireActivity() as AppCompatActivity, list)
                }
            }
        }
    }

    private fun onPageClick(pageId: Long) {
        if (isTagType(actionMode)) {
            viewModel.togglePageSelection(pageId)
            return
        }
        launchWithPage(pageId) { page ->
            val entry = HistoryEntry(ReadingListPage.toPageTitle(page), HistoryEntry.SOURCE_READING_LIST)
            ReadingListBehaviorsUtil.updateReadingListPage(page)
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
        }
    }

    private fun onPageLongClick(pageId: Long) {
        val containingLists = viewModel.containingLists(pageId)
        if (containingLists.isEmpty()) {
            return
        }
        ExclusiveBottomSheetPresenter.show(
            childFragmentManager,
            ReadingListItemActionsDialog.newInstance(
                containingLists.first().title,
                containingLists.size,
                pageId,
                actionMode != null,
                showMoveAction = false
            )
        )
    }

    private fun onToggleOfflineClick(pageId: Long) {
        launchWithPage(pageId) { page ->
            if (Prefs.isDownloadOnlyOverWiFiEnabled && !DeviceUtil.isOnWiFi &&
                page.status == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                page.offline = false
            }
            if (page.saving) {
                Toast.makeText(requireContext(), R.string.reading_list_article_save_in_progress, Toast.LENGTH_LONG).show()
            } else {
                ReadingListBehaviorsUtil.toggleOffline(requireActivity(), page) {
                    viewModel.updatePageDownloadProgress(page)
                }
            }
        }
    }

    private fun onDiscoverCardClick() {
        startActivity(ReadingListActivity.newIntent(requireActivity(), ReadingListMode.RECOMMENDED))
    }

    private fun onPageChipClick(listId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = AppDatabase.instance.readingListDao().getListWithPagesById(listId)?.toReadingList() ?: return@launch
            startActivity(ReadingListActivity.newIntent(requireContext(), list))
        }
    }

    // PageRow OnLongClick ReadingListItemActionsDialog Callbacks
    override fun onToggleItemOffline(pageId: Long) {
        launchWithPage(pageId) { page ->
            ReadingListBehaviorsUtil.togglePageOffline(requireActivity(), page) {
                viewModel.updatePageDownloadProgress(page)
            }
        }
    }

    override fun onShareItem(pageId: Long) {
        launchWithPage(pageId) { page ->
            ShareUtil.shareText(requireActivity(), ReadingListPage.toPageTitle(page))
        }
    }

    override fun onAddItemToOther(pageId: Long) {
        launchWithPage(pageId) { page ->
            ExclusiveBottomSheetPresenter.show(
                childFragmentManager,
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY)
            )
        }
    }

    override fun onMoveItemToOther(pageId: Long) {
        launchWithPage(pageId) { page ->
            ExclusiveBottomSheetPresenter.show(
                childFragmentManager,
                MoveToReadingListDialog.newInstance(page.listId, ReadingListPage.toPageTitle(page), InvokeSource.READING_LIST_ACTIVITY)
            )
        }
    }

    override fun onSelectItem(pageId: Long) {
        beginMultiSelect()
        viewModel.togglePageSelection(pageId)
    }

    override fun onDeleteItem(pageId: Long) {
        launchWithPage(pageId) { page ->
            val lists = AppDatabase.instance.readingListDao()
                .getListsByIds(viewModel.containingLists(pageId).mapTo(mutableSetOf()) { it.id })
            if (lists.isNotEmpty()) {
                 ReadingListBehaviorsUtil.deletePages(requireActivity(), lists, page, {}) {}
            }
        }
    }

    private fun launchWithPage(pageId: Long, action: suspend (ReadingListPage) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val page = AppDatabase.instance.readingListPageDao().getPageById(pageId) ?: return@launch
            action(page)
        }
    }

    private fun beginMultiSelect() {
        if (!isTagType(actionMode)) {
            viewModel.setSelectionMode(true)
            val callback = when (viewModel.uiState.value.selectedTab) {
                SavedTab.ALL_ARTICLES -> articleSelectionCallback
                SavedTab.COLLECTIONS -> collectionSelectionCallback
            }
            actionMode = (requireActivity() as AppCompatActivity)
                .startSupportActionMode(callback)
        }
    }

    private val collectionSelectionCallback = object : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.menuInflater.inflate(R.menu.menu_action_mode_reading_lists, menu)
            actionMode = mode
            val deleteItem = menu.findItem(R.id.menu_delete_selected)
            deleteItem.isEnabled = false
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selectedIds = viewModel.selectionState.value.selectedListIds
            val listRows = viewModel.uiState.value.rows.filterIsInstance<ReadingListRow.ListRow>()
            val selectedLists = listRows.filter { it.list.id in selectedIds }
            val allListIds = listRows.map { it.list.id }
            val allSelected = allListIds.isNotEmpty() && selectedIds.containsAll(allListIds)
            val onlyDefaultSelected = selectedLists.size == 1 && selectedLists.single().list.isDefault

            mode.title = if (selectedLists.isEmpty()) "" else {
                getString(R.string.multi_select_items_selected, selectedLists.size)
            }
            val fullOpacity = 255
            val halfOpacity = 80
            menu.findItem(R.id.menu_delete_selected).apply {
                isEnabled = selectedLists.isNotEmpty() && !onlyDefaultSelected
                icon?.alpha = if (selectedLists.isEmpty() || onlyDefaultSelected) halfOpacity else fullOpacity
            }
            menu.findItem(R.id.menu_export_selected).apply {
                isEnabled = selectedLists.isNotEmpty()
                val exportColor = ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color)
                title = buildSpannedString {
                    color(ColorUtils.setAlphaComponent(exportColor, if (selectedLists.isEmpty()) halfOpacity else fullOpacity)) {
                        append(getString(R.string.reading_lists_action_menu_export_lists))
                    }
                }
            }
            menu.findItem(R.id.menu_select).apply {
                setIcon(when {
                    selectedLists.isEmpty() -> R.drawable.ic_outline_library_add_check_24
                    allSelected -> R.drawable.ic_deselect_all
                    else -> R.drawable.ic_select_indeterminate
                })
                title = when {
                    selectedLists.isEmpty() -> getString(R.string.notifications_menu_check_all)
                    allSelected -> getString(R.string.notifications_menu_uncheck_all)
                    else -> ""
                }
            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_delete_selected -> {
                    onDeleteSelected()
                    true
                }
                R.id.menu_export_selected -> {
                    exportSelectedLists()
                    true
                }
                R.id.menu_select -> {
                    val allListIds = viewModel.uiState.value.rows
                        .filterIsInstance<ReadingListRow.ListRow>()
                        .map { it.list.id }
                    val selectedIds = viewModel.selectionState.value.selectedListIds
                    if (allListIds.isNotEmpty() && selectedIds.containsAll(allListIds)) {
                        viewModel.clearListSelection()
                    } else {
                        viewModel.selectAllLists()
                    }
                    true
                }
                else -> super.onActionItemClicked(mode, menuItem)
            }
        }

        override fun onDeleteSelected() {
            deleteSelectedLists()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            viewModel.setSelectionMode(false)
            actionMode = null
            super.onDestroyActionMode(mode)
        }
    }

    private val articleSelectionCallback = object : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.menuInflater.inflate(R.menu.menu_action_mode_reading_list_articles, menu)
            actionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selectedIds = viewModel.selectionState.value.selectedPageIds
            val allPageIds = viewModel.uiState.value.rows
                .filterIsInstance<ReadingListRow.PageRow>()
                .map { it.page.id }
            val allSelected = allPageIds.isNotEmpty() && selectedIds.containsAll(allPageIds)

            mode.title = if (selectedIds.isEmpty()) "" else {
                getString(R.string.multi_select_items_selected, selectedIds.size)
            }
            menu.findItem(R.id.menu_delete_selected).apply {
                isEnabled = selectedIds.isNotEmpty()
                icon?.alpha = if (selectedIds.isEmpty()) 80 else 255
            }
            menu.findItem(R.id.menu_select).apply {
                setIcon(when {
                    selectedIds.isEmpty() -> R.drawable.ic_outline_library_add_check_24
                    allSelected -> R.drawable.ic_deselect_all
                    else -> R.drawable.ic_select_indeterminate
                })
                title = when {
                    selectedIds.isEmpty() -> getString(R.string.notifications_menu_check_all)
                    allSelected -> getString(R.string.notifications_menu_uncheck_all)
                    else -> ""
                }
            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_delete_selected -> {
                    onDeleteSelected()
                    true
                }
                R.id.menu_remove_from_offline -> {
                    updateSelectedPagesOffline(saveForOffline = false)
                    true
                }
                R.id.menu_save_for_offline -> {
                    updateSelectedPagesOffline(saveForOffline = true)
                    true
                }
                R.id.menu_add_to_another_list -> {
                    addSelectedPagesToList()
                    true
                }
                R.id.menu_select -> {
                    val allPageIds = viewModel.uiState.value.rows
                        .filterIsInstance<ReadingListRow.PageRow>()
                        .map { it.page.id }
                    val selectedIds = viewModel.selectionState.value.selectedPageIds
                    if (allPageIds.isNotEmpty() && selectedIds.containsAll(allPageIds)) {
                        viewModel.clearPageSelection()
                    } else {
                        viewModel.selectAllPages()
                    }
                    true
                }
                else -> super.onActionItemClicked(mode, menuItem)
            }
        }

        override fun onDeleteSelected() {
            deleteSelectedPages()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            viewModel.setSelectionMode(false)
            actionMode = null
            super.onDestroyActionMode(mode)
        }
    }

    private fun updateSelectedPagesOffline(saveForOffline: Boolean) {
        val selectedPageIds = viewModel.selectionState.value.selectedPageIds
        viewLifecycleOwner.lifecycleScope.launch {
            val pages = AppDatabase.instance.readingListPageDao().getPagesByIds(selectedPageIds)
            if (pages.isEmpty()) {
                return@launch
            }
            if (saveForOffline) {
                ReadingListBehaviorsUtil.savePagesForOffline(requireActivity(), pages) {
                    actionMode?.finish()
                }
            } else {
                ReadingListBehaviorsUtil.removePagesFromOffline(requireActivity(), pages) {
                    actionMode?.finish()
                }
            }
        }
    }

    private fun addSelectedPagesToList() {
        val selectedPageIds = viewModel.selectionState.value.selectedPageIds
        viewLifecycleOwner.lifecycleScope.launch {
            val titles = AppDatabase.instance.readingListPageDao()
                .getPagesByIds(selectedPageIds)
                .distinctBy { it.lang to it.apiTitle }
                .map { ReadingListPage.toPageTitle(it) }
            if (titles.isEmpty()) {
                return@launch
            }
            val dialog = AddToReadingListDialog.newInstance(titles, InvokeSource.READING_LIST_ACTIVITY).apply {
                dismissListener = DialogInterface.OnDismissListener { actionMode?.finish() }
            }
            ExclusiveBottomSheetPresenter.show(childFragmentManager, dialog)
        }
    }

    /**
     * produces a list of [ReadingList] containing only the selected pages to delete from each list.
     * This is needed because the selection state only contains page IDs.
     */
    private fun deleteSelectedPages() {
        val selectedPageIds = viewModel.selectionState.value.selectedPageIds
        val selectedRows = viewModel.uiState.value.rows
            .asSequence()
            .filterIsInstance<ReadingListRow.PageRow>()
            .filter { it.page.id in selectedPageIds }
            .toList()
        val selectedPageKeys = selectedRows.mapTo(mutableSetOf()) { it.page.lang to it.page.apiTitle }
        val containingListIds = selectedRows.flatMapTo(mutableSetOf()) { row ->
            row.containingLists.map { it.id }
        }
        if (selectedRows.isEmpty() || containingListIds.isEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val readingLists = mutableListOf<ReadingList>()
            val relations = AppDatabase.instance.readingListDao().getListsWithPagesByIds(containingListIds)
            relations.forEach { relation ->
                val pagesToDelete = relation.pages.filter { page ->
                    page.status != ReadingListPage.STATUS_QUEUE_FOR_DELETE && (page.lang to page.apiTitle) in selectedPageKeys
                }
                if (pagesToDelete.isNotEmpty()) {
                    relation.list.pages.clear()
                    relation.list.pages.addAll(pagesToDelete)
                    readingLists.add(relation.list)
                }
            }

            ReadingListBehaviorsUtil.deletePagesFromLists(requireActivity(), selectedRows.size, readingLists, {}) {
                actionMode?.finish()
            }
        }
    }

    private fun deleteSelectedLists() {
        val selectedIds = viewModel.selectionState.value.selectedListIds
        viewLifecycleOwner.lifecycleScope.launch {
            val lists = AppDatabase.instance.readingListDao().getListsWithPagesByIds(selectedIds)
                .map { it.toReadingList() }
            if (lists.isEmpty()) {
                return@launch
            }
            ReadingListBehaviorsUtil.deleteReadingLists(requireActivity(), lists) {
                ReadingListBehaviorsUtil.showDeleteListsUndoSnackbar(requireActivity(), lists) {}
                actionMode?.finish()
            }
        }
    }

    private fun exportSelectedLists() {
        val selectedIds = viewModel.selectionState.value.selectedListIds
        viewLifecycleOwner.lifecycleScope.launch {
            val lists = AppDatabase.instance.readingListDao().getListsWithPagesByIds(selectedIds)
                .map { it.toReadingList() }
            if (lists.isNotEmpty()) {
                ReadingListsExportImportHelper.exportLists(requireActivity() as BaseActivity, lists)
                actionMode?.finish()
            }
        }
    }

    companion object {
        fun newInstance(): ReadingListsFragment {
            return ReadingListsFragment()
        }
    }
}
