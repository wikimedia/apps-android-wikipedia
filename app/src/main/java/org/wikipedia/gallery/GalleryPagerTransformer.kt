package org.wikipedia.gallery

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class GalleryPagerTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) =
        when {
            position < -1 -> { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.translationX = 0f
            }
            position <= 0 -> { // [-1,0]
                val scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - abs(position))
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                // fade out
                view.alpha = 1 + position
                // keep it in place (undo the default translation)
                view.translationX = -(view.width * position)
            }
            position <= 1 -> { // (0,1]
                // don't do anything to it when it's sliding in.
            }
            else -> { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.translationX = 0f
            }
        }

    companion object {
        private const val MIN_SCALE = 0.9f
    }
}
