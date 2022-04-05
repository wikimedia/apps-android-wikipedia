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
import org.wikipedia.databinding.FragmentCustomizeToolbarBinding
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.views.DefaultViewHolder

class CustomizeToolbarFragment : Fragment() {
    private var _binding: FragmentCustomizeToolbarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomizeToolbarViewModel by viewModels()

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var adapter: RecyclerItemAdapter
    private lateinit var customizeToolbarEvent: CustomizeToolbarEvent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        customizeToolbarEvent = CustomizeToolbarEvent()
        _binding = FragmentCustomizeToolbarBinding.inflate(LayoutInflater.from(context), container, false)
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
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        customizeToolbarEvent.logCustomization(Prefs.customizeToolbarOrder.toMutableList(), Prefs.customizeToolbarMenuOrder.toMutableList())
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
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_toolbar_header, parent, false))
                }
                VIEW_TYPE_EMPTY_PLACEHOLDER -> {
                    EmptyPlaceholderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_toolbar_empty_placeholder, parent, false))
                }
                VIEW_TYPE_DESCRIPTION -> {
                    DescriptionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_toolbar_description, parent, false))
                }
                VIEW_TYPE_SET_TO_DEFAULT -> {
                    SetToDefaultViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_toolbar_set_to_default, parent, false))
                }
                else -> {
                    ItemHolder(CustomizeToolbarItemView(parent.context))
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
            return true
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return if (isMovable(viewHolder)) makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) else 0
        }

        override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (isMovable(target)) {
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

        private fun isMovable(target: RecyclerView.ViewHolder): Boolean {
            // TODO: Add (target is HeaderViewHolder) with matching title string to make them swappable between categories
            return target is ItemHolder
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem(@StringRes stringRes: Int) {
            itemView.findViewById<TextView>(R.id.headerTitle).setText(stringRes)
        }
    }

    private inner class ItemHolder constructor(itemView: CustomizeToolbarItemView) : DefaultViewHolder<CustomizeToolbarItemView>(itemView) {
        fun bindItem(pageActionItem: PageActionItem, position: Int) {
            view.setContents(pageActionItem, position)
        }
    }

    private inner class DescriptionViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView)

    private inner class SetToDefaultViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        init {
            itemView.findViewById<TextView>(R.id.resetToDefaultButton).setOnClickListener {
                viewModel.resetToDefault()
                setupRecyclerView()
            }
        }
    }

    private inner class EmptyPlaceholderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView)

    companion object {
        const val VIEW_TYPE_DESCRIPTION = 0
        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_ITEM = 2
        const val VIEW_TYPE_SET_TO_DEFAULT = 3
        const val VIEW_TYPE_EMPTY_PLACEHOLDER = 4
        const val TOOLBAR_ITEMS_LIMIT = 5

        fun newInstance(): CustomizeToolbarFragment {
            return CustomizeToolbarFragment()
        }
    }
}
