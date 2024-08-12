package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.util.Resource


class OnThisDayGameActivity : BaseActivity() {
    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels { OnThisDayGameViewModel.Factory(intent.extras!!) }

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
            viewModel.submitCurrentResponse()

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
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false


        val event = viewModel.events[gameState.currentEventIndex % viewModel.events.size]
        binding.questionText.text = event.text
        binding.yearChoice1.text = event.year.toString()

        animateDot(1)


        binding.currentQuestionContainer.isVisible = true
    }

    private fun animateDot(dotIndex: Int) {
        val dotView = when (dotIndex) {
            0 -> binding.questionDot1pulse
            1 -> binding.questionDot2pulse
            else -> binding.questionDot3pulse
        }
        binding.questionDot1pulse.isVisible = false
        binding.questionDot2pulse.isVisible = false
        binding.questionDot3pulse.isVisible = false
        dotView.isVisible = true

        // Create ObjectAnimators for scaleX, scaleY, and alpha
        val scaleX = ObjectAnimator.ofFloat(dotView, "scaleX", 1f, 2.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(dotView, "scaleY", 1f, 2.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(dotView, "alpha", 0.8f, 0.2f, 1f)

        // Set duration and interpolator for smooth easing
        val duration = 1500
        scaleX.setDuration(duration.toLong())
        scaleY.setDuration(duration.toLong())
        alpha.setDuration(duration.toLong())

        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        alpha.interpolator = AccelerateDecelerateInterpolator()


        // Create an AnimatorSet to play animations together
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
