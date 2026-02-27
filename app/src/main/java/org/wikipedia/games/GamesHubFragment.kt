package org.wikipedia.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.NotificationButtonView
import java.time.LocalDate

class GamesHubFragment : Fragment() {

    private lateinit var notificationButtonView: NotificationButtonView
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

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    GamesHubScreen()
                }
            }
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
    fun GamesHubScreen() {
        val languageList = WikipediaApp.instance.languageState.appLanguageCodes
        var selectedLanguage by remember { mutableStateOf(WikipediaApp.instance.languageState.appLanguageCode) }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            Box(
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
                        val langText = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(langCode) ?: langCode
                        val isEnabled = OnThisDayGameViewModel.isLangSupported(langCode) // TODO: Add check for other games when they are added
                        val isSelected = langCode == selectedLanguage
                        val textColor = if (isEnabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
                        val snackbarMessage = stringResource(R.string.games_hub_activity_games_unavailable_message, langText)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isEnabled) {
                                    FeedbackUtil.makeSnackbar(requireActivity(), snackbarMessage)
                                        .setAction(R.string.games_hub_activity_games_unavailable_message_learn_more_action) {
                                            UriUtil.visitInExternalBrowser(requireActivity(), getString(R.string.on_this_day_game_wiki_languages_url).toUri())
                                        }
                                        .show()
                                    return@FilterChip
                                }
                                selectedLanguage = languageList[index]
                            },
                            colors = FilterChipDefaults.filterChipColors().copy(
                                selectedContainerColor = WikipediaTheme.colors.additionColor
                            ),
                            border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor),
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
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(WikiGames.entries.size) { index ->
                        when (WikiGames.entries[index]) {
                            WikiGames.WHICH_CAME_FIRST -> {
                                if (OnThisDayGameViewModel.isLangSupported(selectedLanguage)) {
                                    Text(
                                        text = stringResource(WikiGames.entries[index].titleRes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = WikipediaTheme.colors.primaryColor
                                    )

                                    // Load fix cards: Today, last 3 days and archive
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(5) { cardIndex ->
                                            val dateTitle = DateUtil.getShortDateString(
                                                LocalDate.now().minusDays(cardIndex.toLong())
                                            )
                                            when (cardIndex) {
                                                0 -> {
                                                    // TODO: put cards with different game states
                                                }

                                                in 1..3 -> {
                                                    // TODO: put cards with different game states
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
        }
    }

    companion object {
        fun newInstance(): GamesHubFragment {
            return GamesHubFragment()
        }
    }
}
