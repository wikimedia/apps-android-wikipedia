package org.wikipedia.talk.replies

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentDefaultRepliesBinding
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.TextInputDialog
import java.util.*

class DefaultRepliesFragment : Fragment(), DefaultRepliesItemView.Callback {
    private var _binding: FragmentDefaultRepliesBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var adapter: ItemAdapter
    private lateinit var invokeSource: InvokeSource
    private var app: WikipediaApp = WikipediaApp.getInstance()
    private val defaultRepliesList = mutableListOf<String>()
    private val selectedReplies = mutableListOf<String>()
    private var actionMode: ActionMode? = null
    private val multiSelectCallback: MultiSelectCallback = MultiSelectCallback()
    private var interactionsCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDefaultRepliesBinding.inflate(inflater, container, false)
        invokeSource = requireActivity().intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        prepareList()
        setupRecyclerView()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_REPLY && resultCode == Activity.RESULT_OK) {
            interactionsCount += data!!.getIntExtra(ADD_REPLY_INTERACTIONS, 0)
            prepareList()
            requireActivity().invalidateOptionsMenu()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_default_replies, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove_default_reply -> {
                beginRemoveMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCheckedChanged(position: Int) {
        toggleSelectedReplies(defaultRepliesList[position])
    }

    override fun onLongPress(position: Int) {
        if (actionMode == null) {
            beginRemoveMode()
        }
        toggleSelectedReplies(defaultRepliesList[position])
        adapter.notifyDataSetChanged()
    }

    private fun prepareList() {
        // TODO: get from database.
    }

    private fun setupRecyclerView() {
        binding.recyclerView.setHasFixedSize(true)
        adapter = ItemAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun updateDefaultReplies() {
        // TODO: save to database
        adapter.notifyDataSetChanged()
        requireActivity().invalidateOptionsMenu()
    }

    private inner class ItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var checkboxEnabled = false
        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0 -> { VIEW_TYPE_HEADER }
                itemCount - 1 -> { VIEW_TYPE_FOOTER }
                else -> { VIEW_TYPE_ITEM }
            }
        }

        override fun getItemCount(): Int {
            return defaultRepliesList.size + NUM_HEADERS + NUM_FOOTERS
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                Companion.VIEW_TYPE_HEADER -> {
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_section_header, parent, false))
                }
                Companion.VIEW_TYPE_FOOTER -> {
                    FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_default_reply_footer, parent, false))
                }
                else -> {
                    ItemHolder(DefaultRepliesItemView(parent.context))
                }
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            if (holder is ItemHolder) {
                holder.bindItem(defaultRepliesList[pos - NUM_HEADERS], pos - NUM_FOOTERS)
                holder.view.setCheckBoxEnabled(checkboxEnabled)
                holder.view.setCheckBoxChecked(selectedReplies.contains(defaultRepliesList[pos - NUM_HEADERS]))
                holder.view.setDragHandleEnabled(defaultRepliesList.size > 1 && !checkboxEnabled)
                holder.view.setOnClickListener {
                    if (actionMode != null) {
                        toggleSelectedReplies(defaultRepliesList[pos - NUM_HEADERS])
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is ItemHolder) {
                holder.view.setDragHandleTouchListener { v: View, event: MotionEvent ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            interactionsCount++
                            itemTouchHelper.startDrag(holder)
                        }
                        MotionEvent.ACTION_UP -> v.performClick()
                    }
                    false
                }
                holder.view.callback = this@DefaultRepliesFragment
            } else if (holder is FooterViewHolder) {
                holder.view.visibility = if (checkboxEnabled) View.GONE else View.VISIBLE
                holder.view.setOnClickListener {
                    showTextInputDialog()
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ItemHolder) {
                holder.view.callback = null
                holder.view.setDragHandleTouchListener(null)
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            Collections.swap(defaultRepliesList, oldPosition - NUM_HEADERS, newPosition - NUM_FOOTERS)
            notifyItemMoved(oldPosition, newPosition)
        }

        fun onCheckboxEnabled(enabled: Boolean) {
            checkboxEnabled = enabled
        }
    }

    private fun showTextInputDialog() {
        TextInputDialog(requireContext()).let { textInputDialog ->
            textInputDialog.callback = object : TextInputDialog.Callback {
                override fun onShow(dialog: TextInputDialog) {
                    dialog.setHint(R.string.talk_default_replies_input_dialog_hint)
                }

                override fun onTextChanged(text: CharSequence, dialog: TextInputDialog) {
                    text.toString().trim().let {
                        when {
                            it.isEmpty() -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(false)
                            }
                            defaultRepliesList.contains(it) -> {
                                dialog.setError(
                                    dialog.context.getString(
                                        R.string.reading_list_title_exists,
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

                override fun onSuccess(text: CharSequence, secondaryText: CharSequence) {
                    val defaultReply = text.toString().trim()
                    // TODO: save to database
                }

                override fun onCancel() {}
            }
            textInputDialog.showSecondaryText(false)
        }.show()
    }

    private inner class RearrangeableItemTouchHelperCallback constructor(private val adapter: ItemAdapter) : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return if (viewHolder is ItemHolder) makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) else -1
        }

        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (target is ItemHolder) {
                adapter.onMoveItem(source.adapterPosition, target.getAdapterPosition())
            }
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.post {
                if (isAdded) {
                    updateDefaultReplies()
                }
            }
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        init {
            itemView.findViewById<TextView>(R.id.section_header_text).setText(R.string.talk_default_replies_title)
        }
    }

    private inner class ItemHolder constructor(itemView: DefaultRepliesItemView) : DefaultViewHolder<DefaultRepliesItemView>(itemView) {
        fun bindItem(languageCode: String, position: Int) {
            view.setContents(languageCode, position)
        }
    }

    private inner class FooterViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView)

    private fun setMultiSelectEnabled(enabled: Boolean) {
        adapter.onCheckboxEnabled(enabled)
        adapter.notifyDataSetChanged()
        requireActivity().invalidateOptionsMenu()
    }

    private fun beginRemoveMode() {
        (requireActivity() as AppCompatActivity).startSupportActionMode(multiSelectCallback)
        setMultiSelectEnabled(true)
    }

    private fun toggleSelectedReplies(code: String) {
        if (selectedReplies.contains(code)) {
            selectedReplies.remove(code)
        } else {
            selectedReplies.add(code)
        }
    }

    private fun unselectAllReplies() {
        selectedReplies.clear()
        adapter.notifyDataSetChanged()
    }

    private fun deleteSelectedReplies() {
        interactionsCount++
        prepareList()
        unselectAllReplies()
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.setTitle(R.string.talk_default_replies_multiple_remove)
            mode.menuInflater.inflate(R.menu.menu_action_mode_default_replies, menu)
            actionMode = mode
            selectedReplies.clear()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onDeleteSelected() {
            showRemoveRepliesDialog()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            unselectAllReplies()
            setMultiSelectEnabled(false)
            actionMode = null
            super.onDestroyActionMode(mode)
        }
    }

    fun showRemoveRepliesDialog() {
        if (selectedReplies.size > 0) {
            AlertDialog.Builder(requireActivity()).let {
                it
                .setTitle(resources.getQuantityString(R.plurals.talk_default_replies_remove_dialog_title, selectedReplies.size))
                .setPositiveButton(R.string.talk_default_replies_remove_dialog_ok_button) { _, _ ->
                    deleteSelectedReplies()
                    actionMode?.finish()
                }
                .setNegativeButton(R.string.talk_default_replies_remove_dialog_cancel_button, null)
                it.show()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
        private const val NUM_HEADERS = 1
        private const val NUM_FOOTERS = 1
        const val ADD_REPLY_INTERACTIONS = "add_reply_interactions"
        @JvmStatic
        fun newInstance(invokeSource: InvokeSource): DefaultRepliesFragment {
            val instance = DefaultRepliesFragment()
            instance.arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            return instance
        }
    }
}
