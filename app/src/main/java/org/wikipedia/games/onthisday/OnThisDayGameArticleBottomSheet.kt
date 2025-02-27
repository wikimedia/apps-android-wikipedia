package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.DialogOnThisDayGameArticleBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.parcelable
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.AllowSnackbarOverBottomSheet
import org.wikipedia.views.ViewUtil

class OnThisDayGameArticleBottomSheet : ExtendedBottomSheetDialogFragment(), AllowSnackbarOverBottomSheet {
    fun interface Callback {
        fun onPageBookmarkChanged(page: PageSummary)
    }

    private var _binding: DialogOnThisDayGameArticleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private lateinit var pageSummary: PageSummary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageSummary = requireArguments().parcelable<PageSummary>(Constants.ARG_TITLE)!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.root.parent as View).peekHeight = DimenUtil.displayHeightPx
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogOnThisDayGameArticleBinding.inflate(inflater, container, false)
        WikiGamesEvent.submit("impression", "game_play", slideName = "game_end_article")

        binding.articleTitle.text = StringUtil.fromHtml(pageSummary.displayTitle)
        binding.articleDescription.text = StringUtil.fromHtml(pageSummary.description)

        if (pageSummary.thumbnailUrl.isNullOrEmpty()) {
            binding.articleThumbnail.isInvisible = true
            binding.articleSummaryContainer.isInvisible = false
            binding.articleSummary.text = StringUtil.fromHtml(pageSummary.extractHtml)
        } else {
            binding.articleThumbnail.isInvisible = false
            binding.articleSummaryContainer.isInvisible = true
            ViewUtil.loadImage(
                binding.articleThumbnail,
                pageSummary.thumbnailUrl,
                placeholderId = R.mipmap.launcher
            )
        }

        val event = viewModel.getEventByPageTitle(pageSummary.apiTitle)
        binding.relatedEventInfo.text = StringUtil.fromHtml(event.text)

        val isCorrect = viewModel.getQuestionCorrectByPageTitle(pageSummary.apiTitle)
        val answerIcon = if (isCorrect) R.drawable.check_circle_24px else R.drawable.ic_cancel_24px
        val answerIconTintList = ResourceUtil.getThemedColorStateList(requireContext(), if (isCorrect) R.attr.success_color else R.attr.destructive_color)
        val answerText = if (isCorrect) R.string.on_this_day_game_article_answer_correct_stats_message else R.string.on_this_day_game_article_answer_incorrect_stats_message
        binding.answerStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(answerIcon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(binding.answerStatus, answerIconTintList)
        binding.answerStatus.text = getString(answerText)

        val isSaved = viewModel.savedPages.contains(pageSummary)
        val bookmarkResource = if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
        binding.saveButton.setImageResource(bookmarkResource)
        binding.saveButton.setOnClickListener {
            WikiGamesEvent.submit("save_click", "game_play", slideName = "game_end_article")
            onBookmarkIconClick(binding.saveButton, pageSummary)
        }
        binding.shareButton.setOnClickListener {
            WikiGamesEvent.submit("share_click", "game_play", slideName = "game_end_article")
            ShareUtil.shareText(requireActivity(), pageSummary.getPageTitle(viewModel.wikiSite))
        }
        FeedbackUtil.setButtonTooltip(binding.shareButton, binding.saveButton)
        binding.readArticleButton.setOnClickListener {
            WikiGamesEvent.submit("read_click", "game_play", slideName = "game_end_article")
            val entry = HistoryEntry(pageSummary.getPageTitle(viewModel.wikiSite), HistoryEntry.SOURCE_ON_THIS_DAY_GAME)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }
        return binding.root
    }

    private fun onBookmarkIconClick(view: ImageView, pageSummary: PageSummary) {
        val pageTitle = pageSummary.getPageTitle(viewModel.wikiSite)
        val isSaved = viewModel.savedPages.contains(pageSummary)
        if (isSaved) {
            LongPressMenu(view, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, addToDefault, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(requireActivity(), page.listId, pageTitle, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                    }
                }

                override fun onRemoveRequest() {
                    super.onRemoveRequest()
                    viewModel.savedPages.remove(pageSummary)
                    view.setImageResource(R.drawable.ic_bookmark_border_white_24dp)
                    callback()?.onPageBookmarkChanged(pageSummary)
                }
            }).show(HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_GAME))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), pageTitle, true, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
            viewModel.savedPages.add(pageSummary)
            view.setImageResource(R.drawable.ic_bookmark_white_24dp)
            callback()?.onPageBookmarkChanged(pageSummary)
        }
    }

    private fun callback(): Callback? {
        return getCallback(this@OnThisDayGameArticleBottomSheet, Callback::class.java)
    }

    companion object {
        fun newInstance(page: PageSummary): OnThisDayGameArticleBottomSheet {
            return OnThisDayGameArticleBottomSheet().apply {
                arguments = bundleOf(
                    Constants.ARG_TITLE to page,
                )
            }
        }
    }
}
