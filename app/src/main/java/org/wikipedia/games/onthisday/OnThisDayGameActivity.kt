package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels()

    private val goNextAnimatorSet = AnimatorSet()
    private val cardAnimatorSet = AnimatorSet()
    private lateinit var mediaPlayer: MediaPlayer

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.on_this_day_game_title)
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

            binding.appBarLayout.updatePadding(top = newStatusBarInsets.top)

            var params = binding.currentQuestionContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = DimenUtil.getToolbarHeightPx(this) + newStatusBarInsets.top + newNavBarInsets.top
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            params.bottomMargin = newStatusBarInsets.bottom + newNavBarInsets.bottom

            params = binding.fragmentContainerFinish.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = DimenUtil.getToolbarHeightPx(this) + newStatusBarInsets.top + newNavBarInsets.top
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            params.bottomMargin = newStatusBarInsets.bottom + newNavBarInsets.bottom

            params = binding.dateText.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = DimenUtil.roundedDpToPx(20f) + newStatusBarInsets.top + newNavBarInsets.top

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
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_help -> {
                // TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
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

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerStart, OnThisDayGameOnboardingFragment.newInstance(viewModel.invokeSource), null)
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
            .add(R.id.fragmentContainerFinish, OnThisDayGameFinalFragment.newInstance(viewModel.invokeSource), null)
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

    fun animateQuestions() {
        binding.questionCard1.alpha = 0f
        binding.questionCard2.alpha = 0f
        val translationX1 = ObjectAnimator.ofFloat(binding.questionCard1, "translationX", DimenUtil.dpToPx(400f), 0f)
        val translationA1 = ObjectAnimator.ofFloat(binding.questionCard1, "alpha", 0f, 1f)
        val translationX2 = ObjectAnimator.ofFloat(binding.questionCard2, "translationX", DimenUtil.dpToPx(400f), 0f)
        val translationA2 = ObjectAnimator.ofFloat(binding.questionCard2, "alpha", 0f, 1f)

        val duration = 750L
        translationX1.setDuration(duration)
        translationX1.interpolator = DecelerateInterpolator()
        translationA1.setDuration(duration)
        translationA1.interpolator = DecelerateInterpolator()
        translationX2.setDuration(duration)
        translationX2.startDelay = duration
        translationX2.interpolator = DecelerateInterpolator()
        translationA2.setDuration(duration)
        translationA2.startDelay = duration
        translationA2.interpolator = DecelerateInterpolator()

        cardAnimatorSet.cancel()
        cardAnimatorSet.playTogether(translationX1, translationA1, translationX2, translationA2)
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

    companion object {
        fun newIntent(context: Context, invokeSource: Constants.InvokeSource, date: LocalDate? = null): Intent {
            val intent = Intent(context, OnThisDayGameActivity::class.java)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            if (date != null) {
                intent.putExtra(OnThisDayGameViewModel.EXTRA_DATE, date.atStartOfDay().toInstant(ZoneOffset.UTC).epochSecond)
            }
            return intent
        }
    }
}
