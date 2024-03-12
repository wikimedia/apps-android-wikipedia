package org.wikipedia.edit.templates

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityTemplatesSearchBinding
import org.wikipedia.databinding.ItemTemplatesSearchBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class TemplatesSearchActivity : BaseActivity() {
    private lateinit var binding: ActivityTemplatesSearchBinding
    private lateinit var insertTemplateFragment: InsertTemplateFragment

    private var templatesSearchAdapter: TemplatesSearchAdapter? = null

    val viewModel: TemplatesSearchViewModel by viewModels { TemplatesSearchViewModel.Factory(intent.extras!!) }

    private val searchCloseListener = SearchView.OnCloseListener {
        closeSearch()
        false
    }

    private val searchQueryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(queryText: String): Boolean {
            DeviceUtil.hideSoftKeyboard(this@TemplatesSearchActivity)
            return true
        }

        override fun onQueryTextChange(queryText: String): Boolean {
            binding.searchCabView.setCloseButtonVisibility(queryText)
            startSearch(queryText.trim())
            return true
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplatesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initSearchView()

        templatesSearchAdapter = TemplatesSearchAdapter()
        binding.templateRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.templateRecyclerView.adapter = templatesSearchAdapter

        insertTemplateFragment = supportFragmentManager.findFragmentById(R.id.insertTemplateFragment) as InsertTemplateFragment

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.searchTemplatesFlow.collectLatest {
                        templatesSearchAdapter?.submitData(it)
                    }
                }
                launch {
                    templatesSearchAdapter?.loadStateFlow?.collectLatest {
                        binding.searchProgressBar.isVisible = it.append is LoadState.Loading || it.refresh is LoadState.Loading
                        val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && templatesSearchAdapter?.itemCount == 0)
                        binding.emptyMessage.isVisible = showEmpty
                    }
                }
            }
        }
    }

    private fun initSearchView() {
        binding.searchCabView.setOnQueryTextListener(searchQueryListener)
        binding.searchCabView.setOnCloseListener(searchCloseListener)
        binding.searchCabView.setSearchHintTextColor(ResourceUtil.getThemedColor(this, R.attr.secondary_color))
        binding.searchCabView.queryHint = getString(R.string.templates_search_hint)
        val searchEditPlate = binding.searchCabView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun startSearch(term: String?) {
        viewModel.searchQuery = term
        templatesSearchAdapter?.refresh()
    }

    private fun closeSearch() {
        DeviceUtil.hideSoftKeyboard(this)
    }

    private fun showInsertTemplateFragment() {
        binding.searchCabView.isVisible = false
        binding.insertTemplateButton.isVisible = true
        insertTemplateFragment.show()
    }

    override fun onBackPressed() {
        // TODO: handle back press on template data screen
        super.onBackPressed()
    }

    private inner class TemplatesSearchDiffCallback : DiffUtil.ItemCallback<PageTitle>() {
        override fun areItemsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem.prefixedText == newItem.prefixedText && oldItem.namespace == newItem.namespace
        }
    }

    private inner class TemplatesSearchAdapter : PagingDataAdapter<PageTitle, RecyclerView.ViewHolder>(TemplatesSearchDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): TemplatesSearchItemHolder {
            return TemplatesSearchItemHolder(ItemTemplatesSearchBinding.inflate(layoutInflater))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as TemplatesSearchItemHolder).bindItem(it)
            }
        }
    }

    private inner class TemplatesSearchItemHolder(val binding: ItemTemplatesSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItem(pageTitle: PageTitle) {
            binding.itemTitle.text = StringUtil.removeNamespace(pageTitle.displayText)
            binding.itemDescription.isVisible = !pageTitle.description.isNullOrEmpty()
            binding.itemDescription.text = pageTitle.description
            StringUtil.boldenKeywordText(binding.itemTitle, binding.itemTitle.text.toString(), viewModel.searchQuery)

            itemView.setOnClickListener {
                viewModel.selectedTemplate = pageTitle
                showInsertTemplateFragment()
            }
        }
    }

    companion object {
        fun newIntent(context: Context, wikiSite: WikiSite, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TemplatesSearchActivity::class.java)
                .putExtra(Constants.ARG_WIKISITE, wikiSite)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
