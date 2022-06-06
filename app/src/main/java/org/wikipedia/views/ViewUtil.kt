package org.wikipedia.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.forEach
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
import java.util.*

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
                .load(if ((Prefs.isImageDownloadEnabled || force) && !TextUtils.isEmpty(url)) Uri.parse(url) else null)
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
        return ColorDrawable(getThemedColor(context, R.attr.material_theme_border_color))
    }

    fun setCloseButtonInActionMode(context: Context, actionMode: ActionMode) {
        val binding = ViewActionModeCloseButtonBinding.inflate(LayoutInflater.from(context))
        actionMode.customView = binding.root
        binding.closeButton.setOnClickListener { actionMode.finish() }
    }

    fun formatLangButton(langButton: TextView, langCode: String,
                         langButtonTextSizeSmaller: Int, langButtonTextSizeLarger: Int) {
        val langCodeStandardLength = 3
        val langButtonTextMaxLength = 7
        if (langCode.length > langCodeStandardLength) {
            langButton.textSize = langButtonTextSizeSmaller.toFloat()
            if (langCode.length > langButtonTextMaxLength) {
                langButton.text = langCode.substring(0, langButtonTextMaxLength).uppercase(Locale.ENGLISH)
            }
            return
        }
        langButton.textSize = langButtonTextSizeLarger.toFloat()
    }

    fun adjustImagePlaceholderHeight(containerWidth: Float, thumbWidth: Float, thumbHeight: Float): Int {
        return (Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat() / thumbWidth * thumbHeight * containerWidth / Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat()).toInt()
    }

    tailrec fun Context.getActivity(): Activity? = this as? Activity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    fun setTouchListenersToViews(currentView: View?, onTouchListener: View.OnTouchListener) {
        if (currentView == null) {
            return
        }
        if (!hasTouchListener(currentView)) {
            currentView.setOnTouchListener(onTouchListener)
        }
        if (currentView is ViewGroup) {
            currentView.forEach {
                setTouchListenersToViews(it, onTouchListener)
            }
        }
    }

    private fun hasTouchListener(v: View): Boolean {
        try {
            getListenerInfo(v)?.let {
                val listenerInfoClass = it.javaClass
                val field = listenerInfoClass.getDeclaredField("mOnTouchListener")
                field.isAccessible = true
                if (field[it] != null) return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getListenerInfo(v: View): Any? {
        return try {
            val field = Class.forName("android.view.View").getDeclaredField("mListenerInfo")
            field.isAccessible = true
            field[v] as Any
        } catch (e: Exception) {
            null
        }
    }
}
