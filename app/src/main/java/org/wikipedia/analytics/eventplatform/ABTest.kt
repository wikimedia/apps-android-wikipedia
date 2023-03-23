package org.wikipedia.analytics.eventplatform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.settings.PrefsIoUtil
import kotlin.random.Random

class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    val aBTestGroup get() = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
    
    init {
        assignTestGroup()
    }

    private fun assignTestGroup() {
        var testGroup = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
        if (testGroup == -1) {
            runBlocking {
                // initialize the group if it hasn't been yet.
                testGroup = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount)
                if (testGroup == GROUP_2 && AccountUtil.isLoggedIn) {
                    if (isUserExperienced()) {
                        testGroup = GROUP_3
                    }
                }
                PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, testGroup)
            }
        }
    }

    private suspend fun isUserExperienced(): Boolean =
        withContext(Dispatchers.Default) {
            var totalContributions = 0

            val homeSiteResponse = async { ServiceFactory.get(WikipediaApp.instance.wikiSite)
                .getUserContrib(AccountUtil.userName!!, 10) }
            val commonsResponse = async { ServiceFactory.get(Constants.commonsWikiSite)
                .getUserContrib(AccountUtil.userName!!, 10) }
            val wikidataResponse = async { ServiceFactory.get(Constants.wikidataWikiSite)
                .getUserContrib(AccountUtil.userName!!, 10) }

            totalContributions += homeSiteResponse.await().query?.userInfo?.editCount ?: 0
            totalContributions += commonsResponse.await().query?.userInfo?.editCount ?: 0
            totalContributions += wikidataResponse.await().query?.userInfo?.editCount ?: 0

            return@withContext totalContributions > EXP_CONTRIBUTOR_REQ
        }

    companion object {
        private const val AB_TEST_KEY_PREFIX = "ab_test_"
        const val MACHINE_GEN_DESC = "mBART25"
        const val EXP_CONTRIBUTOR_REQ = 50
        const val GROUP_SIZE_2 = 2
        const val GROUP_1 = 0
        const val GROUP_2 = 1
        const val GROUP_3 = 2
    }
}
