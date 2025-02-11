package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.history.HistoryEntry
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.ViewUtil
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class OnThisDayGameActivity : BaseActivity(), BaseActivity.Callback {

    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels()

    private val goNextAnimatorSet = AnimatorSet()
    private val cardAnimatorSet = AnimatorSet()
    private lateinit var mediaPlayer: MediaPlayer
    private var newStatusBarInsets: Insets? = null
    private var newNavBarInsets: Insets? = null
    private var toolbarHeight: Int = 0
    private var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>? = null

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        callback = this

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        mediaPlayer = MediaPlayer.create(this, R.raw.sound_logo)

        binding.errorView.retryClickListener = View.OnClickListener {
            viewModel.loadGameState()
        }
        binding.errorView.backClickListener = View.OnClickListener {
            finish()
        }

        binding.questionCard1.setOnClickListener {
            if (viewModel.gameState.value is OnThisDayGameViewModel.CurrentQuestion || viewModel.gameState.value is OnThisDayGameViewModel.GameStarted) {
                viewModel.submitCurrentResponse((it.tag as OnThisDay.Event).year)
            }
        }
        binding.questionCard2.setOnClickListener {
            if (viewModel.gameState.value is OnThisDayGameViewModel.CurrentQuestion || viewModel.gameState.value is OnThisDayGameViewModel.GameStarted) {
                viewModel.submitCurrentResponse((it.tag as OnThisDay.Event).year)
            }
        }

        binding.nextQuestionText.setOnClickListener {
            viewModel.submitCurrentResponse(0)
        }

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val newStatusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val newNavBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            this.newStatusBarInsets = newStatusBarInsets
            this.newNavBarInsets = newNavBarInsets
            toolbarHeight = DimenUtil.getToolbarHeightPx(this)

            binding.appBarLayout.updatePadding(top = newStatusBarInsets.top)

            var params = binding.currentQuestionContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = toolbarHeight + newStatusBarInsets.top + newNavBarInsets.top
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            params.bottomMargin = newStatusBarInsets.bottom + newNavBarInsets.bottom

            params = binding.fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = toolbarHeight + newStatusBarInsets.top + newNavBarInsets.top
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            params.bottomMargin = newStatusBarInsets.bottom + newNavBarInsets.bottom

            binding.bottomSheetCoordinatorLayout.updatePadding(
                top = calculateBottomSheetTopPadding(newStatusBarInsets.top + newNavBarInsets.top),
                bottom = newStatusBarInsets.bottom + newNavBarInsets.bottom,
                left = newStatusBarInsets.left + newNavBarInsets.left,
                right = newStatusBarInsets.right + newNavBarInsets.right
            )

            windowInsets
        }

        viewModel.gameState.observe(this) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is OnThisDayGameViewModel.CurrentQuestion -> onCurrentQuestion(it.data)
                is OnThisDayGameViewModel.GameStarted -> onGameStarted(it.data)
                is OnThisDayGameViewModel.CurrentQuestionCorrect -> onCurrentQuestionCorrect(it.data)
                is OnThisDayGameViewModel.CurrentQuestionIncorrect -> onCurrentQuestionIncorrect(it.data)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded(it.data)
                is Resource.Error -> updateOnError(it.throwable)
            }
        }

        updateOnLoading()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (viewModel.gameState.value is OnThisDayGameViewModel.GameEnded) {
            menuInflater.inflate(R.menu.menu_on_this_day_game, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val notificationItem = menu.findItem(R.id.menu_notifications)
        notificationItem?.setIcon(Prefs.otdNotificationState.getIcon())
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (viewModel.gameState.value !is OnThisDayGameViewModel.GameStarted
                    && viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
                    showPauseDialog()
                    true
                } else {
                    onFinish()
                    true
                }
            }
            R.id.menu_learn_more -> {
                UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.on_this_day_game_wiki_url)))
                true
            }
            R.id.menu_report_feature -> {
                FeedbackUtil.composeEmail(this,
                    subject = getString(R.string.on_this_day_game_report_email_subject),
                    body = getString(R.string.on_this_day_game_report_email_body))
                true
            }
            R.id.menu_notifications -> {
                OnThisDayGameNotificationManager(this).handleNotificationClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }
        if (viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
            showPauseDialog()
            return
        }
        super.onBackPressed()
        onFinish()
    }

    private fun onFinish() {
        if (WikipediaApp.instance.haveMainActivity) {
            finish()
        } else {
            goToMainTab()
        }
    }

    private fun goToMainTab() {
        startActivity(MainActivity.newIntent(this)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
            .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()))
        finish()
    }

    private fun showPauseDialog() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon)
            .setIcon(R.drawable.ic_pause_filled_24)
            .setTitle(R.string.on_this_day_game_pause_title)
            .setMessage(R.string.on_this_day_game_pause_body)
            .setPositiveButton(R.string.on_this_day_game_pause_positive) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.on_this_day_game_pause_negative, null)
            .show()
    }

    @SuppressLint("RestrictedApi")
    private fun calculateBottomSheetTopPadding(insets: Int): Int {
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

    private fun updateOnLoading() {
        binding.errorView.isVisible = false
        binding.progressText.isVisible = false
        binding.scoreText.isVisible = false
        binding.dateText.isVisible = false
        binding.currentQuestionContainer.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.isVisible = false
        binding.progressText.isVisible = false
        binding.scoreText.isVisible = false
        binding.dateText.isVisible = false
        binding.currentQuestionContainer.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(t)
    }

    private fun updateGameState(gameState: OnThisDayGameViewModel.GameState) {
        goNextAnimatorSet.cancel()

        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        binding.progressText.isVisible = true
        binding.scoreText.isVisible = true
        binding.dateText.isVisible = true
        binding.questionDate1.isVisible = false
        binding.questionDate2.isVisible = false
        binding.questionStatusIcon1.isVisible = false
        binding.questionStatusIcon2.isVisible = false

        MonthDay.of(viewModel.currentMonth, viewModel.currentDay).let {
            binding.dateText.text = it.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMM d")))
        }

        binding.progressText.text = getString(R.string.on_this_day_game_progress, gameState.currentQuestionIndex + 1, gameState.totalQuestions)
        binding.scoreText.text = getString(R.string.on_this_day_game_score, gameState.answerState.count { it })

        val event1 = gameState.currentQuestionState.event1
        val event2 = gameState.currentQuestionState.event2

        binding.questionCard1.tag = event1
        binding.questionCard2.tag = event2

        binding.questionDate1.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event1.year, viewModel.currentMonth, viewModel.currentDay))
        binding.questionText1.maxLines = Integer.MAX_VALUE
        binding.questionText1.text = event1.text
        binding.questionText1.post {
            if (!isDestroyed) {
                // this seems to be the only way to properly ellipsize the text in its layout.
                if (binding.questionText1.lineHeight > 0) {
                    binding.questionText1.maxLines = (binding.questionText1.measuredHeight / binding.questionText1.lineHeight)
                }
            }
        }

        val thumbnailUrl1 = event1.pages.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl1.isNullOrEmpty()) {
            binding.questionThumbnail1.setImageResource(R.mipmap.launcher)
        } else {
            ViewUtil.loadImage(binding.questionThumbnail1, thumbnailUrl1, placeholderId = R.mipmap.launcher)
        }

        binding.questionDate2.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event2.year, viewModel.currentMonth, viewModel.currentDay))
        binding.questionText2.maxLines = Integer.MAX_VALUE
        binding.questionText2.text = event2.text
        binding.questionText2.post {
            if (!isDestroyed) {
                // this seems to be the only way to properly ellipsize the text in its layout.
                if (binding.questionText2.lineHeight > 0) {
                    binding.questionText2.maxLines = (binding.questionText2.measuredHeight / binding.questionText2.lineHeight)
                }
            }
        }

        val thumbnailUrl2 = event2.pages.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl2.isNullOrEmpty()) {
            binding.questionThumbnail2.setImageResource(R.mipmap.launcher)
        } else {
            ViewUtil.loadImage(binding.questionThumbnail2, thumbnailUrl2, placeholderId = R.mipmap.launcher)
        }

        binding.whichCameFirstText.isVisible = true
        binding.whichCameFirstText.setText(R.string.on_this_day_game_title)
        binding.pointsText.isVisible = false
        binding.nextQuestionText.isVisible = false

        binding.questionDate1.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
        binding.questionDate2.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
        binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_neutral)
        binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_neutral)

        binding.centerContent.isVisible = false
        binding.currentQuestionContainer.isVisible = true
        supportInvalidateOptionsMenu()
    }

    private fun onGameStarted(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.dateText.isVisible = false
        binding.progressText.isVisible = false

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, OnThisDayGameOnboardingFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.progressText.isVisible = false
        binding.scoreText.isVisible = false
        binding.currentQuestionContainer.isVisible = false

        mediaPlayer.start()

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, OnThisDayGameFinalFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
    }

    private fun onCurrentQuestion(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)
        animateQuestions()
    }

    private fun onCurrentQuestionCorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.whichCameFirstText.isVisible = false
        binding.correctIncorrectText.setText(R.string.on_this_day_game_correct)
        binding.pointsText.isVisible = true
        binding.nextQuestionText.isVisible = false
        binding.centerContent.isVisible = true

        if (gameState.currentQuestionState.event1.year < gameState.currentQuestionState.event2.year) {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate1.setTextColor(Color.WHITE)
            setCorrectIcon(binding.questionStatusIcon1)
        } else {
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate2.setTextColor(Color.WHITE)
            setCorrectIcon(binding.questionStatusIcon2)
        }

        enqueueGoNext(gameState)
    }

    private fun onCurrentQuestionIncorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.whichCameFirstText.isVisible = false
        binding.correctIncorrectText.setText(R.string.on_this_day_game_incorrect)
        binding.nextQuestionText.isVisible = false
        binding.centerContent.isVisible = true

        if (gameState.currentQuestionState.event1.year < gameState.currentQuestionState.event2.year) {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_incorrect)
            setCorrectIcon(binding.questionStatusIcon1)
            setIncorrectIcon(binding.questionStatusIcon2)
        } else {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_incorrect)
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_correct)
            setIncorrectIcon(binding.questionStatusIcon1)
            setCorrectIcon(binding.questionStatusIcon2)
        }
        binding.questionDate1.setTextColor(Color.WHITE)
        binding.questionDate2.setTextColor(Color.WHITE)

        enqueueGoNext(gameState)
    }

    private fun setCorrectIcon(view: ImageView) {
        view.setImageResource(R.drawable.check_circle_24px)
        view.imageTintList = ResourceUtil.getThemedColorStateList(this, R.attr.success_color)
        view.isVisible = true
    }

    private fun setIncorrectIcon(view: ImageView) {
        view.setImageResource(R.drawable.ic_cancel_24px)
        view.imageTintList = ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
        view.isVisible = true
    }

    private fun enqueueGoNext(gameState: OnThisDayGameViewModel.GameState) {
        binding.questionDate1.isVisible = true
        binding.questionDate2.isVisible = true

        binding.nextQuestionText.postDelayed({
            if (!isDestroyed) {
                animateGoNext(gameState)
            }
        }, 500)
    }

    fun openArticleBottomSheet(pageSummary: PageSummary, updateBookmark: () -> Unit) {
        if (viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
            return
        }
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
                val topPadding = toolbarHeight + slideOffset * ((newStatusBarInsets?.top ?: 0) - toolbarHeight)
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
        val answerIconTintList = if (isCorrect) ResourceUtil.getThemedColorStateList(this, R.attr.success_color) else ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
        val answerText = if (isCorrect) R.string.on_this_day_game_article_answer_correct_stats_message else R.string.on_this_day_game_article_answer_incorrect_stats_message
        dialogBinding.answerStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(answerIcon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(dialogBinding.answerStatus, answerIconTintList)
        dialogBinding.answerStatus.text = getString(answerText)

        val isSaved = viewModel.savedPages.contains(pageSummary)
        val bookmarkResource = if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
        dialogBinding.saveButton.setImageResource(bookmarkResource)
        dialogBinding.saveButton.setOnClickListener {
            onBookmarkIconClick(dialogBinding.saveButton, pageSummary)
        }
        dialogBinding.shareButton.setOnClickListener {
            ShareUtil.shareText(this, pageSummary.getPageTitle(WikipediaApp.instance.wikiSite))
        }
        FeedbackUtil.setButtonTooltip(dialogBinding.shareButton, dialogBinding.saveButton)
        dialogBinding.readArticleButton.setOnClickListener {
            val entry = HistoryEntry(pageSummary.getPageTitle(WikipediaApp.instance.wikiSite), HistoryEntry.SOURCE_ON_THIS_DAY_GAME)
            startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
        }
    }

    private fun onBookmarkIconClick(view: ImageView, pageSummary: PageSummary) {
        val pageTitle = pageSummary.getPageTitle(WikipediaApp.instance.wikiSite)
        val isSaved = viewModel.savedPages.contains(pageSummary)
        if (isSaved) {
            LongPressMenu(view, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(this@OnThisDayGameActivity, pageTitle, addToDefault, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(this@OnThisDayGameActivity, page.listId, pageTitle, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
                    }
                }

                override fun onRemoveRequest() {
                    super.onRemoveRequest()
                    viewModel.savedPages.remove(pageSummary)
                    view.setImageResource(R.drawable.ic_bookmark_border_white_24dp)
                }
            }).show(HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_GAME))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(this@OnThisDayGameActivity, pageTitle, true, InvokeSource.ON_THIS_DAY_GAME_ACTIVITY)
            viewModel.savedPages.add(pageSummary)
            view.setImageResource(R.drawable.ic_bookmark_white_24dp)
        }
    }

    fun animateQuestions() {
        binding.dateText.isVisible = true
        binding.progressText.isVisible = true

        binding.whichCameFirstText.alpha = 0f
        binding.questionCard1.alpha = 0f
        binding.questionCard2.alpha = 0f
        val textA1 = ObjectAnimator.ofFloat(binding.whichCameFirstText, "alpha", 0f, 1f)
        val translationX1 = ObjectAnimator.ofFloat(binding.questionCard1, "translationX", DimenUtil.dpToPx(400f), 0f)
        val translationA1 = ObjectAnimator.ofFloat(binding.questionCard1, "alpha", 0f, 1f)
        val translationX2 = ObjectAnimator.ofFloat(binding.questionCard2, "translationX", DimenUtil.dpToPx(400f), 0f)
        val translationA2 = ObjectAnimator.ofFloat(binding.questionCard2, "alpha", 0f, 1f)

        val duration = 750L
        textA1.setDuration(duration)
        textA1.interpolator = DecelerateInterpolator()
        translationX1.setDuration(duration)
        translationX1.startDelay = duration
        translationX1.interpolator = DecelerateInterpolator()
        translationA1.setDuration(duration)
        translationA1.startDelay = duration
        translationA1.interpolator = DecelerateInterpolator()
        translationX2.setDuration(duration)
        translationX2.startDelay = duration * 2
        translationX2.interpolator = DecelerateInterpolator()
        translationA2.setDuration(duration)
        translationA2.startDelay = duration * 2
        translationA2.interpolator = DecelerateInterpolator()

        binding.questionCard1.isEnabled = false
        binding.questionCard2.isEnabled = false
        cardAnimatorSet.cancel()
        cardAnimatorSet.playTogether(textA1, translationX1, translationA1, translationX2, translationA2)
        cardAnimatorSet.doOnEnd {
            binding.questionCard1.isEnabled = true
            binding.questionCard2.isEnabled = true
        }
        cardAnimatorSet.start()
    }

    private fun animateGoNext(gameState: OnThisDayGameViewModel.GameState) {
        binding.whichCameFirstText.isVisible = false
        binding.nextQuestionText.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
        binding.nextQuestionText.isVisible = true

        val translationX = ObjectAnimator.ofFloat(binding.nextQuestionText, "translationX", 0f, DimenUtil.dpToPx(-8f), 0f)

        val duration = 1500L
        translationX.setDuration(duration)
        translationX.interpolator = AccelerateDecelerateInterpolator()
        translationX.repeatCount = ObjectAnimator.INFINITE

        goNextAnimatorSet.cancel()
        goNextAnimatorSet.playTogether(translationX)
        goNextAnimatorSet.start()
    }

    fun requestPermissionAndScheduleGameNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, invokeSource: Constants.InvokeSource, wikiSite: WikiSite): Intent {
            val intent = Intent(context, OnThisDayGameActivity::class.java)
                .putExtra(Constants.ARG_WIKISITE, wikiSite)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            if (Prefs.lastOtdGameDateOverride.isNotEmpty()) {
                val date = try {
                    LocalDate.parse(Prefs.lastOtdGameDateOverride, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    LocalDate.now()
                }
                intent.putExtra(OnThisDayGameViewModel.EXTRA_DATE, date.atStartOfDay().toInstant(ZoneOffset.UTC).epochSecond)
            }
            return intent
        }
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
        }
    }
}
