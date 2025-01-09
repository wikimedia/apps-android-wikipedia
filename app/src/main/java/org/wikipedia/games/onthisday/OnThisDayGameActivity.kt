package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels { OnThisDayGameViewModel.Factory(intent.extras!!) }

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.on_this_day_game_title)

        binding.errorView.retryClickListener = View.OnClickListener {
            viewModel.loadGameState()
        }
        binding.errorView.backClickListener = View.OnClickListener {
            finish()
        }

        binding.questionCard1.setOnClickListener {
            if (viewModel.gameState.value is Resource.Success || viewModel.gameState.value is OnThisDayGameViewModel.GameStarted) {
                viewModel.submitCurrentResponse((it.tag as OnThisDay.Event).year)
            }
        }
        binding.questionCard2.setOnClickListener {
            if (viewModel.gameState.value is Resource.Success || viewModel.gameState.value is OnThisDayGameViewModel.GameStarted) {
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

            params = binding.dateText.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = DimenUtil.roundedDpToPx(20f) + newStatusBarInsets.top + newNavBarInsets.top

            windowInsets
        }

        viewModel.gameState.observe(this) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Success -> updateGameState(it.data)
                is OnThisDayGameViewModel.GameStarted -> onGameStarted(it.data)
                is OnThisDayGameViewModel.CurrentQuestionCorrect -> onCurrentQuestionCorrect(it.data)
                is OnThisDayGameViewModel.CurrentQuestionIncorrect -> onCurrentQuestionIncorrect(it.data)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded(it.data)
                is Resource.Error -> updateOnError(it.throwable)
            }
        }

        updateOnLoading()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_on_this_day_game, menu)
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
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        binding.progressText.isVisible = true
        binding.scoreText.isVisible = true
        binding.dateText.isVisible = true

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
        binding.questionText1.text = event1.text

        val thumbnailUrl1 = event1.pages.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl1.isNullOrEmpty()) {
            binding.questionThumbnail1.isVisible = false
        } else {
            binding.questionThumbnail1.isVisible = true
            Glide.with(this)
                .load(thumbnailUrl1)
                .centerCrop()
                .into(binding.questionThumbnail1)
        }

        binding.questionDate2.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event2.year, viewModel.currentMonth, viewModel.currentDay))
        binding.questionText2.text = event2.text

        val thumbnailUrl2 = event2.pages.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl2.isNullOrEmpty()) {
            binding.questionThumbnail2.isVisible = false
        } else {
            binding.questionThumbnail2.isVisible = true
            Glide.with(this)
                .load(thumbnailUrl2)
                .centerCrop()
                .into(binding.questionThumbnail2)
        }

        binding.whichCameFirstText.isVisible = true
        binding.whichCameFirstText.setText(R.string.on_this_day_game_title)
        binding.pointsText.isVisible = false
        binding.nextQuestionText.isVisible = false

        binding.questionDate1.isVisible = false
        binding.questionDate2.isVisible = false
        binding.questionDate1.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
        binding.questionDate2.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
        binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_neutral)
        binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_neutral)

        binding.questionCard1.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = 0 }
        binding.questionCard2.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 0 }

        binding.currentQuestionContainer.isVisible = true
    }


    private fun onCurrentQuestionCorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.whichCameFirstText.setText(R.string.on_this_day_game_correct)
        binding.pointsText.isVisible = true
        binding.nextQuestionText.isVisible = false

        if (gameState.currentQuestionState.event1.year < gameState.currentQuestionState.event2.year) {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate1.setTextColor(Color.WHITE)
        } else {
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate2.setTextColor(Color.WHITE)
        }

        revealQuestionDates(gameState)
    }

    private fun onCurrentQuestionIncorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.whichCameFirstText.setText(R.string.on_this_day_game_incorrect)
        binding.nextQuestionText.isVisible = false

        if (gameState.currentQuestionState.event1.year < gameState.currentQuestionState.event2.year) {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_correct)
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_incorrect)
        } else {
            binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_incorrect)
            binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_correct)
        }
        binding.questionDate1.setTextColor(Color.WHITE)
        binding.questionDate2.setTextColor(Color.WHITE)

        revealQuestionDates(gameState)
    }

    private fun revealQuestionDates(gameState: OnThisDayGameViewModel.GameState) {
        binding.questionDate1.isVisible = true
        binding.questionDate2.isVisible = true

        binding.questionCard1.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = DimenUtil.roundedDpToPx(-10f) }
        binding.questionCard2.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = DimenUtil.roundedDpToPx(-10f) }

        // TODO: animate?
        binding.nextQuestionText.postDelayed({
            if (!isDestroyed) {
                binding.whichCameFirstText.isVisible = false
                binding.pointsText.isVisible = false
                binding.nextQuestionText.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
                binding.nextQuestionText.isVisible = true
            }
        }, 2000)
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        binding.progressText.isVisible = false
        binding.scoreText.isVisible = false
        binding.currentQuestionContainer.isVisible = false

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentOverlayContainer, OnThisDayGameFinalFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
    }

    private fun onGameStarted(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentOverlayContainer, OnThisDayGameOnboardingFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
    }

    /*
    private fun animateDot(dotIndex: Int) {
        dotPulseViews.forEach { it.visibility = View.INVISIBLE }
        if (dotIndex < 0 || dotIndex >= dotPulseViews.size) {
            return
        }
        val dotPulseView = dotPulseViews[dotIndex]
        dotPulseView.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(dotPulseView, "scaleX", 1f, 2.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(dotPulseView, "scaleY", 1f, 2.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(dotPulseView, "alpha", 0.8f, 0.2f, 1f)

        val duration = 1500L
        scaleX.setDuration(duration)
        scaleY.setDuration(duration)
        alpha.setDuration(duration)

        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        alpha.interpolator = AccelerateDecelerateInterpolator()

        dotPulseAnimatorSet.cancel()
        dotPulseAnimatorSet.playTogether(scaleX, scaleY, alpha)
        dotPulseAnimatorSet.doOnEnd { dotPulseAnimatorSet.start() }
        dotPulseAnimatorSet.start()
    }
    */

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
