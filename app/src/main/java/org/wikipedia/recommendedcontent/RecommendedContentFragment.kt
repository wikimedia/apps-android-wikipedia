package org.wikipedia.recommendedcontent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import org.wikipedia.feed.news.NewsActivity
import org.wikipedia.feed.onthisday.OnThisDayActivity
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryFragment
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesActivity
import org.wikipedia.search.RecentSearchesFragment
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class RecommendedContentFragment : Fragment(), RecommendedContentSection.Callback {
    private var _binding: FragmentRecommendedContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecommendedContentViewModel by viewModels { RecommendedContentViewModel.Factory(requireArguments()) }
    private val demoStartTime = System.currentTimeMillis()

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
                launch {
                    viewModel.actionState.collect {
                        when (it) {
                            is Resource.Success -> {
                                reloadHistoryList(it.data.first, it.data.second)
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

    private fun buildDemoButtons() {
        binding.section1.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.IN_THE_NEWS,
                    RecommendedContentSection.ON_THIS_DAY
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section2.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.EXPLORE
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section3.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.EXPLORE,
                    RecommendedContentSection.PLACES_NEAR_YOU
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section4.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.EXPLORE,
                    RecommendedContentSection.PLACES_NEAR_YOU,
                    RecommendedContentSection.RANDOM
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section5.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.RANDOM
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section6.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.PLACES_NEAR_YOU
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section7.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.TOP_READ,
                    RecommendedContentSection.BECAUSE_YOU_READ,
                    RecommendedContentSection.CONTINUE_READING
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section8.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.PLACES_NEAR_YOU
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section9.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.CONTINUE_READING
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
        binding.section10.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentOverlayContainer, newInstance(inHistory = viewModel.inHistory, showTabs = viewModel.showTabs, listOf(
                    RecommendedContentSection.RANDOM
                ).map { it.id }), null)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun buildHistoryList(list: List<PageTitle>) {
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = RecyclerViewAdapter(list)
        binding.searchCard.root.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.background_color))
        binding.historyMoreButton.setOnClickListener {
            if (viewModel.inHistory) {
                (requireParentFragment() as HistoryFragment).reloadHistory()
            } else {
                (requireParentFragment() as RecentSearchesFragment).reloadRecentSearches()
            }
            parentFragmentManager.popBackStack()
        }
        if (viewModel.inHistory) {
            binding.historyMoreButton.text = getString(R.string.recommended_content_view_more_history)
        } else {
            binding.historyMoreButton.text = getString(R.string.recommended_content_more_recent_searches)
        }
    }

    private fun buildRecommendedContent(list: List<Pair<RecommendedContentSection, List<PageSummary>>>) {
        list.forEach { (section, pageSummaries) ->
            val sectionView = RecommendedContentSectionView(requireContext())
            sectionView.buildContent(section, pageSummaries, this)
            binding.recommendedContentContainer.addView(sectionView)
        }
        Toast.makeText(requireContext(), "Demo load time: ${System.currentTimeMillis() - demoStartTime}ms", Toast.LENGTH_SHORT).show()
    }

    private fun reloadHistoryList(position: Int, list: List<PageTitle>) {
        binding.historyList.adapter?.notifyItemRemoved(position)
        (binding.historyList.adapter as RecyclerViewAdapter).setList(list)
    }

    private inner class RecyclerViewAdapter(list: List<PageTitle>): RecyclerView.Adapter<RecyclerViewItemHolder>() {

         var list : List<PageTitle>

        init {
            this.list = list
        }

        fun setList(list: List<PageTitle>) {
            this.list = list
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSearchHistoryBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(list[position], position)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemRecommendedContentSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindItem(pageTitle: PageTitle, position: Int) {
            val listIcon = if (viewModel.inHistory) R.drawable.ic_history_24 else R.drawable.ic_search_white_24dp
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

    override fun onTopReadSelect() {
        viewModel.feedContent?.topRead?.let {
            val topReadCard = TopReadListCard(it, viewModel.wikiSite)
            startActivity(TopReadArticlesActivity.newIntent(requireContext(), topReadCard))
        }
    }

    override fun onExploreSelect() {
        // TODO: discuss this
    }

    override fun onOnThisDaySelect() {
        viewModel.feedContent?.onthisday?.let {
            startActivity(OnThisDayActivity.newIntent(requireContext(), 0, -1, viewModel.wikiSite, Constants.InvokeSource.RECOMMENDED_CONTENT))
        }
    }

    override fun onInTheNewsSelect() {
        viewModel.feedContent?.news?.let {
            startActivity(NewsActivity.newIntent(requireContext(), it.first(), viewModel.wikiSite))
        }
    }

    override fun onPlacesSelect() {
        startActivity(PlacesActivity.newIntent(requireContext()))
    }

    override fun onBecauseYouReadSelect() {
        // TODO: discuss this
    }

    override fun onContinueReadingSelect() {
        // TODO: discuss this
    }

    override fun onRandomSelect() {
        // TODO: discuss this
    }
}
