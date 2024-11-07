package org.wikipedia.robots

import org.wikipedia.TestUtil
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class SystemRobot : BaseRobot() {
    fun turnOnAirplaneMode() = apply {
        TestUtil.setAirplaneMode(true)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun turnOffAirplaneMode() = apply {
        TestUtil.setAirplaneMode(false)
        delay(TestConfig.DELAY_MEDIUM)
    }
}
