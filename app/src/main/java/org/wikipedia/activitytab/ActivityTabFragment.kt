package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import java.time.LocalDateTime

class ActivityTabFragment : Fragment() {
    interface Callback {
        fun onNavigateTo(navTab: NavTab)
    }

    private val viewModel: ActivityTabViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Prefs.activityTabRedDotShown = true

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        userName = AccountUtil.userName,
                        readingHistoryState = viewModel.readingHistoryState.collectAsState().value,
                        donationUiState = viewModel.donationUiState.collectAsState().value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReadingHistory()
        requireActivity().invalidateOptionsMenu()
    }

    fun showOverflowMenu() {
        val toolbar = (requireActivity() as MainActivity).getToolbar()
        val anchorView = toolbar.findViewById<View>(R.id.menu_overflow_button)
        ActivityTabOverflowMenu(requireActivity(), anchorView).show(
            menuRes = R.menu.menu_activity_tab_overflow,
            onMenuItemClick = { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_customize_activity_tab -> {
                        println("orange: menu_customize_activity_tab")
                        startActivity(ActivityTabCustomizationActivity.newIntent(requireContext()))
                        true
                    }
                    R.id.menu_learn_more -> {
                        println("orange: learn more")
                        true
                    }
                    R.id.menu_report_feature -> {
                        println("orange: problem with feature")
                        true
                    }
                    else -> false
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ActivityTabScreen(
        userName: String,
        readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
        donationUiState: UiState<String?>
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            var isRefreshing by remember { mutableStateOf(false) }
            val state = rememberPullToRefreshState()

            if (readingHistoryState is UiState.Success) {
                isRefreshing = false
            }

            PullToRefreshBox(
                onRefresh = {
                    isRefreshing = true
                    viewModel.loadReadingHistory()
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
                LazyColumn {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(paddingValues)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            WikipediaTheme.colors.paperColor,
                                            WikipediaTheme.colors.additionColor
                                        )
                                    )
                                )
                        ) {
                            ReadingHistoryModule(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                userName = userName,
                                readingHistoryState = readingHistoryState,
                                onArticlesReadClick = { callback()?.onNavigateTo(NavTab.SEARCH) },
                                onArticlesSavedClick = { callback()?.onNavigateTo(NavTab.READING_LISTS) },
                                onExploreClick = { callback()?.onNavigateTo(NavTab.EXPLORE) },
                                onCategoryItemClick = { category ->
                                    val pageTitle = viewModel.createPageTitleForCategory(category)
                                    startActivity(
                                        CategoryActivity.newIntent(
                                            requireActivity(),
                                            pageTitle
                                        )
                                    )
                                },
                                wikiErrorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = {
                                        viewModel.loadReadingHistory()
                                    }
                                )
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(paddingValues)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            WikipediaTheme.colors.paperColor,
                                            WikipediaTheme.colors.additionColor
                                        )
                                    )
                                )
                        ) {
                            // impact module

                            // game module

                            if (donationUiState is UiState.Success) {
                                // TODO: default is off. Handle this when building the configuration screen.
                                DonationModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 16.dp),
                                    uiState = donationUiState,
                                    onClick = {
                                        (requireActivity() as? BaseActivity)?.launchDonateDialog(
                                            campaignId = ActivityTabViewModel.CAMPAIGN_ID
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // --- new column ---

                    // timeline module
                }
            }
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenPreview() {
        val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                userName = "User",
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 12345,
                    articlesReadThisMonth = 123,
                    lastArticleReadTime = LocalDateTime.now(),
                    articlesReadByWeek = listOf(0, 12, 34, 56),
                    articlesSavedThisMonth = 23,
                    lastArticleSavedTime = LocalDateTime.of(2025, 6, 1, 12, 30),
                    articlesSaved = listOf(
                        PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
                        PageTitle(text = "Industrial design", wiki = site, thumbUrl = null, description = "Process of design applied to physical products", displayText = null),
                        PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
                        PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
                        PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null)
                    ),
                    topCategories = listOf(
                        Category(2025, 1, "Category:Ancient history", "en", 1),
                        Category(2025, 1, "Category:World literature", "en", 1),
                    )
                )),
                donationUiState = UiState.Success("5 days ago")
            )
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenEmptyPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                userName = "User",
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 0,
                    articlesReadThisMonth = 0,
                    lastArticleReadTime = null,
                    articlesReadByWeek = listOf(0, 0, 0, 0),
                    articlesSavedThisMonth = 0,
                    lastArticleSavedTime = null,
                    articlesSaved = emptyList(),
                    topCategories = emptyList()
                )),
                donationUiState = UiState.Success("Unknown")
            )
        }
    }

    companion object {
        fun newInstance(): ActivityTabFragment {
            return ActivityTabFragment().apply {
                arguments = Bundle().apply {
                    // TODO
                }
            }
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}
