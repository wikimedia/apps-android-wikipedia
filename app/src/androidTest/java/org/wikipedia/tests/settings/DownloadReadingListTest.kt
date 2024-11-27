import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.ReadingListRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.SavedScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class DownloadReadingListTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val bottomNavRobot = BottomNavRobot()
    private val settingsRobot = SettingsRobot()
    private val systemRobot = SystemRobot()
    private val savedScreenRobot = SavedScreenRobot()
    private val searchRobot = SearchRobot()
    private val pageRobot = PageRobot()
    private val readingListRobot = ReadingListRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToSavedPage()
        savedScreenRobot
            .dismissSyncReadingList()
        bottomNavRobot
            .navigateToSearchPage()
        searchRobot
            .tapSearchView()
            .typeTextInView("apple")
            .clickOnItemFromSearchList(0)
        pageRobot
            .dismissTooltip(activity)

        readingListRobot
            .saveArticleToReadingList()
            .addToReadingList(context)
            .typeNameOfTheList("😎")
            .saveTheList(context)
            .viewTheList(context)
            .clickOnGotIt()
            .verifyArticleHasDownloaded()
            .pressBack()
            .pressBack()
            .navigateUp()
            .clickNoThanks(context)

        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()

        settingsRobot
            .toggleDownloadReadingList()
            .pressBack()

        searchRobot
            .tapSearchView()
            .typeTextInView("orange")
            .clickOnItemFromSearchList(0)
        pageRobot
            .dismissContributionDialog()
        readingListRobot
            .saveArticleToReadingList()
            .addToReadingList(context)
            .clickCreateList()
            .typeNameOfTheList("😎😍")
            .saveTheList(context)
            .viewTheList(context)
            .verifyArticleHasNotDownloaded()
    }
}
