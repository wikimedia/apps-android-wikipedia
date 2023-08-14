package org.wikipedia.talk.template

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.databinding.FragmentTalkTemplatesBinding
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.ResourceUtil

class TalkTemplatesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentTalkTemplatesBinding? = null

    private val viewModel: TalkTemplatesViewModel by viewModels()
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentTalkTemplatesBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.talk_templates_manage_title)

        return binding.root
    }

    private val requestNewTemplate = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadTalkTemplates()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.talkTemplatesRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.talkTemplatesRefreshView.setOnRefreshListener { viewModel.loadTalkTemplates() }
        binding.talkTemplatesErrorView.retryClickListener = View.OnClickListener { viewModel.loadTalkTemplates() }

        binding.talkTemplatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is TalkTemplatesViewModel.UiState.Loading -> onLoading()
                        is TalkTemplatesViewModel.UiState.Success -> onSuccess()
                        is TalkTemplatesViewModel.UiState.Error -> onError(it.throwable)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_talk_templates, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_new_message -> {
                requestNewTemplate.launch(AddTemplateActivity.newIntent(requireContext()))
                true
            }
            else -> false
        }
    }

    private fun onLoading() {
        binding.talkTemplatesEmptyContainer.visibility = View.GONE
        binding.talkTemplatesRecyclerView.visibility = View.GONE
        binding.talkTemplatesErrorView.visibility = View.GONE
        binding.talkTemplatesProgressBar.isVisible = !binding.talkTemplatesRefreshView.isRefreshing
    }

    private fun onSuccess() {
        binding.talkTemplatesErrorView.visibility = View.GONE
        binding.talkTemplatesRefreshView.isRefreshing = false
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesRecyclerView.adapter = RecyclerAdapter(viewModel.talkTemplatesList)
        WatchlistAnalyticsHelper.logWatchlistItemCountOnLoad(requireContext(), viewModel.talkTemplatesList.size)
        binding.talkTemplatesRecyclerView.visibility = View.VISIBLE
    }

    private fun onError(t: Throwable) {
        binding.talkTemplatesRefreshView.isRefreshing = false
        binding.talkTemplatesProgressBar.visibility = View.GONE
        binding.talkTemplatesErrorView.setError(t)
        binding.talkTemplatesErrorView.visibility = View.VISIBLE
    }

    internal inner class TalkTemplatesItemViewHolder(private val templatesItemView: TalkTemplatesItemView) : RecyclerView.ViewHolder(templatesItemView.rootView) {
        fun bindItem(item: TalkTemplate, position: Int) {
            templatesItemView.setContents(item, position)
        }
    }

    internal inner class RecyclerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        constructor(items: List<TalkTemplate>) : this() {
            this.items = items
        }

        private var items: List<TalkTemplate> = ArrayList()

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return TalkTemplatesItemViewHolder(TalkTemplatesItemView(requireContext()))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as TalkTemplatesItemViewHolder).bindItem(items[position], position)
        }
    }

    companion object {
        fun newInstance(): TalkTemplatesFragment {
            return TalkTemplatesFragment()
        }
    }
}
