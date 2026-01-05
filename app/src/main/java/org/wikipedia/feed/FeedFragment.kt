package org.wikipedia.feed

import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.databinding.FragmentFeedBinding
import org.wikipedia.feed.configure.ConfigureActivity
import org.wikipedia.feed.configure.ConfigureItemLanguageDialogView
import org.wikipedia.feed.configure.LanguageItemAdapter
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItemView
import org.wikipedia.feed.random.RandomCardView
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.RegionalLanguageVariantSelectionDialog
import org.wikipedia.games.onthisday.OnThisDayGameMainMenuFragment
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.random.RandomActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class FeedFragment : Fragment() {
    interface Callback {
        fun onFeedSearchRequested(view: View)
        fun onFeedVoiceSearchRequested()
        fun onFeedSelectPage(entry: HistoryEntry, openInNewBackgroundTab: Boolean)
        fun onFeedSelectPageWithAnimation(entry: HistoryEntry, sharedElements: Array<Pair<View, String>>)
        fun onFeedAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onFeedMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
        fun onFeedNewsItemSelected(card: NewsCard, view: NewsItemView)
        fun onFeedSeCardFooterClicked()
        fun onFeedShareImage(card: FeaturedImageCard)
        fun onFeedDownloadImage(image: FeaturedImage)
        fun onFeaturedImageSelected(card: FeaturedImageCard)
        fun onLoginRequested()
        fun updateToolbarElevation(elevate: Boolean)
    }

    private val requestFeedConfigurationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED) {
            refresh()
        }
    }

    private val requestLanguageChangeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED) {
            refresh()
        }
    }

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedPagingAdapter
    private val feedCallback = FeedCallback()
    private val callback get() = getCallback(this, Callback::class.java)
    private var shouldElevateToolbar = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)

        // FeedPagingAdapter must extend PagingDataAdapter<Card, RecyclerView.ViewHolder>
        feedAdapter = FeedPagingAdapter()
        feedAdapter.callback = feedCallback
        binding.feedView.adapter = feedAdapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            feedAdapter.refresh() // trigger Paging to reload
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // submit paging data to adapter
                launch {
                    viewModel.pagingData.collectLatest { pagingData ->
                        feedAdapter.submitData(pagingData)
                    }
                }
                // drive swipe refresh by loadState
                launch {
                    feedAdapter.loadStateFlow.collectLatest { loadStates ->
                        val isRefreshing = when {
                            loadStates.refresh is LoadState.Loading -> true
                            loadStates.source.refresh is LoadState.Loading -> true
                            loadStates.mediator?.refresh is LoadState.Loading -> true
                            else -> false
                        }
                        binding.swipeRefreshLayout.isRefreshing = isRefreshing
                    }
                }
            }
        }

        // remove old manual trigger; paging will load automatically
        callback?.updateToolbarElevation(shouldElevateToolbar())
        return binding.root
    }

    override fun onDestroyView() {
        binding.swipeRefreshLayout.setOnRefreshListener(null)
        binding.feedView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        maybeShowRegionalLanguageVariantDialog()
        OnThisDayGameMainMenuFragment.maybeShowOnThisDayGameDialog(requireActivity(), InvokeSource.FEED)
        feedAdapter.notifyDataSetChanged()
    }

    fun shouldElevateToolbar(): Boolean {
        return shouldElevateToolbar
    }

    fun scrollToTop() {
        binding.feedView.smoothScrollToPosition(0)
    }

    fun onGoOffline() {
        feedAdapter.notifyDataSetChanged()
//        viewModel.requestOfflineCard()
    }

    fun onGoOnline() {
        feedAdapter.notifyDataSetChanged()
//        viewModel.removeOfflineCard()
        viewModel.loadMore()
    }

    fun refresh() {
//        viewModel.refresh()
    }

    fun updateHiddenCards() {
//        viewModel.updateHiddenCards()
    }

    private inner class FeedCallback : FeedAdapter.Callback {
        override fun onRequestMore() {
            binding.feedView.post {
                if (isAdded) {
                    viewModel.loadMore()
                }
            }
        }

        override fun onRetryFromOffline() {
            refresh()
        }

        override fun onError(t: Throwable) {
            FeedbackUtil.showError(requireActivity(), t)
        }

        override fun onSelectPage(card: Card, entry: HistoryEntry, openInNewBackgroundTab: Boolean) {
            callback?.onFeedSelectPage(entry, openInNewBackgroundTab)
        }

        override fun onSelectPage(card: Card, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>) {
            callback?.onFeedSelectPageWithAnimation(entry, sharedElements)
        }

        override fun onAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
            callback?.onFeedAddPageToList(entry, addToDefault)
        }

        override fun onMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
            callback?.onFeedMovePageToList(sourceReadingListId, entry)
        }

        override fun onSearchRequested(view: View) {
            callback?.onFeedSearchRequested(view)
        }

        override fun onVoiceSearchRequested() {
            callback?.onFeedVoiceSearchRequested()
        }

        override fun onRequestDismissCard(card: Card): Boolean {
//            val position = viewModel.dismissCard(card)
//            if (position < 0) return false
//            showDismissCardUndoSnackbar(card, position)
            return true
        }

        override fun onRequestEditCardLanguages(card: Card) {
            showCardLangSelectDialog(card)
        }

        override fun onRequestCustomize(card: Card) {
            showConfigureActivity(card.type().code())
        }

        override fun onNewsItemSelected(card: NewsCard, view: NewsItemView) {
            callback?.onFeedNewsItemSelected(card, view)
        }

        override fun onShareImage(card: FeaturedImageCard) {
            callback?.onFeedShareImage(card)
        }

        override fun onDownloadImage(image: FeaturedImage) {
            callback?.onFeedDownloadImage(image)
        }

        override fun onFeaturedImageSelected(card: FeaturedImageCard) {
            callback?.onFeaturedImageSelected(card)
        }

        override fun onAnnouncementPositiveAction(card: Card, uri: Uri) {
            when {
                uri.toString() == UriUtil.LOCAL_URL_LOGIN -> callback?.onLoginRequested()
                uri.toString() == UriUtil.LOCAL_URL_SETTINGS -> requestLanguageChangeLauncher.launch(SettingsActivity.newIntent(requireContext()))
                uri.toString() == UriUtil.LOCAL_URL_CUSTOMIZE_FEED -> {
                    showConfigureActivity(card.type().code())
                    onRequestDismissCard(card)
                }
                uri.toString() == UriUtil.LOCAL_URL_LANGUAGES -> showLanguagesActivity(InvokeSource.ANNOUNCEMENT)
                else -> UriUtil.handleExternalLink(requireContext(), uri)
            }
        }

        override fun onAnnouncementNegativeAction(card: Card) {
            onRequestDismissCard(card)
        }

        override fun onRandomClick(view: RandomCardView) {
            view.card?.let {
                startActivity(RandomActivity.newIntent(requireActivity(), it.wikiSite(), InvokeSource.FEED))
            }
        }

        override fun onFooterClick(card: Card) {
            if (card is TopReadListCard) {
                startActivity(TopReadArticlesActivity.newIntent(requireContext(), card))
            }
        }

        override fun onSeCardFooterClicked() {
            callback?.onFeedSeCardFooterClicked()
        }
    }

    private inner class FeedScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val shouldElevate = binding.feedView.firstVisibleItemPosition != 0
            if (shouldElevate != shouldElevateToolbar) {
                shouldElevateToolbar = shouldElevate
                requireActivity().invalidateOptionsMenu()
                callback?.updateToolbarElevation(shouldElevateToolbar())
            }
        }
    }

    private fun showDismissCardUndoSnackbar(card: Card, position: Int) {
        val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
        snackbar.setAction(R.string.reading_list_item_delete_undo) {
//            viewModel.undoDismissCard(card, position)
        }
        snackbar.show()
    }

    private fun showCardLangSelectDialog(card: Card) {
        val contentType = card.type().contentType()
        if (contentType != null && contentType.isPerLanguage) {
            val adapter = LanguageItemAdapter(requireContext(), contentType)
            val view = ConfigureItemLanguageDialogView(requireContext())
            val tempDisabledList = ArrayList(contentType.langCodesDisabled)
            view.setContentType(adapter.langList, tempDisabledList)
            MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(contentType.titleId)
                .setPositiveButton(R.string.feed_lang_selection_dialog_ok_button_text) { _, _ ->
                    contentType.langCodesDisabled.clear()
                    contentType.langCodesDisabled.addAll(tempDisabledList)
                    refresh()
                }
                .setNegativeButton(R.string.feed_lang_selection_dialog_cancel_button_text, null)
                .show()
        }
    }

    private fun maybeShowRegionalLanguageVariantDialog() {
        val deprecatedLanguageCodes = listOf(AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE, AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE)
        val primaryLanguage = WikipediaApp.instance.languageState.appLanguageCode
        val remainingLanguages = WikipediaApp.instance.languageState.appLanguageCodes.toMutableList().apply {
            remove(primaryLanguage)
        }
        if (deprecatedLanguageCodes.contains(primaryLanguage)) {
            val dialog = RegionalLanguageVariantSelectionDialog(requireContext()).show()
            dialog.setOnDismissListener {
                refresh()
            }
        } else if (remainingLanguages.any(deprecatedLanguageCodes::contains)) {
            MaterialAlertDialogBuilder(requireContext())
                .setCancelable(false)
                .setTitle(R.string.feed_language_variants_removal_secondary_dialog_title)
                .setMessage(R.string.feed_language_variants_removal_secondary_dialog_message)
                .setPositiveButton(R.string.feed_language_variants_removal_secondary_dialog_settings) { _, _ ->
                    val list = RegionalLanguageVariantSelectionDialog.removeNonRegionalLanguageVariants()
                    WikipediaApp.instance.languageState.setAppLanguageCodes(list)
                    refresh()
                    showLanguagesActivity(InvokeSource.FEED)
                }
                .show()
        }
    }

    private fun showConfigureActivity(invokeSource: Int) {
        requestFeedConfigurationLauncher.launch(ConfigureActivity.newIntent(requireActivity(), invokeSource))
    }

    private fun showLanguagesActivity(invokeSource: InvokeSource) {
        requestLanguageChangeLauncher.launch(WikipediaLanguagesActivity.newIntent(requireActivity(), invokeSource))
    }

    companion object {
        fun newInstance(): FeedFragment {
            return FeedFragment().apply {
                retainInstance = true
            }
        }
    }
}
