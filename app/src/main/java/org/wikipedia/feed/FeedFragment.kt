package org.wikipedia.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.BackPressedHandler
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.FeedFunnel
import org.wikipedia.databinding.FragmentFeedBinding
import org.wikipedia.feed.FeedCoordinatorBase.FeedUpdateListener
import org.wikipedia.feed.configure.ConfigureActivity
import org.wikipedia.feed.configure.ConfigureItemLanguageDialogView
import org.wikipedia.feed.configure.LanguageItemAdapter
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.feed.mostread.MostReadArticlesActivity
import org.wikipedia.feed.mostread.MostReadListCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItemView
import org.wikipedia.feed.random.RandomCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import java.util.*

class FeedFragment : Fragment(), BackPressedHandler {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var feedAdapter: FeedAdapter<View>
    private val feedCallback = FeedCallback()
    private val feedScrollListener = FeedScrollListener()
    private val callback get() = getCallback(this, Callback::class.java)
    private var app: WikipediaApp = WikipediaApp.getInstance()
    private var coordinator: FeedCoordinator = FeedCoordinator(app)
    private var funnel: FeedFunnel = FeedFunnel(app)
    private var shouldElevateToolbar = false

    interface Callback {
        fun onFeedSearchRequested(view: View)
        fun onFeedVoiceSearchRequested()
        fun onFeedSelectPage(entry: HistoryEntry, openInNewBackgroundTab: Boolean)
        fun onFeedSelectPageWithAnimation(entry: HistoryEntry, shareElements: Array<Pair<View, String>>)
        fun onFeedAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onFeedMovePageToList(sourceReadingList: Long, entry: HistoryEntry)
        fun onFeedNewsItemSelected(card: NewsCard, view: NewsItemView)
        fun onFeedSeCardFooterClicked()
        fun onFeedShareImage(card: FeaturedImageCard)
        fun onFeedDownloadImage(image: FeaturedImage)
        fun onFeaturedImageSelected(card: FeaturedImageCard)
        fun onLoginRequested()
        fun updateToolbarElevation(elevate: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coordinator.more(app.wikiSite)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        feedAdapter = FeedAdapter(coordinator, feedCallback)
        binding.feedView.adapter = feedAdapter
        binding.feedView.addOnScrollListener(feedScrollListener)
        binding.swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
        binding.customizeButton.setOnClickListener { showConfigureActivity(-1) }
        coordinator.setFeedUpdateListener(object : FeedUpdateListener {
            override fun insert(card: Card, pos: Int) {
                if (isAdded) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    feedAdapter.notifyItemInserted(pos)
                }
            }

            override fun remove(card: Card, pos: Int) {
                if (isAdded) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    feedAdapter.notifyItemRemoved(pos)
                }
            }

            override fun finished(shouldUpdatePreviousCard: Boolean) {
                if (!isAdded) {
                    return
                }
                if (feedAdapter.itemCount < 2) {
                    binding.emptyContainer.visibility = View.VISIBLE
                } else {
                    if (shouldUpdatePreviousCard) {
                        feedAdapter.notifyItemChanged(feedAdapter.itemCount - 1)
                    }
                }
            }
        })
        callback?.updateToolbarElevation(shouldElevateToolbar())
        ReadingListSyncAdapter.manualSync()
        Prefs.incrementExploreFeedVisitCount()
        return binding.root
    }

    private fun showRemoveChineseVariantPrompt() {
        if (app.language().appLanguageCodes.contains(AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE) &&
            app.language().appLanguageCodes.contains(AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE) &&
            Prefs.shouldShowRemoveChineseVariantPrompt()) {
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_of_remove_chinese_variants_from_app_lang_title)
                .setMessage(R.string.dialog_of_remove_chinese_variants_from_app_lang_text)
                .setPositiveButton(R.string.dialog_of_remove_chinese_variants_from_app_lang_edit) { _, _ -> showLanguagesActivity(InvokeSource.LANG_VARIANT_DIALOG) }
                .setNegativeButton(R.string.dialog_of_remove_chinese_variants_from_app_lang_no, null)
                .show()
        }
        Prefs.shouldShowRemoveChineseVariantPrompt(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        showRemoveChineseVariantPrompt()
        funnel.enter()

        // Explicitly invalidate the feed adapter, since it occasionally crashes the StaggeredGridLayout
        // on certain devices. (TODO: investigate further)
        feedAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        funnel.exit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_FEED_CONFIGURE &&
            resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED) {
            coordinator.updateHiddenCards()
            refresh()
        } else if (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS &&
            resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED ||
            requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE &&
            resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED) {
            refresh()
        }
    }

    override fun onDestroyView() {
        coordinator.setFeedUpdateListener(null)
        binding.swipeRefreshLayout.setOnRefreshListener(null)
        binding.feedView.removeOnScrollListener(feedScrollListener)
        binding.feedView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator.reset()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    fun shouldElevateToolbar(): Boolean {
        return shouldElevateToolbar
    }

    fun scrollToTop() {
        binding.feedView.smoothScrollToPosition(0)
    }

    fun onGoOffline() {
        feedAdapter.notifyDataSetChanged()
        coordinator.requestOfflineCard()
    }

    fun onGoOnline() {
        feedAdapter.notifyDataSetChanged()
        coordinator.removeOfflineCard()
        coordinator.incrementAge()
        coordinator.more(app.wikiSite)
    }

    fun refresh() {
        funnel.refresh(coordinator.age)
        binding.emptyContainer.visibility = View.GONE
        coordinator.reset()
        feedAdapter.notifyDataSetChanged()
        coordinator.more(app.wikiSite)
    }

    fun updateHiddenCards() {
        coordinator.updateHiddenCards()
    }

    private inner class FeedCallback : FeedAdapter.Callback {
        override fun onShowCard(card: Card?) {
            card?.let {
                funnel.cardShown(it.type(), getCardLanguageCode(it))
            }
        }

        override fun onRequestMore() {
            funnel.requestMore(coordinator.age)
            binding.feedView.post {
                if (isAdded) {
                    coordinator.incrementAge()
                    coordinator.more(app.wikiSite)
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
            callback?.let {
                it.onFeedSelectPage(entry, openInNewBackgroundTab)
                funnel.cardClicked(card.type(), getCardLanguageCode(card))
            }
        }

        override fun onSelectPage(card: Card, entry: HistoryEntry, sharedElements: Array<Pair<View, String>>) {
            callback?.let {
                it.onFeedSelectPageWithAnimation(entry, sharedElements)
                funnel.cardClicked(card.type(), getCardLanguageCode(card))
            }
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
            val position = coordinator.dismissCard(card)
            if (position < 0) {
                return false
            }
            funnel.dismissCard(card.type(), position)
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
            callback?.let {
                it.onFeedNewsItemSelected(card, view)
                funnel.cardClicked(card.type(), card.wikiSite().languageCode())
            }
        }

        override fun onShareImage(card: FeaturedImageCard) {
            callback?.onFeedShareImage(card)
        }

        override fun onDownloadImage(image: FeaturedImage) {
            callback?.onFeedDownloadImage(image)
        }

        override fun onFeaturedImageSelected(card: FeaturedImageCard) {
            callback?.let {
                it.onFeaturedImageSelected(card)
                funnel.cardClicked(card.type(), null)
            }
        }

        override fun onAnnouncementPositiveAction(card: Card, uri: Uri) {
            funnel.cardClicked(card.type(), getCardLanguageCode(card))
            when {
                uri.toString() == UriUtil.LOCAL_URL_LOGIN -> callback?.onLoginRequested()
                uri.toString() == UriUtil.LOCAL_URL_SETTINGS -> startActivityForResult(SettingsActivity.newIntent(requireContext()), Constants.ACTIVITY_REQUEST_SETTINGS)
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
            if (card is MostReadListCard) {
                startActivity(MostReadArticlesActivity.newIntent(requireContext(), card))
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
        val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed), FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.feed_undo_dismiss_card) { coordinator.undoDismissCard(card, position) }
        snackbar.show()
    }

    private fun showCardLangSelectDialog(card: Card) {
        val contentType = card.type().contentType()
        if (contentType.isPerLanguage) {
            val adapter = LanguageItemAdapter(requireContext(), contentType)
            val view = ConfigureItemLanguageDialogView(requireContext())
            val tempDisabledList = ArrayList(contentType.langCodesDisabled)
            view.setContentType(adapter.langList, tempDisabledList)
            AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(contentType.titleId)
                .setPositiveButton(R.string.feed_lang_selection_dialog_ok_button_text) { _, _ ->
                    contentType.langCodesDisabled.clear()
                    contentType.langCodesDisabled.addAll(tempDisabledList)
                    refresh()
                }
                .setNegativeButton(R.string.feed_lang_selection_dialog_cancel_button_text, null)
                .create()
                .show()
        }
    }

    private fun showConfigureActivity(invokeSource: Int) {
        startActivityForResult(ConfigureActivity.newIntent(requireActivity(), invokeSource), Constants.ACTIVITY_REQUEST_FEED_CONFIGURE)
    }

    private fun showLanguagesActivity(invokeSource: InvokeSource) {
        startActivityForResult(WikipediaLanguagesActivity.newIntent(requireActivity(), invokeSource), Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE)
    }

    private fun getCardLanguageCode(card: Card?): String? {
        return if (card is WikiSiteCard) card.wikiSite().languageCode() else null
    }

    companion object {
        fun newInstance(): FeedFragment {
            return FeedFragment().apply {
                retainInstance = true
            }
        }
    }
}
