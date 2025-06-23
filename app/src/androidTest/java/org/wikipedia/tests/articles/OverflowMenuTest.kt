import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants.SEARCH_TERM
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.feature.PageActionItemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class OverflowMenuTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java,
) {
    private val loginRobot = LoginRobot()
    private val pageRobot = PageRobot(context)
    private val homeScreenRobot = HomeScreenRobot()
    private val dialogRobot = DialogRobot()
    private val pageActionItemRobot = PageActionItemRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val systemRobot = SystemRobot()
    private val searchRobot = SearchRobot()

    // androidx.test.espresso.NoMatchingViewException: No views in hierarchy found matching: (view.getId() is <2131297310/org.wikipedia.dev:id/nav_tab_more> and view.getContentDescription() is "More" and (view has effective visibility <VISIBLE> and view.getGlobalVisibleRect() to return non-empty rectangle))
    @Test
    fun runTest() {
        setDeviceOrientation(isLandscape = false)
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .logInUser()
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
            .typeTextInView(SEARCH_TERM)
            .clickOnItemFromSearchList(0)
        dialogRobot
            .dismissBigEnglishDialog()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickShare()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickWatch()
        homeScreenRobot
            .verifyIfSnackBarAppears()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickTalkPage()
            .verifyTalkPageIsOpened()
            .pressBack()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickEditHistory()
            .verifyEditHistoryIsOpened()
            .pressBack()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .assertViewOnMapIsGreyed(Theme.LIGHT)
        pageActionItemRobot
            .clickNewTab()
            .pressBack()
        searchRobot
            .clickOnItemFromSearchList(0)
        dialogRobot
            .dismissBigEnglishDialog()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickExplore()
        searchRobot
            .tapSearchView()
            .typeTextInView(SEARCH_TERM)
            .clickOnItemFromSearchList(0)
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickCategories()
            .verifyCategoryDialogAppears()
            .pressBack()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickEditArticles()
            .pressBack()
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickCustomizeToolbar()
            .verifyCustomizeToolbarIsOpened()
    }
}
