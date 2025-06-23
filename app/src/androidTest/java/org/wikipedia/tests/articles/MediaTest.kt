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
        mediaRobot
            .verifyLeadImageHasGif() // test failed here bug: androidx.test.espresso.base.AssertionErrorHandler$AssertionFailedWithCauseError: 'has gif drawable' doesn't match the selected view.
        // Expected: has gif drawable
        // Got: org.wikipedia.views.FaceAndColorDetectImageView{1006553 VFED..C.. ........ 0,168-1080,768 #7f09075b app:id/view_page_header_image}
        // View Details: FaceAndColorDetectImageView{id=2131298139, res-name=view_page_header_image, desc=Image: Vortex shedding, visibility=VISIBLE, width=1080, height=600, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, layout-params=android.widget.FrameLayout$LayoutParams@YYYYYY, tag=null, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=168.0}
        pageRobot
            .clickLeadImage()
            .swipePagerLeft()
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
            .pinchZoomAction(context, device)
            .tapHamburger(context)
            .goToImagePage(context)
            .verifyImagePageIsVisible()
        mediaRobot
            .navigateUp()
    }
}
