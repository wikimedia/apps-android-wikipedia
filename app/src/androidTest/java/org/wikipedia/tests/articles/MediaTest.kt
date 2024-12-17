package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.MediaRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class MediaTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()
    private val mediaRobot = MediaRobot()
    private val pageRobot = PageRobot(context)

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_VORTEX_SHEDDING)
            .clickOnItemFromSearchList(0)
        pageRobot
            .dismissTooltip(activity)
        mediaRobot
            .verifyLeadImageHasGif()
        pageRobot
            .clickLeadImage()
            .swipePagerLeft()
        mediaRobot
            .pinchZoomAction(context, device)
        pageRobot
            .swipePagerLeft()
        mediaRobot
            .doubleTapToZoomOut()
            .doubleTapToZoomOut()
            .doubleTapToZoomOut()
            .toggleOverlayVisibility()
            .verifyOverlayVisibility(isVisible = false)
            .toggleOverlayVisibility()
            .verifyOverlayVisibility(isVisible = true)
            .clickShareButton()
            .clickCC()
            .verifyCCisClicked()
            .tapHamburger(context)
            .goToImagePage(context)
            .verifyImagePageIsVisible()
        mediaRobot
            .navigateUp()
    }
}
