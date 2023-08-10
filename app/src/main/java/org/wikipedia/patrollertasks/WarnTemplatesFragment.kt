package org.wikipedia.patrollertasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import de.mrapp.android.util.view.ViewHolder
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.databinding.FragmentWarnTemplatesBinding
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.util.ResourceUtil

class WarnTemplatesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentWarnTemplatesBinding? = null

    private val viewModel: WarnTemplatesViewModel by viewModels()
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentWarnTemplatesBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar!!.title = getString(R.string.patroller_warn_templates_manage_title)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.warnTemplatesRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.warnTemplatesRefreshView.setOnRefreshListener { viewModel.loadWarnTemplates() }
        binding.warnTemplatesErrorView.retryClickListener = View.OnClickListener { viewModel.loadWarnTemplates() }

        binding.warnTemplatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is WarnTemplatesViewModel.UiState.Loading -> onLoading()
                        is WarnTemplatesViewModel.UiState.Success -> onSuccess()
                        is WarnTemplatesViewModel.UiState.Error -> onError(it.throwable)
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
        // TODO: add menu
//        inflater.inflate(R.menu.menu_watchlist, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        // TODO: implement this
    }

    private fun onLoading() {
        binding.warnTemplatesEmptyContainer.visibility = View.GONE
        binding.warnTemplatesRecyclerView.visibility = View.GONE
        binding.warnTemplatesErrorView.visibility = View.GONE
        binding.warnTemplatesProgressBar.isVisible = !binding.warnTemplatesRefreshView.isRefreshing
    }

    private fun onSuccess() {
        binding.warnTemplatesErrorView.visibility = View.GONE
        binding.warnTemplatesRefreshView.isRefreshing = false
        binding.warnTemplatesProgressBar.visibility = View.GONE
        binding.warnTemplatesRecyclerView.adapter = RecyclerAdapter(viewModel.warnTemplatesList)
        WatchlistAnalyticsHelper.logWatchlistItemCountOnLoad(requireContext(), viewModel.warnTemplatesList.size)
        binding.warnTemplatesRecyclerView.visibility = View.VISIBLE
    }

    private fun onError(t: Throwable) {
        binding.warnTemplatesRefreshView.isRefreshing = false
        binding.warnTemplatesProgressBar.visibility = View.GONE
        binding.warnTemplatesErrorView.setError(t)
        binding.warnTemplatesErrorView.visibility = View.VISIBLE
    }


    internal inner class WarnTemplateItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: MwQueryResult.WatchlistItem) {
            // TODO: implement this
//            val view = itemView as WatchlistItemView
//            view.setItem(item)
//            view.callback = this@WarnTemplatesFragment
        }
    }

    internal inner class RecyclerAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        constructor(items: List<Any>) : this() {
            this.items = items
        }

        private var items: List<Any> = ArrayList()

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ViewHolder(view!!)
            // TODO: implement this
//            return when (viewType) {
//                return WarnTemplateItemViewHolder(WatchlistItemView(requireContext()))
//            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
               // TODO: implement this
            }
        }
    }

    companion object {
        fun newInstance(): WarnTemplatesFragment {
            return WarnTemplatesFragment()
        }
    }
}
