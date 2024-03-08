package org.wikipedia.edit.templates

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
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
import org.wikipedia.util.StringUtil

class TemplatesSearchActivity : BaseActivity() {
    private lateinit var binding: ActivityTemplatesSearchBinding

    private var templatesSearchAdapter: TemplatesSearchAdapter? = null

    val viewModel: TemplatesSearchViewModel by viewModels { TemplatesSearchViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplatesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setImageZoomHelper()
        supportActionBar?.title = getString(R.string.templates_search_hint)

        binding.templateRecyclerView.layoutManager = LinearLayoutManager(this)

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
            binding.itemTitle.text = StringUtil.fromHtml(pageTitle.displayText)
            binding.itemDescription.isVisible = !pageTitle.description.isNullOrEmpty()
            binding.itemDescription.text = pageTitle.description

            binding.root.setOnClickListener {
                // TODO: implement this
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
