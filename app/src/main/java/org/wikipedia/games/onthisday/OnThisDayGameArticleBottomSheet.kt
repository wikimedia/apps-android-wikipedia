package org.wikipedia.games.onthisday

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class OnThisDayGameArticleBottomSheet(
    val activity: Activity,
    val binding: ActivityOnThisDayGameBinding,
    val viewModel: OnThisDayGameViewModel
) {
    private var statusBarInsets: Insets? = null
    private var toolbarHeight: Int = 0
    private var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>? = null

    fun onBackPressed(): Boolean {
        return if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        } else {
            false
        }
    }

    fun onApplyWindowInsets(statusBarInsets: Insets, toolbarHeight: Int) {
        this.statusBarInsets = statusBarInsets
        this.toolbarHeight = toolbarHeight
    }

    @SuppressLint("RestrictedApi")
    fun calculateBottomSheetTopPadding(insets: Int): Int {
        val collapsedPadding = toolbarHeight + insets
        val expandedPadding = insets
        val topPadding = when (bottomSheetBehavior?.state) {
            BottomSheetBehavior.STATE_COLLAPSED -> collapsedPadding
            BottomSheetBehavior.STATE_EXPANDED -> expandedPadding
            BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                // Calculate a proper padding during dragging/settling
                val offsetFraction = if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_DRAGGING) {
                    // Use the actual drag fraction to avoid jumps.
                    val y = binding.bottomSheetCoordinatorLayout.y
                    val expandedY = binding.bottomSheetCoordinatorLayout.height.toFloat()
                    y / -expandedY + 1
                } else {
                    // During settling, use the last stable state and assume a linear transition.
                    if (bottomSheetBehavior?.lastStableState == BottomSheetBehavior.STATE_EXPANDED) 0f else 1f
                }
                collapsedPadding * (1 - offsetFraction) + expandedPadding * offsetFraction
            }
            else -> collapsedPadding
        }
        return topPadding.toInt()
    }

    fun openArticleBottomSheet(pageSummary: PageSummary, updateBookmark: () -> Unit) {
        if (viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
            return
        }
        WikiGamesEvent.submit("impression", "game_play", slideName = "game_end_article")

        binding.root.requestApplyInsets()
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetCoordinatorLayout).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                binding.root.requestApplyInsets()
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    updateBookmark()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val topPadding = toolbarHeight + slideOffset * ((statusBarInsets?.top ?: 0) - toolbarHeight)
                bottomSheet.updatePadding(
                    top = topPadding.toInt()
                )
            }
        })

        val dialogBinding = binding.articleDialogContainer
        dialogBinding.articleTitle.text = StringUtil.fromHtml(pageSummary.displayTitle)
        dialogBinding.closeButton.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        dialogBinding.articleDescription.text = StringUtil.fromHtml(pageSummary.description)

        if (pageSummary.thumbnailUrl.isNullOrEmpty()) {
            dialogBinding.articleThumbnail.isInvisible = true
            dialogBinding.articleSummaryContainer.isInvisible = false
            dialogBinding.articleSummary.text = StringUtil.fromHtml(pageSummary.extractHtml)
        } else {
            dialogBinding.articleThumbnail.isInvisible = false
            dialogBinding.articleSummaryContainer.isInvisible = true
            ViewUtil.loadImage(
                dialogBinding.articleThumbnail,
                pageSummary.thumbnailUrl,
                placeholderId = R.mipmap.launcher
            )
        }

        val event = viewModel.getEventByPageTitle(pageSummary.apiTitle)
        dialogBinding.relatedEventInfo.text = StringUtil.fromHtml(event.text)

        val isCorrect = viewModel.getQuestionCorrectByPageTitle(pageSummary.apiTitle)
        val answerIcon = if (isCorrect) R.drawable.check_circle_24px else R.drawable.ic_cancel_24px
        val answerIconTintList = if (isCorrect) ResourceUtil.getThemedColorStateList(activity, R.attr.success_color) else ResourceUtil.getThemedColorStateList(activity, R.attr.destructive_color)
        val answerText = if (isCorrect) R.string.on_this_day_game_article_answer_correct_stats_message else R.string.on_this_day_game_article_answer_incorrect_stats_message
        dialogBinding.answerStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(answerIcon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(dialogBinding.answerStatus, answerIconTintList)
        dialogBinding.answerStatus.text = activity.getString(answerText)

        val isSaved = viewModel.savedPages.contains(pageSummary)
        val bookmarkResource = if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
        dialogBinding.saveButton.setImageResource(bookmarkResource)
        dialogBinding.saveButton.setOnClickListener {
            WikiGamesEvent.submit("save_click", "game_play", slideName = "game_end_article")
            onBookmarkIconClick(dialogBinding.saveButton, pageSummary)
        }
        dialogBinding.shareButton.setOnClickListener {
            WikiGamesEvent.submit("share_click", "game_play", slideName = "game_end_article")
            ShareUtil.shareText(activity, pageSummary.getPageTitle(viewModel.wikiSite))
        }
        FeedbackUtil.setButtonTooltip(dialogBinding.shareButton, dialogBinding.saveButton)
        dialogBinding.readArticleButton.setOnClickListener {
            WikiGamesEvent.submit("read_click", "game_play", slideName = "game_end_article")
            val entry = HistoryEntry(pageSummary.getPageTitle(viewModel.wikiSite), HistoryEntry.SOURCE_ON_THIS_DAY_GAME)
            activity.startActivity(PageActivity.newIntentForNewTab(activity, entry, entry.title))
        }
    }

    private fun onBookmarkIconClick(view: ImageView, pageSummary: PageSummary) {
        val pageTitle = pageSummary.getPageTitle(viewModel.wikiSite)
        val isSaved = viewModel.savedPages.contains(pageSummary)
        if (isSaved) {
            LongPressMenu(view, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(activity, pageTitle, addToDefault, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(activity, page.listId, pageTitle, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                    }
                }

                override fun onRemoveRequest() {
                    super.onRemoveRequest()
                    viewModel.savedPages.remove(pageSummary)
                    view.setImageResource(R.drawable.ic_bookmark_border_white_24dp)
                }
            }).show(HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_GAME))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(activity, pageTitle, true, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
            viewModel.savedPages.add(pageSummary)
            view.setImageResource(R.drawable.ic_bookmark_white_24dp)
        }
    }
}
