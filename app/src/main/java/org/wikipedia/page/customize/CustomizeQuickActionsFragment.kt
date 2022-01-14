package org.wikipedia.page.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.CustomizeToolbarEvent
import org.wikipedia.databinding.FragmentCustomizeQuickActionsBinding
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.views.DefaultViewHolder

class CustomizeQuickActionsFragment : Fragment() {
    private var _binding: FragmentCustomizeQuickActionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomizeQuickActionsViewModel by viewModels()

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var adapter: RecyclerItemAdapter
    private lateinit var customizeToolbarEvent: CustomizeToolbarEvent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        customizeToolbarEvent = CustomizeToolbarEvent()
        customizeToolbarEvent.start()
        _binding = FragmentCustomizeQuickActionsBinding.inflate(LayoutInflater.from(context), container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        customizeToolbarEvent.resume()
    }

    override fun onPause() {
        super.onPause()
        customizeToolbarEvent.pause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        binding.resetToDefaultButton.setOnClickListener {
            viewModel.resetToDefault()
            setupRecyclerView()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        customizeToolbarEvent.logCustomization(Prefs.customizeFavoritesQuickActionsOrder.toMutableList(), Prefs.customizeFavoritesMenuOrder.toMutableList())
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.setHasFixedSize(true)
        adapter = RecyclerItemAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        itemTouchHelper = ItemTouchHelper(RearrangeableItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private inner class RecyclerItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {

        override fun getItemViewType(position: Int): Int {
            return viewModel.fullList[position].first
        }

        override fun getItemCount(): Int {
            return viewModel.fullList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_quick_actions_header, parent, false))
                }
                VIEW_TYPE_EMPTY_PLACEHOLDER -> {
                    EmptyPlaceholderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_quick_actions_empty_placeholder, parent, false))
                }
                else -> {
                    ItemHolder(CustomizeQuickActionsItemView(parent.context))
                }
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            when (holder) {
                is ItemHolder -> {
                    holder.bindItem(viewModel.fullList[pos].second as PageActionItem, pos)
                    holder.view.setDragHandleEnabled(true)
                }
                is HeaderViewHolder -> {
                    holder.bindItem(viewModel.fullList[pos].second as Int)
                }
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is ItemHolder) {
                holder.view.setDragHandleTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            itemTouchHelper.startDrag(holder)
                        }
                        MotionEvent.ACTION_UP -> v.performClick()
                        else -> { }
                    }
                    false
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ItemHolder) {
                holder.view.setDragHandleTouchListener(null)
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
            viewModel.swapList(oldPosition, newPosition)
            notifyItemMoved(oldPosition, newPosition)
        }

        fun onItemMoved(rearrangedItems: List<Int>) {
            val removePosition = viewModel.removeEmptyPlaceholder()
            if (removePosition >= 0) {
                notifyItemRemoved(removePosition)
            }
            val addPosition = viewModel.addEmptyPlaceholder()
            if (addPosition >= 0) {
                notifyItemRangeChanged(addPosition, viewModel.fullList.size - addPosition)
            }
            // Notify recycler view adapter that some items in the list have been manually swapped.
            rearrangedItems.forEach {
                notifyItemMoved(it, it + 1)
            }
        }
    }

    private inner class RearrangeableItemTouchHelperCallback constructor(private val adapter: RecyclerItemAdapter) : ItemTouchHelper.Callback() {
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
            if (movableItems(target)) {
                adapter.onMoveItem(source.absoluteAdapterPosition, target.absoluteAdapterPosition)
            }
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.post {
                if (isAdded) {
                    adapter.onItemMoved(viewModel.saveChanges())
                }
            }
        }

        private fun movableItems(target: RecyclerView.ViewHolder): Boolean {
            // TODO: Add (target is HeaderViewHolder) with matching title string to make them swappable between categories
            return target is ItemHolder
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem(@StringRes stringRes: Int) {
            itemView.findViewById<TextView>(R.id.headerTitle).setText(stringRes)
        }
    }

    private inner class ItemHolder constructor(itemView: CustomizeQuickActionsItemView) : DefaultViewHolder<CustomizeQuickActionsItemView>(itemView) {
        fun bindItem(pageActionItem: PageActionItem, position: Int) {
            view.setContents(pageActionItem, position)
        }
    }

    private inner class EmptyPlaceholderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView)

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
        const val VIEW_TYPE_EMPTY_PLACEHOLDER = 2
        const val QUICK_ACTIONS_LIMIT = 5

        fun newInstance(): CustomizeQuickActionsFragment {
            return CustomizeQuickActionsFragment()
        }
    }
}
