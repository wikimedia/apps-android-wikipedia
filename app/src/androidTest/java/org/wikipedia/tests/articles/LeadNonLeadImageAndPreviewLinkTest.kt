package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class LeadNonLeadImageAndPreviewLinkTest : BaseTest<PageActivity>(
    activityClass = PageActivity::class.java,
    dataInjector = DataInjector(
        intentBuilder = {
            action = ACTION_LOAD_IN_CURRENT_TAB
            putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
            putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
        }
    )
) {

    private val pageRobot = PageRobot(context)
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        pageRobot
            .clickLeadImage() // test failed here bug: 06-23 16:08:48.676 13481 13557 E EspressoError: No views in hierarchy found matching: (view.getId() is <2131298139/org.wikipedia.dev:id/view_page_header_image> and (view has effective visibility <VISIBLE> and view.getGlobalVisibleRect() to return non-empty rectangle))
            .swipePagerLeft()
            .pressBack()
            .scrollToNonLeadImage()
            .isGalleryActivityOffline(context, action = {
                pageRobot
                    .swipePagerLeft()
                    .pressBack()
                    .clickLink("3-sphere")
                    .verifyPreviewDialogAppears()
            })
    }
}
