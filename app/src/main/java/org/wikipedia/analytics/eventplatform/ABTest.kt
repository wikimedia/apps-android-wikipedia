package org.wikipedia.analytics.eventplatform

import kotlinx.coroutines.runBlocking
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.PrefsIoUtil
import org.wikipedia.util.log.L
import kotlin.random.Random

class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    var testGroup: Int = -1
    val aBTestGroup: Int
        get() {
            testGroup = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
            if (testGroup == -1) {
                runBlocking {
                    // initialize the group if it hasn't been yet.
                    testGroup = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount)
                    if (testGroup == GROUP_2 && AccountUtil.isLoggedIn) {
                        try {
                            if (MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserExperienced()) {
                                testGroup = GROUP_3
                            }
                        } catch (e: Exception) {
                            L.e(e)
                        }
                    }
                    PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, testGroup)
                }
            }
            return testGroup
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
