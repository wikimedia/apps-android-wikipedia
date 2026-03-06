package org.wikipedia.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.wikigames.OnThisDayGameAction
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.games.onthisday.OnThisDayGameArchiveCalendarHelper
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.NotificationButtonView

class GamesHubFragment : Fragment() {

    private lateinit var notificationButtonView: NotificationButtonView
    private lateinit var onThisDayGameArchiveCalendarHelper: OnThisDayGameArchiveCalendarHelper
    private val viewModel: GamesHubViewModel by viewModels()
    private val launchOnThisDayGameActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.loadOnThisDayGamesPreviews(viewModel.selectedLanguage)
    }
    private val menuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_games_hub_overflow, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_game_stats -> {
                    // TODO: open the Activity Tab
                    true
                }
                R.id.menu_learn_more -> {
                    UriUtil.visitInExternalBrowser(requireActivity(), getString(R.string.games_hub_wiki_url).toUri())
                    true
                }
                else -> false
            }
        }

        override fun onPrepareMenu(menu: Menu) {
            val notificationMenuItem = menu.findItem(R.id.menu_notifications)
            if (AccountUtil.isLoggedIn) {
                notificationMenuItem.isVisible = true
                notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
                notificationButtonView.setOnClickListener {
                    if (AccountUtil.isLoggedIn) {
                        startActivity(NotificationActivity.newIntent(requireActivity()))
                    }
                }
                notificationButtonView.contentDescription = getString(R.string.notifications_activity_title)
                notificationMenuItem.actionView = notificationButtonView
                notificationMenuItem.expandActionView()
                FeedbackUtil.setButtonTooltip(notificationButtonView)
            } else {
                notificationMenuItem.isVisible = false
            }
            updateNotificationDot(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        notificationButtonView = NotificationButtonView(requireActivity())

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

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onThisDayGameArchiveCalendarHelper.unRegister()
    }

    fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    companion object {
        fun newInstance(): GamesHubFragment {
            return GamesHubFragment()
        }
    }
}
