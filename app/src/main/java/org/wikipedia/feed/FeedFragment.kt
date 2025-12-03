package org.wikipedia.feed

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import java.time.LocalDate

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter<View>
    private val feedCallback = FeedCallback()
    private val feedScrollListener = FeedScrollListener()
    private val callback get() = getCallback(this, Callback::class.java)
    private var shouldElevateToolbar = false

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
            viewModel.updateHiddenCards()
            refresh()
        }
    }

    private val requestLanguageChangeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED) {
            refresh()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentFeedBinding.inflate(inflater, container, false)

        feedAdapter = FeedAdapter(viewModel.getCoordinator(), feedCallback)
        binding.feedView.adapter = feedAdapter
        binding.feedView.addOnScrollListener(feedScrollListener)

        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
        binding.customizeButton.setOnClickListener { showConfigureActivity(-1) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cards.collect { cards ->
                        feedAdapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefreshLayout.isRefreshing = isLoading
                    }
                }

                launch {
                    viewModel.isEmpty.collect { isEmpty ->
                        binding.emptyContainer.isVisible = isEmpty
                    }
                }
            }
        }

        viewModel.loadInitialFeed()

        callback?.updateToolbarElevation(shouldElevateToolbar())
        ReadingListSyncAdapter.manualSync()
        Prefs.incrementExploreFeedVisitCount()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        maybeShowRegionalLanguageVariantDialog()
        OnThisDayGameMainMenuFragment.maybeShowOnThisDayGameDialog(requireActivity(), InvokeSource.FEED)
        feedAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        binding.swipeRefreshLayout.setOnRefreshListener(null)
        binding.feedView.removeOnScrollListener(feedScrollListener)
        binding.feedView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    fun shouldElevateToolbar(): Boolean {
        return shouldElevateToolbar
    }

    fun scrollToTop() {
        binding.feedView.smoothScrollToPosition(0)
    }

    fun onGoOffline() {
        feedAdapter.notifyDataSetChanged()
        viewModel.requestOfflineCard()
    }

    fun onGoOnline() {
        feedAdapter.notifyDataSetChanged()
        viewModel.removeOfflineCard()
        viewModel.loadMore()
    }

    fun refresh() {
        viewModel.refresh()
    }

    fun updateHiddenCards() {
        viewModel.updateHiddenCards()
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
            val position = viewModel.dismissCard(card)
            if (position < 0) return false
            showDismissCardUndoSnackbar(card, position)
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
            viewModel.undoDismissCard(card, position)
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

        fun maybeShowExploreFeedSurvey(activity: Activity) {
            if (Prefs.exploreFeedSurveyShown || WikipediaApp.instance.languageState.systemLanguageCode != "en") return

            val currentDate = LocalDate.now()
            val startDate = LocalDate.of(2025, 11, 24)
            val endDate = LocalDate.of(2025, 11, 30)

            if (currentDate !in startDate..endDate) {
                return
            }

            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.explore_feed_survey_dialog_title)
                .setMessage(R.string.explore_feed_survey_dialog_message)
                .setPositiveButton(R.string.explore_feed_survey_dialog_positive_button_label) { _, _ ->
                    UriUtil.handleExternalLink(activity, activity.getString(R.string.explore_feed_survey_url).toUri())
                }
                .setNegativeButton(R.string.explore_feed_survey_dialog_negative_button_label) { _, _ -> }
                .show()
            Prefs.exploreFeedSurveyShown = true
        }
    }
}
