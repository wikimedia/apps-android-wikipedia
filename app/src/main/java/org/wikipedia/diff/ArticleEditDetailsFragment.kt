package org.wikipedia.diff

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.*
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentArticleEditDetailsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.*
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import java.nio.charset.StandardCharsets

class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback, LinkPreviewDialog.Callback {
    private var _binding: FragmentArticleEditDetailsBinding? = null
    private val binding get() = _binding!!

    private var isWatched = false
    private var hasWatchlistExpiry = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    private val viewModel: ArticleEditDetailsViewModel by viewModels { ArticleEditDetailsViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentArticleEditDetailsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.diffRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        FeedbackUtil.setButtonLongPressToast(binding.newerIdButton, binding.olderIdButton)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setUpListeners()
        setLoadingState()

        binding.articleTitleView.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)

        viewModel.watchedStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                isWatched = it.data.query?.firstPage()?.watched ?: false
                hasWatchlistExpiry = it.data.query?.firstPage()?.hasWatchlistExpiry() ?: false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.revisionDetails.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateDiffCharCountView(viewModel.diffSize)
                updateAfterRevisionFetchSuccess()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.diffText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                buildDiffLinesList(it.data.diff)
                updateAfterDiffFetchSuccess()
                binding.progressBar.isVisible = false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.thankStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                FeedbackUtil.showMessage(requireActivity(), getString(R.string.thank_success_message,
                        viewModel.revisionTo?.user))
                setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(),
                        R.attr.material_theme_de_emphasised_color))
                binding.thankButton.isEnabled = false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.watchResponse.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                val firstWatch = it.data.getFirst()
                if (firstWatch != null) {
                    showWatchlistSnackbar(viewModel.lastWatchExpiry, firstWatch)
                }
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.undoEditResponse.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                setLoadingState()
                viewModel.getRevisionDetails(it.data.edit!!.newRevId)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.revision_undo_success), FeedbackUtil.LENGTH_DEFAULT).show()
            } else if (it is Resource.Error) {
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }

        L10nUtil.setConditionalLayoutDirection(requireView(), viewModel.pageTitle.wikiSite.languageCode)

        binding.scrollContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val bounds = Rect()
            binding.contentContainer.offsetDescendantRectToMyCoords(binding.articleTitleDivider, bounds)
            if (scrollY > bounds.top) {
                binding.overlayRevisionDetailsView.visibility = View.VISIBLE
                if (binding.toolbarTitleView.text.isNullOrEmpty()) {
                    binding.toolbarTitleView.text = getString(R.string.revision_diff_compare_title, StringUtil.fromHtml(viewModel.pageTitle.displayText))
                }
            } else {
                binding.overlayRevisionDetailsView.visibility = View.INVISIBLE
                binding.toolbarTitleView.text = ""
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setUpListeners() {
        binding.articleTitleView.setOnClickListener {
            if (viewModel.pageTitle.namespace() == Namespace.USER_TALK || viewModel.pageTitle.namespace() == Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(requireContext(), viewModel.pageTitle, InvokeSource.DIFF_ACTIVITY))
            } else {
                bottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(
                        HistoryEntry(viewModel.pageTitle, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS), null))
            }
        }
        binding.newerIdButton.setOnClickListener {
            setLoadingState()
            viewModel.goForward()
        }
        binding.olderIdButton.setOnClickListener {
            setLoadingState()
            viewModel.goBackward()
        }

        binding.usernameFromButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionFrom, binding.usernameFromButton)
        }

        binding.usernameToButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionTo, binding.usernameToButton)
        }

        binding.thankButton.setOnClickListener { showThankDialog() }

        binding.undoButton.isVisible = ReleaseUtil.isPreBetaRelease
        binding.undoButton.setOnClickListener { showUndoDialog() }

        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val watchlistItem = menu.findItem(R.id.menu_add_watchlist)
        watchlistItem.title = getString(if (isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
        watchlistItem.setIcon(getWatchlistIcon(isWatched, hasWatchlistExpiry))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                ShareUtil.shareText(requireContext(), PageTitle(viewModel.pageTitle.prefixedText,
                        viewModel.pageTitle.wikiSite), viewModel.revisionToId, viewModel.revisionFromId)
                true
            }
            R.id.menu_add_watchlist -> {
                viewModel.watchOrUnwatch(isWatched, WatchlistExpiry.NEVER, isWatched)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserPopupMenu(revision: Revision?, anchorView: View) {
        revision?.let {
            UserTalkPopupHelper.show(requireActivity() as AppCompatActivity, bottomSheetPresenter,
                    PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode),
                            it.user, viewModel.pageTitle.wikiSite), it.isAnon, anchorView,
                    InvokeSource.DIFF_ACTIVITY, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS)
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.errorView.setError(t)
        binding.errorView.isVisible = true
        binding.revisionDetailsView.isVisible = false
        binding.progressBar.isVisible = false
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

    private fun setLoadingState() {
        binding.progressBar.isVisible = true
        binding.revisionDetailsView.isVisible = false
        binding.diffRecyclerView.isVisible = false
    }

    private fun updateAfterRevisionFetchSuccess() {
        viewModel.revisionFrom?.let {
            binding.usernameFromButton.text = it.user
            binding.revisionFromTimestamp.text = DateUtil.getDateAndTimeWithPipe(DateUtil.iso8601DateParse(it.timeStamp))
            binding.overlayRevisionFromTimestamp.text = binding.revisionFromTimestamp.text
            binding.revisionFromEditComment.text = StringUtil.fromHtml(it.parsedcomment.trim())
        }

        viewModel.revisionTo?.let {
            binding.usernameToButton.text = it.user
            binding.revisionToTimestamp.text = DateUtil.getDateAndTimeWithPipe(DateUtil.iso8601DateParse(it.timeStamp))
            binding.overlayRevisionToTimestamp.text = binding.revisionToTimestamp.text
            binding.revisionToEditComment.text = StringUtil.fromHtml(it.parsedcomment.trim())
        }

        setEnableDisableTint(binding.newerIdButton, !viewModel.canGoForward)
        setEnableDisableTint(binding.olderIdButton, viewModel.revisionFromId == 0L)
        binding.newerIdButton.isEnabled = viewModel.canGoForward
        binding.olderIdButton.isEnabled = viewModel.revisionFromId != 0L

        setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))

        binding.thankButton.isEnabled = true
        binding.thankButton.isVisible = AccountUtil.isLoggedIn && !AccountUtil.userName.equals(viewModel.revisionTo?.user)
        binding.revisionDetailsView.isVisible = true
    }

    private fun updateAfterDiffFetchSuccess() {
        binding.diffRecyclerView.isVisible = true
    }

    private fun setEnableDisableTint(view: ImageView, isDisabled: Boolean) {
        ImageViewCompat.setImageTintList(view, AppCompatResources.getColorStateList(requireContext(),
            ResourceUtil.getThemedAttributeId(requireContext(), if (isDisabled)
                R.attr.material_theme_de_emphasised_color else R.attr.material_theme_secondary_color)))
    }

    private fun setButtonTextAndIconColor(view: MaterialButton, themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = ColorStateList.valueOf(themedColor)
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
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, viewModel.pageTitle.displayText))
        } else if (watch.watched) {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            viewModel.pageTitle.displayText,
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
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog: AlertDialog = AlertDialog.Builder(activity)
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    viewModel.sendThanks(viewModel.pageTitle.wikiSite, viewModel.revisionToId)
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

    private fun showUndoDialog() {
        val dialog = UndoEditDialog(requireActivity()) { text ->
            viewModel.revisionTo?.let {
                binding.progressBar.isVisible = true
                viewModel.undoEdit(viewModel.pageTitle, it.user, text.toString(), viewModel.revisionToId, 0)
            }
        }
        dialog.show()
    }

    private fun createSpannable(diff: DiffResponse.DiffItem): CharSequence {
        val spannableString = SpannableStringBuilder(diff.text.ifEmpty { "\n" })
        if (diff.text.isEmpty()) {
            spannableString.setSpan(EmptyLineSpan(ResourceUtil.getThemedColor(requireContext(), android.R.attr.colorBackground),
                    ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_de_emphasised_color)), 0, spannableString.length, 0)
            return spannableString
        }
        when (diff.type) {
            DiffResponse.DIFF_TYPE_LINE_ADDED -> {
                updateDiffTextDecor(spannableString, true, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_LINE_REMOVED -> {
                updateDiffTextDecor(spannableString, false, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                updateDiffTextDecor(spannableString, false, 0, diff.text.length)
            }
            DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                updateDiffTextDecor(spannableString, true, 0, diff.text.length)
            }
        }
        if (diff.highlightRanges.isNotEmpty()) {
            for (highlightRange in diff.highlightRanges) {
                val indices = utf8Indices(diff.text)
                val highlightRangeStart = indices[highlightRange.start]
                val highlightRangeEnd = indices.getOrElse(highlightRange.start + highlightRange.length) { indices.last() }

                if (highlightRange.type == DiffResponse.HIGHLIGHT_TYPE_ADD) {
                    updateDiffTextDecor(spannableString, true, highlightRangeStart, highlightRangeEnd)
                } else {
                    updateDiffTextDecor(spannableString, false, highlightRangeStart, highlightRangeEnd)
                }
            }
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
        if (isAddition) {
            spannableText.setSpan(foregroundAddedColor, start, end, 0)
        } else {
            spannableText.setSpan(foregroundRemovedColor, start, end, 0)
            spannableText.setSpan(StrikethroughSpan(), start, end, 0)
        }
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

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        viewModel.watchOrUnwatch(isWatched, expiry, false)
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

    private fun buildDiffLinesList(diffList: List<DiffResponse.DiffItem>) {
        val items = mutableListOf<DiffLine>()
        var lastItem: DiffLine? = null
        diffList.forEach {
            val item = DiffLine(it)
            // coalesce diff lines that occur on successive line numbers
            if (lastItem != null &&
                    ((item.diff.lineNumber - lastItem!!.diff.lineNumber == 1 && lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_ADDED && item.diff.type == DiffResponse.DIFF_TYPE_LINE_ADDED) ||
                            (item.diff.lineNumber - lastItem!!.diff.lineNumber == 1 && lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT && item.diff.type == DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT) ||
                            (lastItem!!.diff.type == DiffResponse.DIFF_TYPE_LINE_REMOVED && item.diff.type == DiffResponse.DIFF_TYPE_LINE_REMOVED))) {
                if (it.lineNumber > lastItem!!.lineEnd) {
                    lastItem!!.lineEnd = it.lineNumber
                }
                val str = SpannableStringBuilder(lastItem!!.parsedText)
                str.append("\n")
                str.append(item.parsedText)
                lastItem!!.parsedText = str
            } else {
                items.add(item)
                lastItem = item
            }
        }
        binding.diffRecyclerView.adapter = DiffLinesAdapter(items)
    }

    inner class DiffLine(item: DiffResponse.DiffItem) {
        val diff = item
        val lineStart = item.lineNumber
        var lineEnd = item.lineNumber
        var parsedText = createSpannable(diff)
        var expanded = diff.type != DiffResponse.DIFF_TYPE_LINE_WITH_SAME_CONTENT
    }

    private inner class DiffLinesAdapter(val diffLines: List<DiffLine>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return diffLines.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return DiffLineHolder(DiffLineView(requireContext()))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is DiffLineHolder) {
                holder.bindItem(diffLines[pos])
            }
            holder.itemView.tag = pos
        }
    }

    private inner class DiffLineHolder constructor(itemView: DiffLineView) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(item: DiffLine) {
            (itemView as DiffLineView).setItem(item)
        }
    }

    companion object {
        fun newInstance(title: PageTitle, revisionFrom: Long, revisionTo: Long): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment().apply {
                arguments = bundleOf(ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE to title,
                        ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_FROM to revisionFrom,
                        ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_TO to revisionTo)
            }
        }
    }
}
