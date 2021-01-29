package org.wikipedia.diff

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_article_edit_details.*
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.analytics.WatchlistFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.DiffResponse.*
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.GsonUtil
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import java.nio.charset.StandardCharsets

class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback, LinkPreviewDialog.Callback {
    private lateinit var articlePageTitle: PageTitle
    private lateinit var languageCode: String
    private var revisionId: Long = 0
    private var diffSize: Int = 0
    private var username: String? = null
    private var newerRevisionId: Long = 0
    private var olderRevisionId: Long = 0
    private var currentRevision: Revision? = null

    private var watchlistExpiryChanged = false
    private var isWatched = false
    private var watchlistExpirySession = WatchlistExpiry.NEVER
    private val watchlistFunnel = WatchlistFunnel()

    private val disposables = CompositeDisposable()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_edit_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        revisionId = requireArguments().getLong(EXTRA_EDIT_REVISION_ID, 0)
        languageCode = requireArguments().getString(EXTRA_EDIT_LANGUAGE_CODE, AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE)
        articlePageTitle = PageTitle(requireArguments().getString(EXTRA_ARTICLE_TITLE, ""),
                WikiSite.forLanguageCode(languageCode))
        diffSize = requireArguments().getInt(EXTRA_EDIT_SIZE, 0)
        setUpInitialUI()
        setUpListeners()
        fetchEditDetails()
    }

    private fun setUpListeners() {
        articleTitleView.setOnClickListener {
            if (articlePageTitle.namespace() == Namespace.USER_TALK || articlePageTitle.namespace() == Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(requireContext(), articlePageTitle.pageTitleForTalkPage(), InvokeSource.DIFF_ACTIVITY))
            } else {
                bottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(
                        HistoryEntry(articlePageTitle, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS), null))
            }
        }
        newerIdButton.setOnClickListener {
            revisionId = newerRevisionId
            fetchEditDetails()
        }
        olderIdButton.setOnClickListener {
            revisionId = olderRevisionId
            fetchEditDetails()
        }
        watchButton.setOnClickListener {
            if (isWatched) {
                watchlistFunnel.logRemoveArticle()
            } else {
                watchlistFunnel.logAddArticle()
            }
            watchButton.isCheckable = false
            watchOrUnwatchTitle(watchlistExpirySession, isWatched)
        }
        usernameButton.setOnClickListener {
            if (AccountUtil.isLoggedIn && username != null) {
                startActivity(TalkTopicsActivity.newIntent(requireActivity(),
                        PageTitle(UserTalkAliasData.valueFor(languageCode),
                                username!!, WikiSite.forLanguageCode(languageCode)), InvokeSource.DIFF_ACTIVITY))
            }
        }
        thankButton.setOnClickListener { showThankDialog() }
        errorView.setBackClickListener { requireActivity().finish() }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        errorView.setError(t)
        errorView.visibility = VISIBLE
        revisionDetailsView.visibility = GONE
    }

    private fun setUpInitialUI() {
        diffText.movementMethod = ScrollingMovementMethod()
        articleTitleView.text = articlePageTitle.displayText
        if (diffSize >= 0) {
            diffCharacterCountView.setTextColor(if (diffSize > 0) ContextCompat.getColor(requireContext(),
                    R.color.green50) else ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_secondary_color))
            diffCharacterCountView.text = String.format("%+d", diffSize)
        } else {
            diffCharacterCountView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red50))
            diffCharacterCountView.text = String.format("%+d", diffSize)
        }
    }

    private fun fetchEditDetails() {
        hideOrShowViews(true)
        disposables.add(Observable.zip(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getRevisionDetails(articlePageTitle.prefixedText, revisionId),
                ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getWatchedInfo(articlePageTitle.prefixedText), { r, w ->
            isWatched = w.query()!!.firstPage()!!.isWatched
            if (r.query() == null || r.query()!!.firstPage() == null) {
                throw RuntimeException("Received empty response page: " + GsonUtil.getDefaultGson().toJson(r))
            }
            val firstPage = r.query()!!.firstPage()!!
            currentRevision = firstPage.revisions()[0]
            username = currentRevision!!.user
            newerRevisionId = if (firstPage.revisions().size < 2) {
                -1
            } else {
                firstPage.revisions()[1].revId
            }
            olderRevisionId = currentRevision!!.parentRevId
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateUI()
                }) { setErrorState(it!!) })
    }

    private fun hideOrShowViews(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = VISIBLE
            userDetailsFlowView.visibility = INVISIBLE
            editComment.visibility = INVISIBLE
            diffText.visibility = INVISIBLE
        } else {
            progressBar.visibility = INVISIBLE
            userDetailsFlowView.visibility = VISIBLE
            editComment.visibility = VISIBLE
            diffText.visibility = VISIBLE
        }
    }

    private fun updateUI() {
        diffText.scrollTo(0, 0)
        diffText.text = ""
        usernameButton.text = currentRevision!!.user
        editTimestamp.text = DateUtil.getDateAndTimeStringFromTimestampString(currentRevision!!.timeStamp())
        editComment.text = currentRevision!!.comment
        newerIdButton.isClickable = newerRevisionId != -1L
        olderIdButton.isClickable = olderRevisionId != 0L
        setEnableDisableTint(newerIdButton, newerRevisionId == -1L)
        setEnableDisableTint(olderIdButton, olderRevisionId == 0L)
        setButtonTextAndIconColor(thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
        thankButton.isClickable = true
        updateWatchlistButtonUI()
        fetchDiffText()
        requireActivity().invalidateOptionsMenu()
        maybeHideThankButton()
        hideOrShowViews(false)
    }

    private fun maybeHideThankButton() {
        thankButton.visibility = if (AccountUtil.userName.equals(currentRevision?.user)) GONE else VISIBLE
    }

    private fun setEnableDisableTint(view: AppCompatImageView, isDisabled: Boolean) {
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (isDisabled)
                    R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
    }

    private fun setButtonTextAndIconColor(view: MaterialButton, themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = ColorStateList.valueOf(themedColor)
    }

    private fun watchOrUnwatchTitle(@Nullable expiry: WatchlistExpiry?, unwatch: Boolean) {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).watchToken
                .subscribeOn(Schedulers.io())
                .flatMap { response: MwQueryResponse ->
                    val watchToken = response.query()!!.watchToken()
                    if (TextUtils.isEmpty(watchToken)) {
                        throw RuntimeException("Received empty watch token: " + GsonUtil.getDefaultGson().toJson(response))
                    }
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postWatch(if (unwatch) 1 else null, null, articlePageTitle.prefixedText,
                            expiry?.expiry, watchToken!!)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ watchPostResponse: WatchPostResponse ->
                    val firstWatch = watchPostResponse.getFirst()
                    if (firstWatch != null) {
                        isWatched = firstWatch.watched
                        updateWatchlistButtonUI()
                        // Reset to make the "Change" button visible.
                        if (watchlistExpiryChanged && unwatch) {
                            watchlistExpiryChanged = false
                        }

                        if (unwatch) {
                            watchlistFunnel.logRemoveSuccess()
                        } else {
                            watchlistFunnel.logAddSuccess()
                        }

                        showWatchlistSnackbar(expiry, firstWatch)
                    }
                }) { t: Throwable? ->
                    L.d(t)
                    watchButton.isCheckable = true
                })
    }

    private fun updateWatchlistButtonUI() {
        setButtonTextAndIconColor(watchButton, ResourceUtil.getThemedColor(requireContext(),
                if (isWatched) R.attr.color_group_68 else R.attr.colorAccent))
        watchButton.text = getString(if (isWatched) R.string.watchlist_details_watching_label else R.string.watchlist_details_watch_label)
    }

    private fun showWatchlistSnackbar(@Nullable expiry: WatchlistExpiry?, watch: Watch) {
        isWatched = watch.watched
        if (watch.unwatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, articlePageTitle.displayText))
            watchlistExpirySession = WatchlistExpiry.NEVER
        } else if (watch.watched && expiry != null) {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            articlePageTitle.displayText,
                            getString(expiry.stringId)),
                    FeedbackUtil.LENGTH_DEFAULT)
            if (!watchlistExpiryChanged) {
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                    watchlistExpiryChanged = true
                    bottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(expiry))
                }
            }
            watchlistExpirySession = expiry
            snackbar.show()
        }
        watchButton.isCheckable = true
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
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).csrfToken
                .subscribeOn(Schedulers.io())
                .flatMap {
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postThanksToRevision(revisionId, it.query()!!.csrfToken()!!)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    FeedbackUtil.showMessage(activity, getString(R.string.thank_success_message, username))
                    setButtonTextAndIconColor(thankButton, ResourceUtil.getThemedColor(requireContext(),
                            R.attr.material_theme_de_emphasised_color))
                    thankButton.isClickable = false
                }) { t: Throwable? -> L.e(t) })
    }

    private fun fetchDiffText() {
        disposables.add(ServiceFactory.getCoreRest(WikiSite.forLanguageCode(languageCode)).getDiff(olderRevisionId, revisionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    displayDiffs(it.diffs)
                }) { t: Throwable? -> L.e(t) })
    }

    private fun displayDiffs(diffs: List<DiffItem>) {
        val spannableString = SpannableStringBuilder()
        for (diff in diffs) {
            val prefixLength = spannableString.length
            spannableString.append(if (diff.text.isNotEmpty()) diff.text else "\n")
            when (diff.type) {
                DIFF_TYPE_LINE_ADDED -> {
                    updateDiffTextDecor(spannableString, true, prefixLength, prefixLength + diff.text.length)
                }
                DIFF_TYPE_LINE_REMOVED -> {
                    updateDiffTextDecor(spannableString, false, prefixLength, prefixLength + diff.text.length)
                }
                DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                    updateDiffTextDecor(spannableString, false, prefixLength, prefixLength + diff.text.length)
                }
                DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                    updateDiffTextDecor(spannableString, true, prefixLength, prefixLength + diff.text.length)
                }
            }
            if (diff.highlightRanges.isNotEmpty()) {
                for (highlightRange in diff.highlightRanges) {
                    val indices = utf8Indices(diff.text)
                    val highlightRangeStart = indices[highlightRange.start]
                    val highlightRangeEnd = if (highlightRange.start + highlightRange.length < indices.size) indices[highlightRange.start + highlightRange.length] else indices[indices.size - 1]

                    if (highlightRange.type == HIGHLIGHT_TYPE_ADD) {
                        updateDiffTextDecor(spannableString, true, prefixLength + highlightRangeStart, prefixLength + highlightRangeEnd)
                    } else {
                        updateDiffTextDecor(spannableString, false, prefixLength + highlightRangeStart, prefixLength + highlightRangeEnd)
                    }
                }
            }
            spannableString.append("\n")
        }
        diffText.text = spannableString
    }

    private fun updateDiffTextDecor(spannableText: SpannableStringBuilder, isAddition: Boolean, start: Int, end: Int) {
        val boldStyle = StyleSpan(Typeface.BOLD)
        val foregroundAddedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_64))
        val foregroundRemovedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_66))
        spannableText.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                if (isAddition) R.attr.color_group_65 else R.attr.color_group_67)), start, end, 0)
        spannableText.setSpan(boldStyle, start, end, 0)
        spannableText.setSpan(if (isAddition) foregroundAddedColor else foregroundRemovedColor, start, end, 0)
    }

    private fun utf8Indices(s: String): IntArray {
        val indices = IntArray(s.toByteArray(StandardCharsets.UTF_8).size)
        var ptr = 0
        var count = 0
        for (i in s.indices) {
            val c = s.codePointAt(i)
            when {
                c <= 0x7F -> count = 1
                c <= 0x7FF -> count = 2
                c <= 0xFFFF -> count = 3
                c <= 0x1FFFFF -> count = 4
            }
            for (j in 0 until count) {
                indices[ptr++] = i
            }
        }
        return indices
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_user_profile_page)?.title = getString(R.string.menu_option_user_profile, currentRevision?.user)
        menu.findItem(R.id.menu_user_contributions_page)?.title = getString(R.string.menu_option_user_contributions, currentRevision?.user)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                ShareUtil.shareText(requireContext(), PageTitle(articlePageTitle.prefixedText,
                        WikiSite.forLanguageCode(languageCode)), revisionId, olderRevisionId)
                true
            }
            R.id.menu_user_profile_page -> {
                FeedbackUtil.showUserProfilePage(requireContext(), username!!, languageCode)
                true
            }
            R.id.menu_user_contributions_page -> {
                FeedbackUtil.showUserContributionsPage(requireContext(), username!!, languageCode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_EDIT_REVISION_ID = "revisionId"
        const val EXTRA_EDIT_LANGUAGE_CODE = "languageCode"
        const val EXTRA_EDIT_SIZE = "diffSize"

        fun newInstance(articleTitle: String, revisionId: Long, languageCode: String, diffSize: Int): ArticleEditDetailsFragment {
            val articleEditDetailsFragment = ArticleEditDetailsFragment()
            val args = Bundle()
            args.putString(EXTRA_ARTICLE_TITLE, articleTitle)
            args.putLong(EXTRA_EDIT_REVISION_ID, revisionId)
            args.putString(EXTRA_EDIT_LANGUAGE_CODE, languageCode)
            args.putInt(EXTRA_EDIT_SIZE, diffSize)
            articleEditDetailsFragment.arguments = args
            return articleEditDetailsFragment
        }
    }

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        watchlistFunnel.logAddExpiry()
        watchOrUnwatchTitle(expiry, false)
        bottomSheetPresenter.dismiss(childFragmentManager)
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        if (inNewTab) {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
        } else {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
        }
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        copyLink(title.uri)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.show(childFragmentManager,
                AddToReadingListDialog.newInstance(title, InvokeSource.LINK_PREVIEW_MENU))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(requireContext(), title)
    }

    private fun copyLink(uri: String?) {
        setPlainText(requireContext(), null, uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }
}
