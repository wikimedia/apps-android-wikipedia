package org.wikipedia.edit.insertmedia

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityInsertMediaBinding
import org.wikipedia.databinding.ItemEditActionbarButtonBinding
import org.wikipedia.databinding.ItemInsertMediaBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.FaceAndColorDetectImageView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.SearchActionProvider
import org.wikipedia.views.ViewUtil

class InsertMediaActivity : BaseActivity() {
    private lateinit var binding: ActivityInsertMediaBinding
    private lateinit var insertMediaSettingsFragment: InsertMediaSettingsFragment
    private lateinit var insertMediaAdvancedSettingsFragment: InsertMediaAdvancedSettingsFragment

    private var insertMediaAdapter: InsertMediaAdapter? = null
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    val viewModel: InsertMediaViewModel by viewModels { InsertMediaViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setImageZoomHelper()
        supportActionBar?.title = getString(R.string.insert_media_title)

        binding.refreshView.setOnRefreshListener {
            binding.refreshView.isRefreshing = false
            insertMediaAdapter?.refresh()
        }

        binding.searchContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(this@InsertMediaActivity, R.attr.background_color))
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)

        if (viewModel.invokeSource != Constants.InvokeSource.EDIT_ADD_IMAGE) {
            insertMediaAdapter = InsertMediaAdapter()
            binding.recyclerView.adapter = insertMediaAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.insertMediaFlow.collectLatest {
                        insertMediaAdapter?.submitData(it)
                    }
                }
                launch {
                    insertMediaAdapter?.loadStateFlow?.collectLatest {
                        binding.progressBar.isVisible = it.append is LoadState.Loading || it.refresh is LoadState.Loading
                        val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && insertMediaAdapter?.itemCount == 0)
                        binding.emptyMessage.isVisible = showEmpty
                    }
                }
            }
        }

        insertMediaSettingsFragment = supportFragmentManager.findFragmentById(R.id.insertMediaSettingsFragment) as InsertMediaSettingsFragment
        insertMediaAdvancedSettingsFragment = supportFragmentManager.findFragmentById(R.id.insertMediaAdvancedSettingsFragment) as InsertMediaAdvancedSettingsFragment

        binding.searchInputField.text = viewModel.searchQuery
        binding.searchContainer.setOnClickListener {
            if (actionMode == null) {
                actionMode = startSupportActionMode(searchActionModeCallback)
            }
        }
        adjustRefreshViewLayoutParams(false)

        if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE &&
                viewModel.selectedImage != null && savedInstanceState == null) {
            binding.imageInfoContainer.isVisible = false

            binding.root.post {
                if (!isDestroyed) {
                    showMediaSettingsFragment()
                    adjustRefreshViewLayoutParams(true)
                }
            }
        }
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
        menuInsertItem.title = if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
            getString(R.string.onboarding_continue)
        } else {
            getString(R.string.insert_media_insert_button)
        }
        menuNextItem.isEnabled = viewModel.selectedImage != null
        applyActionBarButtonStyle(menuNextItem, menuNextItem.isEnabled)
        applyActionBarButtonStyle(menuInsertItem, insertMediaSettingsFragment.captionText.isNotEmpty() &&
                insertMediaSettingsFragment.alternativeText.isNotEmpty())
        applyActionBarButtonStyle(menuSaveItem, menuSaveItem.isEnabled)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val selectedImage = viewModel.selectedImage
        return when (item.itemId) {
            R.id.menu_next -> {
                showMediaSettingsFragment()
                adjustRefreshViewLayoutParams(true)
                true
            }
            R.id.menu_save -> {
                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE && selectedImage != null) {
                    ImageRecommendationsEvent.logAction("advanced_setting_save", "caption_entry",
                        ImageRecommendationsEvent.getActionDataString(filename = selectedImage.prefixedText, recommendationSource = viewModel.selectedImageSource,
                            recommendationSourceProjects = viewModel.selectedImageSourceProjects, acceptanceState = "accepted"), selectedImage.wikiSite.languageCode)
                }
                onBackPressed()
                true
            }
            R.id.menu_insert -> {
                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE && selectedImage != null) {
                    ImageRecommendationsEvent.logAction("caption_continue", "caption_entry",
                        ImageRecommendationsEvent.getActionDataString(filename = selectedImage.prefixedText, recommendationSource = viewModel.selectedImageSource,
                            recommendationSourceProjects = viewModel.selectedImageSourceProjects, acceptanceState = "accepted",
                            captionAdd = insertMediaSettingsFragment.captionText.isNotEmpty(), altTextAdd = insertMediaSettingsFragment.alternativeText.isNotEmpty()
                        ), selectedImage.wikiSite.languageCode
                    )
                }
                selectedImage?.let {
                    val intent = Intent()
                        .putExtra(EXTRA_IMAGE_TITLE, it)
                        .putExtra(RESULT_IMAGE_CAPTION, insertMediaSettingsFragment.captionText)
                        .putExtra(RESULT_IMAGE_ALT, insertMediaSettingsFragment.alternativeText)
                        .putExtra(RESULT_IMAGE_TYPE, viewModel.imageType)
                        .putExtra(RESULT_IMAGE_POS, viewModel.imagePosition)
                        .putExtra(RESULT_IMAGE_SIZE, viewModel.imageSize)
                    setResult(RESULT_INSERT_MEDIA_SUCCESS, intent)
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (insertMediaSettingsFragment.handleBackPressed()) {
            if (insertMediaAdapter != null) {
                binding.imageInfoContainer.isVisible = true
                binding.searchContainer.isVisible = true
                supportActionBar?.title = getString(R.string.insert_media_title)
                adjustRefreshViewLayoutParams(false)
            } else {
                finish()
            }
            return
        }
        if (insertMediaAdvancedSettingsFragment.handleBackPressed()) {
            insertMediaSettingsFragment.show()
            return
        }
        super.onBackPressed()
    }

    private fun adjustRefreshViewLayoutParams(removeLayoutBehavior: Boolean) {
        binding.scrollableContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior = if (removeLayoutBehavior) null else AppBarLayout.ScrollingViewBehavior()
            topMargin = if (removeLayoutBehavior) DimenUtil.getToolbarHeightPx(this@InsertMediaActivity) else 0
        }
        (binding.toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = if (removeLayoutBehavior) AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL else
            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
    }

    private fun applyActionBarButtonStyle(menuItem: MenuItem, emphasize: Boolean) {
        val actionBarButtonBinding = ItemEditActionbarButtonBinding.inflate(layoutInflater)
        menuItem.actionView = actionBarButtonBinding.root
        actionBarButtonBinding.editActionbarButtonText.text = menuItem.title
        actionBarButtonBinding.editActionbarButtonText.setTextColor(
            ResourceUtil.getThemedColor(this,
                if (emphasize) R.attr.progressive_color else R.attr.placeholder_color))
        actionBarButtonBinding.root.tag = menuItem
        actionBarButtonBinding.root.isEnabled = menuItem.isEnabled
        actionBarButtonBinding.root.setOnClickListener { onOptionsItemSelected(it.tag as MenuItem) }
    }

    private fun showMediaSettingsFragment() {
        binding.imageInfoContainer.isVisible = false
        binding.searchContainer.isVisible = false
        binding.progressBar.isVisible = false
        insertMediaSettingsFragment.show()
    }

    fun showMediaAdvancedSettingsFragment() {
        binding.imageInfoContainer.isVisible = false
        binding.searchContainer.isVisible = false
        insertMediaSettingsFragment.hide(false)
        insertMediaAdvancedSettingsFragment.show()
    }

    private fun showSelectedImage() {
        binding.toolbarContainer.setExpanded(true)
        viewModel.selectedImage?.let {
            ImageZoomHelper.setViewZoomable(binding.selectedImage)
            binding.emptyImageContainer.isVisible = false
            binding.selectedImageContainer.isVisible = true
            binding.progressBar.isVisible = true
            binding.selectedImage.loadImage(
                Uri.parse(ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl!!, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)),
                roundedCorners = false, cropped = false, emptyPlaceholder = true, listener = object : FaceAndColorDetectImageView.OnImageLoadListener {
                    override fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int) {
                        if (!isDestroyed) {
                            val params = binding.imageInfoButton.layoutParams as FrameLayout.LayoutParams
                            val containerAspect = binding.imageViewContainer.width.toFloat() / binding.imageViewContainer.height.toFloat()
                            val bmpAspect = bmpWidth.toFloat() / bmpHeight.toFloat()

                            if (bmpAspect > containerAspect) {
                                params.marginEnd = DimenUtil.roundedDpToPx(8f)
                            } else {
                                val width = binding.imageViewContainer.height.toFloat() * bmpAspect
                                params.marginEnd = DimenUtil.roundedDpToPx(8f) + (binding.imageViewContainer.width / 2 - width.toInt() / 2)
                            }
                            binding.imageInfoButton.layoutParams = params
                            binding.progressBar.isVisible = false
                        }
                    }

                    override fun onImageFailed() {
                        if (!isDestroyed) {
                            binding.progressBar.isVisible = false
                        }
                    }
                })

            binding.selectedImageContainer.setOnClickListener { _ ->
                it.wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)
                startActivity(FilePageActivity.newIntent(this@InsertMediaActivity, it))
            }
        } ?: run {
            binding.emptyImageContainer.isVisible = true
            binding.selectedImageContainer.isVisible = false
        }
    }

    private inner class InsertMediaDiffCallback : DiffUtil.ItemCallback<PageTitle>() {
        override fun areItemsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem.prefixedText == newItem.prefixedText && oldItem.namespace == newItem.namespace
        }
    }

    private inner class InsertMediaAdapter : PagingDataAdapter<PageTitle, RecyclerView.ViewHolder>(InsertMediaDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): InsertMediaItemHolder {
            return InsertMediaItemHolder(ItemInsertMediaBinding.inflate(layoutInflater))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as InsertMediaItemHolder).bindItem(it)
            }
        }
    }

    private inner class InsertMediaItemHolder(val binding: ItemInsertMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItem(pageTitle: PageTitle) {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, pageTitle.thumbUrl)
            binding.imageDescription.text = StringUtil.removeHTMLTags(pageTitle.description.orEmpty().ifEmpty { pageTitle.displayText })

            binding.selectedIcon.isVisible = pageTitle == viewModel.selectedImage

            binding.root.setOnClickListener {
                viewModel.selectedImage = if (pageTitle == viewModel.selectedImage) null else pageTitle
                actionMode?.finish()
                showSelectedImage()
                invalidateOptionsMenu()
                insertMediaAdapter?.notifyDataSetChanged()
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
            searchActionProvider?.setQueryText(viewModel.searchQuery)
            searchActionProvider?.selectAllQueryTexts()
            val menuItem = menu.add(searchHintString)
            MenuItemCompat.setActionProvider(menuItem, searchActionProvider)
            actionMode = mode
            binding.imageInfoContainer.isVisible = false
            binding.searchContainer.isVisible = false
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.searchQuery = s.ifEmpty { viewModel.originalSearchQuery }
            insertMediaAdapter?.refresh()
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
        const val EXTRA_IMAGE_TITLE = "imageTitle"
        const val EXTRA_IMAGE_SOURCE = "imageSource"
        const val EXTRA_IMAGE_SOURCE_PROJECTS = "imageSourceProjects"
        const val EXTRA_ATTEMPT_INSERT_INTO_INFOBOX = "attemptInsertIntoInfobox"
        const val EXTRA_INSERTED_INTO_INFOBOX = "insertedIntoInfobox"
        const val RESULT_IMAGE_CAPTION = "resultImageCaption"
        const val RESULT_IMAGE_ALT = "resultImageAlt"
        const val RESULT_IMAGE_SIZE = "resultImageSize"
        const val RESULT_IMAGE_TYPE = "resultImageType"
        const val RESULT_IMAGE_POS = "resultImagePos"
        const val RESULT_INSERT_MEDIA_SUCCESS = 100

        fun newIntent(context: Context, wikiSite: WikiSite, searchQuery: String,
                      invokeSource: Constants.InvokeSource, imageTitle: PageTitle? = null,
                      imageSource: String = "", imageSourceProjects: String = ""): Intent {
            return Intent(context, InsertMediaActivity::class.java)
                .putExtra(Constants.ARG_WIKISITE, wikiSite)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
                .putExtra(EXTRA_IMAGE_TITLE, imageTitle)
                .putExtra(EXTRA_IMAGE_SOURCE, imageSource)
                .putExtra(EXTRA_IMAGE_SOURCE_PROJECTS, imageSourceProjects)
                .putExtra(EXTRA_SEARCH_QUERY, searchQuery)
        }
    }
}
