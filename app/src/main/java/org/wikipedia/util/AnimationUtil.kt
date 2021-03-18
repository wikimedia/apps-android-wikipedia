package org.wikipedia.util

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class AnimationUtil private constructor() {
    class PagerTransformer(private val rtl: Boolean) : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            if (!rtl) {
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        view.rotation = 0f
                        view.translationX = 0f
                        view.translationZ = -position
                    }
                    position <= 0 -> { // [-1,0]
                        val factor = position * 45f
                        view.rotation = factor
                        view.translationX = view.width * position / 2
                        view.alpha = 1f
                        view.translationZ = -position
                    }
                    position <= 1 -> { // (0,1]
                        // keep it in place (undo the default translation)
                        view.translationX = -(view.width * position)
                        // but move it slightly down
                        view.translationY = DimenUtil.roundedDpToPx(12f) * position
                        view.translationZ = -position
                        // and make it translucent
                        view.alpha = 1f - position * 0.5f
                        // view.setAlpha(1f);
                        view.rotation = 0f
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        view.rotation = 0f
                        view.translationX = 0f
                        view.translationZ = -position
                    }
                }
            } else {
                when {
                    position > 1 -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        view.rotation = 0f
                        view.translationX = 0f
                        view.translationZ = -position
                    }
                    position > 0 -> { // (0,1]
                        // keep it in place (undo the default translation)
                        view.translationX = view.width * position
                        // but move it slightly down
                        view.translationY = DimenUtil.roundedDpToPx(12f) * position
                        view.translationZ = -position
                        // and make it translucent
                        view.alpha = 1f - position * 0.5f
                        // view.setAlpha(1f);
                        view.rotation = 0f
                    }
                    position >= -1 -> { // [-1,0]
                        val factor = position * 45f
                        view.rotation = -factor
                        view.translationX = -(view.width * position / 2)
                        view.alpha = 1f
                        view.translationZ = -position
                    }
                    else -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        view.rotation = 0f
                        view.translationX = 0f
                        view.translationZ = -position
                    }
                }
            }
        }
    }
}
