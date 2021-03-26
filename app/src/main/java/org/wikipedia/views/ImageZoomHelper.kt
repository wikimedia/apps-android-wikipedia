package org.wikipedia.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.contains
import androidx.core.view.forEach
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Based on:
 * https://github.com/okaybroda/ImageZoom/blob/master/library/src/main/java/com/viven/imagezoom/ImageZoomHelper.java
 */

@Suppress("SameParameterValue", "unused")
class ImageZoomHelper(activity: Activity) {
    interface OnZoomListener {
        fun onImageZoomStarted(view: View?)
        fun onImageZoomEnded(view: View?)
    }

    private lateinit var twoPointCenter: IntArray
    private lateinit var originalXY: IntArray
    private lateinit var parentOfZoomableView: ViewGroup
    private lateinit var zoomableViewLP: ViewGroup.LayoutParams
    private var zoomableView: View? = null
    private var zoomableViewFrameLP: FrameLayout.LayoutParams? = null
    private var darkView: View? = null
    private var decorView: ViewGroup? = null
    private var viewIndex = 0
    private var originalDistance = 0.0
    private var pivotX = 0
    private var pivotY = 0
    private var isAnimatingDismiss = false
    private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
    private val zoomListeners: MutableList<OnZoomListener> = ArrayList()

