package org.wikipedia.edit.insertmedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityInsertMediaBinding
import org.wikipedia.databinding.ItemInsertMediaBinding
import org.wikipedia.gallery.GalleryItemFragment
import org.wikipedia.gallery.ImageLicense
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.search.SearchResult
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.*

class InsertMediaActivity : BaseActivity() {
    private lateinit var binding: ActivityInsertMediaBinding

    private val insertMediaAdapter = InsertMediaAdapter()
    private val insertMediaLoadHeader = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaLoadFooter = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaConcatAdapter = insertMediaAdapter.withLoadStateHeaderAndFooter(insertMediaLoadHeader, insertMediaLoadFooter)
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    private val viewModel: InsertMediaViewModel by viewModels { InsertMediaViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.insert_media_title)

        binding.refreshView.setOnRefreshListener {
            binding.refreshView.isRefreshing = false
            insertMediaAdapter.notifyDataSetChanged()
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = insertMediaConcatAdapter

        lifecycleScope.launch {
            viewModel.insertMediaFlow.collectLatest {
                insertMediaAdapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            insertMediaAdapter.loadStateFlow.collectLatest {
                insertMediaLoadHeader.loadState = it.refresh
                insertMediaLoadFooter.loadState = it.append
                val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && insertMediaAdapter.itemCount == 0)
                if (showEmpty) {
                    insertMediaConcatAdapter.addAdapter(EmptyItemAdapter(R.string.search_no_results_found))
                }
            }
        }
    }

    private fun showSelectedImage() {
        viewModel.selectedImage?.let {
            binding.emptyImageContainer.isVisible = false
            binding.selectedImageContainer.isVisible = true
            ViewUtil.loadImageWithRoundedCorners(binding.selectedImage, it.pageTitle.thumbUrl)
            binding.selectedImageDescription.text = it.pageTitle.displayText
            setLicenseInfo(it)
        } ?: run {
            binding.emptyImageContainer.isVisible = true
            binding.selectedImageContainer.isVisible = false
        }
    }

    private fun setLicenseInfo(mediaSearchResult: MediaSearchResult) {
        val metadata = mediaSearchResult.imageInfo?.metadata ?: return

        val license = ImageLicense(metadata.license(), metadata.licenseShortName(), metadata.licenseUrl())

        if (license.licenseIcon == R.drawable.ic_license_by) {
            binding.licenseIcon.setImageResource(R.drawable.ic_license_cc)
            binding.licenseIconBy.setImageResource(R.drawable.ic_license_by)
            binding.licenseIconBy.visibility = View.VISIBLE
            binding.licenseIconSa.setImageResource(R.drawable.ic_license_sharealike)
            binding.licenseIconSa.visibility = View.VISIBLE
        } else {
            binding.licenseIcon.setImageResource(license.licenseIcon)
            binding.licenseIconBy.visibility = View.GONE
            binding.licenseIconSa.visibility = View.GONE
        }

        binding.licenseIcon.contentDescription = metadata.licenseShortName().ifBlank {
            getString(R.string.gallery_fair_use_license)
        }
        binding.licenseIcon.tag = metadata.licenseUrl()
        DeviceUtil.setContextClickAsLongClick(binding.licenseContainer)
        val creditStr = metadata.artist().ifEmpty { metadata.credit() }

        binding.creditText.text = StringUtil.fromHtml(creditStr.ifBlank { getString(R.string.gallery_uploader_unknown) })
    }

    private inner class LoadingItemAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadingViewHolder>() {
        override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
            holder.bindItem(loadState, retry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingViewHolder {
            return LoadingViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }
    }

    private inner class EmptyItemAdapter(@StringRes private val text: Int) : RecyclerView.Adapter<EmptyViewHolder>() {
        override fun onBindViewHolder(holder: EmptyViewHolder, position: Int) {
            holder.bindItem(text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyViewHolder {
            return EmptyViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class InsertMediaDiffCallback : DiffUtil.ItemCallback<MediaSearchResult>() {
        override fun areItemsTheSame(oldItem: MediaSearchResult, newItem: MediaSearchResult): Boolean {
            return oldItem.pageTitle.prefixedText == newItem.pageTitle.prefixedText && oldItem.pageTitle.namespace == newItem.pageTitle.namespace
        }

        override fun areContentsTheSame(oldItem: MediaSearchResult, newItem: MediaSearchResult): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class InsertMediaAdapter : PagingDataAdapter<MediaSearchResult, RecyclerView.ViewHolder>(InsertMediaDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): InsertMediaItemHolder {
            return InsertMediaItemHolder(ItemInsertMediaBinding.inflate(layoutInflater))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as InsertMediaItemHolder).bindItem(it)
            }
        }
    }

    private inner class LoadingViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(loadState: LoadState, retry: () -> Unit) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            progressBar.isVisible = loadState is LoadState.Loading
            errorView.isVisible = loadState is LoadState.Error
            errorView.retryClickListener = View.OnClickListener { retry() }
            if (loadState is LoadState.Error) {
                errorView.setError(loadState.error)
            }
        }
    }

    private inner class EmptyViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(@StringRes text: Int) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            val emptyMessage = itemView.findViewById<TextView>(R.id.emptyMessage)
            progressBar.isVisible = false
            errorView.isVisible = false
            emptyMessage.text = getString(text)
            emptyMessage.isVisible = true
        }
    }

    private inner class InsertMediaItemHolder constructor(val binding: ItemInsertMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItem(searchResult: MediaSearchResult) {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, searchResult.pageTitle.thumbUrl)
            binding.imageDescription.text = searchResult.pageTitle.displayText

            binding.selectedIcon.isVisible = searchResult == viewModel.selectedImage

            binding.root.setOnClickListener {
                viewModel.selectedImage = if (searchResult == viewModel.selectedImage) null else searchResult
                showSelectedImage()
                insertMediaAdapter.notifyDataSetChanged()
            }
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {
        var searchActionProvider: SearchActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchActionProvider = SearchActionProvider(this@InsertMediaActivity, searchHintString,
                object : SearchActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchActionProvider)

            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.searchQuery = s
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
        }

        override fun getSearchHintString(): String {
            return viewModel.searchQuery
        }

        override fun getParentContext(): Context {
            return this@InsertMediaActivity
        }
    }

    companion object {
        const val EXTRA_SEARCH_QUERY = "searchQuery"

        fun newIntent(context: Context, searchQuery: String): Intent {
            return Intent(context, InsertMediaActivity::class.java)
                    .putExtra(EXTRA_SEARCH_QUERY, searchQuery)
        }
    }
}
