package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.LicenseRobot
import org.wikipedia.robots.feature.MediaRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SpecialArticleTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()
    private val mediaRobot = MediaRobot()
    private val pageRobot = PageRobot(context)
    private val licenseRobot = LicenseRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_VORTEX_SHEDDING)
            .clickOnItemFromSearchList(0)
        mediaRobot
            .verifyLeadImageHasGif()
        searchRobot
            .clickSearchFromPageView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_AVATAR_2009)
            .clickOnItemFromSearchList(0)
        pageRobot
            .clickLeadImage()
        licenseRobot
            .verifyImageIsCopyrighted()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasCCLicense()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasCCLicense()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasCCLicense()
        pageRobot
            .pressBack()
        searchRobot
            .clickSearchFromPageView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_BILL_CLINTON)
            .clickOnItemFromSearchList(0)
        pageRobot
            .clickLeadImage()
        licenseRobot
                .verifyImageHasPdLicense()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasPdLicense()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasCCLicense()
        pageRobot
            .swipePagerLeft()
        licenseRobot
            .verifyImageHasPdLicense()
        pageRobot
            .pressBack()
        searchRobot
            .clickSearchFromPageView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_INDIA)
            .clickOnItemFromSearchList(0)
        pageRobot
            .scrollToAdministrativeDivisionOfIndiaArticle()
            .scrollToAndhraPradeshOnIndiaArticle()
            .verifyPreviewDialogAppears()
            .pressBack()
            .pressBack()
        searchRobot
            .clickSearchFromPageView()
            .typeTextInView(TestConstants.SPECIAL_ARTICLE_USA)
            .clickOnItemFromSearchList(0)
        pageRobot
            .scrollToCollapsingTables()
            .clickToExpandQuickFactsTable()
    }
}
