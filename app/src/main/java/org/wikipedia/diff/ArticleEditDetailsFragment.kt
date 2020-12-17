package org.wikipedia.diff

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_article_edit_details.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_ARTICLE_TITLE
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_LANGUAGE_CODE
import org.wikipedia.diff.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_REVISION_ID
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.GsonUtil
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.TalkTopicsActivity.Companion.newIntent
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog


class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback {
    private lateinit var articleTitle: String
    private var revisionId: Long = 0
    private var username: String? = null
    private var newerRevisionId: Long = 0
    private var olderRevisionId: Long = 0
    private lateinit var languageCode: String
    private val disposables = CompositeDisposable()
    private var menu: Menu? = null
    private var watchlistExpiryChanged = false
    private var isWatched = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_edit_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        articleTitle = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_ARTICLE_TITLE), "")
        revisionId = requireActivity().intent
                .getLongExtra(EXTRA_SOURCE_EDIT_REVISION_ID, 0)
        languageCode = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_EDIT_LANGUAGE_CODE), "en")
        articleTitleView.text = articleTitle
        articleTitleView.setOnClickListener {
            val title = PageTitle(articleTitle, WikiSite.forLanguageCode(languageCode))
            if (title.namespace() == Namespace.USER_TALK || title.namespace() == Namespace.TALK) {
                startActivity(newIntent(requireContext(), title.pageTitleForTalkPage()))
            } else {
                startActivity(PageActivity.newIntentForNewTab(requireContext(),
                        HistoryEntry(title, HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY), title))
            }
        }
        newerButton.setOnClickListener {
            revisionId = newerRevisionId
            fetchNeighborEdits()
        }
        olderButton.setOnClickListener {
            revisionId = olderRevisionId
            fetchNeighborEdits()
        }
        watchButton.setOnClickListener {
            watchOrUnwatchTitle(WatchlistExpiry.NEVER, isWatched)
        }
        usernameButton.setOnClickListener {
            if (AccountUtil.isLoggedIn() && username != null) {
                startActivity(newIntent(requireActivity(),
                        PageTitle(UserTalkAliasData.valueFor(languageCode),
                                username!!, WikiSite.forLanguageCode(languageCode))))
            }
        }
        thankButton.setOnClickListener { showThankDialog() }
        fetchNeighborEdits()
        highlightDiffText()
    }

    private fun setButtonTextAndIconColor(view: MaterialButton, themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = (ColorStateList.valueOf(themedColor))
    }

    private fun watchOrUnwatchTitle(expiry: WatchlistExpiry, unwatch: Boolean) {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).watchToken
                .subscribeOn(Schedulers.io())
                .flatMap { response: MwQueryResponse ->
                    val watchToken = response.query()!!.watchToken()
                    if (TextUtils.isEmpty(watchToken)) {
                        throw RuntimeException("Received empty watch token: " + GsonUtil.getDefaultGson().toJson(response))
                    }
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postWatch(if (unwatch) 1 else null, null, articleTitle,
                            if (expiry != null) expiry.expiry else null, watchToken!!)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ watchPostResponse: WatchPostResponse ->
                    val firstWatch = watchPostResponse.getFirst()
                    isWatched = firstWatch!!.watched
                    if (firstWatch != null) {
                        // Reset to make the "Change" button visible.
                        if (watchlistExpiryChanged && unwatch) {
                            watchlistExpiryChanged = false
                        }
                        showWatchlistSnackbar(expiry, firstWatch)
                    }
                }) { t: Throwable? -> L.d(t) })
    }

    private fun showWatchlistSnackbar(expiry: WatchlistExpiry, watch: Watch) {
        if (watch.unwatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, articleTitle))
            setButtonTextAndIconColor(watchButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
            watchButton.text = getString(R.string.watchlist_details_watch_label)
        } else if (watch.watched && expiry != null) {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            articleTitle,
                            getString(expiry.stringId)),
                    FeedbackUtil.LENGTH_DEFAULT)
            if (!watchlistExpiryChanged) {
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) { view ->
                    watchlistExpiryChanged = true
                    bottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(expiry))
                }
            }
            snackbar.show()
            setButtonTextAndIconColor(watchButton, ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_62))
            watchButton.text = getString(R.string.watchlist_details_watching_label)
        }
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog: AlertDialog = AlertDialog.Builder(activity)
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    sendThanks()
                }
                .setNegativeButton(R.string.thank_dialog_negative_button_text, null)
                .create()
        dialog.layoutInflater.inflate(R.layout.view_thank_dialog, parent)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.secondary_text_color))
        }
        dialog.show()
    }

    private fun sendThanks() {
        ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).csrfToken
                .subscribeOn(Schedulers.io())
                .flatMap { response: MwQueryResponse ->
                    val csrfToken = response.query()!!.csrfToken()
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postThanksToRevision(revisionId, csrfToken!!)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    FeedbackUtil.showMessage(activity, getString(R.string.thank_success_message, username))
                    setButtonTextAndIconColor(thankButton, ResourceUtil.getThemedColor(requireContext(),
                            R.attr.material_theme_de_emphasised_color))
                    thankButton.isClickable = false
                })
                { Consumer { t: Throwable? -> L.e(t) } }
    }

    private fun fetchNeighborEdits() {
        fetchEditDetails()
    }

    private fun updateUI(@NonNull currentRevision: Revision) {
        usernameButton.text = currentRevision.user
        editTimestamp.text = DateUtil.getDateAndTimeStringFromTimestampString(currentRevision.timeStamp())

        newerButton.isClickable = newerRevisionId.compareTo(-1) != 0
        olderButton.isClickable = olderRevisionId.compareTo(0) != 0
        ImageViewCompat.setImageTintList(newerButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (newerRevisionId.compareTo(-1) == 0)
                    R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        ImageViewCompat.setImageTintList(olderButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (olderRevisionId.compareTo(0) == 0)
                    R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        menu?.findItem(R.id.menu_user_profile_page)?.title = getString(R.string.menu_option_user_profile, currentRevision.user)
        menu?.findItem(R.id.menu_user_contributions_page)?.title = getString(R.string.menu_option_user_contributions, currentRevision.user)
        setButtonTextAndIconColor(thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
        thankButton.isClickable = true
        updateWatchlistButtonUI()
    }

    private fun updateWatchlistButtonUI() {
        ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getWatchedInfo(articleTitle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    isWatched = it.query()!!.firstPage()!!.isWatched

                    setButtonTextAndIconColor(watchButton, ResourceUtil.getThemedColor(requireContext(),
                            if (isWatched) R.attr.color_group_62 else R.attr.colorAccent))
                    watchButton.text = getString(if (isWatched) R.string.watchlist_details_watching_label
                    else R.string.watchlist_details_watch_label)
                })
                { Consumer { t: Throwable? -> L.e(t) } }
    }

    private fun fetchEditDetails() {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getRevisionDetails(articleTitle, revisionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val currentRevision = response.query()!!.firstPage()!!.revisions()[0]
                    username = currentRevision.user
                    newerRevisionId = if (response.query()!!.firstPage()!!.revisions().size < 2) {
                        -1
                    } else {
                        response.query()!!.firstPage()!!.revisions()[1].revId
                    }
                    olderRevisionId = currentRevision.parentRevId

                    updateUI(currentRevision)
                }) { t: Throwable? -> L.e(t) })

    }

    private fun highlightDiffText() {
        val spannableString = SpannableString(diffText.text.toString())
        spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                R.attr.color_group_57)), 0, diffText.text.length - 1, 0)
        diffText.text = spannableString
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
        this.menu = menu
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                ShareUtil.shareText(requireContext(), PageTitle(articleTitle, WikiSite.forLanguageCode(languageCode)), revisionId, olderRevisionId)
                true
            }
            R.id.menu_user_profile_page -> {
                FeedbackUtil.showUserProfilePage(requireContext(), username)
                true
            }
            R.id.menu_user_contributions_page -> {
                FeedbackUtil.showUserContributionsPage(requireContext(), username)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance(): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment()
        }
    }

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        watchOrUnwatchTitle(expiry, false)
        bottomSheetPresenter.dismiss(childFragmentManager)
    }
}
