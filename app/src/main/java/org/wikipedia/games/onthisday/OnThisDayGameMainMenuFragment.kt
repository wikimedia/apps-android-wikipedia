package org.wikipedia.games.onthisday

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.FragmentOnThisDayGameMainMenuBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
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
        binding.dateText.text = DateUtil.getShortDateString(viewModel.currentDate)
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
                    prepareAndOpenArchiveCalendar(viewModel)
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
                prepareAndOpenArchiveCalendar(viewModel)
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
        private const val SHOW_ON_EXPLORE_FEED_COUNT = 2

        fun newInstance(invokeSource: InvokeSource): OnThisDayGameMainMenuFragment {
            return OnThisDayGameMainMenuFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun maybeShowOnThisDayGameDialog(activity: Activity, invokeSource: InvokeSource, articleWikiSite: WikiSite = WikipediaApp.instance.wikiSite) {
            val wikiSite = WikipediaApp.instance.wikiSite
            // Both of the primary language and the article language should be in the supported languages list.
            if (!Prefs.otdEntryDialogShown &&
                OnThisDayGameViewModel.isLangSupported(wikiSite.languageCode) &&
                OnThisDayGameViewModel.isLangSupported(articleWikiSite.languageCode) &&
                (invokeSource != InvokeSource.FEED || Prefs.exploreFeedVisitCount >= SHOW_ON_EXPLORE_FEED_COUNT)) {
                Prefs.otdEntryDialogShown = true
                WikiGamesEvent.submit("impression", "game_modal")
                val dialogView = activity.layoutInflater.inflate(R.layout.dialog_on_this_day_game, null)
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setView(dialogView)
                    .setCancelable(false)
                    .show()
                dialogView.findViewById<Button>(R.id.playGameButton).setOnClickListener {
                    WikiGamesEvent.submit("enter_click", "game_modal")
                    activity.startActivityForResult(OnThisDayGameActivity.newIntent(activity, invokeSource, wikiSite), 0)
                    dialog.dismiss()
                }
                dialogView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
                    FeedbackUtil.showMessage(activity, R.string.on_this_day_game_entry_dialog_snackbar_message)
                    dialog.dismiss()
                }
            }
        }
    }
}
