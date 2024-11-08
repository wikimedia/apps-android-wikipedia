package org.wikipedia.robots

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
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

    fun clickAllowOnSystemDialog() = apply {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val allowButton = device.findObject(UiSelector().text("Allow"))
            if (allowButton.exists()) {
                allowButton.click()
            }
        } catch (e: Exception) {
            Log.d("dialog", "Dialog did not appear or couldn't be clicked.")
        }
    }
}
