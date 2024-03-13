package org.wikipedia.talk.template

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.FragmentTalkTemplatesBinding
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.TalkReplyActivity
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.template.TalkTemplatesActivity.Companion.EXTRA_TEMPLATE_MANAGEMENT
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.SwipeableItemTouchHelperCallback

class TalkTemplatesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentTalkTemplatesBinding? = null

    private val viewModel: TalkTemplatesViewModel by viewModels { TalkTemplatesViewModel.Factory(requireArguments()) }
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var adapter: RecyclerAdapter
    private val selectedItems = mutableListOf<TalkTemplate>()
    private var actionMode: ActionMode? = null
    private val multiSelectCallback = MultiSelectCallback()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentTalkTemplatesBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(if (viewModel.templateManagementMode) R.string.talk_warn_saved_messages else R.string.talk_warn)

        return binding.root
    }

    private val requestNewTemplate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        binding.talkTemplatesTabLayout.getTabAt(0)?.select()

        if (result.resultCode == RESULT_OK) {
            viewModel.loadTalkTemplates()
            PatrollerExperienceEvent.logAction("save_message_toast", "pt_templates")
            FeedbackUtil.showMessage(this, R.string.talk_templates_new_message_saved)
        }
        if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS || result.resultCode == TalkReplyActivity.RESULT_SAVE_TEMPLATE) {
            val pageTitle = requireArguments().parcelable<PageTitle>(Constants.ARG_TITLE)!!
            val message = if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS) {
                PatrollerExperienceEvent.logAction("publish_message_toast", "pt_warning_messages")
                R.string.talk_warn_submitted
            } else {
                PatrollerExperienceEvent.logAction("publish_message_saved_toast", "pt_warning_messages")
                R.string.talk_warn_submitted_and_saved
            }
            updateAndNotifyAdapter()
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(message))
            snackbar.setAction(R.string.patroller_tasks_patrol_edit_snackbar_view) {
                PatrollerExperienceEvent.logAction("publish_message_view_click", "pt_warning_messages")
                startActivity(TalkTopicsActivity.newIntent(requireContext(), pageTitle, Constants.InvokeSource.DIFF_ACTIVITY))
            }
            snackbar.show()
        }
    }

    private val requestEditTemplate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            binding.talkTemplatesTabLayout.getTabAt(0)?.select()
            viewModel.loadTalkTemplates()
            PatrollerExperienceEvent.logAction("update_message_toast", "pt_templates")
            FeedbackUtil.showMessage(this, R.string.talk_templates_edit_message_updated)
        }
        if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS || result.resultCode == TalkReplyActivity.RESULT_SAVE_TEMPLATE) {
            val pageTitle = requireArguments().parcelable<PageTitle>(Constants.ARG_TITLE)!!
            val message = if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS) {
                PatrollerExperienceEvent.logAction("publish_message_toast", "pt_warning_messages")
                R.string.talk_warn_submitted
            } else {
                PatrollerExperienceEvent.logAction("publish_message_saved_toast", "pt_warning_messages")
                R.string.talk_warn_submitted_and_saved
            }
            updateAndNotifyAdapter()
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(message))
            snackbar.setAction(R.string.patroller_tasks_patrol_edit_snackbar_view) {
                PatrollerExperienceEvent.logAction("publish_message_view_click", "pt_warning_messages")
                startActivity(TalkTopicsActivity.newIntent(requireContext(), pageTitle, Constants.InvokeSource.DIFF_ACTIVITY))
            }
            snackbar.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.talkTemplatesErrorView.retryClickListener = View.OnClickListener { viewModel.loadTalkTemplates() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is TalkTemplatesViewModel.UiState.Loading -> onLoading()
                            is TalkTemplatesViewModel.UiState.Success -> onSuccess()
                            is TalkTemplatesViewModel.UiState.Error -> onError(it.throwable)
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect {
                        when (it) {
                            is TalkTemplatesViewModel.ActionState.Deleted -> onDeleted(it.size)
                            is TalkTemplatesViewModel.ActionState.Error -> onActionError(it.throwable)
                        }
                    }
                }
            }
        }

        binding.addTemplateFab.setOnClickListener {
            val pageTitle = requireArguments().parcelable<PageTitle>(Constants.ARG_TITLE)!!
            val fromRevisionId = requireArguments().getLong(TalkReplyActivity.FROM_REVISION_ID)
            val toRevisionId = requireArguments().getLong(TalkReplyActivity.TO_REVISION_ID)
            requestNewTemplate.launch(TalkReplyActivity.newIntent(requireContext(), pageTitle, null,
                null, invokeSource = Constants.InvokeSource.DIFF_ACTIVITY, fromDiff = true, templateManagementMode = viewModel.templateManagementMode,
                fromRevisionId = fromRevisionId, toRevisionId = toRevisionId))
        }

        binding.talkTemplatesTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (actionMode != null) {
                    actionMode?.finish()
                }
                updateAndNotifyAdapter()
                updateEmptyState()
                requireActivity().invalidateOptionsMenu()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
        binding.talkTemplatesEmptyStateTextView.text = StringUtil.fromHtml(getString(R.string.talk_templates_empty_message))

        binding.talkTemplatesEmptyStateTextView.movementMethod = LinkMovementMethodExt { _ ->
            binding.talkTemplatesTabLayout.getTabAt(1)?.select()
            updateAndNotifyAdapter()
        }
    }

    fun updateEmptyState() {
        binding.talkTemplatesEmptyContainer.isVisible = adapter.templatesList.isEmpty()
        binding.talkTemplatesRecyclerView.isVisible = adapter.templatesList.isNotEmpty()
    }
    fun updateAndNotifyAdapter() {
        adapter.templatesList.clear()
        adapter.templatesList.addAll(if (binding.talkTemplatesTabLayout.selectedTabPosition == 0) viewModel.talkTemplatesList else viewModel.savedTemplatesList)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_edit_messages).isVisible = binding.talkTemplatesTabLayout.selectedTabPosition == 0
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_talk_templates, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_edit_messages -> {
                if (actionMode == null) {
                    beginRemoveItemsMode()
                    updateAndNotifyAdapter()
                }
                true
            }
            else -> false
        }
    }

    private fun setRecyclerView() {
        binding.talkTemplatesRecyclerView.setHasFixedSize(true)
        adapter = RecyclerAdapter()
        binding.talkTemplatesRecyclerView.adapter = adapter
        binding.talkTemplatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.talkTemplatesRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, drawStart = true, drawEnd = false))
        val touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        touchCallback.swipeableEnabled = true
        itemTouchHelper = ItemTouchHelper(if(actionMode!=null)RearrangeableItemTouchHelperCallback(adapter) else touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.talkTemplatesRecyclerView)
        updateAndNotifyAdapter()
    }

    fun updateTouchHelper() {
        val touchCallback = SwipeableItemTouchHelperCallback(requireContext())
        touchCallback.swipeableEnabled = true
        itemTouchHelper = ItemTouchHelper(if(actionMode!=null)RearrangeableItemTouchHelperCallback(adapter) else touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.talkTemplatesRecyclerView)
    }
    private fun onLoading() {
        binding.talkTemplatesEmptyContainer.visibility = View.GONE
        binding.talkTemplatesRecyclerView.visibility = View.GONE
        binding.talkTemplatesErrorView.visibility = View.GONE
    }

    private fun onSuccess() {
        setRecyclerView()
        requireActivity().invalidateOptionsMenu()
        binding.talkTemplatesEmptyContainer.isVisible = viewModel.talkTemplatesList.isEmpty()
        binding.talkTemplatesErrorView.visibility = View.GONE
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesRecyclerView.isVisible = viewModel.talkTemplatesList.isNotEmpty()
        if (binding.talkTemplatesEmptyContainer.isVisible) {
            PatrollerExperienceEvent.logAction("templates_empty_impression", "pt_templates")
        }
    }

    private fun onDeleted(size: Int) {
        PatrollerExperienceEvent.logAction("message_deleted_toast", "pt_templates")
        val messageStr = resources.getQuantityString(R.plurals.talk_templates_message_deleted, size)
        FeedbackUtil.showMessage(this, messageStr)
        binding.talkTemplatesEmptyContainer.isVisible = viewModel.talkTemplatesList.isEmpty()
        binding.talkTemplatesRecyclerView.isVisible = viewModel.talkTemplatesList.isNotEmpty()
        if (binding.talkTemplatesEmptyContainer.isVisible) {
            PatrollerExperienceEvent.logAction("templates_empty_impression", "pt_templates")
        }
        actionMode?.finish()
        unselectAllTalkTemplates()
    }

    private fun onActionError(t: Throwable) {
        FeedbackUtil.showMessage(this, t.toString())
    }

    private fun onError(t: Throwable) {
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesErrorView.setError(t)
        binding.talkTemplatesErrorView.visibility = View.VISIBLE
    }

    internal inner class TalkTemplatesItemViewHolder(val templatesItemView: TalkTemplatesItemView) : RecyclerView.ViewHolder(templatesItemView.rootView) , SwipeableItemTouchHelperCallback.Callback{
        private lateinit var entry: TalkTemplate

        fun bindItem(item: TalkTemplate, position: Int) {
            this.entry = item
            templatesItemView.setContents(item, position, binding.talkTemplatesTabLayout.selectedTabPosition == 1)
        }

        override fun onSwipe() {
            selectedItems.add(entry)
            deleteSelectedTalkTemplates()
        }

        override fun isSwipeable(): Boolean {
            return true
        }
    }

    internal inner class RecyclerAdapter : RecyclerView.Adapter<TalkTemplatesItemViewHolder>(), TalkTemplatesItemView.Callback {

        private var checkboxEnabled = false
        var templatesList = mutableListOf<TalkTemplate>()

        override fun getItemCount(): Int {
            return templatesList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalkTemplatesItemViewHolder {
            return TalkTemplatesItemViewHolder(TalkTemplatesItemView(requireContext()))
        }

        override fun onBindViewHolder(holder: TalkTemplatesItemViewHolder, position: Int) {
            val talkTemplate = templatesList[position]
            holder.bindItem(talkTemplate, position)
            holder.templatesItemView.setCheckBoxEnabled(checkboxEnabled)
            holder.templatesItemView.setCheckBoxChecked(selectedItems.contains(talkTemplate))
            holder.templatesItemView.setDragHandleEnabled(actionMode != null)
        }

        override fun onViewAttachedToWindow(holder: TalkTemplatesItemViewHolder) {
            super.onViewAttachedToWindow(holder)
            holder.templatesItemView.setDragHandleTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> itemTouchHelper.startDrag(holder)
                    MotionEvent.ACTION_UP -> v.performClick()
                    else -> { }
                }
                false
            }
            holder.templatesItemView.callback = this
        }

        override fun onViewDetachedFromWindow(holder: TalkTemplatesItemViewHolder) {
            holder.templatesItemView.setDragHandleTouchListener(null)
            holder.templatesItemView.callback = null
            super.onViewDetachedFromWindow(holder)
        }

        override fun onClick(position: Int) {
            if (position == 0 && binding.talkTemplatesTabLayout.selectedTabPosition == 1) {
                return
            }
            if (actionMode != null) {
                toggleSelectedItem(templatesList[position])
                adapter.notifyItemChanged(position)
            } else {
                PatrollerExperienceEvent.logAction("edit_message_click", "pt_templates")
                val pageTitle = requireArguments().parcelable<PageTitle>(Constants.ARG_TITLE)!!
                val fromRevisionId = requireArguments().getLong(TalkReplyActivity.FROM_REVISION_ID)
                val toRevisionId = requireArguments().getLong(TalkReplyActivity.TO_REVISION_ID)
                requestEditTemplate.launch(TalkReplyActivity.newIntent(requireContext(), pageTitle, null, null, invokeSource = Constants.InvokeSource.DIFF_ACTIVITY,
                    fromDiff = true, selectedTemplate = templatesList[position], templateManagementMode = viewModel.templateManagementMode, fromRevisionId = fromRevisionId,
                    toRevisionId = toRevisionId, isSavedTemplate = binding.talkTemplatesTabLayout.selectedTabPosition == 1))
            }
        }

        override fun onCheckedChanged(position: Int) {
            toggleSelectedItem(templatesList[position])
        }

        override fun onLongPress(position: Int) {
            if (binding.talkTemplatesTabLayout.selectedTabPosition == 1) {
                return
            }
            if (actionMode == null) {
                beginRemoveItemsMode()
            }
            toggleSelectedItem(templatesList[position])
            adapter.notifyItemChanged(position)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            viewModel.swapList(oldPosition, newPosition)
            viewModel.updateItemOrder()
            notifyItemMoved(oldPosition, newPosition)
        }

        fun onCheckboxEnabled(enabled: Boolean) {
            checkboxEnabled = enabled
        }
    }

    private fun setMultiSelectEnabled(enabled: Boolean) {
        adapter.onCheckboxEnabled(enabled)
        adapter.notifyItemRangeChanged(0, viewModel.talkTemplatesList.size)
        requireActivity().invalidateOptionsMenu()
    }

    private fun beginRemoveItemsMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(multiSelectCallback)
        setMultiSelectEnabled(true)
    }

    private fun toggleSelectedItem(talkTemplate: TalkTemplate) {
        if (selectedItems.contains(talkTemplate)) {
            selectedItems.remove(talkTemplate)
        } else {
            selectedItems.add(talkTemplate)
        }
        actionMode?.title = getString(R.string.multi_select_items_selected, selectedItems.size)
    }

    private fun unselectAllTalkTemplates() {
        selectedItems.clear()
        actionMode?.title = getString(R.string.multi_select_items_selected, selectedItems.size)
        updateAndNotifyAdapter()
    }

    private fun selectAllTalkTemplates(mode: ActionMode) {
        adapter.templatesList.filterNot { selectedItems.contains(it) }
            .forEach { selectedItems.add(it) }
        mode.title = getString(R.string.multi_select_items_selected, selectedItems.size)
        updateAndNotifyAdapter()
    }

    private fun deleteSelectedTalkTemplates() {
        viewModel.deleteTemplates(selectedItems)
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.setTitle(R.string.talk_templates_menu_remove_message)
            mode.menuInflater.inflate(R.menu.menu_action_mode_talk_templates, menu)
            actionMode = mode
            selectedItems.clear()
            updateTouchHelper()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = getString(R.string.multi_select_items_selected, selectedItems.size)
            selectedItems.size.toString()
            menu.findItem(R.id.menu_check_all).isVisible = true
            menu.findItem(R.id.menu_uncheck_all).isVisible = false
            return super.onPrepareActionMode(mode, menu)
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            super.onActionItemClicked(mode, menuItem)
            when (menuItem.itemId) {
                R.id.menu_check_all -> {
                    selectAllTalkTemplates(mode)
                    menuItem.isVisible = false
                    mode.menu.findItem(R.id.menu_uncheck_all).isVisible = true
                    return true
                }
                R.id.menu_uncheck_all -> {
                    unselectAllTalkTemplates()
                    menuItem.isVisible = false
                    mode.menu.findItem(R.id.menu_check_all).isVisible = true
                    return true
                }
            }
            return false
        }

        override fun onDeleteSelected() {
            if (selectedItems.size > 0) {
                PatrollerExperienceEvent.logAction("more_menu_remove_confirm", "pt_templates")
                val messageStr = resources.getQuantityString(
                    R.plurals.talk_templates_message_delete_description,
                    selectedItems.size
                )
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme_Delete)
                    .setMessage(messageStr)
                    .setPositiveButton(R.string.talk_templates_edit_message_dialog_delete) { _, _ ->
                        PatrollerExperienceEvent.logAction("message_delete_click", "pt_templates")
                        deleteSelectedTalkTemplates()
                    }
                    .setNegativeButton(R.string.talk_templates_new_message_dialog_cancel) { _, _ ->
                        PatrollerExperienceEvent.logAction("message_delete_cancel", "pt_templates")
                    }
                    .show()
            } else {
                actionMode?.finish()
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            unselectAllTalkTemplates()
            setMultiSelectEnabled(false)
            actionMode = null
            super.onDestroyActionMode(mode)
            updateTouchHelper()
        }
    }

    private inner class RearrangeableItemTouchHelperCallback constructor(private val adapter: RecyclerAdapter) : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.onMoveItem(source.absoluteAdapterPosition, target.absoluteAdapterPosition)
            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.post {
                if (isAdded) {
                    updateAndNotifyAdapter()
                }
            }
        }
    }

    companion object {
        val savedMessagesTitleList = listOf(-1, R.string.patroller_saved_message_title_vandalism, R.string.patroller_saved_message_title_editing_tests, R.string.patroller_saved_message_title_npov,
            R.string.patroller_saved_message_title_auto_trans, R.string.patroller_saved_message_title_coi_rem, R.string.patroller_saved_message_title_final_warning,
            R.string.patroller_saved_message_title_copy_vio, R.string.patroller_saved_message_title_edit_summary_reminder, R.string.patroller_saved_message_title_do_not_censor, R.string.patroller_saved_message_title_art_imp)

        val savedMessagesBodyList = listOf(R.string.talk_warn_saved_messages_usage_instruction, R.string.patroller_saved_message_body_vandalism, R.string.patroller_saved_message_body_editing_tests, R.string.patroller_saved_message_body_npov,
            R.string.patroller_saved_message_body_auto_trans, R.string.patroller_saved_message_body_coi_rem, R.string.patroller_saved_message_body_final_warning,
            R.string.patroller_saved_message_body_copy_vio, R.string.patroller_saved_message_body_edit_summary, R.string.patroller_saved_message_body_do_not_censor, R.string.patroller_saved_message_body_art_imp)

        fun newInstance(pageTitle: PageTitle?, templateManagement: Boolean = false, fromRevisionId: Long = -1, toRevisionId: Long = -1): TalkTemplatesFragment {
            return TalkTemplatesFragment().apply {
                arguments = bundleOf(Constants.ARG_TITLE to pageTitle,
                    EXTRA_TEMPLATE_MANAGEMENT to templateManagement,
                    TalkReplyActivity.FROM_REVISION_ID to fromRevisionId,
                    TalkReplyActivity.TO_REVISION_ID to toRevisionId)
            }
        }
    }
}
