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

    @Test
    fun runTest() {
        pageRobot
            .clickLeadImage()
            .swipePagerLeft()
            .pressBack()
            .scrollToNonLeadImage()
            .swipePagerLeft()
            .pressBack()
            .clickLink("3-sphere")
            .verifyPreviewDialogAppears()
    }
}