    fun onDispatchTouchEvent(ev: MotionEvent): Boolean {
        val activity = activityWeakReference.get() ?: return false
        if (ev.pointerCount == 2) {
            if (zoomableView == null) {
                val view = findZoomableView(ev, activity.findViewById(android.R.id.content))
                if (view != null) {
                    zoomableView = view

                    // get view's original location relative to the window
                    originalXY = IntArray(2)
                    view.getLocationInWindow(originalXY)

                    // this FrameLayout will be the zoomableView's temporary parent
                    val frameLayout = FrameLayout(view.context)

                    // this view is to gradually darken the backdrop as user zooms
                    darkView = View(view.context)
                    darkView?.setBackgroundColor(Color.BLACK)
                    darkView?.alpha = 0f

                    // adding darkening backdrop to the frameLayout
                    frameLayout.addView(darkView, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT))
                    decorView = activity.window.decorView as ViewGroup
                    decorView!!.addView(frameLayout)

                    // get the parent of the zoomable view and get its index and layout param
                    parentOfZoomableView = zoomableView!!.parent as ViewGroup
                    viewIndex = parentOfZoomableView.indexOfChild(zoomableView)
                    zoomableViewLP = zoomableView!!.layoutParams

                    // this is the new layout param for the zoomableView
                    zoomableViewFrameLP = FrameLayout.LayoutParams(
                            view.width, view.height)
                    zoomableViewFrameLP?.leftMargin = originalXY[0]
                    zoomableViewFrameLP?.topMargin = originalXY[1]

                    // zoomableView has to be removed from parent view before being added to its new parent
                    parentOfZoomableView.removeView(zoomableView)
                    frameLayout.addView(zoomableView, zoomableViewFrameLP)

                    // Pointer variables to store the original touch positions
                    val pointerCoords1 = MotionEvent.PointerCoords()
                    ev.getPointerCoords(0, pointerCoords1)
                    val pointerCoords2 = MotionEvent.PointerCoords()
                    ev.getPointerCoords(1, pointerCoords2)

                    // storing distance between the two positions to be compared later on for zooming
                    originalDistance = getDistance(pointerCoords1.x.toDouble(), pointerCoords2.x.toDouble(),
                            pointerCoords1.y.toDouble(), pointerCoords2.y.toDouble())

                    // storing center point of the two pointers to move the view according to the touch position
                    twoPointCenter = intArrayOf(
                            ((pointerCoords2.x + pointerCoords1.x) / 2).toInt(),
                            ((pointerCoords2.y + pointerCoords1.y) / 2).toInt()
                    )

                    // storing pivot point for zooming image from its touch coordinates
                    pivotX = ev.rawX.toInt() - originalXY[0]
                    pivotY = ev.rawY.toInt() - originalXY[1]
                    isZooming = true
                    sendZoomEventToListeners(zoomableView, true)
                    return true
                }
            } else {
                val pointerCoords1 = MotionEvent.PointerCoords()
                ev.getPointerCoords(0, pointerCoords1)
                val pointerCoords2 = MotionEvent.PointerCoords()
                ev.getPointerCoords(1, pointerCoords2)
                val newCenter = intArrayOf(
                        ((pointerCoords2.x + pointerCoords1.x) / 2).toInt(),
                        ((pointerCoords2.y + pointerCoords1.y) / 2).toInt()
                )
                val currentDistance = getDistance(pointerCoords1.x.toDouble(), pointerCoords2.x.toDouble(),
                        pointerCoords1.y.toDouble(), pointerCoords2.y.toDouble()).toInt()
                val pctIncrease = (currentDistance - originalDistance) / originalDistance
                zoomableView!!.pivotX = pivotX.toFloat()
                zoomableView!!.pivotY = pivotY.toFloat()
                zoomableView!!.scaleX = (1 + pctIncrease).toFloat()
                zoomableView!!.scaleY = (1 + pctIncrease).toFloat()
                if (zoomableView != null && zoomableViewFrameLP != null) {
                    updateZoomableViewMargins((newCenter[0] - twoPointCenter[0] + originalXY[0]).toFloat(), (
                            newCenter[1] - twoPointCenter[1] + originalXY[1]).toFloat())
                }
                val step = 8
                darkView?.alpha = (pctIncrease / step).toFloat()
                return true
            }
        } else {
            if (zoomableView != null && !isAnimatingDismiss) {
                isAnimatingDismiss = true
                val scaleYStart = zoomableView!!.scaleY
                val scaleXStart = zoomableView!!.scaleX
                val leftMarginStart = zoomableViewFrameLP?.leftMargin
                val topMarginStart = zoomableViewFrameLP?.topMargin
                val alphaStart = darkView!!.alpha
                val scaleYEnd = 1f
                val scaleXEnd = 1f
                val leftMarginEnd = originalXY[0]
                val topMarginEnd = originalXY[1]
                val alphaEnd = 0f
                val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
                valueAnimator.duration = activity.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                valueAnimator.addUpdateListener { animator: ValueAnimator ->
                    val animatedFraction = animator.animatedFraction
                    if (zoomableView != null && zoomableViewFrameLP != null) {
                        updateZoomableView(animatedFraction, scaleYStart, scaleXStart,
                                leftMarginStart!!, topMarginStart!!,
                                scaleXEnd, scaleYEnd, leftMarginEnd, topMarginEnd)
                    }
                    darkView?.alpha = (alphaEnd - alphaStart) * animatedFraction + alphaStart
                }
                valueAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        end()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        end()
                    }

                    fun end() {
                        if (zoomableView != null && zoomableViewFrameLP != null) {
                            updateZoomableView(1f, scaleYStart, scaleXStart,
                                    leftMarginStart!!, topMarginStart!!,
                                    scaleXEnd, scaleYEnd, leftMarginEnd, topMarginEnd)
                        }
                        dismissDialogAndViews()
                        valueAnimator.removeAllListeners()
                        valueAnimator.removeAllUpdateListeners()
                    }
                })
                valueAnimator.start()
                return true
            }
            isZooming = false
        }
        return false
    }

    private fun updateZoomableView(animatedFraction: Float, scaleYStart: Float,
                                   scaleXStart: Float, leftMarginStart: Int,
                                   topMarginStart: Int, scaleXEnd: Float, scaleYEnd: Float,
                                   leftMarginEnd: Int, topMarginEnd: Int) {
        zoomableView!!.scaleX = (scaleXEnd - scaleXStart) * animatedFraction + scaleXStart
        zoomableView!!.scaleY = (scaleYEnd - scaleYStart) * animatedFraction + scaleYStart
        updateZoomableViewMargins(
                (leftMarginEnd - leftMarginStart) * animatedFraction + leftMarginStart,
                (topMarginEnd - topMarginStart) * animatedFraction + topMarginStart)
    }

    private fun updateZoomableViewMargins(left: Float, top: Float) {
        zoomableViewFrameLP!!.leftMargin = left.toInt()
        zoomableViewFrameLP!!.topMargin = top.toInt()
        zoomableView!!.layoutParams = zoomableViewFrameLP
    }

    /**
     * Dismiss dialog and set views to null for garbage collection
     */
    private fun dismissDialogAndViews() {
        sendZoomEventToListeners(zoomableView, false)
        if (zoomableView != null) {
            zoomableView!!.visibility = View.VISIBLE
            val parent = zoomableView!!.parent as ViewGroup
            parent.removeView(zoomableView)
            parentOfZoomableView.addView(zoomableView, viewIndex, zoomableViewLP)
        }
        decorView = null
        darkView = null
        resetOriginalViewAfterZoom()
        isAnimatingDismiss = false
    }

    fun addOnZoomListener(onZoomListener: OnZoomListener) {
        zoomListeners.add(onZoomListener)
    }

    fun removeOnZoomListener(onZoomListener: OnZoomListener) {
        zoomListeners.remove(onZoomListener)
    }

    private fun sendZoomEventToListeners(zoomableView: View?, zoom: Boolean) {
        for (onZoomListener in zoomListeners) {
            if (zoom) {
                onZoomListener.onImageZoomStarted(zoomableView)
            } else {
                onZoomListener.onImageZoomEnded(zoomableView)
            }
        }
    }

    private fun resetOriginalViewAfterZoom() {
        if (zoomableView != null) {
            zoomableView!!.invalidate()
            zoomableView = null
        }
    }

    /**
     * Get distance between two points
     *
     * @param x1 distance x from
     * @param x2 distance x end
     * @param y1 distance y from
     * @param y2 distance y end
     * @return distance
     */
    private fun getDistance(x1: Double, x2: Double, y1: Double, y2: Double): Double {
        return sqrt((x2 - x1).pow(2.0) + (y2 - y1).pow(2.0))
    }

    /**
     * Finds the view that has the R.id.zoomable tag and also contains the x and y coordinations
     * of two pointers
     *
     * @param event MotionEvent that contains two pointers
     * @param view  View to find in
     * @return zoomable View
     */
    private fun findZoomableView(event: MotionEvent, view: View): View? {
        if (view is ViewGroup) {
            val pointerCoords1 = MotionEvent.PointerCoords()
            event.getPointerCoords(0, pointerCoords1)
            val pointerCoords2 = MotionEvent.PointerCoords()
            event.getPointerCoords(1, pointerCoords2)

            val point1 = Point(pointerCoords1.x.toInt(), pointerCoords1.y.toInt())
            val point2 = Point(pointerCoords2.x.toInt(), pointerCoords2.y.toInt())

            view.forEach {
                if (getIntTag(it) and FLAG_UNZOOMABLE == 0) {
                    val location = IntArray(2)
                    it.getLocationOnScreen(location)
                    val visibleRect = Rect(location[0], location[1], location[0] + it.width, location[1] + it.height)
                    if (point1 in visibleRect && point2 in visibleRect) {
                        return if (getIntTag(it) and FLAG_ZOOMABLE != 0) it else findZoomableView(event, it)
                    }
                }
            }
        }
        return null
    }

    companion object {
        private const val FLAG_ZOOMABLE = 1
        private const val FLAG_UNZOOMABLE = 2

        @JvmStatic
        var isZooming = false
            private set

        private fun getIntTag(view: View): Int {
            return if (view.tag == null) 0 else view.tag as Int
        }

        @JvmStatic
        fun setViewZoomable(view: View) {
            view.tag = getIntTag(view) or FLAG_ZOOMABLE
        }

        fun clearViewZoomable(view: View) {
            view.tag = getIntTag(view) and FLAG_ZOOMABLE.inv()
        }
    }
}
