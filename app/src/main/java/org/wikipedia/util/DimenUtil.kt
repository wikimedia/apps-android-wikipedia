package org.wikipedia.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Window
import androidx.annotation.DimenRes
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import kotlin.math.roundToInt

object DimenUtil {
    fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)
    }

    fun roundedDpToPx(dp: Float): Int {
        return dpToPx(dp).roundToInt()
    }

    private fun pxToDp(px: Float): Float {
        return px / densityScalar
    }

    fun roundedPxToDp(px: Float): Int {
        return pxToDp(px).roundToInt()
    }

    val densityScalar: Float
        get() = displayMetrics.density

    @JvmStatic
    fun getFloat(@DimenRes id: Int): Float {
        return getValue(id).float
    }

    fun getDimension(@DimenRes id: Int): Float {
        return TypedValue.complexToFloat(getValue(id).data)
    }

    @JvmStatic
    fun getFontSizeFromSp(window: Window, fontSp: Float): Float {
        val metrics = DisplayMetrics()
        window.windowManager.defaultDisplay.getMetrics(metrics)
        return fontSp / metrics.scaledDensity
    }

    // TODO: use getResources().getDimensionPixelSize()?  Define leadImageWidth with px, not dp?
    fun calculateLeadImageWidth(): Int {
        val res = WikipediaApp.getInstance().resources
        return (res.getDimension(R.dimen.leadImageWidth) / densityScalar).toInt()
    }

    val displayWidthPx: Int
        get() = displayMetrics.widthPixels

    val displayHeightPx: Int
        get() = displayMetrics.heightPixels

    fun getContentTopOffsetPx(context: Context): Int {
        return roundedDpToPx(getContentTopOffset(context))
    }

    private fun getContentTopOffset(context: Context): Float {
        return getToolbarHeight(context)
    }

    private fun getValue(@DimenRes id: Int): TypedValue {
        val typedValue = TypedValue()
        resources.getValue(id, typedValue, true)
        return typedValue
    }

    private val displayMetrics: DisplayMetrics
        get() = resources.displayMetrics
    private val resources: Resources
        get() = WikipediaApp.getInstance().resources

    fun getNavigationBarHeight(context: Context): Float {
        val id = getNavigationBarId(context)
        return if (id > 0) getDimension(id) else 0f
    }

    private fun getToolbarHeight(context: Context): Float {
        return roundedPxToDp(getToolbarHeightPx(context).toFloat()).toFloat()
    }

    fun getToolbarHeightPx(context: Context): Int {
        val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(
                androidx.appcompat.R.attr.actionBarSize
        ))
        val size = styledAttributes.getDimensionPixelSize(0, 0)
        styledAttributes.recycle()
        return size
    }

    @DimenRes
    private fun getNavigationBarId(context: Context): Int {
        return context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun leadImageHeightForDevice(context: Context): Int {
        return if (isLandscape(context)) (displayWidthPx * articleHeaderViewScreenHeightRatio()).toInt() else (displayHeightPx * articleHeaderViewScreenHeightRatio()).toInt()
    }

    private fun articleHeaderViewScreenHeightRatio(): Float {
        return getFloat(R.dimen.articleHeaderViewScreenHeightRatio)
    }
}
