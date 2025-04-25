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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
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

    private val cardAnimatorSetIn = AnimatorSet()
    private val cardAnimatorSetOut = AnimatorSet()
    private lateinit var mediaPlayer: MediaPlayer

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        callback = this

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        mediaPlayer = MediaPlayer()

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

        // Add long-press listeners to the cards
        binding.questionCard1.setOnLongClickListener {
            showFullCardText(binding.questionText1, binding.questionThumbnail1, true)
            binding.questionText1.tag = true
            true
        }
        binding.questionCard1.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && (binding.questionText1.tag as? Boolean) == true) {
                showFullCardText(binding.questionText1, binding.questionThumbnail1, false)
            }
            false
        }

        binding.questionCard2.setOnLongClickListener {
            showFullCardText(binding.questionText2, binding.questionThumbnail2, true)
            binding.questionText2.tag = true
            true
        }
        binding.questionCard2.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && (binding.questionText2.tag as? Boolean) == true) {
                showFullCardText(binding.questionText2, binding.questionThumbnail2, false)
            }
            false
        }

        binding.nextQuestionText.setOnClickListener {
            viewModel.submitCurrentResponse(0)
            binding.nextQuestionText.isVisible = false
        }

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val newStatusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val newNavBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            val toolbarHeight = DimenUtil.getToolbarHeightPx(this)

            binding.appBarLayout.updatePadding(top = newStatusBarInsets.top)

            binding.currentQuestionContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = toolbarHeight + newStatusBarInsets.top + newNavBarInsets.top
                leftMargin = newStatusBarInsets.left + newNavBarInsets.left
                rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            }

            binding.bottomContent.updatePadding(bottom = newStatusBarInsets.bottom + newNavBarInsets.bottom)

            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = toolbarHeight + newStatusBarInsets.top + newNavBarInsets.top
                leftMargin = newStatusBarInsets.left + newNavBarInsets.left
                rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            }
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
                onBackPressed()
                true
            }
            R.id.menu_learn_more -> {
                WikiGamesEvent.submit("about_click", "game_play", slideName = viewModel.getCurrentScreenName())
                UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.on_this_day_game_wiki_url)))
                true
            }
            R.id.menu_report_feature -> {
                WikiGamesEvent.submit("report_click", "game_play", slideName = viewModel.getCurrentScreenName())

                FeedbackUtil.composeEmail(this,
                    subject = getString(R.string.on_this_day_game_report_email_subject),
                    body = getString(R.string.on_this_day_game_report_email_body))
                true
            }
            R.id.menu_notifications -> {
                WikiGamesEvent.submit("notification_click", "game_play", slideName = viewModel.getCurrentScreenName())

                OnThisDayGameNotificationManager.handleNotificationClick(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
        }
    }

    override fun onBackPressed() {
        WikiGamesEvent.submit("exit_click", "game_play", slideName = viewModel.getCurrentScreenName())
        if (viewModel.gameState.value !is Resource.Loading &&
            !isOnboardingFragmentVisible() &&
            viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
            showPauseDialog()
            return
        }
        if (isShareFragmentVisible()) {
            supportFragmentManager.popBackStack()
            return
        }
        super.onBackPressed()
        onFinish()
    }

    private fun isShareFragmentVisible(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) is OnThisDayGameShareFragment
    }

    private fun isOnboardingFragmentVisible(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) is OnThisDayGameOnboardingFragment
    }

    private fun onFinish() {
        if (viewModel.invokeSource == Constants.InvokeSource.NOTIFICATION) {
            goToMainTab()
        } else {
            finish()
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
        WikiGamesEvent.submit("impression", "pause_modal", slideName = viewModel.getCurrentScreenName())
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon)
            .setIcon(R.drawable.ic_pause_filled_24)
            .setTitle(R.string.on_this_day_game_pause_title)
            .setMessage(R.string.on_this_day_game_pause_body)
            .setPositiveButton(R.string.on_this_day_game_pause_positive) { _, _ ->
                WikiGamesEvent.submit("pause_click", "pause_modal", slideName = viewModel.getCurrentScreenName())
                finish()
            }
            .setNegativeButton(R.string.on_this_day_game_pause_negative) { _, _ ->
                WikiGamesEvent.submit("cancel_click", "pause_modal", slideName = viewModel.getCurrentScreenName())
            }
            .show()
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
        binding.questionText1.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 0 }
        binding.questionText1.text = event1.text
        layoutTextViewForEllipsize(binding.questionText1)

        val thumbnailUrl1 = viewModel.getThumbnailUrlForEvent(event1)
        binding.questionThumbnail1.tag = thumbnailUrl1.isNullOrEmpty()
        if (thumbnailUrl1.isNullOrEmpty()) {
            binding.questionThumbnail1.isVisible = false
        } else {
            binding.questionThumbnail1.isVisible = true
            ViewUtil.loadImage(binding.questionThumbnail1, thumbnailUrl1, placeholderId = R.mipmap.launcher)
        }

        binding.questionDate2.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event2.year, viewModel.currentMonth, viewModel.currentDay))
        binding.questionText2.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 0 }
        binding.questionText2.text = event2.text
        layoutTextViewForEllipsize(binding.questionText2)

        val thumbnailUrl2 = viewModel.getThumbnailUrlForEvent(event2)
        binding.questionThumbnail2.tag = thumbnailUrl2.isNullOrEmpty()
        if (thumbnailUrl2.isNullOrEmpty()) {
            binding.questionThumbnail2.isVisible = false
        } else {
            binding.questionThumbnail2.isVisible = true
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
        binding.correctIncorrectText.text = null
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

        setResult(RESULT_OK, Intent().putExtra(OnThisDayGameFinalFragment.EXTRA_GAME_COMPLETED, true))

        binding.progressText.isVisible = false
        binding.scoreText.isVisible = false
        binding.currentQuestionContainer.isVisible = false

        playSound("sound_logo")

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, OnThisDayGameFinalFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
    }

    private fun onCurrentQuestion(gameState: OnThisDayGameViewModel.GameState) {
        if (gameState.currentQuestionIndex > 0 && binding.questionText1.text.isNotEmpty()) {
            animateQuestionsOut {
                updateGameState(gameState)
                animateQuestionsIn()
            }
        } else {
            updateGameState(gameState)
            animateQuestionsIn()
        }
    }

    private fun onCurrentQuestionCorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        updateQuestionEndLayout()
        binding.correctIncorrectText.setText(R.string.on_this_day_game_correct)
        binding.pointsText.isVisible = true

        if (gameState.currentQuestionState.event1.year < gameState.currentQuestionState.event2.year) {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate1.setTextColor(Color.WHITE)
            setCorrectIcon(binding.questionStatusIcon1)
        } else {
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate2.setTextColor(Color.WHITE)
            setCorrectIcon(binding.questionStatusIcon2)
        }
        playSound("sound_completion")
        enqueueGoNext(gameState)
    }

    private fun onCurrentQuestionIncorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        updateQuestionEndLayout()
        binding.correctIncorrectText.setText(R.string.on_this_day_game_incorrect)

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

        playSound("sound_error")
        enqueueGoNext(gameState)
    }

    private fun updateQuestionEndLayout() {
        binding.whichCameFirstText.isVisible = false
        binding.nextQuestionText.isVisible = false
        binding.centerContent.isVisible = true
        if (!binding.questionThumbnail1.isVisible) {
            binding.questionText1.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = DimenUtil.roundedDpToPx(40f) }
        }
        if (!binding.questionThumbnail2.isVisible) {
            binding.questionText2.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = DimenUtil.roundedDpToPx(40f) }
        }
    }

    private fun layoutTextViewForEllipsize(textView: TextView, ellipsize: Boolean = true) {
        textView.maxLines = Int.MAX_VALUE
        textView.post {
            if (!isDestroyed && ellipsize) {
                // this seems to be the only way to properly ellipsize the text in its layout.
                if (textView.lineHeight > 0) {
                    textView.maxLines = (textView.measuredHeight / textView.lineHeight)
                }
            }
        }
    }

    private fun showFullCardText(textView: TextView, imageView: ImageView, showFullText: Boolean) {
        imageView.isVisible = !showFullText && (imageView.tag as? Boolean) == false
        layoutTextViewForEllipsize(textView, !showFullText)
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
        binding.questionCard1.isEnabled = false
        binding.questionCard2.isEnabled = false

        binding.whichCameFirstText.isVisible = false
        binding.nextQuestionText.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
        binding.nextQuestionText.isVisible = true
    }

    fun animateQuestionsIn() {
        WikiGamesEvent.submit("impression", "game_play", slideName = viewModel.getCurrentScreenName())
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
        var delay = 500L
        val interpolator = DecelerateInterpolator()
        textA1.duration = duration
        textA1.startDelay = delay
        textA1.interpolator = interpolator
        translationX1.duration = duration
        delay += duration
        translationX1.startDelay = delay
        translationX1.interpolator = interpolator
        translationA1.duration = duration
        translationA1.startDelay = delay
        delay += duration
        translationA1.interpolator = interpolator
        translationX2.duration = duration
        translationX2.startDelay = delay
        translationX2.interpolator = interpolator
        translationA2.duration = duration
        translationA2.startDelay = delay
        translationA2.interpolator = interpolator

        binding.questionCard1.isEnabled = false
        binding.questionCard2.isEnabled = false
        cardAnimatorSetIn.removeAllListeners()
        cardAnimatorSetIn.cancel()
        cardAnimatorSetIn.playTogether(textA1, translationX1, translationA1, translationX2, translationA2)
        cardAnimatorSetIn.doOnEnd {
            binding.questionCard1.isEnabled = true
            binding.questionCard2.isEnabled = true
        }
        cardAnimatorSetIn.start()
    }

    fun animateQuestionsOut(onFinished: () -> Unit) {
        binding.questionCard1.alpha = 1f
        binding.questionCard2.alpha = 1f
        binding.questionDate1.isInvisible = true
        binding.questionDate2.isInvisible = true
        binding.centerContent.isInvisible = true

        val translationX1 = ObjectAnimator.ofFloat(binding.questionCard1, "translationX", 0f, DimenUtil.dpToPx(-400f))
        val translationA1 = ObjectAnimator.ofFloat(binding.questionCard1, "alpha", 1f, 0f)
        val translationX2 = ObjectAnimator.ofFloat(binding.questionCard2, "translationX", 0f, DimenUtil.dpToPx(-400f))
        val translationA2 = ObjectAnimator.ofFloat(binding.questionCard2, "alpha", 1f, 0f)

        val duration = 250L
        val interpolator = AccelerateInterpolator()
        translationX1.duration = duration
        translationX1.interpolator = interpolator
        translationA1.duration = duration
        translationA1.interpolator = interpolator
        translationX2.duration = duration
        translationX2.startDelay = duration
        translationX2.interpolator = interpolator
        translationA2.duration = duration
        translationA2.startDelay = duration
        translationA2.interpolator = interpolator

        cardAnimatorSetOut.removeAllListeners()
        cardAnimatorSetOut.cancel()
        cardAnimatorSetOut.playTogether(translationX1, translationA1, translationX2, translationA2)
        cardAnimatorSetOut.doOnEnd {
            binding.root.post {
                if (!isDestroyed) {
                    onFinished()
                }
            }
        }
        cardAnimatorSetOut.start()
    }

    fun requestPermissionAndScheduleGameNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
                }
                else -> requestPermissionLauncher.launch(permission)
            }
        } else {
            OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
        }
    }

    private fun playSound(soundName: String) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, Uri.parse("android.resource://$packageName/raw/$soundName"))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            L.e(e)
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
}
