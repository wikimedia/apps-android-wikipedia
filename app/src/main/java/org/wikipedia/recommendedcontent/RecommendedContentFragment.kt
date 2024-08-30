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
import org.wikipedia.Constants
import org.wikipedia.analytics.metricsplatform.ExperimentalLinkPreviewInteraction
import org.wikipedia.analytics.metricsplatform.RecommendedContentAnalyticsHelper
import org.wikipedia.databinding.FragmentRecommendedContentBinding
import org.wikipedia.databinding.ItemRecommendedContentSearchHistoryBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class RecommendedContentFragment : Fragment() {
    private var _binding: FragmentRecommendedContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecommendedContentViewModel by viewModels { RecommendedContentViewModel.Factory(requireArguments()) }

    private val parentSearchFragment get() = requireParentFragment().requireParentFragment() as SearchFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRecommendedContentBinding.inflate(layoutInflater, container, false)


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.recentSearchesState.collect {
                        when (it) {
                            is Resource.Success -> {
                                buildHistoryList(it.data)
                            }
                            is Resource.Error -> {
                                parentSearchFragment.onSearchProgressBar(false)
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
                                parentSearchFragment.onSearchProgressBar(true)
                            }
                            is Resource.Success -> {
                                buildRecommendedContent(it.data)
                            }
                            is Resource.Error -> {
                                parentSearchFragment.onSearchProgressBar(false)
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
                                reloadRecentSearchesList(it.data.first, it.data.second)
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
        parentSearchFragment.setUpLanguageScroll(Prefs.selectedLanguagePositionInSearch)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // TODO: need to refresh the list after searching
    private fun buildHistoryList(list: List<PageTitle>) {
        binding.recentSearchesList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentSearchesList.adapter = RecyclerViewAdapter(list)
    }

    private fun buildRecommendedContent(list: List<PageSummary>) {
        parentSearchFragment.onSearchProgressBar(false)
        if (list.isEmpty()) {
            parentSearchFragment.analyticsEvent = ExperimentalLinkPreviewInteraction(HistoryEntry.SOURCE_SEARCH, RecommendedContentAnalyticsHelper.abcTest.getGroupName(), false)
                .also { it.logImpression() }
            parentFragmentManager.popBackStack()
            return
        }
        parentSearchFragment.analyticsEvent = ExperimentalLinkPreviewInteraction(HistoryEntry.SOURCE_SEARCH, RecommendedContentAnalyticsHelper.abcTest.getGroupName(), true)
            .also { it.logImpression() }
        binding.recommendedContent.buildContent(list, parentSearchFragment.analyticsEvent)
    }

    private fun reloadRecentSearchesList(position: Int, list: List<PageTitle>) {
        val adapter = binding.recentSearchesList.adapter as RecyclerViewAdapter
        adapter.notifyItemRemoved(position)
        adapter.setList(list)
        adapter.notifyItemRangeChanged(0, list.size)
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
            binding.listItem.text = StringUtil.fromHtml(pageTitle.displayText)
            binding.deleteIcon.setOnClickListener {
                viewModel.removeRecentSearchItem(pageTitle, position)
            }

            binding.listItem.setOnClickListener {
                parentSearchFragment.setSearchText(pageTitle.displayText)
            }
        }
    }

    fun reload(wikiSite: WikiSite) {
        viewModel.reload(wikiSite)
    }

    companion object {
        const val ARG_SECTION_IDS = "sectionIds"

        fun newInstance(wikiSite: WikiSite, sectionIds: List<Int>) = RecommendedContentFragment().apply {
            arguments = bundleOf(
                Constants.ARG_WIKISITE to wikiSite,
                ARG_SECTION_IDS to sectionIds
            )
        }
    }
}
