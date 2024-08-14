package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
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
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import java.time.MonthDay
import java.time.format.DateTimeFormatter

class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels { OnThisDayGameViewModel.Factory(intent.extras!!) }

    private val yearButtonViews = mutableListOf<Button>()
    private val dotViews = mutableListOf<ImageView>()
    private val dotPulseViews = mutableListOf<View>()
    private val dotPulseAnimatorSet = AnimatorSet()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
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

        binding.submitButton.setOnClickListener {
            (yearButtonViews.firstOrNull { it.isSelected }?.tag as? Int)?.let {
                viewModel.submitCurrentResponse(it)
            }
            dotPulseAnimatorSet.cancel()
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
        initDynamicViews(gameState)
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        val event = gameState.currentQuestionState.event

        MonthDay.of(viewModel.currentMonth, viewModel.currentDay).let {
            it.format(DateTimeFormatter.ofPattern("MMMM d"))
            binding.onDayText.text = getString(R.string.on_this_day_game_on_date, it.format(DateTimeFormatter.ofPattern("MMMM d")))
        }

        binding.questionText.isVisible = true
        binding.questionText.text = event.text

        val thumbnailUrl = event.pages()?.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl.isNullOrEmpty()) {
            binding.questionThumbnail.isVisible = false
        } else {
            binding.questionThumbnail.isVisible = false //true
            Glide.with(this)
                .load(thumbnailUrl)
                .centerCrop()
                .into(binding.questionThumbnail)
        }

        // update year buttons with the year selections from the state
        yearButtonViews.forEachIndexed { index, view ->
            view.isVisible = true
            view.isEnabled = true
            val year = gameState.currentQuestionState.yearChoices[index]
            view.text = year.toString()
            view.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            view.tag = year
        }

        // set color tint of the dots based on the current question index
        dotViews.forEachIndexed { index, view ->
            view.backgroundTintList = if (index == gameState.currentQuestionIndex && !gameState.currentQuestionState.goToNext) {
                view.setImageResource(0)
                ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
            } else {
                if (index > gameState.currentQuestionIndex) {
                    view.setImageResource(0)
                    ResourceUtil.getThemedColorStateList(this, R.attr.inactive_color)
                } else {
                    if (gameState.answerState.getOrNull(index) == true) {
                        view.setImageResource(R.drawable.ic_check_black_24dp)
                        ResourceUtil.getThemedColorStateList(this, R.attr.success_color)
                    } else {
                        view.setImageResource(R.drawable.ic_close_black_24dp)
                        ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
                    }
                }
            }
        }

        // animate the dot for the current question
        animateDot(if (gameState.currentQuestionState.goToNext) -1 else gameState.currentQuestionIndex)

        binding.submitButton.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
        binding.submitButton.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions) R.string.on_this_day_game_finish else R.string.on_this_day_game_submit)
        binding.currentQuestionContainer.isVisible = true
    }

    private fun onCurrentQuestionCorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

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
    }

    private fun onCurrentQuestionIncorrect(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

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
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        updateGameState(gameState)

        yearButtonViews.forEach {
            it.isEnabled = false
            it.isVisible = false
        }
        binding.questionText.isVisible = false
        binding.questionThumbnail.isVisible = false

        setSubmitEnabled(false, isNext = true)
        binding.submitButton.setText(R.string.on_this_day_game_finish)

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

    private fun setButtonHighlighted(button: View? = null) {
        var atLeastOneHighlighted = false
        binding.currentQuestionContainer.children.forEach { child ->
            if (child is MaterialButton && child.tag is Int) {
                child.isSelected = child == button
                if (child == button) {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
                    child.setTextColor(Color.WHITE)
                    atLeastOneHighlighted = true
                } else {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.background_color)
                    child.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
                    child.isSelected = false
                }
            }
        }
        setSubmitEnabled(atLeastOneHighlighted)
    }

    private fun initDynamicViews(gameState: OnThisDayGameViewModel.GameState) {
        if (yearButtonViews.isEmpty()) {
            gameState.currentQuestionState.yearChoices.forEach { year ->
                val viewId = View.generateViewId()
                val button = MaterialButton(this)
                yearButtonViews.add(button)
                button.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT)
                TextViewCompat.setCompoundDrawableTintList(button, ColorStateList.valueOf(Color.WHITE))
                button.id = viewId
                button.tag = 0
                binding.currentQuestionContainer.addView(button)
                button.setOnClickListener {
                    setButtonHighlighted(it)
                }
            }
            binding.yearButtonsFlow.referencedIds = yearButtonViews.map { it.id }.toIntArray()
        }
        setButtonHighlighted()

        if (dotViews.isEmpty()) {
            val dotSize = DimenUtil.roundedDpToPx(20f)
            for (i in 0 until gameState.totalQuestions) {
                val pulseViewId = View.generateViewId()
                val pulseView = View(this)
                dotPulseViews.add(pulseView)
                pulseView.layoutParams = ViewGroup.LayoutParams(dotSize, dotSize)
                pulseView.setBackgroundResource(R.drawable.shape_circle)
                pulseView.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
                pulseView.id = pulseViewId
                binding.currentQuestionContainer.addView(pulseView)

                val viewId = View.generateViewId()
                val dotView = ImageView(this)
                dotViews.add(dotView)
                dotView.layoutParams = ViewGroup.LayoutParams(dotSize, dotSize)
                dotView.setPadding(DimenUtil.roundedDpToPx(1f))
                dotView.setBackgroundResource(R.drawable.shape_circle)
                dotView.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.inactive_color)
                dotView.imageTintList = ColorStateList.valueOf(Color.WHITE)
                dotView.id = viewId
                dotView.isVisible = true
                binding.currentQuestionContainer.addView(dotView)
            }
            binding.questionDotsFlow.referencedIds = dotViews.map { it.id }.toIntArray()
            binding.questionDotsFlowPulse.referencedIds = dotPulseViews.map { it.id }.toIntArray()
        }
    }

    private fun setSubmitEnabled(enabled: Boolean, isNext: Boolean = false) {
        binding.submitButton.backgroundTintList = if (isNext) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray600)) else ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
        binding.submitButton.setText(if (isNext) R.string.on_this_day_game_next else R.string.on_this_day_game_submit)
        binding.submitButton.isEnabled = enabled
        binding.submitButton.alpha = if (enabled) 1f else 0.5f
    }

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

    companion object {

        fun newIntent(context: Context, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, OnThisDayGameActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
