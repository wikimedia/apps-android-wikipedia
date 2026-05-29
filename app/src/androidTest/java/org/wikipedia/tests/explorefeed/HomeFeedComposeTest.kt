package org.wikipedia.tests.explorefeed

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.base.BaseTest
import org.wikipedia.compose.components.NotificationBellState
import org.wikipedia.compose.components.menu.PageOverflowMenu
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.CommunityContentState
import org.wikipedia.feed.FeedEmptyState
import org.wikipedia.feed.ForYouContentState
import org.wikipedia.feed.HomeScreen
import org.wikipedia.feed.HomeScreenTestTags
import org.wikipedia.feed.HomeTab
import org.wikipedia.feed.TabsState
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageState
import org.wikipedia.main.MainActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeFeedComposeTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java
) {

    @Test
    fun communityTab_clickForYouTab_invokesSelectTabCallback() {
        var selectedTab: HomeTab? = null

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            onSelectTab = { tab, _ -> selectedTab = tab }
        )

        composeTestRule.onNodeWithTag(HomeScreenTestTags.TAB_FOR_YOU).performClick()

        assertEquals(HomeTab.FOR_YOU, selectedTab)
    }

    @Test
    fun languageMenu_selectLanguage_invokesLanguageSelectedCallback() {
        val languageState = AppLanguageState(context).apply {
            setAppLanguageCodes(listOf("en", "es"))
        }
        var selectedLanguageCode: String? = null

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            languageState = languageState,
            onLanguageSelected = { selectedLanguageCode = it }
        )

        composeTestRule.onNodeWithTag(HomeScreenTestTags.LANGUAGE_MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(HomeScreenTestTags.languageItem("es")).performClick()

        assertEquals("es", selectedLanguageCode)
    }

    @Test
    fun languageMenu_clickManageLanguages_invokesManageLanguagesCallback() {
        val languageState = AppLanguageState(context).apply {
            setAppLanguageCodes(listOf("en", "es"))
        }
        var manageClicked = false

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            languageState = languageState,
            onManageLanguagesClick = { manageClicked = true }
        )

        composeTestRule.onNodeWithTag(HomeScreenTestTags.LANGUAGE_MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(HomeScreenTestTags.LANGUAGE_MANAGE_BUTTON).performClick()

        assertTrue(manageClicked)
    }

    @Test
    fun toolbar_clickTabCounter_invokesTabClickCallback() {
        var tabClicked = false

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            tabsState = TabsState(count = 2, pulse = false),
            onTabClick = { tabClicked = true }
        )

        composeTestRule.onNodeWithTag(HomeScreenTestTags.TOOLBAR_TAB_BUTTON).performClick()

        assertTrue(tabClicked)
    }

    @Test
    fun toolbar_clickNotificationBell_invokesNotificationCallback() {
        var notificationClicked = false

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            notificationBellState = NotificationBellState(unreadCount = 3, canShow = true),
            onNotificationClick = { notificationClicked = true }
        )

        composeTestRule.onNodeWithTag(HomeScreenTestTags.TOOLBAR_NOTIFICATION_BUTTON).performClick()

        assertTrue(notificationClicked)
    }

    @Test
    fun communityEmptyState_clickManageModules_invokesManageModulesCallback() {
        var manageModulesClicked = false

        setHomeScreenContent(
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(emptyState = FeedEmptyState.ALL_MODULES_HIDDEN),
            onManageModulesClick = { manageModulesClicked = true }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_feed_screen_all_modules_disabled_btn_label))
            .performClick()

        assertTrue(manageModulesClicked)
    }

    @Test
    fun forYouEmptyState_clickManageModules_invokesManageModulesCallback() {
        var manageModulesClicked = false

        setHomeScreenContent(
            selectedTab = HomeTab.FOR_YOU,
            forYouContentState = ForYouContentState(emptyState = FeedEmptyState.ALL_MODULES_HIDDEN),
            onManageModulesClick = { manageModulesClicked = true }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.home_feed_screen_all_modules_disabled_btn_label))
            .performClick()

        assertTrue(manageModulesClicked)
    }

    @Test
    fun pageOverflowMenu_clickItem_invokesItemAction() {
        var clickedAction: String? = null
        val menuKey = "top-read-0-0"

        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                PageOverflowMenu(
                    menuKey = menuKey,
                    overflowMenuState = createOverflowState(menuKey),
                    onDismiss = {},
                    items = listOf(
                        "Open" to { clickedAction = "open" },
                        "Save" to { clickedAction = "save" }
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("page_overflow_item_1").performClick()

        assertEquals("save", clickedAction)
    }

    @Test
    fun pageOverflowMenu_whenMenuKeyDoesNotMatch_doesNotShowItems() {
        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                PageOverflowMenu(
                    menuKey = "top-read-0-0",
                    overflowMenuState = createOverflowState("different-key"),
                    onDismiss = {},
                    items = listOf("Open" to {})
                )
            }
        }

        composeTestRule.onNodeWithTag("page_overflow_item_0").assertDoesNotExist()
    }

    private fun setHomeScreenContent(
        selectedTab: HomeTab,
        languageState: AppLanguageState = AppLanguageState(context),
        communityContentState: CommunityContentState = CommunityContentState(isInitialLoading = true),
        forYouContentState: ForYouContentState = ForYouContentState(isInitialLoading = true),
        tabsState: TabsState = TabsState(count = 1, pulse = false),
        notificationBellState: NotificationBellState = NotificationBellState(unreadCount = 0, canShow = false),
        onSelectTab: (HomeTab, org.wikipedia.feed.model.Card?) -> Unit = { _, _ -> },
        onLanguageSelected: (String) -> Unit = {},
        onManageLanguagesClick: () -> Unit = {},
        onTabClick: () -> Unit = {},
        onNotificationClick: () -> Unit = {},
        onManageModulesClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                HomeScreen(
                    wikiSite = WikiSite.forLanguageCode("en"),
                    languageState = languageState,
                    selectedTab = selectedTab,
                    communityContentState = communityContentState,
                    forYouContentState = forYouContentState,
                    overflowMenuState = null,
                    tabsState = tabsState,
                    notificationBellState = notificationBellState,
                    onSelectTab = onSelectTab,
                    onLanguageSelected = onLanguageSelected,
                    onManageLanguagesClick = onManageLanguagesClick,
                    onTabClick = onTabClick,
                    onNotificationClick = onNotificationClick,
                    onManageModulesClick = onManageModulesClick
                )
            }
        }
    }

    private fun createOverflowState(menuKey: String): PageOverflowMenuViewModel.PageOverflowMenuState {
        val historyEntry = HistoryEntry(
            PageTitle(null, WikiSite.forLanguageCode("en"), "Earth"),
            HistoryEntry.SOURCE_INTERNAL_LINK
        )
        return PageOverflowMenuViewModel.PageOverflowMenuState(
            entry = historyEntry,
            items = emptyList(),
            menuKey = menuKey
        )
    }
}
