package org.wikipedia.games

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.feed.wikigames.OnThisDayGameAction
import org.wikipedia.feed.wikigames.OnThisDayGameCardCompleted
import org.wikipedia.feed.wikigames.OnThisDayGameCardPreview
import org.wikipedia.feed.wikigames.OnThisDayGameCardProgress
import org.wikipedia.feed.wikigames.OnThisDayGameCardSimple
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.games.onthisday.OnThisDayGameArchiveCalendarHelper
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.UriUtil
import org.wikipedia.views.NotificationButtonView
import java.time.LocalDate

class GamesHubFragment : Fragment() {

    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: GamesHubViewModel by viewModels()
    private var selectedLanguage: String = WikipediaApp.instance.languageState.appLanguageCodes.first {
        WikiGames.WHICH_CAME_FIRST.isLangSupported(it)
    }
    private val menuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_games_hub_overflow, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_game_stats -> {
                    val intent = MainActivity.newIntent(requireContext())
                        .apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())
                            putExtra(Constants.INTENT_EXTRA_SCROLL_TO_GAMES, true)
                        }
                    startActivity(intent)
                    requireActivity().finish()
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

        return ComposeView(requireContext()).apply {
            setContent {
                val transition = rememberInfiniteTransition()
                BaseTheme {
                    GamesHubScreen(
                        onThisDayGameUiState = viewModel.onThisDayGameUiState.collectAsState().value,
                        transition = transition
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
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

    @Composable
    fun GamesHubScreen(
        onThisDayGameUiState: UiState<List<OnThisDayCardGameState>>,
        transition: InfiniteTransition
    ) {
        val languageList = WikipediaApp.instance.languageState.appLanguageCodes
        var selectedLanguage by remember { mutableStateOf(selectedLanguage) }
        var isRefreshing by remember { mutableStateOf(false) }
        val state = rememberPullToRefreshState()

        var onThisDayGameArchiveCalendarHelper by remember { mutableStateOf(OnThisDayGameArchiveCalendarHelper(
                fragment = this@GamesHubFragment,
                languageCode = selectedLanguage,
                onDateSelected = { }
            ))
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->

            PullToRefreshBox(
                onRefresh = {
                    isRefreshing = true
                    viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                },
                isRefreshing = isRefreshing,
                state = state,
                indicator = {
                    Indicator(
                        state = state,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = WikipediaTheme.colors.paperColor,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(languageList.size) { index ->
                            val langCode = languageList[index]
                            GamesHubLanguageChip(
                                isSelected = langCode == selectedLanguage,
                                langCode = langCode,
                                onSelected = {
                                    selectedLanguage = langCode
                                    this@GamesHubFragment.selectedLanguage = langCode
                                    viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                    onThisDayGameArchiveCalendarHelper.unRegister()
                                    onThisDayGameArchiveCalendarHelper = OnThisDayGameArchiveCalendarHelper(
                                        fragment = this@GamesHubFragment,
                                        languageCode = selectedLanguage,
                                        onDateSelected = { date ->
                                            startActivity(OnThisDayGameActivity.newIntent(requireActivity(), InvokeSource.GAMES_HUB, WikiSite.forLanguageCode(selectedLanguage), date))
                                        }
                                    ).also {
                                        it.register()
                                    }
                                }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(WikiGames.entries.size) { index ->
                            when (WikiGames.entries[index]) {
                                WikiGames.WHICH_CAME_FIRST -> {
                                    when (onThisDayGameUiState) {
                                        is UiState.Loading -> {
                                            GamesHubLoadingShimmer(transition = transition)
                                        }

                                        is UiState.Error -> {
                                            isRefreshing = false
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                WikiErrorView(
                                                    modifier = Modifier
                                                        .fillMaxWidth(),
                                                    caught = onThisDayGameUiState.error,
                                                    errorClickEvents = WikiErrorClickEvents {
                                                        isRefreshing = true
                                                        viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                                    },
                                                    retryForGenericError = true
                                                )
                                            }
                                        }

                                        is UiState.Success -> {
                                            isRefreshing = false
                                            OnThisDayGameCards(
                                                position = index,
                                                selectedLanguage = selectedLanguage,
                                                gamesData = onThisDayGameUiState.data,
                                                onThisDayGameAction = { gameStatus, gameDate ->
                                                    when (gameStatus) {
                                                        OnThisDayGameAction.PlayArchive -> {
                                                            onThisDayGameArchiveCalendarHelper.show()
                                                        }
                                                        OnThisDayGameAction.CountdownFinished -> {
                                                            isRefreshing = true
                                                            viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                                        }
                                                        else -> {
                                                            startActivity(
                                                                OnThisDayGameActivity.newIntent(
                                                                    context = requireActivity(),
                                                                    invokeSource = InvokeSource.FEED,
                                                                    wikiSite = WikiSite.forLanguageCode(selectedLanguage),
                                                                    date = gameDate,
                                                                    gameStatus = if (gameStatus == OnThisDayGameAction.ReviewResults)
                                                                        DailyGameHistory.GAME_COMPLETED else DailyGameHistory.GAME_IN_PROGRESS
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GamesHubLoadingShimmer(
        transition: InfiniteTransition
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                    .height(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(
                        heightMultiplier = 0f,
                        transition = transition
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    )
                    .height(350.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(
                        heightMultiplier = 0f,
                        transition = transition
                    )
            )
        }
    }

    @Composable
    fun GamesHubLanguageChip(
        isSelected: Boolean,
        langCode: String,
        onSelected: () -> Unit,
    ) {
        val langText = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(langCode) ?: langCode
        val isEnabled = WikiGames.WHICH_CAME_FIRST.isLangSupported(langCode) // TODO: Add check for other games when they are added
        val textColor = if (isEnabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
        val snackbarMessage = stringResource(
            R.string.games_hub_activity_games_unavailable_message,
            langText
        )
        FilterChip(
            selected = isSelected,
            onClick = {
                if (!isEnabled) {
                    FeedbackUtil.makeSnackbar(
                        requireActivity(),
                        snackbarMessage
                    )
                        .setAction(R.string.games_hub_activity_games_unavailable_message_learn_more_action) {
                            UriUtil.visitInExternalBrowser(
                                requireActivity(),
                                getString(R.string.on_this_day_game_wiki_languages_url).toUri()
                            )
                        }
                        .show()
                    return@FilterChip
                }
                onSelected()
            },
            colors = FilterChipDefaults.filterChipColors().copy(
                selectedContainerColor = WikipediaTheme.colors.additionColor
            ),
            border = BorderStroke(
                width = 1.dp,
                color = WikipediaTheme.colors.borderColor
            ),
            label = {
                Text(
                    text = langText,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            },
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_black_24dp),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = null
                    )
                }
            }
        )
    }

    @Composable
    fun OnThisDayGameCards(
        position: Int,
        selectedLanguage: String,
        gamesData: List<OnThisDayCardGameState>,
        onThisDayGameAction: (OnThisDayGameAction, LocalDate) -> Unit
    ) {
        if (WikiGames.WHICH_CAME_FIRST.isLangSupported(selectedLanguage)) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = requireContext().getString(selectedLanguage, WikiGames.entries[position].titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = WikipediaTheme.colors.primaryColor
            )

            // Load fix cards: Today, last 3 days and archive
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(5) { cardIndex ->
                    val gameDate = LocalDate.now().minusDays(cardIndex.toLong())
                    val dateTitle = DateUtil.getMonthOnlyDateString(gameDate)
                    when (cardIndex) {
                        in 0..3 -> {
                            val gameState = gamesData.getOrNull(cardIndex) ?: return@items
                            val isArchiveGame = cardIndex != 0
                            OnThisDayGameCardContent(
                                modifier = Modifier
                                    .width(if (!isArchiveGame) 320.dp else 170.dp)
                                    .height(350.dp)
                                    .padding(vertical = 8.dp),
                                game = WikiGame.OnThisDayGame(gameState),
                                dateTitle = dateTitle,
                                isArchiveGame = isArchiveGame,
                                onThisDayGameAction = {
                                    onThisDayGameAction(it, gameDate)
                                }
                            )
                        }
                        else -> {
                            OnThisDayGameCardSimple(
                                modifier = Modifier
                                    .width(170.dp)
                                    .height(350.dp)
                                    .padding(vertical = 8.dp),
                                iconRes = R.drawable.event_repeat_24dp,
                                iconTint = WikipediaTheme.colors.primaryColor,
                                titleText = stringResource(R.string.on_this_day_game_card_archive_label),
                                onPlayClick = {
                                    onThisDayGameAction(OnThisDayGameAction.PlayArchive, gameDate)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun OnThisDayGameCardContent(
        modifier: Modifier,
        game: WikiGame.OnThisDayGame,
        dateTitle: String,
        isArchiveGame: Boolean,
        onThisDayGameAction: (OnThisDayGameAction) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (game.state) {
                is OnThisDayCardGameState.Preview -> {
                    if (!isArchiveGame) {
                        OnThisDayGameCardPreview(
                            modifier = modifier,
                            state = game.state,
                            titleText = dateTitle,
                            onPlayClick = {
                                onThisDayGameAction(OnThisDayGameAction.Play)
                            }
                        )
                    } else {
                        OnThisDayGameCardSimple(
                            modifier = modifier,
                            iconRes = R.drawable.ic_events_24dp,
                            iconTint = WikipediaTheme.colors.progressiveColor,
                            titleText = dateTitle,
                            onPlayClick = {
                                onThisDayGameAction(OnThisDayGameAction.Play)
                            }
                        )
                    }
                }

                is OnThisDayCardGameState.InProgress -> {
                    OnThisDayGameCardProgress(
                        modifier = modifier,
                        isArchiveGame = isArchiveGame,
                        state = game.state,
                        titleText = dateTitle,
                        onContinueClick = {
                            onThisDayGameAction(OnThisDayGameAction.Play)
                        }
                    )
                }

                is OnThisDayCardGameState.Completed -> {
                    OnThisDayGameCardCompleted(
                        modifier = modifier,
                        isArchiveGame = isArchiveGame,
                        state = game.state,
                        titleText = dateTitle,
                        onPlayClick = { onThisDayGameAction(OnThisDayGameAction.Play) },
                        onReviewResult = { onThisDayGameAction(OnThisDayGameAction.ReviewResults) },
                        onPlayTheArchive = { onThisDayGameAction(OnThisDayGameAction.PlayArchive) },
                        onCountDownFinished = { onThisDayGameAction(OnThisDayGameAction.CountdownFinished) }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(): GamesHubFragment {
            return GamesHubFragment()
        }
    }
}
