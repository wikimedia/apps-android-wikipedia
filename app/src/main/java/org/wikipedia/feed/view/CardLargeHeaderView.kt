package org.wikipedia.feed.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.palette.graphics.Palette
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewCardHeaderLargeBinding
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
        resetBackgroundColor()
    }

    val sharedElements: Array<Pair<View, String>>
        get() = TransitionUtil.getSharedElements(context, binding.viewCardHeaderLargeImage)

    fun setLanguageCode(langCode: String): CardLargeHeaderView {
        L10nUtil.setConditionalLayoutDirection(this, langCode)
        return this
    }

    fun setImage(uri: Uri?): CardLargeHeaderView {
        binding.viewCardHeaderLargeImage.visibility = if (uri == null) GONE else VISIBLE
        binding.viewCardHeaderLargeImage.loadImage(uri, true, ImageLoadListener())
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
        setGradientDrawableBackground(ContextCompat.getColor(context, R.color.base100),
                ContextCompat.getColor(context, R.color.base20))
    }

    private inner class ImageLoadListener : OnImageLoadListener {
        override fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int) {
            var color1 = palette.getLightVibrantColor(ContextCompat.getColor(context, R.color.base70))
            var color2 = palette.getLightMutedColor(ContextCompat.getColor(context, R.color.base30))
            if (WikipediaApp.getInstance().currentTheme.isDark) {
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

        // card background
        gradientDrawable.alpha = 70
        gradientDrawable.cornerRadius = binding.viewCardHeaderLargeBorderBase.radius
        binding.viewCardHeaderLargeContainer.background = gradientDrawable

        // card border's background, which depends on the margin that is applied to the borderBaseView
        gradientDrawable.alpha = 90
        gradientDrawable.cornerRadius = resources.getDimension(R.dimen.wiki_card_radius)
        binding.viewCardHeaderLargeBorderContainer.background = gradientDrawable
    }
}
