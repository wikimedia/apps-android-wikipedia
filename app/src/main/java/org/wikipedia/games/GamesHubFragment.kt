package org.wikipedia.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.wikigames.OnThisDayGameAction
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.games.onthisday.OnThisDayGameArchiveCalendarHelper
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class GamesHubFragment : Fragment() {
    private lateinit var onThisDayGameArchiveCalendarHelper: OnThisDayGameArchiveCalendarHelper
    private val launchOnThisDayGameActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadOnThisDayGamesPreviews(viewModel.selectedLanguage)
    }

    val viewModel: GamesHubViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        WikiGamesEvent.submit(action = "impression", activeInterface = "games_hub", langCode = viewModel.selectedLanguage)
        onThisDayGameArchiveCalendarHelper = OnThisDayGameArchiveCalendarHelper(
                fragment = this@GamesHubFragment,
                languageCode = viewModel.selectedLanguage,
                onDateSelected = { date ->
                    launchOnThisDayGameActivity.launch(
                        OnThisDayGameActivity.newIntent(
                            context = requireActivity(),
                            invokeSource = InvokeSource.GAMES_HUB,
                            wikiSite = WikiSite.forLanguageCode(viewModel.selectedLanguage),
                            date = date
                        )
                    )
                }
            )
        onThisDayGameArchiveCalendarHelper.register()

        return CoordinatorLayout(requireContext()).apply {
            addView(ComposeView(requireContext()).apply {
                layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
                )
                setContent {
                    val activity = requireActivity()
                    val languageList = WikipediaApp.instance.languageState.appLanguageCodes
                    val transition = rememberInfiniteTransition()
                    BaseTheme {
                        GamesHubScreen(
                            viewModel = viewModel,
                            onThisDayGameUiState = viewModel.onThisDayGameUiState.collectAsState().value,
                            onThisDayGameArchiveCalendarHelper = onThisDayGameArchiveCalendarHelper,
                            onPlay = { gameDate, gameStatus ->
                                launchOnThisDayGameActivity.launch(
                                    OnThisDayGameActivity.newIntent(
                                        context = activity,
                                        invokeSource = InvokeSource.GAMES_HUB,
                                        wikiSite = WikiSite.forLanguageCode(viewModel.selectedLanguage),
                                        date = gameDate,
                                        gameStatus = if (gameStatus == OnThisDayGameAction.ReviewResults)
                                            DailyGameHistory.GAME_COMPLETED else DailyGameHistory.GAME_IN_PROGRESS
                                    )
                                )
                            },
                            onShowArchive = {
                                onThisDayGameArchiveCalendarHelper.show()
                            },
                            onShowDisabledMessage = {
                                FeedbackUtil.makeSnackbar(this, it)
                                    .setAction(R.string.games_hub_activity_games_unavailable_message_learn_more_action) {
                                        UriUtil.visitInExternalBrowser(
                                            activity,
                                            getString(R.string.on_this_day_game_wiki_languages_url).toUri()
                                        )
                                    }
                                    .show()
                            },
                            languageList = languageList,
                            transition = transition
                        )
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onThisDayGameArchiveCalendarHelper.unRegister()
    }

    companion object {
        fun newInstance(): GamesHubFragment {
            return GamesHubFragment()
        }
    }
}
