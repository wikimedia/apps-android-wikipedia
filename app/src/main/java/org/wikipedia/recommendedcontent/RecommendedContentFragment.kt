package org.wikipedia.recommendedcontent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentRecommendedContentBinding
import org.wikipedia.databinding.ItemRecommendedContentSearchHistoryBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryFragment
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchFragment
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

        buildDemoButtons()

        binding.searchCard.root.isVisible = viewModel.inHistory
        binding.searchCard.root.setOnClickListener {
            (requireParentFragment() as HistoryFragment).openSearchActivity(Constants.InvokeSource.RECOMMENDED_CONTENT, null, it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.historyState.collect {
                        when (it) {
                            is Resource.Success -> {
                                buildHistoryList(it.data)
                            }
                            is Resource.Error -> {
                                parentFragmentManager.popBackStack()
                                L.d(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    viewModel.recommendedContentState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                (requireParentFragment().requireParentFragment() as SearchFragment).onSearchProgressBar(true)
                            }
                            is Resource.Success -> {
                                buildRecommendedContent(it.data)
                            }
                            is Resource.Error -> {
                                parentFragmentManager.popBackStack()
                                L.d(it.throwable)
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect {
                        when (it) {
                            is Resource.Success -> {
                                reloadHistoryList(it.data.first, it.data.second)
                            }

                            is Resource.Error -> {
                                L.d(it.throwable)
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSearchHistory()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildDemoButtons() {
        binding.personalizedSection.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory,
                    RecommendedContentSection.personalizeList().map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.generalizedSection.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory,
                    RecommendedContentSection.generalizedList().map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
    }

    // TODO: need to refresh the list after searching
    private fun buildHistoryList(list: List<PageTitle>) {
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = RecyclerViewAdapter(list)
        binding.searchCard.root.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.background_color))
    }

    private fun buildRecommendedContent(list: List<PageSummary>) {
        (requireParentFragment().requireParentFragment() as SearchFragment).onSearchProgressBar(false)
        if (list.isEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }
        binding.recommendedContent.buildContent(list)
    }

    private fun reloadHistoryList(position: Int, list: List<PageTitle>) {
        (binding.historyList.adapter as RecyclerViewAdapter).setList(list)
        binding.historyList.adapter?.notifyItemRemoved(position)
    }

    private inner class RecyclerViewAdapter(list: List<PageTitle>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {

         var pages: List<PageTitle>

        init {
            this.pages = list
        }

        fun setList(list: List<PageTitle>) {
            this.pages = list
        }

        override fun getItemCount(): Int {
            return pages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSearchHistoryBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(pages[position], position)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemRecommendedContentSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindItem(pageTitle: PageTitle, position: Int) {
            val listIcon = if (!viewModel.inHistory) R.drawable.ic_history_24 else R.drawable.ic_search_white_24dp
            binding.listItem.text = StringUtil.fromHtml(pageTitle.displayText)
            binding.listItem.setCompoundDrawablesWithIntrinsicBounds(listIcon, 0, 0, 0)
            binding.deleteIcon.setOnClickListener {
                if (viewModel.inHistory) {
                    viewModel.removeHistoryItem(pageTitle, position)
                } else {
                    viewModel.removeRecentSearchItem(pageTitle, position)
                }
            }

            binding.listItem.setOnClickListener {
                if (viewModel.inHistory) {
                    val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_RECOMMENDED_CONTENT)
                    startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
                } else {
                    (requireParentFragment().requireParentFragment() as SearchFragment).setSearchText(pageTitle.displayText)
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    companion object {
        const val ARG_IN_HISTORY = "inHistory"
        const val ARG_SECTION_IDS = "sectionIds"

        fun newInstance(inHistory: Boolean, sectionIds: List<Int>) = RecommendedContentFragment().apply {
            arguments = bundleOf(
                ARG_IN_HISTORY to inHistory,
                ARG_SECTION_IDS to sectionIds
            )
        }
    }
}
