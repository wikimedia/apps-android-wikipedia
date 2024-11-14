package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewSuggestedEditsStatsBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

internal class SuggestedEditsStatsView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewSuggestedEditsStatsBinding.inflate(LayoutInflater.from(context), this)
    var tooltipText: String = ""

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding.description.setOnLongClickListener {
            if (tooltipText.isNotEmpty()) {
                FeedbackUtil.showTooltip(context as Activity, binding.description, tooltipText,
                    aboveOrBelow = false,
                    autoDismiss = true
                )
            }
            true
        }
    }

    fun setTitle(titleText: String) {
        binding.title.text = titleText
    }

    fun setDescription(descriptionText: String) {
        binding.description.text = descriptionText
    }

    fun setImageDrawable(@DrawableRes imageDrawable: Int) {
        binding.image.setImageResource(imageDrawable)
    }

    fun getTitleView(): View {
        return binding.title
    }

    fun setGoodnessState(severity: Int) {
        val iconRes: Int
        val iconTint: Int
        val textRes: Int
        val circleProgress: Double

        when (severity) {
            0 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.progressive_color; textRes = R.string.suggested_edits_quality_perfect_text; circleProgress = 100.0 }
            1 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.progressive_color; textRes = R.string.suggested_edits_quality_excellent_text; circleProgress = 85.0 }
            2 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.progressive_color; textRes = R.string.suggested_edits_quality_very_good_text; circleProgress = 75.0 }
            3 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.progressive_color; textRes = R.string.suggested_edits_quality_good_text; circleProgress = 55.0 }
            4 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.highlight_color; textRes = R.string.suggested_edits_quality_okay_text; circleProgress = 40.0 }
            5 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.highlight_color; textRes = R.string.suggested_edits_quality_sufficient_text; circleProgress = 30.0 }
            else -> { iconRes = R.drawable.ic_exclamation_borderless; iconTint = R.attr.destructive_color; textRes = R.string.suggested_edits_quality_poor_text; circleProgress = 20.0 }
        }

        binding.circularProgressBar.setCurrentProgress(circleProgress)
        binding.circularProgressBar.progressBackgroundColor = ResourceUtil.getThemedColor(context, R.attr.paper_color)
        binding.circularProgressBar.progressColor = ResourceUtil.getThemedColor(context, iconTint)
        binding.circularProgressBar.visibility = VISIBLE

        binding.circularProgressBarOverlay.visibility = VISIBLE

        binding.description.text = context.getString(textRes)

        binding.image.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(binding.image, ResourceUtil.getThemedColorStateList(context, iconTint))

        binding.image.updateLayoutParams {
            width = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.suggested_edits_icon_size))
            height = width
        }
        binding.image.requestLayout()
    }
}
