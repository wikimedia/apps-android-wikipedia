package org.wikipedia.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
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
        radius = DimenUtil.dpToPx(12f)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (WikipediaApp.getInstance().currentTheme) {
                Theme.DARK -> {
                    cardElevation = DimenUtil.dpToPx(8f)
                    outlineAmbientShadowColor = ContextCompat.getColor(getContext(), R.color.base0)
                    outlineSpotShadowColor = ContextCompat.getColor(getContext(), R.color.base0)
                }
                Theme.BLACK -> {
                    cardElevation = 0f
                }
                else -> {
                    cardElevation = DimenUtil.dpToPx(8f)
                    outlineAmbientShadowColor = ContextCompat.getColor(getContext(), R.color.base70)
                    outlineSpotShadowColor = ContextCompat.getColor(getContext(), R.color.base70)
                }
            }
        } else {
            cardElevation = DimenUtil.dpToPx(2f)
        }

        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }
}