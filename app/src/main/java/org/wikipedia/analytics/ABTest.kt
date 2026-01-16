package org.wikipedia.analytics

import org.wikipedia.settings.PrefsIoUtil
import kotlin.random.Random

open class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    val group: Int
        get() {
            testGroup = PrefsIoUtil.getInt(AB_TEST_KEY_PREFIX + abTestName, -1)
            if (testGroup == -1) {
                assignGroup()
                PrefsIoUtil.setInt(AB_TEST_KEY_PREFIX + abTestName, testGroup)
            }
            return testGroup
        }

    protected var testGroup: Int = -1

    protected open fun assignGroup() {
        testGroup = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount)
    }

    companion object {
        private const val AB_TEST_KEY_PREFIX = "ab_test_"
        const val GROUP_SIZE_2 = 2
        const val GROUP_SIZE_3 = 3
        const val GROUP_1 = 0
        const val GROUP_2 = 1
        const val GROUP_3 = 2
    }
}
