package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

open class WikiCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        MaterialCardView(context, attrs, defStyleAttr) {

    init {
        var hasBorder = true
        var cardRadius = context.resources.getDimension(R.dimen.wiki_card_radius)
        var elevation = DimenUtil.dpToPx(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) 8f else 2f)
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.WikiCardView) {
                hasBorder = getBoolean(R.styleable.WikiCardView_hasBorder, true)
                cardRadius = getDimension(R.styleable.WikiCardView_radius, cardRadius)
                elevation = getDimension(R.styleable.WikiCardView_elevation, elevation)
            }
        }

        setup(cardRadius, elevation, hasBorder)
    }

    private fun setup(cardRadius: Float, elevation: Float, hasBorder: Boolean) {
        radius = cardRadius
        if (hasBorder) {
            setDefaultBorder()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (WikipediaApp.getInstance().currentTheme) {
                Theme.DARK -> {
                    cardElevation = elevation
                    outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.base0)
                    outlineSpotShadowColor = ContextCompat.getColor(context, R.color.base0)
                }
                Theme.BLACK -> {
                    cardElevation = 0f
                }
                else -> {
                    cardElevation = elevation
                    outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.base70)
                    outlineSpotShadowColor = ContextCompat.getColor(context, R.color.base70)
                }
            }
        } else {
            cardElevation = elevation
        }

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        rippleColor = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.material_theme_border_color))
    }

    fun setDefaultBorder() {
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
}
