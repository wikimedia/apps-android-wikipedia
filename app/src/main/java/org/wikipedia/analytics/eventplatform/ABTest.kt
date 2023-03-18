package org.wikipedia.analytics.eventplatform

import kotlinx.coroutines.*
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.util.log.L
import kotlin.random.Random

class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    var group: Int = -1
    val aBTestGroup: Int
        get() {
            group = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
            if (group == -1) {
                // initialize the group if it hasn't been yet.
                checkAndAssignExperienceUser()
                if (group == -1) {
                    group = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount - 1)
                    PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, group)
                }
            }
            return group
        }

    private val isEnrolled = PrefsIoUtil.contains(AB_TEST_KEY_PREFIX + abTestName)

    private fun checkAndAssignExperienceUser() {
        var totalContributions = 0

        CoroutineScope(Dispatchers.Main).launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                val homeSiteResponse = async { ServiceFactory.get(WikipediaApp.instance.wikiSite)
                    .getUserContributions(AccountUtil.userName!!, 10, null) }
                val commonsResponse = async { ServiceFactory.get(Constants.commonsWikiSite)
                    .getUserContributions(AccountUtil.userName!!, 10, null) }
                val wikidataResponse = async { ServiceFactory.get(Constants.wikidataWikiSite)
                    .getUserContributions(AccountUtil.userName!!, 10, null) }
                homeSiteResponse.await()
                    .blockingSubscribe { totalContributions += it.query?.userInfo!!.editCount }
                commonsResponse.await()
                    .blockingSubscribe { totalContributions += it.query?.userInfo!!.editCount }
                wikidataResponse.await()
                    .blockingSubscribe { totalContributions += it.query?.userInfo!!.editCount }

                if (totalContributions > EXP_CONTRIBUTOR_REQ) {
                    group = GROUP_3
                    PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, GROUP_3)
                }
            }
        }
    }

    companion object {
        private const val AB_TEST_KEY_PREFIX = "ab_test_"
        const val MACHINE_GEN_DESC = "mBART25"
        const val EXP_CONTRIBUTOR_REQ = 50
        const val GROUP_SIZE_2 = 2
        const val GROUP_SIZE_3 = 3
        const val GROUP_1 = 0
        const val GROUP_2 = 1
        const val GROUP_3 = 2
    }
}
