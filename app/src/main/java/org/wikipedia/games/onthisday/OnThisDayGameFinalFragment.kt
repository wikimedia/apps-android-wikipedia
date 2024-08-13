package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentOnThisDayGameFinalBinding
import org.wikipedia.util.Resource

class OnThisDayGameFinalFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameFinalBinding? = null
    val binding get() = _binding!!

    private val viewModel: OnThisDayGameFinalViewModel by viewModels { OnThisDayGameFinalViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameFinalBinding.inflate(inflater, container, false)

        binding.resetButton.setOnClickListener {

        }

        viewModel.gameState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> updateOnLoading()
                is Resource.Success -> updateState(it.data)
                is Resource.Error -> updateOnError(it.throwable)
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

    private fun updateState(gameState: OnThisDayGameViewModel.GameState) {
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
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameFinalFragment {
            return OnThisDayGameFinalFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
