package org.wikipedia.diff

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentArticleEditDetailsBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.*
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import java.nio.charset.StandardCharsets

class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback, LinkPreviewDialog.Callback {
    private var _binding: FragmentArticleEditDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var articlePageTitle: PageTitle
    private lateinit var languageCode: String
    private lateinit var wikiSite: WikiSite
    private var revisionId: Long = 0
    private var diffSize: Int = 0
    private var username: String? = null
    private var newerRevisionId: Long = 0
    private var olderRevisionId: Long = 0
    private var currentRevision: Revision? = null

    private var isWatched = false
    private var hasWatchlistExpiry = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    private val viewModel: ArticleEditDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        revisionId = requireArguments().getLong(EXTRA_EDIT_REVISION_ID, 0)
        languageCode = requireArguments().getString(EXTRA_EDIT_LANGUAGE_CODE, AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE)
        wikiSite = WikiSite.forLanguageCode(languageCode)
        articlePageTitle = PageTitle(requireArguments().getString(EXTRA_ARTICLE_TITLE, ""), wikiSite)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentArticleEditDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setUpInitialUI()
        setUpListeners()

        viewModel.watchedStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                isWatched = it.data.query?.firstPage()?.watched ?: false
                hasWatchlistExpiry = it.data.query?.firstPage()?.hasWatchlistExpiry() ?: false
                updateWatchlistButtonUI()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.revisionDetails.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                val firstPage = it.data.query?.firstPage()!!
                currentRevision = firstPage.revisions[0]
                revisionId = currentRevision!!.revId
                username = currentRevision!!.user
                newerRevisionId = if (firstPage.revisions.size < 2) {
                    -1
                } else {
                    firstPage.revisions[1].revId
                }
                olderRevisionId = currentRevision!!.parentRevId
                updateUI()
                if (olderRevisionId > 0L) {
                    viewModel.getDiffText(wikiSite, olderRevisionId, revisionId)
                } else {
                    binding.progressBar.visibility = INVISIBLE
                }
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.diffText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                binding.diffText.text = createSpannable(it.data.diff)
                updateDiffCharCountView(diffSize)
                binding.diffCharacterCountView.visibility = VISIBLE
                binding.progressBar.visibility = INVISIBLE
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.thankStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                FeedbackUtil.showMessage(requireActivity(), getString(R.string.thank_success_message, username))
                setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(),
                        R.attr.material_theme_de_emphasised_color))
                binding.thankButton.isClickable = false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.watchResponse.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                val firstWatch = it.data.getFirst()
                if (firstWatch != null) {
                    showWatchlistSnackbar(viewModel.lastWatchExpiry, firstWatch)
                    updateWatchlistButtonUI()
                }
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
                binding.watchButton.isCheckable = true
            }
        }

        viewModel.getWatchedStatus(articlePageTitle)
        beginFetchEditDetails()

        L10nUtil.setConditionalLayoutDirection(requireView(), languageCode)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setUpListeners() {
        binding.articleTitleView.setOnClickListener {
            if (articlePageTitle.namespace() == Namespace.USER_TALK || articlePageTitle.namespace() == Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(requireContext(), articlePageTitle, InvokeSource.DIFF_ACTIVITY))
            } else {
                bottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(
                        HistoryEntry(articlePageTitle, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS), null))
            }
        }
        binding.newerIdButton.setOnClickListener {
            revisionId = newerRevisionId
            beginFetchEditDetails()
        }
        binding.olderIdButton.setOnClickListener {
            revisionId = olderRevisionId
            beginFetchEditDetails()
        }
        binding.watchButton.setOnClickListener {
            binding.watchButton.isCheckable = false
            viewModel.watchOrUnwatch(articlePageTitle, isWatched, WatchlistExpiry.NEVER, isWatched)
        }
        binding.usernameButton.setOnClickListener {
            if (AccountUtil.isLoggedIn && username != null) {
                startActivity(TalkTopicsActivity.newIntent(requireActivity(),
                        PageTitle(UserTalkAliasData.valueFor(languageCode),
                                username!!, wikiSite), InvokeSource.DIFF_ACTIVITY))
            }
        }
        binding.thankButton.setOnClickListener { showThankDialog() }
        binding.errorView.backClickListener = OnClickListener { requireActivity().finish() }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.errorView.setError(t)
        binding.errorView.visibility = VISIBLE
        binding.revisionDetailsView.visibility = GONE
        binding.progressBar.visibility = INVISIBLE
    }

    private fun setUpInitialUI() {
        binding.diffText.movementMethod = ScrollingMovementMethod()
        binding.articleTitleView.text = articlePageTitle.displayText
        updateDiffCharCountView(diffSize)
    }

    private fun updateDiffCharCountView(diffSize: Int) {
        binding.diffCharacterCountView.text = String.format(if (diffSize != 0) "%+d" else "%d", diffSize)
        if (diffSize >= 0) {
            binding.diffCharacterCountView.setTextColor(if (diffSize > 0) ContextCompat.getColor(requireContext(),
                    R.color.green50) else ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_secondary_color))
        } else {
            binding.diffCharacterCountView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red50))
        }
    }

    private fun beginFetchEditDetails() {
        hideOrShowViews(true)
        viewModel.getRevisionDetails(articlePageTitle, revisionId)
    }

    private fun hideOrShowViews(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = VISIBLE
            binding.usernameButton.visibility = INVISIBLE
            binding.thankButton.visibility = INVISIBLE
            binding.editComment.visibility = INVISIBLE
            binding.diffText.visibility = INVISIBLE
            binding.diffCharacterCountView.visibility = INVISIBLE
        } else {
            binding.usernameButton.visibility = VISIBLE
            binding.thankButton.visibility = VISIBLE
            binding.editComment.visibility = VISIBLE
            binding.diffText.visibility = VISIBLE
        }
    }

    private fun updateUI() {
        binding.diffText.scrollTo(0, 0)
        binding.diffText.text = ""
        binding.usernameButton.text = currentRevision!!.user
        binding.editTimestamp.text = DateUtil.getDateAndTimeWithPipe(DateUtil.iso8601DateParse(currentRevision!!.timeStamp))
        binding.editComment.text = currentRevision!!.comment
        binding.newerIdButton.isClickable = newerRevisionId != -1L
        binding.olderIdButton.isClickable = olderRevisionId != 0L
        setEnableDisableTint(binding.newerIdButton, newerRevisionId == -1L)
        setEnableDisableTint(binding.olderIdButton, olderRevisionId == 0L)
        setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
        binding.thankButton.isClickable = true
        requireActivity().invalidateOptionsMenu()
        maybeHideThankButton()
        hideOrShowViews(false)
    }

    private fun maybeHideThankButton() {
        binding.thankButton.visibility = if (AccountUtil.userName.equals(currentRevision?.user)) GONE else VISIBLE
    }

    private fun setEnableDisableTint(view: AppCompatImageView, isDisabled: Boolean) {
        ImageViewCompat.setImageTintList(view, AppCompatResources.getColorStateList(requireContext(),
            ResourceUtil.getThemedAttributeId(requireContext(), if (isDisabled)
                R.attr.material_theme_de_emphasised_color else R.attr.primary_text_color)))
    }

    private fun setButtonTextAndIconColor(view: MaterialButton, themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = ColorStateList.valueOf(themedColor)
    }

    private fun updateWatchlistButtonUI() {
        setButtonTextAndIconColor(binding.watchButton, ResourceUtil.getThemedColor(requireContext(),
                if (isWatched) R.attr.color_group_68 else R.attr.colorAccent))
        binding.watchButton.text = getString(if (isWatched) R.string.watchlist_details_watching_label else R.string.watchlist_details_watch_label)
        binding.watchButton.setIconResource(getWatchlistIcon(isWatched, hasWatchlistExpiry))
    }

    @DrawableRes
    private fun getWatchlistIcon(isWatched: Boolean, hasWatchlistExpiry: Boolean): Int {
        return if (isWatched && !hasWatchlistExpiry) {
            R.drawable.ic_star_24
        } else if (!isWatched) {
            R.drawable.ic_baseline_star_outline_24
        } else {
            R.drawable.ic_baseline_star_half_24
        }
    }

    private fun showWatchlistSnackbar(expiry: WatchlistExpiry, watch: Watch) {
        isWatched = watch.watched
        hasWatchlistExpiry = expiry != WatchlistExpiry.NEVER
        if (watch.unwatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, articlePageTitle.displayText))
        } else if (watch.watched) {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            articlePageTitle.displayText,
                            getString(expiry.stringId)),
                    FeedbackUtil.LENGTH_DEFAULT)
            if (!viewModel.watchlistExpiryChanged) {
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                    viewModel.watchlistExpiryChanged = true
                    bottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(expiry))
                }
            }
            snackbar.show()
        }
        binding.watchButton.isCheckable = true
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog: AlertDialog = AlertDialog.Builder(activity)
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    viewModel.sendThanks(wikiSite, revisionId)
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

    private fun createSpannable(diffs: List<DiffResponse.DiffItem>): CharSequence {
        val spannableString = SpannableStringBuilder()
        diffSize = 0
        for (diff in diffs) {
            val prefixLength = spannableString.length
            spannableString.append(diff.text.ifEmpty { "\n" })
            when (diff.type) {
                DiffResponse.DIFF_TYPE_LINE_ADDED -> {
                    diffSize += diff.text.length + 1
                    updateDiffTextDecor(spannableString, true, prefixLength, prefixLength + diff.text.length)
                }
                DiffResponse.DIFF_TYPE_LINE_REMOVED -> {
                    diffSize -= diff.text.length + 1
                    updateDiffTextDecor(spannableString, false, prefixLength, prefixLength + diff.text.length)
                }
                DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                    diffSize -= diff.text.length + 1
                    updateDiffTextDecor(spannableString, false, prefixLength, prefixLength + diff.text.length)
                }
                DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                    diffSize += diff.text.length + 1
                    updateDiffTextDecor(spannableString, true, prefixLength, prefixLength + diff.text.length)
                }
            }
            if (diff.highlightRanges.isNotEmpty()) {
                for (highlightRange in diff.highlightRanges) {
                    val indices = utf8Indices(diff.text)
                    val highlightRangeStart = indices[highlightRange.start]
                    val highlightRangeEnd = if (highlightRange.start + highlightRange.length < indices.size) indices[highlightRange.start + highlightRange.length] else indices[indices.size - 1]

                    if (highlightRange.type == DiffResponse.HIGHLIGHT_TYPE_ADD) {
                        diffSize += highlightRange.length
                        updateDiffTextDecor(spannableString, true, prefixLength + highlightRangeStart, prefixLength + highlightRangeEnd)
                    } else {
                        diffSize -= highlightRange.length
                        updateDiffTextDecor(spannableString, false, prefixLength + highlightRangeStart, prefixLength + highlightRangeEnd)
                    }
                }
            }
            spannableString.append("\n")
        }
        return spannableString
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val userProfileMenuItem = menu.findItem(R.id.menu_user_profile_page)
        currentRevision?.let {
            if (it.isAnon) {
                userProfileMenuItem.isVisible = false
            } else {
                userProfileMenuItem.title = getString(R.string.menu_option_user_profile, it.user)
            }
            menu.findItem(R.id.menu_user_contributions_page).title = getString(R.string.menu_option_user_contributions, it.user)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                ShareUtil.shareText(requireContext(), PageTitle(articlePageTitle.prefixedText,
                        wikiSite), revisionId, olderRevisionId)
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

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        viewModel.watchOrUnwatch(articlePageTitle, isWatched, expiry, false)
        bottomSheetPresenter.dismiss(childFragmentManager)
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

    companion object {
        const val EXTRA_ARTICLE_TITLE = "articleTitle"
        const val EXTRA_EDIT_REVISION_ID = "revisionId"
        const val EXTRA_EDIT_LANGUAGE_CODE = "languageCode"

        fun newInstance(articleTitle: String, revisionId: Long, languageCode: String): ArticleEditDetailsFragment {
            val articleEditDetailsFragment = ArticleEditDetailsFragment()
            articleEditDetailsFragment.arguments = bundleOf(EXTRA_ARTICLE_TITLE to articleTitle,
                    EXTRA_EDIT_REVISION_ID to revisionId, EXTRA_EDIT_LANGUAGE_CODE to languageCode)
            return articleEditDetailsFragment
        }
    }
}
