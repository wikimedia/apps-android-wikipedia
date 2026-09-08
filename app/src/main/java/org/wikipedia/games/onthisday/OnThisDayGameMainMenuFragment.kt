package org.wikipedia.games.onthisday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.FragmentOnThisDayGameMainMenuBinding
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import java.time.LocalDate

class OnThisDayGameMainMenuFragment : OnThisDayGameBaseFragment() {
    private var _binding: FragmentOnThisDayGameMainMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameMainMenuBinding.inflate(inflater, container, false)

        WikiGamesEvent.submit("impression", "game_play", slideName = "game_start", isArchive = viewModel.isArchiveGame)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.errorView.retryClickListener = View.OnClickListener {
            viewModel.loadGameState()
        }
        binding.errorView.backClickListener = View.OnClickListener {
            requireActivity().finish()
        }
        observeGameState()
    }

    private fun observeGameState() {
        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> updateOnLoading()
                is OnThisDayGameViewModel.GameStarted -> handleGameStarted()
                is OnThisDayGameViewModel.CurrentQuestion -> handleCurrentQuestion(state.data)
                is OnThisDayGameViewModel.GameEnded -> handleGameEnded(state.data)
                is Resource.Error -> updateOnError(state.throwable)
            }
        }
    }

    private fun updateOnLoading() {
        binding.gameMenuContainer.isVisible = false
        binding.progressBar.isVisible = true
        binding.errorView.isVisible = false
    }

    private fun showGameMenu() {
        binding.dateText.text = DateUtil.getShortDateString(viewModel.currentDate)
        binding.gameMenuContainer.isVisible = true
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false
    }

    private fun updateOnError(t: Throwable) {
        binding.errorView.isVisible = true
        binding.progressBar.isVisible = false
        binding.gameMenuContainer.isVisible = false
        binding.errorView.setError(t)
        binding.errorView.setIconColorFilter(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        binding.errorView.setErrorTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
    }

    private fun handleGameStarted() {
        showGameMenu()
        with(binding) {
            playGameButton.setOnClickListener {
                WikiGamesEvent.submit("play_click", "game_play", slideName = "game_start", isArchive = viewModel.isArchiveGame)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, OnThisDayGamePlayFragment.newInstance(), null)
                    .commit()
            }
        }
    }

    private fun handleCurrentQuestion(state: OnThisDayGameViewModel.GameState) {
        showGameMenu()
        with(binding) {
            val questionIndex = state.currentQuestionIndex + 1
            gameMessageText.text = getString(R.string.on_this_day_game_current_progress_message, questionIndex)

            val playGameButtonText = if (viewModel.isArchiveGame) getString(R.string.on_this_day_game_continue_playing_btn_text) else getString(R.string.on_this_day_game_continue_btn_text)
            playGameButton.text = playGameButtonText

            playGameButton.setOnClickListener {
                startGame()
            }

            if (viewModel.isArchiveGame) {
                playArchiveButton.isVisible = true
                playArchiveButton.setOnClickListener {
                    prepareAndOpenArchiveCalendar(viewModel.wikiSite.languageCode)
                }
            }
        }
    }

    private fun handleGameEnded(state: OnThisDayGameViewModel.GameState) {
        showGameMenu()
        with(binding) {
            val score = state.answerState.count { it }
            binding.gameMessageText.text = getString(R.string.on_this_day_game_score_message, score, state.totalQuestions)
            playGameButton.text = getString(R.string.on_this_day_game_review_results_btn_text)
            playGameButton.setOnClickListener {
                showGameResults()
            }
            playArchiveButton.isVisible = true
            playArchiveButton.setOnClickListener {
                prepareAndOpenArchiveCalendar(viewModel.wikiSite.languageCode)
            }
        }
    }

    private fun startGame() {
        WikiGamesEvent.submit("play_click", "game_play", slideName = "game_start", isArchive = viewModel.isArchiveGame)
        requireActivity().supportFragmentManager.popBackStack()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OnThisDayGamePlayFragment.newInstance(), null)
            .commit()
    }

    private fun showGameResults() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OnThisDayGameResultFragment.newInstance(viewModel.invokeSource), null)
            .commit()
    }

    override fun onArchiveDateSelected(date: LocalDate) {
        viewModel.relaunchForDate(date)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OnThisDayGamePlayFragment.newInstance(), null)
            .commit()
    }

    companion object {
        fun newInstance(invokeSource: InvokeSource): OnThisDayGameMainMenuFragment {
            return OnThisDayGameMainMenuFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }
    }
}
