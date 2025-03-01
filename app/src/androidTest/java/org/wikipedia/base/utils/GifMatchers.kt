package org.wikipedia.base.utils

import android.view.View
import android.widget.ImageView
import androidx.test.espresso.matcher.BoundedMatcher
import com.bumptech.glide.load.resource.gif.GifDrawable
import org.hamcrest.Description
import org.hamcrest.Matcher

object GifMatchers {
    fun hasGifDrawable(): Matcher<View> {
        return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has gif drawable")
            }

            override fun matchesSafely(imageView: ImageView): Boolean {
                val drawable = imageView.drawable

                return when (drawable) {
                    is GifDrawable -> true
                    else -> false
                }
            }
        }
    }
}
