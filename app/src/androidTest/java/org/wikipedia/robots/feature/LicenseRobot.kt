package org.wikipedia.robots.feature

import BaseRobot
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.utils.DrawableMatcher

class LicenseRobot : BaseRobot() {
    fun verifyImageIsCopyrighted() = apply {
        onView(withId(R.id.license_icon))
            .check(matches(DrawableMatcher.withDrawableId(R.drawable.ic_license_cite)))
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyImageHasCCLicense() = apply {
        onView(withId(R.id.license_icon))
            .check(matches(DrawableMatcher.withDrawableId(R.drawable.ic_license_cc)))
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyImageHasPdLicense() = apply {
        onView(withId(R.id.license_icon))
            .check(matches(DrawableMatcher.withDrawableId(R.drawable.ic_license_pd)))
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyImageHasSaLicense() = apply {
        onView(withId(R.id.license_icon))
            .check(matches(DrawableMatcher.withDrawableId(R.drawable.ic_license_by)))
        delay(TestConfig.DELAY_SHORT)
    }
}
