package org.wikipedia.recommendedcontent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.databinding.FragmentRecommendedContentBinding
import org.wikipedia.databinding.ItemRecommendedContentSearchHistoryBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class RecommendedContentFragment : Fragment() {
    private var _binding: FragmentRecommendedContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecommendedContentViewModel by viewModels { RecommendedContentViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRecommendedContentBinding.inflate(layoutInflater, container, false)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.historyState.collect {
                        when (it) {
                            is Resource.Success -> {
                                buildHistoryList(it.data)
                            }
                            is Resource.Error -> {
                                // TODO: implement error
                                L.d(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    viewModel.recommendedContentState.collect {
                        when (it) {
                            is Resource.Success -> {
                                buildRecommendedContent(it.data)
                            }
                            is Resource.Error -> {
                                // TODO: implement error
                                L.d(it.throwable)
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildHistoryList(list: List<PageTitle>) {
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = RecyclerViewAdapter(list)
        binding.searchCard.root.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.background_color))
        binding.historyMoreButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        if (viewModel.inHistory) {
            binding.historyMoreButton.text = getString(R.string.recommended_content_view_more_history)
        } else {
            binding.historyMoreButton.text = getString(R.string.recommended_content_more_recent_searches)
        }
    }

    private fun buildRecommendedContent(list: List<Pair<RecommendedContentSection, List<PageSummary>>>) {
        L.d("buildRecommendedContent list: $list")
        list.forEach { (section, pageSummaries) ->
            val sectionView = RecommendedContentSectionView(requireContext())
            sectionView.buildContent(section, pageSummaries)
            binding.recommendedContentContainer.addView(sectionView)
        }
    }

    private inner class RecyclerViewAdapter(val list: List<PageTitle>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSearchHistoryBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(list[position])
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemRecommendedContentSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private lateinit var pageTitle: PageTitle

        init {
            itemView.setOnClickListener(this)
        }

        fun bindItem(pageTitle: PageTitle) {
            this.pageTitle = pageTitle
            val listIcon = if (viewModel.inHistory) R.drawable.ic_history_24 else R.drawable.ic_search_white_24dp
            binding.listItem.text = StringUtil.fromHtml(pageTitle.displayText)
            binding.listItem.setCompoundDrawablesWithIntrinsicBounds(listIcon, 0, 0, 0)
            binding.deleteIcon.setOnClickListener {
                // TODO: implement this method.
                if (viewModel.inHistory) {
                } else {
                }
            }
        }

        override fun onClick(v: View) {
            val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_RECOMMENDED_CONTENT)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }
    }

    companion object {
        const val ARG_IN_HISTORY = "inHistory"
        const val ARG_SHOW_TABS = "showTabs"
        const val ARG_SECTION_IDS = "sectionIds"

        fun newInstance(inHistory: Boolean, showTabs: Boolean, sectionIds: List<Int>) = RecommendedContentFragment().apply {
            arguments = bundleOf(
                ARG_IN_HISTORY to inHistory,
                ARG_SHOW_TABS to showTabs,
                ARG_SECTION_IDS to sectionIds
            )
        }
    }
}
