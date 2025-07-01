package org.wikipedia.games.onthisday

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.FragmentOnThisDayGamePlayBinding
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil
import org.wikipedia.views.WikiCardView
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class OnThisDayGamePlayFragment : Fragment() {
    private var _binding: FragmentOnThisDayGamePlayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()
    private val cardAnimatorSetIn = AnimatorSet()
    private val cardAnimatorSetOut = AnimatorSet()
    private lateinit var mediaPlayer: MediaPlayer
    private var selectedCardView: WikiCardView? = null
    private var mainActivity: OnThisDayGameActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnThisDayGamePlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaPlayer = MediaPlayer()
        mainActivity = (activity as? OnThisDayGameActivity)

        binding.errorView.retryClickListener = View.OnClickListener {
            viewModel.loadGameState()
        }

        binding.errorView.backClickListener = View.OnClickListener {
            requireActivity().finish()
        }

        binding.questionCard1.setOnClickListener {
            enqueueSubmit(it as WikiCardView)
        }
        binding.questionContainer1.setOnClickListener {
            enqueueSubmit(binding.questionCard1)
        }
        binding.questionText1.setOnClickListener {
            enqueueSubmit(binding.questionCard1)
        }
        binding.questionCard2.setOnClickListener {
            enqueueSubmit(it as WikiCardView)
        }
        binding.questionContainer2.setOnClickListener {
            enqueueSubmit(binding.questionCard2)
        }
        binding.questionText2.setOnClickListener {
            enqueueSubmit(binding.questionCard2)
        }
        binding.questionText1.movementMethod = ScrollingMovementMethod()
        binding.questionText2.movementMethod = ScrollingMovementMethod()

        binding.nextQuestionText.setOnClickListener {
            binding.nextQuestionText.isVisible = false
            if (selectedCardView != null) {
                val event = (selectedCardView!!.tag as OnThisDay.Event)
                resetCardBorders()
                selectedCardView = null
                viewModel.submitCurrentResponse(event.year)
            } else {
                viewModel.submitCurrentResponse(0)
                binding.nextQuestionText.isVisible = false
            }
        }

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val newStatusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val newNavBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.currentQuestionContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = newStatusBarInsets.left + newNavBarInsets.left
                rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            }

            binding.bottomContent.updatePadding(bottom = newStatusBarInsets.bottom + newNavBarInsets.bottom)

            windowInsets
        }

        binding.scoreView.generateViews(OnThisDayGameViewModel.MAX_QUESTIONS)
        viewModel.gameState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is OnThisDayGameViewModel.CurrentQuestion -> onCurrentQuestion(it.data)
                is OnThisDayGameViewModel.GameStarted -> onGameStarted(it.data)
                is OnThisDayGameViewModel.CurrentQuestionCorrect -> onCurrentQuestionCorrect(it.data)
                is OnThisDayGameViewModel.CurrentQuestionIncorrect -> onCurrentQuestionIncorrect(it.data)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded()
                is Resource.Error -> updateOnError(it.throwable)
            }
        }
    }

    private fun updateOnLoading() {
        binding.errorView.isVisible = false
        binding.scoreView.isVisible = false

        binding.currentQuestionContainer.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun onGameStarted(gameState: OnThisDayGameViewModel.GameState) {
        updateInitialScores(gameState)
        updateGameState(gameState)
    }

    private fun onCurrentQuestion(gameState: OnThisDayGameViewModel.GameState) {
        updateInitialScores(gameState)
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

    private fun setCorrectIcon(view: ImageView) {
        view.setImageResource(R.drawable.check_circle_24px)
        view.imageTintList = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.success_color)
        view.isVisible = true
    }

    private fun setIncorrectIcon(view: ImageView) {
        view.setImageResource(R.drawable.ic_cancel_24px)
        view.imageTintList = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.destructive_color)
        view.isVisible = true
    }

    private fun enqueueGoNext(gameState: OnThisDayGameViewModel.GameState) {
        binding.questionDate1.isVisible = true
        binding.questionDate2.isVisible = true
        binding.questionCard1.isEnabled = false
        binding.questionCard2.isEnabled = false
        binding.questionText1.isEnabled = false
        binding.questionText2.isEnabled = false

        binding.whichCameFirstText.isVisible = false
        binding.nextQuestionText.setText(if (gameState.currentQuestionIndex >= gameState.totalQuestions - 1) R.string.on_this_day_game_finish else R.string.on_this_day_game_next)
        binding.nextQuestionText.isVisible = true
    }

    fun animateQuestionsIn() {
        WikiGamesEvent.submit("impression", "game_play", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
        mainActivity?.showAppBarDateText()
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
        binding.questionText1.isEnabled = false
        binding.questionText2.isEnabled = false
        cardAnimatorSetIn.removeAllListeners()
        cardAnimatorSetIn.cancel()
        cardAnimatorSetIn.playTogether(textA1, translationX1, translationA1, translationX2, translationA2)
        cardAnimatorSetIn.doOnEnd {
            binding.questionCard1.isEnabled = true
            binding.questionCard2.isEnabled = true
            binding.questionText1.isEnabled = true
            binding.questionText2.isEnabled = true
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
                if (!requireActivity().isDestroyed) {
                    onFinished()
                }
            }
        }
        cardAnimatorSetOut.start()
    }

    private fun onGameEnded() {
        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.fragmentContainer, OnThisDayGameOverFragment.newInstance(viewModel.invokeSource), null)
            ?.addToBackStack(null)
            ?.commit()
        mainActivity?.setResult(RESULT_OK, Intent().putExtra(OnThisDayGameOverFragment.EXTRA_GAME_COMPLETED, true))

        playSound("sound_logo")
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.isVisible = false
        binding.scoreView.isVisible = false
        mainActivity?.hideAppBarDateText()
        binding.currentQuestionContainer.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(t)
    }

    fun updateGameState(gameState: OnThisDayGameViewModel.GameState) {
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        binding.scoreView.isVisible = true
        mainActivity?.showAppBarDateText()
        binding.questionDate1.isVisible = false
        binding.questionDate2.isVisible = false
        binding.questionStatusIcon1.isVisible = false
        binding.questionStatusIcon2.isVisible = false

        MonthDay.of(viewModel.currentMonth, viewModel.currentDay).let {
            val text = it.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMM d")))
            mainActivity?.updateAppBarDateText(text)
        }

        binding.scoreView.updateScore(gameState.answerState, gameState.currentQuestionIndex, gameState.currentQuestionState.goToNext)

        val event1 = gameState.currentQuestionState.event1
        val event2 = gameState.currentQuestionState.event2

        resetCardBorders()
        binding.questionCard1.tag = event1
        binding.questionCard2.tag = event2

        binding.questionDate1.text = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.of(event1.year, viewModel.currentMonth, viewModel.currentDay))
        binding.questionText1.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 0 }
        binding.questionText1.text = event1.text
        binding.questionText1.scrollY = 0

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
        binding.questionText2.scrollY = 0

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

        binding.questionDate1.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_color))
        binding.questionDate2.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_color))
        binding.questionDate1.setBackgroundResource(R.drawable.game_date_background_neutral)
        binding.questionDate2.setBackgroundResource(R.drawable.game_date_background_neutral)

        binding.centerContent.isVisible = false
        binding.correctIncorrectText.text = null
        binding.currentQuestionContainer.isVisible = true

        binding.root.post {
            if (!requireActivity().isDestroyed) {
                binding.questionContainer1.minimumHeight = binding.questionScroll1.height - DimenUtil.roundedDpToPx(16f)
                binding.questionContainer2.minimumHeight = binding.questionScroll2.height - DimenUtil.roundedDpToPx(16f)
            }
        }
        mainActivity?.supportInvalidateOptionsMenu()
    }

    private fun enqueueSubmit(cardView: WikiCardView) {
        if (viewModel.gameState.value is OnThisDayGameViewModel.CurrentQuestion || viewModel.gameState.value is OnThisDayGameViewModel.GameStarted) {
            resetCardBorders()

            binding.nextQuestionText.setText(R.string.on_this_day_game_submit)
            binding.nextQuestionText.isVisible = true

            cardView.setStrokeColor(ResourceUtil.getThemedColorStateList(requireContext(), R.attr.progressive_color))
            cardView.setStrokeWidth(DimenUtil.roundedDpToPx(2f))
            selectedCardView = cardView
        }
    }

    private fun resetCardBorders() {
        val otherCardView = if (selectedCardView == binding.questionCard1) binding.questionCard2 else binding.questionCard1
        binding.questionCard1.setStrokeColor(otherCardView.strokeColorStateList)
        binding.questionCard1.setStrokeWidth(otherCardView.strokeWidth)
        binding.questionCard2.setStrokeColor(otherCardView.strokeColorStateList)
        binding.questionCard2.setStrokeWidth(otherCardView.strokeWidth)
    }

    fun updateInitialScores(gameState: OnThisDayGameViewModel.GameState) {
        binding.scoreView.updateInitialScores(gameState.answerState, gameState.currentQuestionIndex)
    }

    fun playSound(soundName: String) {
        if (Prefs.isOtdSoundOn) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(
                    requireContext(),
                    "android.resource://${requireContext().packageName}/raw/$soundName".toUri()
                )
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    companion object {
        fun newInstance(): OnThisDayGamePlayFragment {
            return OnThisDayGamePlayFragment()
        }
    }
}
