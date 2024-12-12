package org.wikipedia.robots.feature

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.GifMatchers

class MediaRobot : BaseRobot() {

    fun verifyLeadImageHasGif() = apply {
        onView(withId(R.id.view_page_header_image))
            .perform(waitForAsyncLoading())
            .check(matches(GifMatchers.hasGifDrawable()))
    }
}
