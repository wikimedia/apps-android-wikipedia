package org.wikipedia.readinglist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.ReadingListsOverflowView

class ReadingListsComposeFragment : Fragment(), SortReadingListsDialog.Callback {

    private val viewModel: ReadingListsViewModel by viewModels()
    private var actionMode: ActionMode? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let(::importReadingLists)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                    val selectionState by viewModel.selectionState.collectAsState()
                    ReadingListsComposeScreen(
                        uiState = uiState,
                        isRefreshing = isRefreshing,
                        pullToRefreshEnabled = !RemoteConfig.config.disableReadingListSync,
                        isSelectionMode = selectionState.enabled,
                        selectedListIds = selectionState.selectedListIds,
                        onOnboardingAction = ::onOnboardingAction,
                        onRefresh = ::onRefresh,
                        onListClick = ::onListClick,
                        onListLongClick = ::onListLongClick,
                        onListSelectionChange = ::toggleListSelection
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
                    // The list itself updates reactively via the DB flow; this just stops the spinner.
                    if (event is ReadingListSyncEvent) {
                        viewModel.setRefreshing(false)
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
    }

    private fun onRefresh() {
        viewModel.setRefreshing(true)
        if (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount) {
            ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(requireActivity())
            viewModel.setRefreshing(false)
        } else {
            Prefs.isReadingListSyncEnabled = true
            ReadingListSyncAdapter.manualSyncWithForce()
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

    // TODO migration: filter the list based on the search query
    private val searchActionModeCallback = object : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
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
            viewModel.setSearchActive(false)
            viewModel.setSearchQuery(null)
            if (isAdded) {
                (requireParentFragment() as MainFragment).setBottomNavVisible(true)
            }
        }

        override fun getSearchHintString(): String {
            return getString(R.string.filter_hint_filter_my_lists_and_articles)
        }

        override fun getParentContext(): Context {
            return requireContext()
        }
    }

    // Overflow menu
    fun showReadingListsOverflowMenu() {
        ReadingListsOverflowView(requireContext()).show(
            (requireActivity() as MainActivity).getToolbar()
                .findViewById(R.id.menu_overflow_button), overflowCallback
        )
    }

    private val overflowCallback = object : ReadingListsOverflowView.Callback {
        override fun sortByClick() {
            ExclusiveBottomSheetPresenter.show(
                childFragmentManager,
                SortReadingListsDialog.newInstance(
                    Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC)
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
        requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
            val input = inputStream.bufferedReader().use { it.readText() }
            ReadingListsExportImportHelper.importLists(requireActivity() as AppCompatActivity, input)
        }
    }

    private fun onListClick(listId: Long) {
        if (actionMode != null) {
            toggleListSelection(listId)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.instance.readingListDao().getListById(listId, true)?.let { list ->
                RecommendedReadingListEvent.submit("open_list_click", "rrl_saved")
                startActivity(ReadingListActivity.newIntent(requireContext(), list))
            }
        }
    }

    private fun onListLongClick(listId: Long) {
        // TODO migration: show full context menu like XML version
    }

    private fun toggleListSelection(listId: Long) {
        viewModel.toggleListSelection(listId)
    }

    private fun beginMultiSelect() {
        if (actionMode == null) {
            viewModel.setSelectionMode(true)
            actionMode = (requireActivity() as AppCompatActivity)
                .startSupportActionMode(multiSelectModeCallback)
        }
    }

    private val multiSelectModeCallback = object : MultiSelectActionModeCallback() {
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

    private fun deleteSelectedLists() {
        val selectedIds = viewModel.selectionState.value.selectedListIds
        viewLifecycleOwner.lifecycleScope.launch {
            val lists = AppDatabase.instance.readingListDao().getAllLists()
                .filter { it.id in selectedIds }
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
            val lists = AppDatabase.instance.readingListDao().getAllLists()
                .filter { it.id in selectedIds }
            if (lists.isNotEmpty()) {
                ReadingListsExportImportHelper.exportLists(requireActivity() as BaseActivity, lists)
                actionMode?.finish()
            }
        }
    }

    companion object {
        fun newInstance(): ReadingListsComposeFragment {
            return ReadingListsComposeFragment()
        }
    }
}
