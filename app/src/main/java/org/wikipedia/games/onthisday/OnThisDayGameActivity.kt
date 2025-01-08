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
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
            // viewModel.submitCurrentResponse(it)
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
        binding.currentQuestionContainer.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.isVisible = false
        binding.currentQuestionContainer.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(t)
    }

    private fun updateGameState(gameState: OnThisDayGameViewModel.GameState) {
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        val event = gameState.currentQuestionState.event

        binding.questionDate1.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event.year, viewModel.currentMonth, viewModel.currentDay))

        binding.questionText1.text = event.text

        val thumbnailUrl = event.pages.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl.isNullOrEmpty()) {
            binding.questionThumbnail1.isVisible = false
        } else {
            binding.questionThumbnail1.isVisible = true
            Glide.with(this)
                .load(thumbnailUrl)
                .centerCrop()
                .into(binding.questionThumbnail1)
        }

        //binding.submitButton.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
        //binding.submitButton.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions) R.string.on_this_day_game_finish else R.string.on_this_day_game_submit)

        binding.currentQuestionContainer.isVisible = true
    }


    private fun onCurrentQuestionCorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        /*
        yearButtonViews.forEach {
            it.isEnabled = false
            if ((it.tag as Int) == gameState.currentQuestionState.event.year) {
                it.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.success_color)
                it.setTextColor(Color.WHITE)
                it.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_black_24dp, 0)
                it.isSelected = true
            } else {
                it.isSelected = false
            }
        }

        setSubmitEnabled(true, isNext = true)
        binding.submitButton.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
         */
    }

    private fun onCurrentQuestionIncorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        /*
        yearButtonViews.forEach {
            it.isEnabled = false
            if ((it.tag as Int) == gameState.currentQuestionState.event.year) {
                it.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.success_color)
                it.setTextColor(Color.WHITE)
                it.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_black_24dp, 0)
                it.isSelected = true
            } else if ((it.tag as Int) == gameState.currentQuestionState.yearSelected) {
                it.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
                it.setTextColor(Color.WHITE)
                it.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_close_black_24dp, 0)
                it.isSelected = true
            } else {
                it.isSelected = false
            }
        }

        yearButtonViews.firstOrNull { (it.tag as Int) == gameState.currentQuestionState.event.year }?.let {
            it.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.success_color)
            it.setTextColor(Color.WHITE)
        }

        yearButtonViews.firstOrNull { (it.tag as Int) == gameState.currentQuestionState.yearSelected }?.let {
            it.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
            it.setTextColor(Color.WHITE)
        }

        setSubmitEnabled(true, isNext = true)
        binding.submitButton.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
         */
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        /*
        binding.questionText.isVisible = false
        binding.questionThumbnail.isVisible = false

        setSubmitEnabled(false, isNext = true)
        binding.submitButton.setText(R.string.on_this_day_game_finish)
         */

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

    private fun setSubmitEnabled(enabled: Boolean, isNext: Boolean = false) {
        /*
        binding.submitButton.backgroundTintList = if (isNext) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray600)) else ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
        binding.submitButton.setText(if (isNext) R.string.on_this_day_game_next else R.string.on_this_day_game_submit)
        binding.submitButton.isEnabled = enabled
        binding.submitButton.alpha = if (enabled) 1f else 0.5f
         */
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
