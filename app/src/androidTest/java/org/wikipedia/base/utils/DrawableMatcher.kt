package org.wikipedia.base.utils

import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description

object DrawableMatcher {
    fun withDrawableId(@DrawableRes expectedId: Int) =
        object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with drawable from resource id: $expectedId")
            }

            override fun matchesSafely(imageView: ImageView): Boolean {
                if (expectedId < 0) return false

                val context = imageView.context
                val resources = imageView.resources

                try {
                    val expectedName = resources.getResourceEntryName(expectedId)
                    val expectedType = resources.getResourceTypeName(expectedId)

                    val foundId = resources.getIdentifier(expectedName, expectedType, context.packageName)
                    return foundId == expectedId
                } catch (e: Resources.NotFoundException) {
                    return false
                }
            }
        }
}
