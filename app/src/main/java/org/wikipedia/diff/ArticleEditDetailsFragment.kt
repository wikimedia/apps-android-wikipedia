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
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.Key.CHARSET
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
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
import org.wikipedia.dataclient.restbase.DiffResponse.*
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
    private var watchlistExpirySession: WatchlistExpiry? = null


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
        diffText.movementMethod = ScrollingMovementMethod()

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
        newerIdButton.setOnClickListener {
            revisionId = newerRevisionId
            fetchEditDetails()
        }
        olderIdButton.setOnClickListener {
            revisionId = olderRevisionId
            fetchEditDetails()
        }
        watchButton.setOnClickListener {
            watchOrUnwatchTitle(watchlistExpirySession, isWatched)
        }
        usernameButton.setOnClickListener {
            if (AccountUtil.isLoggedIn() && username != null) {
                startActivity(newIntent(requireActivity(),
                        PageTitle(UserTalkAliasData.valueFor(languageCode),
                                username!!, WikiSite.forLanguageCode(languageCode))))
            }
        }
        thankButton.setOnClickListener { showThankDialog() }
        fetchEditDetails()
    }

    private fun fetchEditDetails() {
        disposables.add(Observable.zip(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getRevisionDetails(articleTitle, revisionId),
                ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getWatchedInfo(articleTitle),
                { r, w ->
                    isWatched = w.query()!!.firstPage()!!.isWatched
                    val currentRevision = r.query()!!.firstPage()!!.revisions()[0]
                    username = currentRevision.user
                    newerRevisionId = if (r.query()!!.firstPage()!!.revisions().size < 2) {
                        -1
                    } else {
                        r.query()!!.firstPage()!!.revisions()[1].revId
                    }
                    olderRevisionId = currentRevision.parentRevId
                    currentRevision
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateUI(it)
                }) { t: Throwable? -> L.e(t) })
    }

    private fun updateUI(@NonNull currentRevision: Revision) {
        diffText.scrollTo(0, 0)
        diffText.text = ""
        usernameButton.text = currentRevision.user
        editTimestamp.text = DateUtil.getDateAndTimeStringFromTimestampString(currentRevision.timeStamp())
        editComment.text = currentRevision.comment

        newerIdButton.isClickable = newerRevisionId.compareTo(-1) != 0
        olderIdButton.isClickable = olderRevisionId.compareTo(0) != 0
        ImageViewCompat.setImageTintList(newerIdButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (newerRevisionId.compareTo(-1) == 0)
                    R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        ImageViewCompat.setImageTintList(olderIdButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                ResourceUtil.getThemedAttributeId(requireContext(), if (olderRevisionId.compareTo(0) == 0)
                    R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color))))
        menu?.findItem(R.id.menu_user_profile_page)?.title = getString(R.string.menu_option_user_profile, currentRevision.user)
        menu?.findItem(R.id.menu_user_contributions_page)?.title = getString(R.string.menu_option_user_contributions, currentRevision.user)
        setButtonTextAndIconColor(thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
        thankButton.isClickable = true
        updateWatchlistButtonUI()
        fetchDiffText()
    }

    private fun setButtonTextAndIconColor(@NonNull view: MaterialButton, @NonNull themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = (ColorStateList.valueOf(themedColor))
    }

    private fun watchOrUnwatchTitle(@Nullable expiry: WatchlistExpiry?, @NonNull unwatch: Boolean) {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).watchToken
                .subscribeOn(Schedulers.io())
                .flatMap { response: MwQueryResponse ->
                    val watchToken = response.query()!!.watchToken()
                    if (TextUtils.isEmpty(watchToken)) {
                        throw RuntimeException("Received empty watch token: " + GsonUtil.getDefaultGson().toJson(response))
                    }
                    ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).postWatch(if (unwatch) 1 else null, null, articleTitle,
                            expiry?.expiry, watchToken!!)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ watchPostResponse: WatchPostResponse ->
                    val firstWatch = watchPostResponse.getFirst()
                    isWatched = firstWatch!!.watched
                    updateWatchlistButtonUI()
                    // Reset to make the "Change" button visible.
                    if (watchlistExpiryChanged && unwatch) {
                        watchlistExpiryChanged = false
                    }
                    showWatchlistSnackbar(expiry, firstWatch)
                }) { t: Throwable? -> L.d(t) })
    }

    private fun updateWatchlistButtonUI() {
        setButtonTextAndIconColor(watchButton, ResourceUtil.getThemedColor(requireContext(),
                if (isWatched) R.attr.color_group_62 else R.attr.colorAccent))
        watchButton.text = getString(if (isWatched) R.string.watchlist_details_watching_label
        else R.string.watchlist_details_watch_label)
    }

    private fun showWatchlistSnackbar(@Nullable expiry: WatchlistExpiry?, @NonNull watch: Watch) {
        isWatched = watch.watched
        if (watch.unwatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, articleTitle))
            watchlistExpirySession = null

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
            watchlistExpirySession = null
            snackbar.show()
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

    private fun fetchDiffText() {
        disposables.add(ServiceFactory.getCoreRest(WikiSite.forLanguageCode(languageCode)).getDiff(olderRevisionId, revisionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    displayDiff(response.diffs)
                }) { t: Throwable? -> L.e(t) })
    }

    private fun displayDiff(diffs: MutableList<DiffItem>) {
        for (diff in diffs) {
            val prefixLength = diffText.text.length
            val foregroundAddedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_64))
            val foregroundRemovedColor = ForegroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_65))
            val boldStyle = StyleSpan(Typeface.BOLD)

            val spannableString = SpannableStringBuilder(diffText.text).append(if (diff.text.isNotEmpty()) diff.text else "\n")
            when (diff.type) {
                DIFF_TYPE_LINE_WITH_SAME_CONTENT -> {
                }
                DIFF_TYPE_LINE_ADDED -> {
                    spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                            R.attr.edit_green_highlight)), prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(boldStyle, prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(foregroundAddedColor, prefixLength, prefixLength + diff.text.length, 0)
                }
                DIFF_TYPE_LINE_REMOVED -> {
                    spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                            R.attr.edit_red_highlight)), prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(boldStyle, prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(foregroundRemovedColor, prefixLength, prefixLength + diff.text.length, 0)
                }
                DIFF_TYPE_LINE_WITH_DIFF -> {
                    for (hightlightRange in diff.highlightRanges) {
                        val highlightRangeStart = if (languageCode == "en") hightlightRange.start
                        else getByteInCharacters(diff.text, hightlightRange.start, 0)
                        val highlightRangeLength = if (languageCode == "en") hightlightRange.length
                        else getByteInCharacters(diff.text, hightlightRange.length, highlightRangeStart)

                        if (hightlightRange.type == HIGHLIGHT_TYPE_ADD) {
                            spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                                    R.attr.edit_green_highlight)), prefixLength + highlightRangeStart,
                                    prefixLength + highlightRangeStart + highlightRangeLength, 0)
                            spannableString.setSpan(foregroundAddedColor, prefixLength + highlightRangeStart,
                                    prefixLength + highlightRangeStart + highlightRangeLength, 0)

                        } else {
                            spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                                    R.attr.edit_red_highlight)), prefixLength + highlightRangeStart,
                                    prefixLength + highlightRangeStart + highlightRangeLength, 0)
                            spannableString.setSpan(foregroundRemovedColor, prefixLength + highlightRangeStart,
                                    prefixLength + highlightRangeStart + highlightRangeLength, 0)
                        }
                        spannableString.setSpan(boldStyle, prefixLength + highlightRangeStart,
                                prefixLength + highlightRangeStart + highlightRangeLength, 0)
                    }
                }
                DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                    spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                            R.attr.edit_red_highlight)), prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(boldStyle, prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(foregroundRemovedColor, prefixLength, prefixLength + diff.text.length, 0)
                }
                DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                    spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(),
                            R.attr.edit_green_highlight)), prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(boldStyle, prefixLength, prefixLength + diff.text.length, 0)
                    spannableString.setSpan(foregroundAddedColor, prefixLength, prefixLength + diff.text.length, 0)
                }
            }
            diffText.text = spannableString.append("\n")
        }
    }

    private fun getByteInCharacters(diffText: String?, byteLength: Int, start: Int): Int {
        var charCount = 0
        var bytes = byteLength

        for (pos in start until diffText!!.length - 1) {
            val idBytes = diffText[pos].toString().toByteArray(CHARSET).size
            charCount++
            bytes -= idBytes
            if (bytes <= 0) {
                break
            }
        }
        return charCount
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(@NonNull menu: Menu, @NonNull inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
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

    override fun onExpirySelect(@NonNull expiry: WatchlistExpiry) {
        watchOrUnwatchTitle(expiry, false)
        bottomSheetPresenter.dismiss(childFragmentManager)
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }
}
