package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.ViewActionModeCloseButtonBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.WhiteBackgroundTransformation

object ViewUtil {
    private val CENTER_CROP_ROUNDED_CORNERS = MultiTransformation(CenterCrop(), WhiteBackgroundTransformation(), RoundedCorners(roundedDpToPx(2f)))
    val ROUNDED_CORNERS = RoundedCorners(roundedDpToPx(15f))
    val CENTER_CROP_LARGE_ROUNDED_CORNERS = MultiTransformation(CenterCrop(), WhiteBackgroundTransformation(), ROUNDED_CORNERS)

    fun loadImageWithRoundedCorners(view: ImageView, url: String?, largeRoundedSize: Boolean = false) {
        loadImage(view, url, true, largeRoundedSize)
    }

    fun loadImage(view: ImageView, url: String?, roundedCorners: Boolean = false, largeRoundedSize: Boolean = false, force: Boolean = false,
                  listener: RequestListener<Drawable?>? = null) {
        val placeholder = getPlaceholderDrawable(view.context)
        var builder = Glide.with(view)
                .load(if ((Prefs.isImageDownloadEnabled || force) && !url.isNullOrEmpty()) Uri.parse(url) else null)
                .placeholder(placeholder)
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .error(placeholder)
        builder = if (roundedCorners) {
            builder.transform(if (largeRoundedSize) CENTER_CROP_LARGE_ROUNDED_CORNERS else CENTER_CROP_ROUNDED_CORNERS)
        } else {
            builder.transform(WhiteBackgroundTransformation())
        }
        if (listener != null) {
            builder = builder.listener(listener)
        }
        builder.into(view)
    }

    fun getPlaceholderDrawable(context: Context): Drawable {
        return ColorDrawable(getThemedColor(context, R.attr.border_color))
    }

    fun setCloseButtonInActionMode(context: Context, actionMode: ActionMode) {
        val binding = ViewActionModeCloseButtonBinding.inflate(LayoutInflater.from(context))
        actionMode.customView = binding.root
        binding.closeButton.setOnClickListener { actionMode.finish() }
    }

    fun formatLangButton(langButton: TextView, langCode: String,
                         langButtonTextSizeSmaller: Int, langButtonTextSizeLarger: Int) {
        val langCodeStandardLength = 3
        if (langCode.length > langCodeStandardLength) {
            langButton.textSize = langButtonTextSizeSmaller.toFloat()
            return
        }
        langButton.textSize = langButtonTextSizeLarger.toFloat()
    }

    fun adjustImagePlaceholderHeight(containerWidth: Float, thumbWidth: Float, thumbHeight: Float): Int {
        return (Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat() / thumbWidth * thumbHeight * containerWidth / Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat()).toInt()
    }

    tailrec fun Context.getActivity(): Activity? = this as? Activity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    fun findClickableViewAtPoint(parentView: View, x: Int, y: Int): View? {
        val location = IntArray(2)
        parentView.getLocationOnScreen(location)
        val rect = Rect(location[0], location[1], location[0] + parentView.width, location[1] + parentView.height)
        if (rect.contains(x, y) && parentView.isVisible) {
            if (parentView is ViewGroup) {
                for (i in parentView.childCount - 1 downTo 0) {
                    val v = parentView.getChildAt(i)
                    findClickableViewAtPoint(v, x, y)?.let { return it }
                }
            }
            if (parentView.isEnabled && parentView.isClickable) {
                return parentView
            }
        }
        return null
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
