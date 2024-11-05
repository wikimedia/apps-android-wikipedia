package org.wikipedia.main

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.EditorRobot
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.PageRobot
import org.wikipedia.robots.SearchRobot
import org.wikipedia.robots.ThemeRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTest2 : BaseTest<MainActivity>(
    MainActivity::class.java
) {
    private val searchRobot = SearchRobot()
    private val pageRobot = PageRobot()
    private val onboardingRobot = OnboardingRobot()
    private val editorRobot = EditorRobot()
    private val themeRobot = ThemeRobot()

    @Test
    fun smokeTest2() {
        onboardingRobot
            .checkWelcomeScreenViewsForVisibility()
            .verifyAppLanguageMatchesDeviceLanguage()
            .swipeAllTheWayToEnd()
            .swipeBackToWelcomeScreen()
            .moveAllTheWayToEndUsingTapButton()
            .swipeBackToWelcomeScreen()
            .checkWelcomeScreenViewsForVisibility()
            .skipWelcomeScreen()

        searchRobot
            .performSearch(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)

        setDeviceOrientation(true)
        pressBack()

        // Make sure the same title appears in the new screen orientation
        searchRobot.verifySearchResult(ARTICLE_TITLE)

        // Rotate the device back to the original orientation
        setDeviceOrientation(false)

        searchRobot.clickOnItemFromSearchList(0)

        pageRobot
            .dismissTooltip(activity)
            .clickLink("3-sphere")
            .previewArticle()
            .clickLink("Sphere")
            .openInNewTab()
            .verifyTabCount("2")
            .goBackToOriginalArticle()
            .verifyHeaderViewWithLeadImage()
            .clickLeadImage()
            .swipeLeft()
            .clickOverflowMenu("More options")
            .visitImagePage()
            .goBackToGalleryView()
            .goBackToOriginalArticle()
            .enableJavaScript()
            .verifyArticleTitle(ARTICLE_TITLE)
            .clickEditPencilAtTopOfArticle()
        editorRobot
            .clickEditIntroductionMenuItem()
            .dismissDialogIfShown()
        themeRobot
            .clickThemeIconOnEditPage()
            .increaseTextSize()
            .increaseTextSize()
            .pressBack()
            .clickThemeIconOnEditPage()
            .decreaseTextSize()
            .decreaseTextSize()
            .pressBack()
        editorRobot
            .typeInEditWindow()
            .tapNext()
            .tapNext()
            .clickDefaultEditSummaryChoices()
            .navigateUp()
            .navigateUp()
            .navigateUp()
            .remainInEditWorkflow()
            .pressBack()
            .leaveEditWorkflow()
        pageRobot
            .launchTabsScreen()
    }

    companion object {
        const val SEARCH_TERM = "hopf fibration"
        const val ARTICLE_TITLE = "Hopf fibration"
        const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
    }
}
