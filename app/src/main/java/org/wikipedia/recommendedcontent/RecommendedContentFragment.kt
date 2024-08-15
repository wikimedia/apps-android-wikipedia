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
import org.wikipedia.R
import org.wikipedia.databinding.FragmentRecommendedContentBinding
import org.wikipedia.databinding.ItemRecommendedContentSearchHistoryBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class RecommendedContentFragment : Fragment() {
    private var _binding: FragmentRecommendedContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecommendedContentViewModel by viewModels { RecommendedContentViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRecommendedContentBinding.inflate(inflater, container, false)

        binding.historyList.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.historyState.collect {
                    when (it) {
                        is Resource.Success -> {
                            binding.historyList.adapter = RecyclerViewAdapter(it.data)
                        }
                        is Resource.Error -> {
                            // TODO: implement error
                        }
                    }
                }
            }

            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.recommendedContentState.collect {
                    when (it) {
                        is Resource.Success -> {
                        }
                        is Resource.Error -> {
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

    private inner class RecyclerViewAdapter(val list: List<PageTitle>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemRecommendedContentSearchHistoryBinding.inflate(layoutInflater, parent))
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
            binding.listItem.setCompoundDrawablesWithIntrinsicBounds(0, listIcon, 0, 0)
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

        fun newInstance(wikiSite: WikiSite, inHistory: Boolean, showTabs: Boolean) = RecommendedContentFragment().apply {
            arguments = bundleOf(
                Constants.ARG_WIKISITE to wikiSite,
                ARG_IN_HISTORY to inHistory,
                ARG_SHOW_TABS to showTabs
            )
        }
    }
}
