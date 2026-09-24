package org.wikipedia.analytics

import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.PrefsIoUtil
import kotlin.random.Random

abstract class ABTest(private val abTestName: String, private val abTestGroupCount: Int) {

    val name get() = abTestName

    val groupCount get() = abTestGroupCount

    val preferenceKey get() = AB_TEST_KEY_PREFIX + abTestName
    val exposureEventSentKey get() = AB_TEST_EXPOSURE_SENT_PREFIX + abTestName

    val group: Int
        get() {
            testGroup = PrefsIoUtil.getInt(preferenceKey, -1)
            if (testGroup == -1) {
                assignGroup()
                PrefsIoUtil.setInt(preferenceKey, testGroup)
            }
            return testGroup
        }

    protected var testGroup: Int = -1

    protected open fun assignGroup() {
        testGroup = Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(abTestGroupCount)
    }

    abstract fun getGroupName(): String

    open fun shouldInstrument(): Boolean {
        return true
    }

    fun maybeSendExposureEvent() {
        PrefsIoUtil.getInt(exposureEventSentKey, 0).let { sentCount ->
            if (sentCount < AB_TEST_EXPOSURE_SENT_MAX_TIMES) {
                TestKitchenAdapter.submitExperimentExposure(this)
                PrefsIoUtil.setInt(exposureEventSentKey, sentCount + 1)
            }
        }
    }

    companion object {
        private const val AB_TEST_KEY_PREFIX = "ab_test_"
        private const val AB_TEST_EXPOSURE_SENT_PREFIX = "ab_test_exposure_sent_"
        private const val AB_TEST_EXPOSURE_SENT_MAX_TIMES = 3

        const val GROUP_SIZE_2 = 2
        const val GROUP_SIZE_3 = 3
        const val GROUP_1 = 0
        const val GROUP_2 = 1
        const val GROUP_3 = 2
    }
}
