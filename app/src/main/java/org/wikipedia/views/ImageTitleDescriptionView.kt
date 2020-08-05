package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.skydoves.balloon.showAlignBottom
import kotlinx.android.synthetic.main.view_image_title_description.view.*
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil

internal class ImageTitleDescriptionView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var tooltipText: String = ""

    init {
        View.inflate(context, R.layout.view_image_title_description, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setOnLongClickListener {
            if (tooltipText.isNotEmpty()) {
                description.showAlignBottom(FeedbackUtil.showTooltip(context, tooltipText), 0, 16)
            }
            true
        }
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

    fun setDescription(descriptionText: String) {
        description.text = descriptionText
    }

    fun setImageDrawable(@DrawableRes imageDrawable: Int) {
        image.setImageDrawable(AppCompatResources.getDrawable(context, imageDrawable))
    }

    fun setGoodnessState(severity: Int) {
        val iconRes: Int
        val iconTint: Int
        val backgroundTint: Int
        val textRes: Int
        val circleProgress: Double

        when(severity) {
            0 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_perfect_text; circleProgress = 100.0 }
            1 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_excellent_text; circleProgress = 85.0 }
            2 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_very_good_text; circleProgress = 75.0 }
            3 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_good_text; circleProgress = 55.0 }
            4 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; textRes = R.string.suggested_edits_quality_okay_text; circleProgress = 40.0 }
            5 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; textRes = R.string.suggested_edits_quality_sufficient_text; circleProgress = 30.0 }
            else -> { iconRes = R.drawable.ic_exclamation_borderless; iconTint = R.color.red50; backgroundTint = R.color.red90; textRes = R.string.suggested_edits_quality_poor_text; circleProgress = 20.0 }
        }

        circularProgressBar.setCurrentProgress(circleProgress)
        circularProgressBar.progressBackgroundColor = ContextCompat.getColor(context, backgroundTint)
        circularProgressBar.progressColor = ContextCompat.getColor(context, iconTint)
        circularProgressBar.visibility = View.VISIBLE

        ImageViewCompat.setImageTintList(circularProgressBarOverlay, ColorStateList.valueOf(ContextCompat.getColor(context, backgroundTint)))
        circularProgressBarOverlay.visibility = View.VISIBLE

        title.text = context.getString(textRes)

        image.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(ContextCompat.getColor(context, iconTint)))

        val params = image.layoutParams
        params.width = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.suggested_edits_icon_size) * 3 / 4)
        params.height = params.width
        image.layoutParams = params
        image.requestLayout()
    }
}
