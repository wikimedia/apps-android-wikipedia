package org.wikipedia.feed.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewCardHeaderLargeBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil
import org.wikipedia.views.FaceAndColorDetectImageView.OnImageLoadListener

class CardLargeHeaderView : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    val binding = ViewCardHeaderLargeBinding.inflate(LayoutInflater.from(context), this)

    init {
        if (!isInEditMode) {
            resetBackgroundColor()
        }
    }

    val sharedElements get() = TransitionUtil.getSharedElements(context, binding.viewCardHeaderLargeImage)

    fun setLanguageCode(langCode: String): CardLargeHeaderView {
        L10nUtil.setConditionalLayoutDirection(this, langCode)
        return this
    }

    fun setImage(uri: Uri?): CardLargeHeaderView {
        binding.viewCardHeaderLargeImage.visibility = if (uri == null) GONE else VISIBLE
        binding.viewCardHeaderLargeImage.loadImage(uri, roundedCorners = true, cropped = true, listener = ImageLoadListener())
        return this
    }

    fun setTitle(title: String?): CardLargeHeaderView {
        binding.viewCardHeaderLargeTitle.text = StringUtil.fromHtml(title)
        return this
    }

    fun setSubtitle(subtitle: CharSequence?): CardLargeHeaderView {
        binding.viewCardHeaderLargeSubtitle.text = subtitle
        return this
    }

    private fun resetBackgroundColor() {
        setGradientDrawableBackground(ContextCompat.getColor(context, R.color.white),
                ContextCompat.getColor(context, R.color.gray600))
    }

    private inner class ImageLoadListener : OnImageLoadListener {
        override fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int) {
            var color1 = palette.getLightVibrantColor(ContextCompat.getColor(context, R.color.gray300))
            var color2 = palette.getLightMutedColor(ContextCompat.getColor(context, R.color.gray500))
            if (WikipediaApp.instance.currentTheme.isDark) {
                color1 = ResourceUtil.darkenColor(color1)
                color2 = ResourceUtil.darkenColor(color2)
            } else {
                color1 = ResourceUtil.lightenColor(color1)
                color2 = ResourceUtil.lightenColor(color2)
            }
            setGradientDrawableBackground(color1, color2)
        }

        override fun onImageFailed() {
            resetBackgroundColor()
        }
    }

    private fun setGradientDrawableBackground(@ColorInt leftColor: Int, @ColorInt rightColor: Int) {
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(leftColor, rightColor))
        gradientDrawable.alpha = 70
        gradientDrawable.cornerRadius = DimenUtil.dpToPx(12f)
        gradientDrawable.setStroke(DimenUtil.roundedDpToPx(0.5f), ResourceUtil.getThemedColor(context, R.attr.secondary_color))
        background = gradientDrawable
    }
}
