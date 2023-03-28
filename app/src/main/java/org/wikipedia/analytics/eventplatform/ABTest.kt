package org.wikipedia.analytics.eventplatform

import kotlinx.coroutines.runBlocking
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.PrefsIoUtil
import kotlin.random.Random

class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    var group: Int = -1
    val aBTestGroup: Int
        get() {
            group = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
            if (group == -1) {
                // initialize the group if it hasn't been yet.
                group = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount)
                if (group == GROUP_2 && AccountUtil.isLoggedIn) {
                    runBlocking {
                        MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserExperienced()
                    }.also {
                        if (it) {
                            group = GROUP_3
                        }
                        PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, group)
                        return group
                    }
                }
                PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, group)
            }
            return group
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
