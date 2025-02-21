package org.wikipedia.robots

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class SystemRobot : BaseRobot() {
    fun turnOnAirplaneMode() = apply {
        TestUtil.setAirplaneMode(true)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun turnOffAirplaneMode() = apply {
        TestUtil.setAirplaneMode(false)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun turnOffInternet() = apply {
        TestUtil.toggleInternet(false)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun turnOnInternet() = apply {
        TestUtil.toggleInternet(true)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOnSystemDialogWithText(text: String) = apply {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val allowButton = device.findObject(UiSelector().text(text))
            if (allowButton.exists()) {
                allowButton.click()
            }
            delay(TestConfig.DELAY_SHORT)
        } catch (e: Exception) {
            Log.d("dialog", "Dialog did not appear or couldn't be clicked.")
        }
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun enableDarkMode(context: Context) = apply {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            device.executeShellCommand("cmd uimode night yes")
            TestUtil.delay(3)
            device.waitForIdle()
            onView(isRoot()).perform(TestUtil.waitOnId(1000))
        } catch (e: Exception) {
            Log.e("SystemRobot", "Error while enabling dark mode", e)
        }
    }

    fun disableDarkMode(context: Context) = apply {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            device.executeShellCommand("cmd uimode night no")
            TestUtil.delay(3)
            device.waitForIdle()
            onView(isRoot()).perform(TestUtil.waitOnId(1000))
        } catch (e: Exception) {
            Log.e("SystemRobot", "Error while disabling dark mode", e)
        }
    }

    fun dismissTooltip(activity: Activity) = apply {
        system.dismissTooltipIfAny(activity, viewId = R.id.buttonView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
    }
}
