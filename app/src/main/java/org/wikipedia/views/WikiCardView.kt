package org.wikipedia.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.google.android.material.card.MaterialCardView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

open class WikiCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        MaterialCardView(context, attrs, defStyleAttr) {

    init {
        if (!isInEditMode) {
            val (hasBorder, cardRadius, elevation) = context
                .obtainStyledAttributes(attrs, R.styleable.WikiCardView)
                .use {
                    Triple(
                        it.getBoolean(R.styleable.WikiCardView_hasBorder, true),
                        it.getDimension(R.styleable.WikiCardView_radius,
                            context.resources.getDimension(R.dimen.wiki_card_radius)),
                        it.getDimension(R.styleable.WikiCardView_elevation,
                            DimenUtil.dpToPx(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) 8f else 2f))
                    )
                }

            setup(cardRadius, elevation, hasBorder)
        }
    }

    private fun setup(cardRadius: Float, elevation: Float, hasBorder: Boolean) {
        radius = cardRadius
        if (hasBorder) {
            setDefaultBorder()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (WikipediaApp.instance.currentTheme.isDark) {
                cardElevation = 0f
            } else {
                cardElevation = elevation
                outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.gray300)
                outlineSpotShadowColor = ContextCompat.getColor(context, R.color.gray300)
            }
        } else {
            cardElevation = elevation
        }

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        rippleColor = ResourceUtil.getThemedColorStateList(context, R.attr.overlay_color)
    }

    fun setDefaultBorder() {
        strokeWidth = DimenUtil.roundedDpToPx(0.5f)
        strokeColor = ResourceUtil.getThemedColor(context, R.attr.border_color)
    }
}
