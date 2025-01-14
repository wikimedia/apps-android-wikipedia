package org.wikipedia.tests

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.base.TestConfig
import org.wikipedia.page.PageActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class DeepLinkingTest : BaseTest<PageActivity>(
 activityClass = PageActivity::class.java,
    dataInjector = DataInjector(
        intentBuilder = {
            action = Intent.ACTION_VIEW
            data = Uri.parse(TestConfig.TEST_WIKI_URL_APPLE)
            addCategory(Intent.CATEGORY_DEFAULT)
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
            .verifySameArticleAppearsAsURL("Apple")
    }
}
