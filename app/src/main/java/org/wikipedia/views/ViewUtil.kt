package org.wikipedia.views

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.graphics.contains
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.ViewActionModeCloseButtonBinding
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.views.imageservice.ImageLoadListener
import org.wikipedia.views.imageservice.ImageService

object ViewUtil {
    fun loadImageWithRoundedCorners(view: ImageView, url: String?) {
        loadImage(view, url, roundedCorners = true)
    }

    fun loadImage(view: ImageView, url: String?, roundedCorners: Boolean = false, force: Boolean = false,
                  @DrawableRes placeholderId: Int? = null, listener: ImageLoadListener? = null) {
        ImageService.loadImage(
            view,
            url,
            roundedCorners,
            force,
            placeholderId,
            listener
        )
    }

    fun getPlaceholderDrawable(context: Context): Drawable {
        return ColorDrawable(getThemedColor(context, R.attr.border_color))
    }

    fun setCloseButtonInActionMode(context: Context, actionMode: ActionMode) {
        val binding = ViewActionModeCloseButtonBinding.inflate(LayoutInflater.from(context))
        actionMode.customView = binding.root
        binding.closeButton.setOnClickListener { actionMode.finish() }
    }

    fun adjustImagePlaceholderHeight(containerWidth: Float, thumbWidth: Float, thumbHeight: Float): Int {
        return (Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat() / thumbWidth * thumbHeight * containerWidth / Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat()).toInt()
    }

    fun findClickableViewAtPoint(parentView: View, point: Point): View? {
        val location = IntArray(2)
        return parentView.allViews.lastOrNull {
            val (x, y) = location.apply { it.getLocationOnScreen(this) }
            val rect = Rect(x, y, x + it.width, y + it.height)
            point in rect && it.isVisible && it.isEnabled && it.isClickable
        }
    }

    fun jumpToPositionWithoutAnimation(recyclerView: RecyclerView, position: Int) {
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.scrollToPosition(position)
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    fun getTitleViewFromToolbar(toolbar: ViewGroup): TextView? {
        toolbar.children.forEach {
            if (it is TextView) {
                return it
            }
        }
        return null
    }
}
