package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayGameFinalBinding
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.Resource
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class OnThisDayGameFinalFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameFinalBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameFinalBinding.inflate(inflater, container, false)

        binding.resetButton.isVisible = ReleaseUtil.isPreBetaRelease
        binding.resetButton.setOnClickListener {
            viewModel.resetCurrentDay()
        }

        viewModel.gameState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Error -> updateOnError(it.throwable)
                is OnThisDayGameViewModel.GameEnded -> onGameEnded(it.data)
                else -> {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        updateOnLoading()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updateOnLoading() {
        binding.errorView.isVisible = false
        binding.scrollContainer.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun updateOnError(t: Throwable) {
        binding.progressBar.isVisible = false
        binding.scrollContainer.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(t)
    }

    private fun onGameEnded(gameState: OnThisDayGameViewModel.GameState) {
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false
        binding.scrollContainer.isVisible = true
        val totalCorrect = gameState.answerState.count { it }
        binding.resultText.text = getString(R.string.on_this_day_game_result,
            totalCorrect,
            gameState.totalQuestions,
            getString(when(totalCorrect) {
                0 -> R.string.on_this_day_game_encourage0
                1 -> R.string.on_this_day_game_encourage1
                2 -> R.string.on_this_day_game_encourage2
                else -> R.string.on_this_day_game_encourage3
            }))

        var displayStartDate = getStartOfWeekDate(OnThisDayGameViewModel.gameStartDate)
        while (displayStartDate.isBefore(OnThisDayGameViewModel.gameEndDate)) {
            val weekView = WeeklyActivityView(requireContext())
            binding.weeksContainer.addView(weekView)
            weekView.setWeekStats(displayStartDate, gameState)
            displayStartDate = displayStartDate.plusDays(7)
        }
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameFinalFragment {
            return OnThisDayGameFinalFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun getStartOfWeekDate(date: LocalDate, locale: Locale = Locale.getDefault()): LocalDate {
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val daysToSubtract = ((7 + (date.dayOfWeek.value - firstDayOfWeek.value)) % 7).toLong()
            return date.minusDays(daysToSubtract)
        }
    }
}
