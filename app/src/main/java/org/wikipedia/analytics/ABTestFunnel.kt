package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.PrefsIoUtil
import kotlin.random.Random

class ABTestFunnel internal constructor(private val abTestName: String, private val abTestGroupCount: Int) :
        Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    // initialize the group if it hasn't been yet.
    val aBTestGroup: Int
        get() {
            var group = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
            if (group == -1) {
                // initialize the group if it hasn't been yet.
                group = Random(Int.MAX_VALUE).nextInt()
                PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, group)
            }
            return group.mod(abTestGroupCount)
        }

    private val isEnrolled = PrefsIoUtil.contains(AB_TEST_KEY_PREFIX + abTestName)

    override fun preprocessSessionToken(eventData: JSONObject) {}
    fun logGroupEvent(groupEventId: String) {
        log(
                "test_group", groupEventId
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppABTest"
        private const val REV_ID = 19990870
        private const val AB_TEST_KEY_PREFIX = "ab_test_"
        const val GROUP_SIZE_2 = 2
        const val GROUP_SIZE_3 = 3
        const val GROUP_1 = 0
        const val GROUP_2 = 1
        const val GROUP_3 = 2
    }
}
