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
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.FragmentTalkTemplatesBinding
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.views.DrawableItemDecoration

class TalkTemplatesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentTalkTemplatesBinding? = null

    private val viewModel: TalkTemplatesViewModel by viewModels()
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentTalkTemplatesBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.talk_templates_manage_title)

        return binding.root
    }

    private val requestNewTemplate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadTalkTemplates()
            PatrollerExperienceEvent.logAction("save_message_toast", "pt_templates")
            FeedbackUtil.showMessage(this, R.string.talk_templates_new_message_saved)
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
                            is TalkTemplatesViewModel.ActionState.Saved -> onSaved(it.position)
                            is TalkTemplatesViewModel.ActionState.Deleted -> onDeleted(it.position)
                            is TalkTemplatesViewModel.ActionState.Error -> onActionError(it.throwable)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_talk_templates, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_new_message -> {
                PatrollerExperienceEvent.logAction("new_message_click", "pt_templates")
                requestNewTemplate.launch(AddTemplateActivity.newIntent(requireContext()))
                true
            }
            else -> false
        }
    }

    private fun setRecyclerView() {
        binding.talkTemplatesRecyclerView.setHasFixedSize(true)
        val adapter = RecyclerAdapter()
        binding.talkTemplatesRecyclerView.adapter = adapter
        binding.talkTemplatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.talkTemplatesRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, drawStart = true, drawEnd = false))
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.talkTemplatesRecyclerView)
    }

    private fun onLoading() {
        binding.talkTemplatesEmptyContainer.visibility = View.GONE
        binding.talkTemplatesRecyclerView.visibility = View.GONE
        binding.talkTemplatesErrorView.visibility = View.GONE
    }

    private fun onSuccess() {
        setRecyclerView()
        binding.talkTemplatesEmptyContainer.isVisible = viewModel.talkTemplatesList.isEmpty()
        binding.talkTemplatesErrorView.visibility = View.GONE
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesRecyclerView.isVisible = viewModel.talkTemplatesList.isNotEmpty()
        if (binding.talkTemplatesEmptyContainer.isVisible) {
            PatrollerExperienceEvent.logAction("templates_empty_impression", "pt_templates")
        }
    }

    private fun onSaved(position: Int) {
        FeedbackUtil.showMessage(this, R.string.talk_templates_edit_message_updated)
        binding.talkTemplatesRecyclerView.adapter?.notifyItemChanged(position)
    }

    private fun onDeleted(position: Int) {
        PatrollerExperienceEvent.logAction("message_deleted_toast", "pt_templates")
        FeedbackUtil.showMessage(this, R.string.talk_templates_message_deleted)
        binding.talkTemplatesRecyclerView.adapter?.notifyItemRemoved(position)
        binding.talkTemplatesEmptyContainer.isVisible = viewModel.talkTemplatesList.isEmpty()
        binding.talkTemplatesRecyclerView.isVisible = viewModel.talkTemplatesList.isNotEmpty()
        if (binding.talkTemplatesEmptyContainer.isVisible) {
            PatrollerExperienceEvent.logAction("templates_empty_impression", "pt_templates")
        }
    }

    private fun onActionError(t: Throwable) {
        FeedbackUtil.showMessage(this, t.toString())
    }

    private fun onError(t: Throwable) {
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesErrorView.setError(t)
        binding.talkTemplatesErrorView.visibility = View.VISIBLE
    }

    private fun showEditDialog(talkTemplate: TalkTemplate) {
        TalkTemplatesTextInputDialog(requireActivity(), R.string.talk_templates_new_message_dialog_save,
            R.string.talk_templates_edit_message_dialog_delete).let { textInputDialog ->
            textInputDialog.callback = object : TalkTemplatesTextInputDialog.Callback {
                override fun onShow(dialog: TalkTemplatesTextInputDialog) {
                    dialog.setTitleHint(R.string.talk_templates_new_message_dialog_hint)
                    dialog.setTitleText(talkTemplate.title)
                    dialog.setSubjectHint(R.string.talk_templates_new_message_subject_hint)
                    dialog.setSubjectText(talkTemplate.subject)
                    dialog.setBodyHint(R.string.talk_templates_new_message_compose_hint)
                    dialog.setBodyText(talkTemplate.message)
                }

                override fun onTextChanged(text: CharSequence, dialog: TalkTemplatesTextInputDialog) {
                    text.toString().trim().let {
                        when {
                            it.isEmpty() -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(false)
                            }

                            viewModel.talkTemplatesList.any { item -> item.title == it && item.id != talkTemplate.id } -> {
                                dialog.setError(
                                    dialog.context.getString(
                                        R.string.talk_templates_new_message_dialog_exists,
                                        it
                                    )
                                )
                                dialog.setPositiveButtonEnabled(false)
                            }

                            else -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(true)
                            }
                        }
                    }
                }

                override fun onSuccess(titleText: CharSequence, subjectText: CharSequence, bodyText: CharSequence) {
                    PatrollerExperienceEvent.logAction("edit_message_save", "pt_templates")
                    viewModel.updateTalkTemplate(titleText.toString(), subjectText.toString(), bodyText.toString(), talkTemplate)
                }

                override fun onCancel() {
                    PatrollerExperienceEvent.logAction("edit_message_delete", "pt_templates")
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme_Delete)
                        .setMessage(getString(R.string.talk_templates_edit_message_delete_description, talkTemplate.title))
                        .setPositiveButton(R.string.talk_templates_edit_message_dialog_delete) { _, _ ->
                            PatrollerExperienceEvent.logAction("message_delete_click", "pt_templates")
                            viewModel.deleteTemplate(talkTemplate) }
                        .setNegativeButton(R.string.talk_templates_new_message_dialog_cancel) { _, _ ->
                            PatrollerExperienceEvent.logAction("message_delete_cancel", "pt_templates")
                        }
                        .show()
                }

                override fun onDismiss() { }
            }
            textInputDialog.showDialogMessage(false)
            textInputDialog.showSubjectText(true)
            textInputDialog.showBodyText(true)
            textInputDialog.setTitle(R.string.talk_templates_edit_message_dialog_title)
        }.show()
    }

    internal inner class TalkTemplatesItemViewHolder(val templatesItemView: TalkTemplatesItemView) : RecyclerView.ViewHolder(templatesItemView.rootView) {
        fun bindItem(item: TalkTemplate) {
            templatesItemView.setContents(item)
        }
    }

    internal inner class RecyclerAdapter : RecyclerView.Adapter<TalkTemplatesItemViewHolder>(), TalkTemplatesItemView.Callback {

        override fun getItemCount(): Int {
            return viewModel.talkTemplatesList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalkTemplatesItemViewHolder {
            val view = TalkTemplatesItemView(requireContext())
            view.callback = this
            return TalkTemplatesItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: TalkTemplatesItemViewHolder, position: Int) {
            holder.bindItem(viewModel.talkTemplatesList[position])
        }

        override fun onViewAttachedToWindow(holder: TalkTemplatesItemViewHolder) {
            super.onViewAttachedToWindow(holder)
            holder.templatesItemView.setDragHandleTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> itemTouchHelper.startDrag(holder)
                    MotionEvent.ACTION_UP -> v.performClick()
                }
                false
            }
        }

        override fun onViewDetachedFromWindow(holder: TalkTemplatesItemViewHolder) {
            holder.templatesItemView.setDragHandleTouchListener(null)
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            viewModel.swapList(oldPosition, newPosition)
            viewModel.updateItemOrder()
            notifyItemMoved(oldPosition, newPosition)
        }

        override fun onClick(talkTemplate: TalkTemplate) {
            showEditDialog(talkTemplate)
        }
    }

    private inner class RearrangeableItemTouchHelperCallback constructor(private val adapter: RecyclerAdapter) : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean {
            return true
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
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        fun newInstance(): TalkTemplatesFragment {
            return TalkTemplatesFragment()
        }
    }
}
