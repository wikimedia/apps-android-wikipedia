package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class EditIconTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val SEARCH_TERM = "tom and jerry the movie"
    private val SEARCH_TERM_AVATAR = "Avatar"
    private val SEARCH_TERM_VLADIMIR_PUTIN = "Vladimir Putin"
    private val SEARCH_TERM_KIM_JUNG_UN = "Kim Jong-un"
    private val SEARCH_TERM_JOE_BIDEN = "Joe Biden"
    private val SEARCH_TERM_KAMALA_HARRIS = "Kamala Harris"
    private val SEARCH_TERM_DONALD_TRUMP = "Donald Trump"
    private val SEARCH_TERM_MIKE_PENCE = "Mike Pence"
    private val SEARCH_TERM_BARACK_OBAMA = "Barack Obama"
    private val SEARCH_TERM_HILLARY_CLINTON = "Hillary Clinton"
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()
    private val pageRobot = PageRobot(context)
    private val dialogRobot = DialogRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
        assertEditIconProtection(SEARCH_TERM, isProtected = false)
        assertEditIconProtection(SEARCH_TERM_AVATAR, action = {
            dialogRobot
                .dismissBigEnglishDialog()
                .dismissContributionDialog()
        })
        assertEditIconProtection(SEARCH_TERM_VLADIMIR_PUTIN)
        assertEditIconProtection(SEARCH_TERM_KIM_JUNG_UN)
        assertEditIconProtection(SEARCH_TERM_JOE_BIDEN)
        assertEditIconProtection(SEARCH_TERM_KAMALA_HARRIS)
        assertEditIconProtection(SEARCH_TERM_DONALD_TRUMP)
        assertEditIconProtection(SEARCH_TERM_MIKE_PENCE)
        assertEditIconProtection(SEARCH_TERM_BARACK_OBAMA)
        assertEditIconProtection(SEARCH_TERM_HILLARY_CLINTON)
    }

    private fun assertEditIconProtection(
        searchTerm: String,
        isProtected: Boolean = true,
        action: (() -> Unit)? = null
        ) {
        searchRobot
            .typeTextInView(searchTerm)
            .clickOnItemFromSearchList(0)
        action?.invoke()
        pageRobot
            .assertEditButtonProtection(isProtected)
            .pressBack()
    }
}
