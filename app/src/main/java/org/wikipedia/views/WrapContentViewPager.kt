package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import com.duolingo.open.rtlviewpager.RtlViewPager
import kotlin.math.min

class WrapContentViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): RtlViewPager(context, attrs) {

    private var maxHeight = Int.MAX_VALUE

    fun setMaxHeight(height: Int) {
        maxHeight = height
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var newHeightMeasureSpec = heightMeasureSpec
        try {
            var height = 0
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                val h = child.measuredHeight
                if (h > height) {
                    height = h
                }
            }
            if (height != 0) {
                newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(min(height, maxHeight), MeasureSpec.EXACTLY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }
}
