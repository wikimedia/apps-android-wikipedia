package org.wikipedia.page.customize

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentCustomizeFavoritesBinding
import org.wikipedia.language.LanguagesListActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.settings.languages.WikipediaLanguagesItemView
import org.wikipedia.views.DefaultViewHolder
import java.util.*

class CustomizeFavoritesFragment : Fragment() {
    private var _binding: FragmentCustomizeFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentCustomizeFavoritesBinding.inflate(LayoutInflater.from(context), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private inner class RecyclerItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var checkboxEnabled = false
        override fun getItemViewType(position: Int): Int {
            // TODO: set up type
            return 0
        }

        override fun getItemCount(): Int {
            return PageMenuItem.size() + LIST_OF_CATEGORY.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_favorites_header, parent, false))
                }
                VIEW_TYPE_EMPTY_PLACEHOLDER -> {
                    EmptyPlaceholderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_customize_favorites_empty_placeholder, parent, false))
                }
                else -> {
                    ItemHolder(CustomizeFavoritesItemView(parent.context))
                }
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            // TODO
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            // TODO
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is ItemHolder) {
                holder.view.callback = null
                holder.view.setDragHandleTouchListener(null)
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun onMoveItem(oldPosition: Int, newPosition: Int) {
//            Collections.swap(wikipediaLanguages, oldPosition - WikipediaLanguagesFragment.NUM_HEADERS, newPosition - WikipediaLanguagesFragment.NUM_FOOTERS)
            notifyItemMoved(oldPosition, newPosition)
        }

        fun onCheckboxEnabled(enabled: Boolean) {
            checkboxEnabled = enabled
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
            if (target is ItemHolder) {
                adapter.onMoveItem(source.adapterPosition, target.getAdapterPosition())
            }
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.post {
                if (isAdded) {
                    // TODO
                }
            }
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem(position: Int) {
            itemView.findViewById<TextView>(R.id.headerTitle).setText(LIST_OF_CATEGORY[position])
        }
    }

    private inner class ItemHolder constructor(itemView: CustomizeFavoritesItemView) : DefaultViewHolder<CustomizeFavoritesItemView>(itemView) {
        fun bindItem(id: Int, position: Int) {
            view.setContents(PageMenuItem.find(id), position)
        }
    }

    private inner class EmptyPlaceholderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView)

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_EMPTY_PLACEHOLDER = 2
        private val LIST_OF_CATEGORY = listOf(R.string.customize_favorites_category_quick_actions, R.string.customize_favorites_category_menu)

        fun newInstance(): CustomizeFavoritesFragment {
            return CustomizeFavoritesFragment()
        }
    }
}
