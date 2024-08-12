package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil


class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels { OnThisDayGameViewModel.Factory(intent.extras!!) }

    private val yearButtonViews = mutableListOf<View>()
    private val dotViews = mutableListOf<View>()
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
            // TODO
            viewModel.submitCurrentResponse(0)

            dotPulseAnimatorSet.cancel()
        }

        viewModel.gameState.observe(this) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Success -> updateGameState(it.data)
                is Resource.Error -> updateOnError(it.throwable)
            }
        }
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

        // hide and re-show the text to force automatic animation to occur.
        binding.questionText.isVisible = false
        binding.questionText.text = event.text
        binding.questionText.isVisible = true

        val thumbnailUrl = event.pages()?.firstOrNull()?.thumbnailUrl
        if (thumbnailUrl.isNullOrEmpty()) {
            binding.questionThumbnail.isVisible = false
        } else {
            binding.questionThumbnail.isVisible = true
            Glide.with(this)
                .load(thumbnailUrl)
                .centerCrop()
                .into(binding.questionThumbnail)
        }


        // update year buttons with the year selections from the state
        yearButtonViews.forEachIndexed { index, view ->
            val year = gameState.currentQuestionState.yearChoices[index]
            (view as MaterialButton).text = year.toString()
            view.tag = year
        }

        // set color tint of the dots based on the current question index
        dotViews.forEachIndexed { index, view ->
            view.backgroundTintList = if (index == gameState.currentQuestionIndex) {
                ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
            } else {
                if (index > gameState.currentQuestionIndex) {
                    ResourceUtil.getThemedColorStateList(this, R.attr.inactive_color)
                } else {
                    if (gameState.answerState.getOrNull(index) == true) {
                        ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
                    } else {
                        ResourceUtil.getThemedColorStateList(this, R.attr.destructive_color)
                    }
                }
            }
        }

        // animate the dot for the current question
        animateDot(gameState.currentQuestionIndex)

        binding.currentQuestionContainer.isVisible = true
    }

    private fun setButtonHighlighted(button: View? = null) {
        binding.currentQuestionContainer.children.forEach { child ->
            if (child is MaterialButton && child.tag is Int) {
                if (child == button) {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
                    child.setTextColor(Color.WHITE)
                    child.isSelected = true
                } else {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.background_color)
                    child.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
                    child.isSelected = false
                }
            }
        }
        updateSubmitButtonState()
    }

    private fun updateSubmitButtonState() {
        binding.submitButton.isEnabled = yearButtonViews.any { it.isSelected }
        binding.submitButton.alpha = if (binding.submitButton.isEnabled) 1f else 0.5f
    }

    private fun initDynamicViews(gameState: OnThisDayGameViewModel.GameState) {
        // set up dynamic views, if they haven't been added yet
        if (yearButtonViews.isEmpty()) {
            gameState.currentQuestionState.yearChoices.forEach { year ->
                val viewId = View.generateViewId()
                val button = MaterialButton(this)
                yearButtonViews.add(button)
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
            val dotSize = DimenUtil.roundedDpToPx(12f)
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
                val dotView = View(this)
                dotViews.add(dotView)
                dotView.layoutParams = ViewGroup.LayoutParams(dotSize, dotSize)
                dotView.setBackgroundResource(R.drawable.shape_circle)
                dotView.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.inactive_color)
                dotView.id = viewId
                dotView.isVisible = true
                binding.currentQuestionContainer.addView(dotView)
            }
            binding.questionDotsFlow.referencedIds = dotViews.map { it.id }.toIntArray()
            binding.questionDotsFlowPulse.referencedIds = dotPulseViews.map { it.id }.toIntArray()
        }
    }

    private fun animateDot(dotIndex: Int) {
        if (dotIndex >= dotPulseViews.size) {
            return
        }
        dotPulseViews.forEach { it.visibility = View.INVISIBLE }
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
