package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.databinding.DialogSortReadingListsBinding
import org.wikipedia.databinding.ViewReadingListsSortOptionsItemBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class SortReadingListsDialog : ExtendedBottomSheetDialogFragment() {
    fun interface Callback {
        fun onSortOptionClick(position: Int)
    }

    private var _binding: DialogSortReadingListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReadingListSortAdapter
    private lateinit var sortOptions: List<String>
    private var chosenSortOption = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chosenSortOption = requireArguments().getInt(SORT_OPTION)
        sortOptions = resources.getStringArray(
            if (requireArguments().getBoolean(SORT_ARTICLES)) {
                R.array.sort_article_options
            } else {
                R.array.sort_options
            }
        ).asList()
        adapter = ReadingListSortAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogSortReadingListsBinding.inflate(inflater, container, false)
        binding.sortOptionsList.adapter = adapter
        binding.sortOptionsList.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ReadingListSortAdapter : RecyclerView.Adapter<SortItemHolder>() {
        override fun getItemCount(): Int {
            return sortOptions.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): SortItemHolder {
            return SortItemHolder(ViewReadingListsSortOptionsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: SortItemHolder, pos: Int) {
            holder.bindItem(pos)
            holder.itemView.setOnClickListener {
                callback()?.onSortOptionClick(pos)
                dismiss()
            }
        }
    }

    private inner class SortItemHolder constructor(val binding: ViewReadingListsSortOptionsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItem(sortOption: Int) {
            binding.sortType.text = sortOptions[sortOption]
            if (chosenSortOption == sortOption) {
                binding.check.visibility = View.VISIBLE
            } else {
                binding.check.visibility = View.GONE
            }
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    companion object {
        private const val SORT_OPTION = "sortOption"
        private const val SORT_ARTICLES = "sortArticles"

        fun newInstance(sortOption: Int, sortArticles: Boolean = false): SortReadingListsDialog {
            return SortReadingListsDialog().apply {
                arguments = bundleOf(
                    SORT_OPTION to sortOption,
                    SORT_ARTICLES to sortArticles
                )
            }
        }
    }
}
