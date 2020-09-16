package org.wikipedia.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

open class WikiCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : MaterialCardView(context, attrs, defStyleAttr) {

    init {
        var hasBorder = true
        var shadowColor = if (WikipediaApp.getInstance().currentTheme == Theme.DARK)
            ContextCompat.getColor(context, R.color.base0) else ContextCompat.getColor(context, R.color.base70)
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.WikiCardView)
            hasBorder = array.getBoolean(R.styleable.WikiCardView_hasBorder, true)
            shadowColor = array.getColor(R.styleable.WikiCardView_shadowColor, ContextCompat.getColor(context, R.color.base70))
            array.recycle()
        }

        setup(hasBorder, shadowColor)
    }

    private fun setup(hasBorder: Boolean, @ColorInt shadowColor: Int) {
        radius = context.resources.getDimension(R.dimen.wiki_card_radius)
        if (hasBorder) {
            strokeWidth = when (WikipediaApp.getInstance().currentTheme) {
                Theme.DARK -> {
                    DimenUtil.roundedDpToPx(0f)
                }
                Theme.BLACK -> {
                    strokeColor = ContextCompat.getColor(context, R.color.base10)
                    DimenUtil.roundedDpToPx(1f)
                }
                else -> {
                    strokeColor = ContextCompat.getColor(context, R.color.base80)
                    DimenUtil.roundedDpToPx(0.5f)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (WikipediaApp.getInstance().currentTheme) {
                Theme.DARK -> {
                    cardElevation = DimenUtil.dpToPx(8f)
                    outlineAmbientShadowColor = shadowColor
                    outlineSpotShadowColor = shadowColor
                }
                Theme.BLACK -> {
                    cardElevation = 0f
                }
                else -> {
                    cardElevation = DimenUtil.dpToPx(8f)
                    outlineAmbientShadowColor = shadowColor
                    outlineSpotShadowColor = shadowColor
                }
            }
        } else {
            cardElevation = DimenUtil.dpToPx(2f)
        }

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }
}