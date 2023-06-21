package org.wikipedia.views

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.databinding.DialogWikiWrappedBinding

class WikiWrappedDialog(activity: Activity) : MaterialAlertDialogBuilder(activity) {
    private val binding = DialogWikiWrappedBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null

    init {
        setView(binding.root)
            Glide.with(context)
                .load("https://media.itsnicethat.com/original_images/22_Wrapped_Shapes.gif")
                .into(DrawableImageViewTarget(binding.wrappedGifView))
    }
/*

     private inner class HistoryEntryItemAdapter : RecyclerView.Adapter<DefaultViewHolder<*>>() {
        private var historyEntries = mutableListOf<Any>()
        override fun getItemCount(): Int {
            return historyEntries.size
        }

        val isEmpty
            get() = (itemCount == 0 || itemCount == 1 && historyEntries[0] is HistoryFragment.SearchBar)

        override fun getItemViewType(position: Int): Int {
            return when {
                historyEntries[position] is HistoryFragment.SearchBar -> HistoryFragment.VIEW_TYPE_SEARCH_CARD
                historyEntries[position] is String -> HistoryFragment.VIEW_TYPE_HEADER
                else -> HistoryFragment.VIEW_TYPE_ITEM
            }
        }

        fun setList(list: MutableList<Any>) {
            historyEntries = list
            notifyDataSetChanged()
        }

        fun clearList() {
            historyEntries.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<*> {
            return when (viewType) {
                HistoryFragment.VIEW_TYPE_SEARCH_CARD -> {
                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_history_header_with_search, parent, false)
                    SearchCardViewHolder(view)
                }
                HistoryFragment.VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.view_section_header, parent, false)
                    HistoryFragment.HeaderViewHolder(view)
                }
                else -> HistoryEntryItemHolder(PageItemView(requireContext()))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, pos: Int) {
            when (holder) {
                is HistoryFragment.SearchCardViewHolder -> holder.bindItem()
                is HistoryFragment.HistoryEntryItemHolder -> holder.bindItem(historyEntries[pos] as HistoryEntry)
                else -> (holder as HistoryFragment.HeaderViewHolder).bindItem(historyEntries[pos] as String)
            }
        }

        override fun onViewAttachedToWindow(holder: DefaultViewHolder<*>) {
            super.onViewAttachedToWindow(holder)
            if (holder is HistoryFragment.HistoryEntryItemHolder) {
                holder.view.callback = itemCallback
            }
        }

        override fun onViewDetachedFromWindow(holder: DefaultViewHolder<*>) {
            if (holder is HistoryFragment.HistoryEntryItemHolder) {
                holder.view.callback = null
            }
            super.onViewDetachedFromWindow(holder)
        }

        fun hideHeader() {
            if (historyEntries.isNotEmpty() && historyEntries[0] is HistoryFragment.SearchBar) {
                historyEntries.removeAt(0)
                notifyDataSetChanged()
            }
        }
    }
*/

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }
}
