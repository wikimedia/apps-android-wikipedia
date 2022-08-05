package org.wikipedia.edit.insertmedia

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import org.wikipedia.databinding.ItemEditActionbarButtonBinding
import org.wikipedia.databinding.ItemInsertMediaBinding
import org.wikipedia.gallery.ImageLicense
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.util.*
import org.wikipedia.views.SearchActionProvider
import org.wikipedia.views.ViewUtil
import org.wikipedia.views.WikiErrorView

class InsertMediaActivity : BaseActivity() {
    private lateinit var binding: ActivityInsertMediaBinding
    private lateinit var insertMediaSettingsFragment: InsertMediaSettingsFragment
    private lateinit var insertMediaAdvancedSettingsFragment: InsertMediaAdvancedSettingsFragment

    private val insertMediaAdapter = InsertMediaAdapter()
    private val insertMediaLoadHeader = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaLoadFooter = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaConcatAdapter = insertMediaAdapter.withLoadStateHeaderAndFooter(insertMediaLoadHeader, insertMediaLoadFooter)
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    val viewModel: InsertMediaViewModel by viewModels { InsertMediaViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.insert_media_title)

        binding.refreshView.setOnRefreshListener {
            binding.refreshView.isRefreshing = false
            insertMediaAdapter.refresh()
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

        insertMediaSettingsFragment = supportFragmentManager.findFragmentById(R.id.insertMediaSettingsFragment) as InsertMediaSettingsFragment
        insertMediaAdvancedSettingsFragment = supportFragmentManager.findFragmentById(R.id.insertMediaAdvancedSettingsFragment) as InsertMediaAdvancedSettingsFragment

        binding.licenseContainer.setOnClickListener { onLicenseClick() }
        binding.licenseContainer.setOnLongClickListener { onLicenseLongClick() }
        binding.searchInputField.text = viewModel.searchQuery
        binding.searchContainer.setOnClickListener {
            if (actionMode == null) {
                actionMode = startSupportActionMode(searchActionModeCallback)
            }
        }
        DeviceUtil.setContextClickAsLongClick(binding.licenseContainer)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_insert_media, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuNextItem = menu.findItem(R.id.menu_next)
        val menuSaveItem = menu.findItem(R.id.menu_save)
        val menuInsertItem = menu.findItem(R.id.menu_insert)
        menuNextItem.isVisible = !insertMediaSettingsFragment.isActive && !insertMediaAdvancedSettingsFragment.isActive
        menuSaveItem.isVisible = insertMediaAdvancedSettingsFragment.isActive
        menuInsertItem.isVisible = insertMediaSettingsFragment.isActive
        menuNextItem.isEnabled = viewModel.selectedImage != null
        applyActionBarButtonStyle(menuNextItem)
        applyActionBarButtonStyle(menuInsertItem)
        applyActionBarButtonStyle(menuSaveItem)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_next -> {
                showMediaSettingsFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (insertMediaSettingsFragment.handleBackPressed()) {
            binding.imageInfoContainer.isVisible = true
            binding.searchContainer.isVisible = true
            supportActionBar?.title = getString(R.string.insert_media_title)
            return
        }
        if (insertMediaAdvancedSettingsFragment.handleBackPressed()) {
            insertMediaSettingsFragment.show()
            return
        }
        super.onBackPressed()
    }

    private fun applyActionBarButtonStyle(menuItem: MenuItem) {
        val actionBarButtonBinding = ItemEditActionbarButtonBinding.inflate(layoutInflater)
        menuItem.actionView = actionBarButtonBinding.root
        actionBarButtonBinding.editActionbarButtonText.text = menuItem.title
        actionBarButtonBinding.editActionbarButtonText.setTextColor(
            ResourceUtil.getThemedColor(this,
                if (menuItem.isEnabled) R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
        actionBarButtonBinding.root.tag = menuItem
        actionBarButtonBinding.root.isEnabled = menuItem.isEnabled
        actionBarButtonBinding.root.setOnClickListener { onOptionsItemSelected(it.tag as MenuItem) }
    }

    private fun showMediaSettingsFragment() {
        binding.imageInfoContainer.isVisible = false
        binding.searchContainer.isVisible = false
        insertMediaSettingsFragment.show()
    }

    fun showMediaAdvancedSettingsFragment() {
        binding.imageInfoContainer.isVisible = false
        binding.searchContainer.isVisible = false
        insertMediaSettingsFragment.hide(false)
        insertMediaAdvancedSettingsFragment.show()
    }

    private fun showSelectedImage() {
        viewModel.selectedImage?.let {
            binding.emptyImageContainer.isVisible = false
            binding.selectedImageContainer.isVisible = true
            ViewUtil.loadImageWithRoundedCorners(binding.selectedImage, it.pageTitle.thumbUrl)
            binding.selectedImageDescription.text = StringUtil.removeHTMLTags(it.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { it.pageTitle.displayText })
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
        val creditStr = metadata.artist().ifEmpty { metadata.credit() }

        binding.creditText.text = StringUtil.fromHtml(creditStr.ifBlank { getString(R.string.gallery_uploader_unknown) })
    }

    private fun onLicenseClick() {
        if (binding.licenseIcon.contentDescription == null) {
            return
        }
        FeedbackUtil.showMessageAsPlainText((binding.licenseIcon.context as Activity),
            binding.licenseIcon.contentDescription)
    }

    private fun onLicenseLongClick(): Boolean {
        val licenseUrl = binding.licenseIcon.tag as String
        if (licenseUrl.isNotEmpty()) {
            UriUtil.handleExternalLink(this@InsertMediaActivity,
                Uri.parse(UriUtil.resolveProtocolRelativeUrl(licenseUrl)))
        }
        return true
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
            binding.imageDescription.text = StringUtil.removeHTMLTags(searchResult.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { searchResult.pageTitle.displayText })

            binding.selectedIcon.isVisible = searchResult == viewModel.selectedImage

            binding.root.setOnClickListener {
                viewModel.selectedImage = if (searchResult == viewModel.selectedImage) null else searchResult
                showSelectedImage()
                invalidateOptionsMenu()
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
            binding.imageInfoContainer.isVisible = false
            binding.searchContainer.isVisible = false
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.searchQuery = s.ifEmpty { viewModel.originalSearchQuery }
            insertMediaAdapter.refresh()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            binding.imageInfoContainer.isVisible = true
            binding.searchContainer.isVisible = true
            binding.searchInputField.text = viewModel.searchQuery
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
